'use strict';
// Validação offline: não importa Newman, não conecta à API e não acessa PostgreSQL.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname,'../..');
const modules = path.resolve(root,process.env.NEWMAN_MODULE_DIR || 'target/aula16-postman-tools/node_modules');
const Ajv = require(path.join(modules,'ajv'));
const schema = JSON.parse(fs.readFileSync(path.join(root,'target/aula16-postman-collection-schema.json'),'utf8'));
const ajv = new Ajv({schemaId:'auto',allErrors:true,meta:false});
ajv.addMetaSchema(require(path.join(modules,'ajv/lib/refs/json-schema-draft-04.json')));
const config=JSON.parse(fs.readFileSync(path.join(__dirname,'NEWMAN.runner.json'),'utf8'));
require('./EXECUTAR-NEWMAN.cjs').validarConfig(config); // require não executa main.
const c=JSON.parse(fs.readFileSync(path.join(__dirname,config.collection),'utf8'));
assert(ajv.validate(schema,c),'Coleção incompatível com schema oficial v2.1: '+JSON.stringify(ajv.errors));
assert.deepEqual(c.item.map(f=>f.name),config.folders);
assert.equal(c.auth.type,'noauth');
const declared=new Set(c.variable.map(v=>v.key));
for(const v of c.variable) assert.equal(v.value,v.key==='baseUrl'?config.baseUrl:'');
const requests=c.item.flatMap(f=>f.item);
assert.equal(requests.length,40);
let scripts=0,assertions=0;
const state=new Map([['adminLogin','admin.ficticio'],['adminPassword','senha-apenas-em-memoria']]);
let guid=0;
function execute(event, payload, status, requestName) {
    const source=event.script.exec.join('\n');
    new vm.Script(source); scripts++;
    assert(!/console\.|pm\.sendRequest/.test(source),'Script não deve registrar credenciais ou fazer requisições ocultas');
    const pm={info:{requestName},collectionVariables:{set:(k,v)=>state.set(k,v),get:k=>state.get(k),unset:k=>state.delete(k)},
        variables:{get:k=>state.get(k),replaceIn:()=>`${++guid}`.padStart(32,'0')},
        response:{code:status,json:()=>payload,text:()=>status===204||(!payload)?'':JSON.stringify(payload),
            headers:{get:k=>k==='WWW-Authenticate'?'Bearer':undefined}},
        test:(_name,fn)=>{fn();assertions++;},expect:value=>({to:{eql:other=>assert.deepEqual(value,other)}})};
    vm.runInNewContext(source,{pm},{timeout:1000});
}
execute(c.event[0],{},200,'01 Login administrativo');
let id=100;
for(const r of requests) {
    assert(r.request.url.raw.startsWith('{{baseUrl}}/api/'));
    if(r.request.auth.type==='bearer')assert(r.request.auth.bearer[0].value==='token-sintetico-invalido'||/^\{\{\w+\}\}$/.test(r.request.auth.bearer[0].value));
    else assert.equal(r.request.auth.type,'noauth');
    const source=r.event.find(e=>e.listen==='test').script.exec.join('\n');
    const status=Number(source.match(/const esperado = (\d+);/)[1]);
    for(const event of r.event.filter(e=>e.listen==='prerequest'))execute(event,{},status,r.name);
    const raw=r.request.body?.raw;
    if(raw!==undefined && r.name!=='32 JSON malformado') {
        const resolved=raw.replace(/\{\{(\w+)\}\}/g,(_,k)=>{
            assert(declared.has(k),'Variável de corpo não declarada: '+k);
            assert(state.has(k),'Variável usada antes da captura: '+k);
            return String(state.get(k));
        });
        JSON.parse(resolved);
    }
    if(r.request.auth.type==='bearer' && r.request.auth.bearer[0].value.startsWith('{{')) {
        const key=r.request.auth.bearer[0].value.slice(2,-2);
        assert(state.get(key),'Bearer usado antes da captura: '+key);
    }
    const token=(++id).toString().padStart(43,'x');
    const payload=status===201?{id}:status===200?{accessToken:'access-sintetico-'+id,refreshToken:token,tokenType:'Bearer',expiresIn:1800,
        expiresAt:'2026-11-17T19:00:00Z',refreshTokenExpiresAt:'2026-11-18T02:30:00Z'}:
        (r.name.startsWith('22 ')||r.name.startsWith('23 '))?null:{status:401,message:'Refresh token inválido ou expirado',path:'/api/autenticacao/refresh',fields:{}};
    const antes=state.get('refreshToken');
    for(const event of r.event.filter(e=>e.listen==='test'))execute(event,payload,status,r.name);
    if(r.name==='08 R1 para R2')assert.equal(state.get('replayR1'),antes);
    if(r.name==='10 Replay de R1')assert.notEqual(state.get('replayR1'),state.get('refreshToken'));
}
for(const key of ['accessToken','refreshToken','adminAccessToken','adminRefreshToken','fixturePassword'])assert(!state.has(key));
console.log(JSON.stringify({schema:'Postman v2.1 oficial',runnerJson:'válido',folders:c.item.length,requests:requests.length,
    compiledAndSimulatedScripts:scripts,offlineAssertions:assertions,serverRequests:0}));
