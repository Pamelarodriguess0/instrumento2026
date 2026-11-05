# Aula 14 — Postman e Newman: autorização por perfil

Material: [seção 10 da Aula 14](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/14aula/14aula.md#10-laboratório-postman).
Coleção: [Instrumento 2026 - Aula 14.postman_collection.json](Instrumento%202026%20-%20Aula%2014.postman_collection.json).
O registro inicial descreve o Checkpoint 6. A execução real posteriormente autorizada e concluída
no 7I.4 está registrada ao final; as negativas históricas não descrevem mais o estado atual.

## Estado histórico da validação no Checkpoint 6

A coleção **ainda não havia sido executada contra a aplicação ou o PostgreSQL no Checkpoint 6**.
Naquele momento, a execução real e o bootstrap dependiam de autorização posterior, concedida nas etapas seguintes.
As coleções e os runners históricos permanecem intactos.

Foram validados offline: schema oficial Postman Collection v2.1, 155 requisições em 11 pastas,
sintaxe de 163 scripts, preservação das 126 requisições históricas e de suas assertions de resposta.
O validador exercitou 21 assertions selecionadas com respostas fictícias e verificou que os dois cenários
de último administrador são pulados por padrão. Nenhum desses números é resultado Newman/HTTP.

Em 04/11/2026, às 13:55:40 -03:00, a regressão isolada abaixo concluiu com **107 testes aprovados**,
zero falhas, erros ou ignorados e **BUILD SUCCESS**, sem conexão com PostgreSQL:

```powershell
.\mvnw.cmd -B "-Dtest=ContaAcessoAdministracaoTest,PoliticaHttpSecurityTest,AutorizacaoServicesTest" test
```

São 25 testes de API administrativa, 49 de política HTTP e 33 de autorização nos serviços.
Esse total é uma execução própria; não deve ser somado aos 233 testes da regressão do Checkpoint 5.

## Preparar o administrador — somente após autorização

É necessário um administrador válido no **mesmo banco de teste usado pela API**. A conta de desenvolvimento
`operador.loja` não deve ser presumida administrativa. Não a promova por SQL nem reabra cadastros públicos.

Em uma única instância local e banco descartável autorizado, o provisionamento implementado utiliza:

```text
INITIAL_ADMIN_ENABLED=true
INITIAL_ADMIN_NAME=<nome fictício do administrador do laboratório>
INITIAL_ADMIN_EMAIL=<e-mail fictício válido e exclusivo>
INITIAL_ADMIN_CPF=<CPF de teste válido e exclusivo>
INITIAL_ADMIN_LOGIN=<login exclusivo do laboratório>
INITIAL_ADMIN_PASSWORD=<senha local forte: mínimo 12 caracteres Unicode, máximo 72 bytes UTF-8>
```

Esses valores devem ser fornecidos pelo ambiente local; não devem entrar na coleção ou neste documento.
Após criação confirmada, pare a aplicação, desative a flag e reinicie. Pessoa e conta são criadas na mesma
transação, com OPERADOR e ADMINISTRADOR. Se já existir administrador utilizável, o runner não cria outro;
nesse caso use credenciais autorizadas existentes. Duplicidades de CPF, e-mail ou login não são reutilizadas
nem promovidas automaticamente. Não execute simultaneamente múltiplas instâncias com bootstrap habilitado.

**Nenhum desses passos foi executado neste checkpoint.** O administrador preexistente não faz parte da
limpeza da coleção. Se o ambiente inteiro for descartável, sua remoção pertence ao procedimento separado
de descarte, não ao runner Newman.

## Variáveis e tokens

O projeto mantém variáveis na coleção, sem arquivo de ambiente versionado. Configure `adminLogin` e
`adminPassword` somente no ambiente privado do Postman; os valores distribuídos estão vazios.
`baseUrl` começa em `http://localhost:8080`; o runner automatizado exige `http://localhost:18014`.

- `adminAccessToken` e `adminContaId`: capturados pelo login administrativo e `/me`.
- `authLogin`, `authPassword`, `accessToken`: operador principal criado pela execução; Bearer herdado da raiz.
- `blockedLogin`, `blockedPassword`, `blockedAccessToken`: conta fictícia para bloqueio via API.
- `inactiveLogin`, `inactivePassword`, `inactiveAccessToken`: conta fictícia para inativação da Pessoa.
- `promotedAccessToken`: novo token da conta fictícia após receber ADMINISTRADOR; não substitui o token do operador.
- `nextAuthPassword`, `oldAuthPassword`: troca de senha e teste de rejeição da senha anterior no login.
- `runId`, CPFs e IDs: valores gerados/capturados em tempo de execução; variáveis históricas mantidas.
- `invalidToken`: texto propositalmente inválido, exclusivamente para o cenário negativo.

Senhas fictícias são geradas em memória na inicialização; credenciais administrativas não são geradas nem
sobrescritas. Tokens e senhas iniciais estão vazios nos arquivos. Não crie variáveis de ambiente homônimas
para tokens, IDs ou credenciais fictícias: elas podem sobrepor os valores atualizados pelos scripts.
Não exporte ambiente/coleção preenchidos. Não use reporters verbosos, logs de headers/bodies ou o console
para divulgar credenciais. A chave JWT não é necessária para executar o Postman.

## Ordem e contratos

Execute uma iteração inteira por vez, em sequência:

| Pasta | Cenários | Códigos esperados |
|---|---|---|
| 01 — Infraestrutura | Inicialização, health e OpenAPI públicos | 200 |
| 02 — Autorização e conta OPERADOR | Login administrativo, `/me`, quatro Pessoas, três contas, logins e consultas | 200, 201 |
| 03 — Acesso comercial | Bearer válido, ausência/token inválido/Basic, login inválido, cadastro anônimo, OPERADOR na administração | 200, 401, 403 |
| 04 — Validações e conflitos | Cadastro autenticado pelo administrador: login/pessoa duplicados, senhas inválidas | 400, 409 |
| 05 — Alteração da própria senha | Troca, rejeição da anterior, novo login e acesso protegido | 204, 401, 200 |
| 06 — Pessoa inativa | Inativar, rejeitar novo login, reativar e autenticar | 200, 401 |
| 07 — Conta bloqueada | Administrador bloqueia outra conta, login rejeitado, token anterior aceito, desbloqueio | 200, 401 |
| 08 — Estoque e cadastro | Cenários comerciais da Aula 7; OPERADOR negado antes de cada DELETE administrativo | 200, 201, 204, 400, 403, 404, 409, conforme cada requisição |
| 09 — Vendas e histórico | Cenários da Aula 10, preço histórico, estoque e cancelamento; DELETE de item só com ADMINISTRADOR | 200, 201, 400, 403, 404, 409, conforme cada requisição |
| 10 — Administração, perfis e autoproteção | Autorrebaixamento/autobloqueio, validações, conta inexistente, promoção e novo token, restauração | 200, 400, 403, 404 |
| 11 — Último administrador | Experimento isolado, desabilitado por padrão | 400, apenas com preparação específica |

As requisições de cadastro de Pessoa e conta agora usam Bearer; o cadastro de conta exige ADMINISTRADOR.
Os DELETE históricos preservam suas assertions comerciais usando `adminAccessToken`; antes de cada um
há uma tentativa equivalente com `accessToken` esperando 403. Isso inclui a remoção de item de venda,
que retorna 200 para o administrador, e exclusões com impedimento de negócio, que continuam retornando 409.
Não foram inventados endpoints DELETE de Pessoa, conta, fornecedor, cliente ou colaborador.

O login inválido retorna 401 com `ApiError` genérico. Os 401/403 dos filtros não são tratados como esse JSON.
400/404/409 são verificados em requisições autenticadas com perfil suficiente, evitando confundi-los com
falhas de autorização. As respostas administrativas 200 confirmam perfis/status; `/me` confirma o sujeito.

O bloqueio é preparado exclusivamente pelo endpoint administrativo, sem o SQL de status usado na Aula 13.
O login da conta a bloquear é comprovado antes. Tokens anteriores ao bloqueio e à mudança de perfis não
são revogados. Um novo login é necessário para observar perfis novos. A senha nova passa a ser utilizada
pelos logins seguintes. A coleção é sequencial e não simula concorrência transacional; os testes JUnit
continuam sendo a evidência dos cenários concorrentes de estoque.

## Experimento de último administrador

Com um administrador externo utilizável, remover o perfil de outra conta administrativa **não** testa
a proteção do último administrador. Usar o token do próprio último administrador também não isola essa
regra: a autoproteção é verificada primeiro.

Por isso a pasta 11 é desabilitada por `lastAdminScenarioReady=false` e o runner comum a exclui inteiramente.
Somente em uma aplicação/banco descartável exclusivo, prepare previamente:

1. Um único administrador utilizável fictício, com login `a14.ultimo.*`, conta ATIVA e Pessoa ATIVA.
2. Uma segunda conta fictícia, inicialmente administrativa, cujo token ainda válido tenha sido emitido
   antes de ela ser rebaixada por outro administrador. Essa conta já não deve contar como administrador.
3. Informe, apenas em memória/ambiente privado, `lastAdminTargetId`, `lastAdminTargetLogin` e
   `lastAdminActorToken`, além de `disposableDatabaseConfirmed=BANCO_DESCARTAVEL` e
   `lastAdminScenarioReady=true`. Não reutilize o administrador de um banco compartilhado.
4. Execute exclusivamente a pasta 11, sem reinicializar a coleção normal. As duas operações esperam
   400 com a mensagem exata `O sistema deve manter ao menos um administrador ativo`.
5. Consulte novamente a conta e faça novo login para confirmar perfil ADMINISTRADOR e status ATIVA;
   depois descarte as fixtures exclusivas desse ambiente mediante procedimento autorizado.

Este experimento não foi preparado nem executado. O validador offline somente exercita suas assertions
com respostas fictícias e verifica o bloqueio padrão. Os testes isolados de service/API verificam a
invariante com repositórios simulados. A contagem administrativa continua tendo a limitação concorrente
declarada na implementação; a coleção não elimina nem comprova ausência desse risco.

## Validação sem rede nem banco

O [validador](VALIDAR-POSTMAN.cjs) usa Node.js e AJV, compila scripts sem executá-los contra HTTP,
compara integralmente as assertions históricas e exercita um subconjunto das novas assertions com
objetos de resposta fictícios. Não substitui o sandbox completo do Postman ou uma execução Newman.

```powershell
node --check docs/aula14/EXECUTAR-NEWMAN.cjs
node --check docs/aula14/VALIDAR-POSTMAN.cjs
node docs/aula14/VALIDAR-POSTMAN.cjs
```

Dependências locais: `ajv@6`, meta-schema draft-04, e uma cópia do schema oficial v2.1 em
`target/postman-collection-schema.json`. O caminho do schema pode ser indicado por `POSTMAN_SCHEMA_PATH`.
Neste ambiente foram reutilizados os arquivos/ferramentas ignorados de validações anteriores.
Não há dependência nova no `pom.xml` nem instalação de pacote dentro das pastas versionáveis.

## Comando de execução real — preparado, não executado

Pré-requisitos futuros: autorização para escrever fixtures, administrador utilizável no banco descartável,
Java 21, Node.js, Newman e `psql`, aplicação na porta 18014 com profile `test` apontando para
`instrumento2026_test`. Confira a configuração JDBC da aplicação: a porta HTTP sozinha não prova o banco usado.

Se necessário, instale as ferramentas somente em diretório ignorado:

```powershell
npm install --prefix target/aula14-postman-tools --no-save newman@6.2.3 ajv@6.12.6
$env:NEWMAN_MODULE_DIR = 'target/aula14-postman-tools/node_modules'
```

O schema oficial para validação offline é
`https://schema.getpostman.com/json/collection/v2.1.0/collection.json`.
Depois de preparar o ambiente autorizado, fornecer `INITIAL_ADMIN_ENABLED=false` e credenciais
temporárias em DB_TEST_*, SPRING_DATASOURCE_USERNAME/PASSWORD e SPRING_LIQUIBASE_USER/PASSWORD,
somente no ambiente privado. O caminho opcional de importação abaixo deve estar ausente.
Exemplo futuro equivalente em destino, não comando exato da execução registrada:

```powershell
$env:INITIAL_ADMIN_ENABLED = 'false'
$env:DB_TEST_URL = 'jdbc:postgresql://127.0.0.1:55414/instrumento2026_test?currentSchema=public'
.\mvnw.cmd -B "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=18014 --spring.config.additional-location=file:./src/test/resources/application-test.properties --spring.config.import=optional:file:./target/aula14-no-external-config.properties --spring.datasource.url=$env:DB_TEST_URL --spring.liquibase.url=$env:DB_TEST_URL --spring.liquibase.default-schema=public" spring-boot:run
```

Em outro terminal, injete os valores por mecanismo privado de segredos. Os valores abaixo são placeholders,
não credenciais funcionais; não grave os valores reais no histórico de shell ou neste arquivo.

```powershell
$env:AULA14_ADMIN_LOGIN = '<login administrativo local autorizado>'
$env:AULA14_ADMIN_PASSWORD = '<senha administrativa local fornecida com segurança>'
$env:DB_TEST_URL = 'jdbc:postgresql://127.0.0.1:55414/instrumento2026_test?currentSchema=public'
$env:DB_TEST_USERNAME = '<usuário local do banco de testes>'
$env:DB_TEST_PASSWORD = '<senha local do banco fornecida com segurança>'
$env:NEWMAN_BASE_URL = 'http://localhost:18014'
$env:AULA14_ALLOW_TEST_WRITES = 'SIM'
# Neutralize outros PG* conflitantes antes; psql nao recebe currentSchema da URL JDBC.
$env:PGOPTIONS = '-c search_path=public'
node docs/aula14/EXECUTAR-NEWMAN.cjs
```

O [runner](EXECUTAR-NEWMAN.cjs) não lê nem modifica `.env`, não provisiona administradores e não faz
preparação SQL de status/perfis. Exige autorização operacional explícita no ambiente, restringe API e
banco a localhost/teste, executa pastas 01–10 em fase única e transporta tokens somente em memória.
Basic aparece apenas como teste negativo. Não há comandos contendo senhas nos argumentos Newman/psql.

## Limpeza futura

O runner captura IDs somente de recursos efetivamente criados com POST/201 na própria execução,
incluindo requisições que inesperadamente criem um recurso. Em `finally`, remove as fixtures em ordem de
FKs, primeiro itens/vendas, depois contas e demais vínculos/cadastros, e confirma zero remanescentes
por esses IDs. Não trunca tabelas, não remove por prefixo e não apaga o administrador preexistente.
SQL de limpeza é necessário porque não existem endpoints HTTP de exclusão para todos esses recursos.

O resumo fica em `target/aula14-newman-resultados.json`, com totais reais e resultado da limpeza,
sem tokens, credenciais, bodies ou headers. Falhas de execução/limpeza resultam em saída não zero.
Interrupção abrupta do processo pode impedir o `finally`; nesse caso, é necessária inspeção local restrita
às fixtures criadas, antes de repetir. Execução manual no Postman não realiza a limpeza SQL automaticamente.
Ao terminar uma futura execução, remova as variáveis sensíveis da sessão e encerre a aplicação temporária.

**Não há resultado HTTP/Newman nem limpeza real a registrar neste checkpoint: nenhum dado foi criado.**
Naquele checkpoint, execução real, totais e limpeza estavam pendentes; foram concluídos no 7I.4.
A validação manual das interfaces continua distinta dos testes HTTP. Os resultados da Aula 13 não são evidência
da coleção da Aula 14.

## Execução real concluída — 7I.4

Em **05/11/2026 às 09:36:59 -03:00**, Newman terminou com **156 chamadas HTTP, 487 assertions,
zero falhas e código 0**. Pastas 01–10: 153 requisições e três consultas auxiliares.
**A pasta 11 não foi executada.** Validação estática: 155 definições, 163 scripts e 126 cenários
históricos preservados. JUnit havia concluído com 671 aprovados, zero falhas, erros e ignorados,
BUILD SUCCESS e código 0.

O cluster foi confirmado por SQL como `7704384549022663752`, PostgreSQL 17.6,
`127.0.0.1:55414/instrumento2026_test`, schema public, 41 changeSets e lock liberado.
DataSource e Liquibase utilizaram `currentSchema=public`; dez conexões da API foram correlacionadas
com o servidor. O runner não faz sozinho essa comprovação e tem fallback SQL para 5432:
não executar com DB_TEST_URL ausente ou sem correspondência comprovada entre API e limpeza.

O startup efetivo utilizou Java diretamente com classpath de runtime, profile test, importação
externa neutralizada e bootstrap false. A opção `connection-init-sql` foi retirada após causar
falha de binding no Hikari. O runner foi chamado diretamente por arquivo, sem `node -e`.
Recuperação de credencial e execução ocorreram na mesma sessão, somente em memória.

A limpeza confirmou **zero fixtures remanescentes**, incluindo zero registros comerciais.
As contas 225/226 e Pessoas 639/640 pertencem à preparação externa e permaneceram ativas.
A conta 225 foi preservada; a recuperação autorizada alterou somente o hash da conta 226.
A API foi encerrada; o cluster temporário e a instância original foram preservados.
O inventário de limpeza permanece em memória. Tokens antigos mantêm authorities até expirar;
as limitações de contagem concorrente e inativação indireta de Pessoa não foram corrigidas.

Códigos observados, relatórios e alcance das evidências: [EVIDENCIAS.md](EVIDENCIAS.md).
Nenhum resultado HTTP substitui verificação gráfica manual do Swagger/Postman.
