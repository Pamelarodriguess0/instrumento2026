'use strict';
// Preparado para execução futura autorizada. Não inicia API nem acessa PostgreSQL.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../..');
function validarConfig(c) {
    assert.equal(c.collection, 'Instrumento 2026 - Aula 16.postman_collection.json');
    assert.equal(c.baseUrl, 'http://127.0.0.1:18016');
    assert.equal(c.requiredDatabase, 'instrumento2026_test');
    assert.equal(c.requiredHost, '127.0.0.1');
    assert.equal(c.requiredPort, 5432);
    assert.equal(c.requiredSchema, 'public');
    assert.equal(c.iterations, 1);
    assert.equal(c.summary, 'target/aula16-newman-resultados.json');
    assert.equal(c.folders.length, 6);
}
async function main() {
    const c = JSON.parse(fs.readFileSync(path.join(__dirname, 'NEWMAN.runner.json'), 'utf8'));
    validarConfig(c);
    if (process.env.AULA16_ALLOW_TEST_WRITES !== 'SIM' || process.env.AULA16_PROFILE_TEST_CONFIRMED !== 'SIM')
        throw Error('Execução desabilitada: consulte postman.md e confirme o destino da API antes de autorizar escritas.');
    if (process.env.DB_TEST_URL !== 'jdbc:postgresql://127.0.0.1:5432/instrumento2026_test')
        throw Error('Declaração do destino de teste inválida.');
    const {AULA16_ADMIN_LOGIN: adminLogin, AULA16_ADMIN_PASSWORD: adminPassword} = process.env;
    if (!adminLogin || !adminPassword) throw Error('Credenciais administrativas privadas ausentes.');
    const newman = require(path.join(path.resolve(root, process.env.NEWMAN_MODULE_DIR || 'target/aula16-postman-tools/node_modules'), 'newman'));
    const collection = JSON.parse(fs.readFileSync(path.join(__dirname, c.collection), 'utf8'));
    const criados = {pessoas: [], contas: []};
    let limpezaLogica = 0;
    const summary = await new Promise((resolve, reject) => {
        const run = newman.run({collection, folder: c.folders, iterationCount: c.iterations, reporters: [],
            timeoutRequest: 15000, timeoutScript: 10000,
            environment: {values: [{key:'baseUrl', value:c.baseUrl, enabled:true},
                {key:'adminLogin', value:adminLogin, enabled:true}, {key:'adminPassword', value:adminPassword, enabled:true}]}},
            (err, result) => err ? reject(Error('Falha de execução Newman; detalhes brutos omitidos para proteger credenciais.')) : resolve(result));
        run.on('request', (err, e) => {
            if (err || !e.response) return;
            if (e.response.code === 201 && /^0[23] Criar (pessoa|conta) (op|other)$/.test(e.item.name)) {
                try {
                    const id=e.response.json().id;
                    if (Number.isSafeInteger(id) && id > 0) criados[e.item.name.includes('pessoa')?'pessoas':'contas'].push(id);
                } catch (_) { /* Não registrar corpo potencialmente sensível. */ }
            }
            if (e.response.code === 200 && /^3[45] (Bloquear conta|Inativar pessoa) fixture /.test(e.item.name)) limpezaLogica++;
        });
    });
    const report = {requests:summary.run.stats.requests, assertions:summary.run.stats.assertions,
        failures:summary.run.failures.length, fixturesCreated:criados, logicalCleanupResponses:limpezaLogica,
        physicalCleanupRequired:true};
    fs.mkdirSync(path.join(root,'target'),{recursive:true});
    fs.writeFileSync(path.join(root,c.summary),JSON.stringify(report,null,2));
    console.log(JSON.stringify(report)); // Somente contadores e IDs das fixtures, sem erros brutos.
    if (report.failures || report.requests.failed || limpezaLogica !== 4) process.exitCode=1;
}
module.exports = {validarConfig};
if (require.main === module) main().catch(() => {
    console.error('Runner não concluído. Consulte postman.md; se houve escritas, revise as fixtures antes de repetir.');
    process.exitCode=1;
});
