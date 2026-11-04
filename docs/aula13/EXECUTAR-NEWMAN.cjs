// Local only. No credentials, tokens or response bodies are written to reports.
const fs = require('node:fs');
const path = require('node:path');
const cp = require('node:child_process');
const root = path.resolve(__dirname, '../..');
process.chdir(root);
const modules = path.resolve(process.env.NEWMAN_MODULE_DIR || 'target/aula13-postman-tools/node_modules');
const newman = require(path.join(modules, 'newman'));
const collection = JSON.parse(fs.readFileSync(path.join(__dirname, 'Instrumento 2026 - Aula 13.postman_collection.json'), 'utf8'));
const baseUrl = process.env.NEWMAN_BASE_URL || 'http://localhost:18013';
const endpoint = new URL(baseUrl);
if (!['localhost', '127.0.0.1', '[::1]'].includes(endpoint.hostname)) throw Error('Only localhost allowed');
collection.variable.find(v => v.key === 'baseUrl').value = baseUrl;
const env = {...process.env};
if (fs.existsSync('.env')) for (const line of fs.readFileSync('.env', 'utf8').split(/\r?\n/)) {
    const m = line.match(/^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*?)\s*$/);
    if (m && !m[2].startsWith('#') && !(m[1] in env)) env[m[1]] = m[2].replace(/^(['"])(.*)\1$/, '$2');
}
const db = new URL((env.DB_TEST_URL || 'jdbc:postgresql://localhost:5432/instrumento2026_test').replace(/^jdbc:/, ''));
if (db.pathname !== '/instrumento2026_test' || !['localhost', '127.0.0.1', '[::1]'].includes(db.hostname)) throw Error('Only local instrumento2026_test allowed');
const pgEnv = {...process.env, PGPASSWORD: env.DB_TEST_PASSWORD};
const psql = process.env.PSQL_PATH || 'C:/Program Files/PostgreSQL/17/bin/psql.exe';
function sql(args) {
    const r = cp.spawnSync(psql, ['-X', '-v', 'ON_ERROR_STOP=1', '-h', db.hostname, '-p', db.port || '5432',
        '-U', env.DB_TEST_USERNAME || 'instrumento_app', '-d', 'instrumento2026_test', ...args], {env: pgEnv, encoding: 'utf8'});
    if (r.status !== 0) throw Error('SQL preparation/cleanup failed; inspect local database.');
    return r.stdout.trim();
}
const created = Object.fromEntries(['pessoas', 'clientes', 'colaboradores', 'categorias', 'fornecedores', 'instrumentos', 'vendas', 'contas-acesso'].map(k => [k, new Set()]));
let current = collection;
function run(folder) {
    return new Promise((resolve, reject) => {
        let values;
        const runner = newman.run({collection: current, folder, reporters: [], timeoutRequest: 15000, timeoutScript: 10000}, (err, summary) => {
            if (values) Object.entries(values).forEach(([key, value]) => summary?.collection.variables.upsert({key, value}));
            if (summary) current = summary.collection.toJSON();
            if (err) return reject(Error('Newman execution error (credentials omitted)'));
            resolve({requests: summary.run.stats.requests, assertions: summary.run.stats.assertions,
                failures: summary.run.failures.map(f => ({source: f.source.name, kind: f.error.name})),
                executions: summary.run.executions.length});
        });
        runner.on('script', (err, event) => {if (event.execution?.collectionVariables) values = event.execution.collectionVariables.toObject();});
        runner.on('request', (err, event) => {
            if (err || event.request.method !== 'POST' || event.response?.code !== 201) return;
            const resource = new URL(event.request.url.toString()).pathname.match(/^\/api\/([^/]+)$/)?.[1];
            if (!created[resource]) return;
            const id = event.response.json().id;
            if (Number.isSafeInteger(id) && id > 0) created[resource].add(id);
        });
    });
}
function cleanup() {
    const tables = [['vendas', 'venda'], ['contas-acesso', 'conta_acesso'], ['clientes', 'cliente'],
        ['colaboradores', 'colaborador'], ['instrumentos', 'instrumento'], ['fornecedores', 'fornecedor'],
        ['categorias', 'categoria_instrumento'], ['pessoas', 'pessoa']];
    const statements = [];
    const sales = [...created.vendas];
    if (sales.length) statements.push(`DELETE FROM item_venda WHERE venda_id IN (${sales.join(',')});`);
    for (const [key, table] of tables) if (created[key].size) statements.push(`DELETE FROM ${table} WHERE id IN (${[...created[key]].join(',')});`);
    if (statements.length) sql(['-c', 'BEGIN; ' + statements.join(' ') + ' COMMIT;']);
    const remaining = tables.filter(([key]) => created[key].size).map(([key, table]) => `(SELECT count(*) FROM ${table} WHERE id IN (${[...created[key]].join(',')}))`);
    const count = remaining.length ? Number(sql(['-t', '-A', '-c', 'SELECT ' + remaining.join('+')])) : 0;
    if (count !== 0) throw Error('Test fixtures remain after cleanup');
    return {created: Object.fromEntries(Object.entries(created).map(([key, ids]) => [key, ids.size])), remaining: count};
}
(async () => {
    const report = {startedAt: new Date().toISOString(), newman: require(path.join(modules, 'newman/package.json')).version,
        postgresql: sql(['-t', '-A', '-c', 'SHOW server_version']), profile: 'test', phases: []};
    try {
        report.phases.push(await run(current.item.slice(0, 2).map(f => f.name)));
        if (report.phases[0].failures.length) throw Error('Preparation assertions failed');
        const login = current.variable.find(v => v.key === 'blockedLogin')?.value;
        if (!/^a13\.bloqueada\.[a-f0-9]{32}$/.test(login)) throw Error('Invalid blocked fixture identity');
        sql(['-v', 'login_bloqueado=' + login, '-f', path.join(__dirname, 'PREPARAR-CONTA-BLOQUEADA.sql')]);
        current.variable.find(v => v.key === 'blockedFixtureReady').value = 'true';
        report.phases.push(await run(current.item.slice(2).map(f => f.name)));
        if (report.phases[1].failures.length) process.exitCode = 1;
    } catch (e) {report.error = e.message; process.exitCode = 1;}
    finally {
        try {report.cleanup = cleanup();} catch (e) {report.cleanupError = e.message; process.exitCode = 1;}
        report.finishedAt = new Date().toISOString();
        fs.mkdirSync('target', {recursive: true});
        fs.writeFileSync('target/aula13-newman-resultados.json', JSON.stringify(report, null, 2));
        console.log(JSON.stringify(report, null, 2));
    }
})();
