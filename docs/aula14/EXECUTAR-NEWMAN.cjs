// Executes only after a separate authorization to write fixtures to the disposable test database.
// No credentials, tokens, headers, bodies or raw Newman failures are logged/exported.
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const root = path.resolve(__dirname, '../..');
process.chdir(root);
async function main() {
    if (process.env.AULA14_ALLOW_TEST_WRITES !== 'SIM') throw Error('Execução real desabilitada. Consulte POSTMAN.md.');
    const {AULA14_ADMIN_LOGIN: adminLogin, AULA14_ADMIN_PASSWORD: adminPassword,
        DB_TEST_USERNAME: dbUser, DB_TEST_PASSWORD: dbPassword} = process.env;
    if (!adminLogin || !adminPassword || !dbUser || !dbPassword) throw Error('Credenciais locais obrigatórias ausentes.');
    const baseUrl = process.env.NEWMAN_BASE_URL || 'http://localhost:18014';
    const endpoint = new URL(baseUrl);
    const local = host => ['localhost', '127.0.0.1', '[::1]'].includes(host);
    if (!local(endpoint.hostname) || endpoint.port !== '18014' || endpoint.pathname !== '/') throw Error('Use a API local de teste na porta 18014.');
    const db = new URL((process.env.DB_TEST_URL || 'jdbc:postgresql://localhost:5432/instrumento2026_test').replace(/^jdbc:/, ''));
    if (!local(db.hostname) || db.pathname !== '/instrumento2026_test') throw Error('Somente instrumento2026_test local é permitido.');
    const modules = path.resolve(process.env.NEWMAN_MODULE_DIR || 'target/aula12-postman-tools/node_modules');
    const newman = require(path.join(modules, 'newman'));
    const collection = JSON.parse(fs.readFileSync(path.join(__dirname, 'Instrumento 2026 - Aula 14.postman_collection.json'), 'utf8'));
    // Never run the sole-administrator experiment against the shared application/database.
    collection.item = collection.item.filter(f => !f.name.startsWith('11 —'));
    const created = Object.fromEntries(['pessoas', 'clientes', 'colaboradores', 'categorias', 'fornecedores', 'instrumentos', 'vendas', 'contas-acesso'].map(k => [k, new Set()]));
    const psql = process.env.PSQL_PATH || 'C:/Program Files/PostgreSQL/17/bin/psql.exe';
    function sql(statement) {
        const r = cp.spawnSync(psql, ['-X', '-v', 'ON_ERROR_STOP=1', '-t', '-A', '-h', db.hostname,
            '-p', db.port || '5432', '-U', dbUser, '-d', 'instrumento2026_test', '-c', statement],
            {env: {...process.env, PGPASSWORD: dbPassword}, encoding: 'utf8'});
        if (r.status !== 0) throw Error('Falha na verificação/limpeza SQL. Inspecione localmente sem publicar credenciais.');
        return r.stdout.trim();
    }
    function cleanup() {
        const tables = [['vendas', 'venda'], ['contas-acesso', 'conta_acesso'], ['clientes', 'cliente'],
            ['colaboradores', 'colaborador'], ['instrumentos', 'instrumento'], ['fornecedores', 'fornecedor'],
            ['categorias', 'categoria_instrumento'], ['pessoas', 'pessoa']];
        const statements = [];
        if (created.vendas.size) statements.push(`DELETE FROM item_venda WHERE venda_id IN (${[...created.vendas].join(',')});`);
        for (const [key, table] of tables) if (created[key].size) statements.push(`DELETE FROM ${table} WHERE id IN (${[...created[key]].join(',')});`);
        if (statements.length) sql('BEGIN; ' + statements.join(' ') + ' COMMIT;');
        const counts = tables.filter(([key]) => created[key].size).map(([key, table]) => `(SELECT count(*) FROM ${table} WHERE id IN (${[...created[key]].join(',')}))`);
        const remaining = counts.length ? Number(sql('SELECT ' + counts.join('+'))) : 0;
        if (remaining !== 0) throw Error('Fixtures remanescentes após limpeza; inspeção local necessária.');
        return {created: Object.fromEntries(Object.entries(created).map(([key, ids]) => [key, ids.size])), remaining};
    }
    const report = {startedAt: new Date().toISOString(), executed: false};
    try {
        report.postgresql = sql('SHOW server_version');
        const summary = await new Promise((resolve, reject) => {
            const runner = newman.run({collection, reporters: [], timeoutRequest: 15000, timeoutScript: 10000,
                environment: {name: 'Somente memória', values: [
                    {key:'adminLogin', value:adminLogin, enabled:true},
                    {key:'adminPassword', value:adminPassword, enabled:true},
                    {key:'baseUrl', value:baseUrl, enabled:true}
                ]}}, (err, result) => err ? reject(Error('Erro Newman; detalhes omitidos para proteger credenciais.')) : resolve(result));
            runner.on('request', (err, e) => {
                if (err || e.request.method !== 'POST' || e.response?.code !== 201) return;
                const resource = new URL(e.request.url.toString()).pathname.match(/^\/api\/([^/]+)$/)?.[1];
                if (!created[resource]) return;
                const id = e.response.json().id;
                if (Number.isSafeInteger(id) && id > 0) created[resource].add(id);
            });
        });
        report.executed = true;
        report.requests = summary.run.stats.requests;
        report.assertions = summary.run.stats.assertions;
        report.failures = summary.run.failures.length;
        report.failureTypes = summary.run.failures.map(f => f.error.name);
        if (report.failures) process.exitCode = 1;
    } catch (e) { report.error = e.message; process.exitCode = 1; }
    finally {
        try { report.cleanup = cleanup(); } catch (e) { report.cleanupError = e.message; process.exitCode = 1; }
        report.finishedAt = new Date().toISOString();
        fs.mkdirSync('target', {recursive:true});
        fs.writeFileSync('target/aula14-newman-resultados.json', JSON.stringify(report, null, 2));
        console.log(JSON.stringify(report, null, 2));
    }
}
if (require.main === module) main().catch(() => { console.error('Execução não iniciada: confira as condições locais em POSTMAN.md.'); process.exitCode = 1; });
