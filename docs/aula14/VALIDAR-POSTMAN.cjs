// Offline only: parses the collection, compiles scripts and exercises selected assertions with fixtures.
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../..');
const modules = path.resolve(process.env.NEWMAN_MODULE_DIR || path.join(root, 'target/aula12-postman-tools/node_modules'));
const Ajv = require(path.join(modules, 'ajv'));
// Only the expectation vocabulary used by the selected new assertions is exercised here.
// This is not a replacement for executing the full Postman sandbox with Newman.
const normalize = value => Object.prototype.toString.call(value)==='[object Set]'
    ? [...value].sort() : JSON.parse(JSON.stringify(value));
const expect = value => ({to:{eql:other=>assert.deepEqual(normalize(value),normalize(other)),
    include:part=>assert(value.includes(part))}});
const schemaPath = process.env.POSTMAN_SCHEMA_PATH || path.join(root, 'target/postman-collection-schema.json');
const schema = JSON.parse(fs.readFileSync(schemaPath, 'utf8').replace(/^\uFEFF/, ''));
const ajv = new Ajv({schemaId:'id', allErrors:true, meta:false});
ajv.addMetaSchema(require(path.join(modules, 'ajv/lib/refs/json-schema-draft-04.json')));
const c = JSON.parse(fs.readFileSync(path.join(__dirname, 'Instrumento 2026 - Aula 14.postman_collection.json'), 'utf8'));
assert(ajv.validate(schema, c), JSON.stringify(ajv.errors));
const historical = JSON.parse(fs.readFileSync(path.join(root, 'docs/aula13/Instrumento 2026 - Aula 13.postman_collection.json'), 'utf8'));
const requests = [], old = [];
let scripts=0, assertions=0;
function walk(items, list) {
    for(const x of items) {
        for(const e of x.event||[]) { new vm.Script(e.script.exec.join('\n')); scripts++; }
        if(x.item)walk(x.item,list);else list.push(x);
    }
}
walk(c.item,requests);
const compiled = scripts;
walk(historical.item,old);
for(const e of c.event||[])new vm.Script(e.script.exec.join('\n'));
const tests = x => x.event?.filter(e=>e.listen==='test');
for(const x of old) {
    const next=requests.find(n=>n.name===x.name);
    assert(next, 'Cenário histórico ausente: '+x.name);
    assert.deepEqual(tests(next),tests(x),'Assertions históricas alteradas: '+x.name);
    assert.equal(next.request.method,x.request.method);
    assert.deepEqual(next.request.url,x.request.url);
    assert.deepEqual(next.request.body,x.request.body);
}
assert.equal(c.auth.bearer[0].value,'{{accessToken}}');
for(const v of c.variable)if(/Password$|Token$/.test(v.key)&&v.key!=='invalidToken')assert.equal(v.value,'');
for(const x of requests) {
    if(x.request.method==='DELETE')assert.equal(x.request.auth.bearer[0].value,
        x.name.startsWith('OPERADOR proibido')?'{{accessToken}}':'{{adminAccessToken}}');
}
const values=new Map([['adminLogin','admin.ficticio'],['adminPassword','somente-em-memoria-2026']]);
const store={get:k=>values.get(k),set:(k,v)=>values.set(k,v)};
let skipped=0;
function sandbox(x, code, body, listen='test') {
    const pm={variables:{get:k=>values.get(k)},collectionVariables:store,
        execution:{skipRequest:()=>skipped++,setNextRequest:()=>{}},
        response:{code,json:()=>body,to:{have:{status:expected=>assert.equal(code,expected)}}},
        test:(name, fn)=>{fn();assertions++;},expect};
    const context={pm,Buffer,Date,Error,Set};
    for(const e of x.event||[])if(e.listen===listen)vm.runInNewContext(e.script.exec.join('\n'),context,{timeout:1000});
}
const folder=c.item.find(f=>f.name.startsWith('10 —'));
for(const x of folder.item) {
    if(x.name==='Novo login da conta promovida')continue; // existing JWT assertions are syntax checked and preserved.
    const status=Number(x.event.find(e=>e.listen==='test').script.exec[0].match(/HTTP (\d+)/)[1]);
    const body={status:'ATIVA',perfis:x.name.startsWith('Restaurar')?['OPERADOR']:['OPERADOR','ADMINISTRADOR']};
    if(status===400){body.status=400;body.message=x.name.includes('bloqueia')?'Não é permitido bloquear a própria conta':'Não é permitido remover o próprio perfil de administrador';}
    sandbox(x,status,body);
    assert.throws(()=>sandbox(x,status===403?200:403,body),'Assertion deve rejeitar HTTP incorreto');
}
const isolated=c.item.find(f=>f.name.startsWith('11 —'));
for(const x of isolated.item){sandbox(x,0,{},'prerequest');sandbox(x,400,{message:'O sistema deve manter ao menos um administrador ativo'});}
assert.equal(skipped,2);
console.log(JSON.stringify({offline:true,folders:c.item.length,requests:requests.length,
    historicalRequestsPreserved:old.length,scriptsCompiled:compiled+c.event.length,
    assertionsExecutedWithFixtures:assertions,lastAdminRequestsSkippedByDefault:skipped,
    httpCalls:0,databaseConnections:0},null,2));
