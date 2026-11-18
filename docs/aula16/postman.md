# OpenAPI e Postman — Aula 16

**Estado atual em 17/11/2026: execução real Newman validada — 40 requisições, 108 asserções, zero falhas e erros.** A instância exclusiva de teste foi encerrada; permaneceram três pessoas, três contas e 11 sessões revogadas, sem exclusão física. A seção final registra os IDs e a execução real. A validação offline e os 16 testes integrados anteriores continuam evidências separadas.

Referência: [Aula 16 oficial](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/16aula/16aula.md), seções 15 (contratos), 17 (access após logout), 20 (laboratório), 23/D e 24 (OpenAPI/coleção), 25 e 26 (entregas). O professor não define formato específico de runner; o local segue o padrão CJS das aulas anteriores, com manifesto JSON. Os artefatos históricos foram preservados.

## OpenAPI

OpenApiConfig documenta as operações já implementadas, sem editar controllers, DTOs, services ou SecurityConfig. /v3/api-docs e Swagger continuam disponíveis nos profiles existentes; a configuração de produção que os desabilita foi preservada.

| POST /api/autenticacao/ | Segurança | Respostas documentadas |
|---|---|---|
| login | Pública | 200 par; 400 validação/JSON; 401 credenciais em ApiError ou filtro |
| refresh | Pública | 200 par; 400 JSON; 401 genérico em ApiError ou filtro |
| logout | Pública | 204 sem corpo inclusive desconhecido/ausente; 400 JSON; 401 somente se apresentado Bearer inválido/expirado |
| logout-todos | Bearer válido, sem perfil adicional | 204 sem corpo; 401 por identidade inativa/inexistente em ApiError ou pelo filtro |

As três operações públicas têm security vazio; logout-todos explicita bearerAuth. Não se inventou 403 por perfil no global. O 401 do filtro não promete ApiError: usa WWW-Authenticate e pode ter corpo vazio, conforme os testes anteriores. O conteúdo JSON documentado no 401 aplica-se somente ao controller. ApiError preserva timestamp/status/error/message/path/fields; 204 não tem content.

TokenResponse possui seis campos: accessToken, tokenType, expiresIn, expiresAt, refreshToken e refreshTokenExpiresAt. expiresAt é o limite do access; refreshTokenExpiresAt é o limite absoluto da família, preservado nas rotações. A inatividade pode encerrar antes. Os padrões são access de 30 minutos, inatividade PT30M e absoluto PT8H, identificados como configuráveis.

RefreshTokenRequest não exige campo/body obrigatório no schema: o serviço decide 401 no refresh ausente/vazio e 204 no logout. JSON malformado continua 400. A entrada é writeOnly; as credenciais de resposta são readOnly, sem exemplos com tokens ou hash exposto. sid identifica a família no JWT, sem HttpSession.

O teste compara o contrato completo histórico da Aula 10, permitindo explicitamente a evolução de autenticação. Os três endpoints e RefreshTokenRequest foram acrescentados à lista de evolução, sem descartar contratos comerciais.

## Artefatos e sequência

- [Coleção Aula 16](Instrumento%202026%20-%20Aula%2016.postman_collection.json): Postman v2.1, 6 pastas, 40 requisições.
- [Manifesto](NEWMAN.runner.json): ordem, uma iteração, destino esperado e relatório.
- [Runner](EXECUTAR-NEWMAN.cjs): Newman futuro, sem iniciar API ou conectar ao PostgreSQL.
- [Validador offline](VALIDAR-POSTMAN.cjs): schema, estrutura, scripts e referências.

| Pasta | Cenários preparados |
|---|---|
| 01 — Preparação e contas próprias | Login administrativo, criação de duas pessoas/contas OPERADOR, login da outra conta |
| 02 — Autorização, duas rotações e replay | Consulta 200, operação administrativa 403, R1 → R2 → R3, replay R1 e rejeição de R3 |
| 03 — Logout individual e isolamento | Duas famílias, logout/idempotência/desconhecido, refresh 401, access anterior 200 e outra família utilizável |
| 04 — Logout global e isolamento entre contas | Duas famílias, Bearer ausente/inválido 401, global 204 mesmo com ID de outra conta no corpo, rejeição de ambas e outra conta preservada |
| 05 — Erros de entrada | Refresh ausente/vazio/desconhecido 401, JSON malformado 400 |
| 06 — Encerramento e limpeza lógica | Logout da outra conta, bloqueio das duas contas criadas, inativação de suas pessoas e encerramento da sessão administrativa criada neste run |

As rotações substituem o par na mesma rotina de captura. R1 é salvo separadamente para replay. Famílias adicionais usam variáveis próprias; ao final, tokens e senha de fixture são removidos das variáveis de coleção. Não há console com credenciais nem requisições ocultas via pm.sendRequest.

Logins/e-mails únicos, CPFs sintéticos com verificadores e senha de fixture são gerados por execução. IDs são capturados somente depois de 201; os numéricos são interpolados sem aspas no JSON enviado. Nenhuma senha ou token real integra os arquivos.

## Uso manual futuro

Importe apenas a nova coleção; execute todas as pastas em ordem, uma iteração, cookie jar limpo, sem outro cliente renovando os mesmos tokens. Configure baseUrl para a API de teste local na porta 18016 e adminLogin/adminPassword em ambiente privado, com administrador ativo previamente provisionado. As credenciais distribuídas estão vazias.

Não crie variáveis de token/ID no environment com maior precedência: as capturas usam collectionVariables e não devem ser sombreadas por valores antigos. Em uso manual isolado, envie refresh somente no corpo da rota de sessão; no sucesso, substitua o par inteiro. Executar itens soltos não demonstra o fluxo completo.

O Postman pode manter valores capturados na interface: não exporte coleção/environment preenchidos, sincronize credenciais privadas ou publique bodies de sucesso. O access anterior continua válido até exp após logout; não há revogação imediata do JWT.

## Validação offline reproduzível

As ferramentas ficam em target, fora das configurações da aplicação:

```powershell
npm.cmd install --prefix target/aula16-postman-tools --no-save --ignore-scripts ajv@6.12.6
Invoke-WebRequest -UseBasicParsing 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json' -OutFile target/aula16-postman-collection-schema.json
node --check docs/aula16/EXECUTAR-NEWMAN.cjs
node --check docs/aula16/VALIDAR-POSTMAN.cjs
node docs/aula16/VALIDAR-POSTMAN.cjs
```

O validador usa AJV/schema oficial v2.1 e valida o manifesto pelas restrições do runner. Compila os scripts em vm.Script e simula sua sequência com respostas sintéticas: JSON após interpolação, variáveis antes do uso, preservação de R1, troca do par/prazo e remoção de segredos. Não executa Newman nem todo o sandbox Postman, não verifica a API e não substitui execução real.

## Runner — instruções preparadas antes da execução real

Antes de habilitá-lo, é necessária autorização para escritas e comprovação externa de que a API está em profile test, DataSource/Liquibase explícitos em instrumento2026_test, 127.0.0.1:5432/public, sem importação de .env. A porta HTTP e a URL JDBC declarada não comprovam sozinhas o destino efetivo da aplicação.

O runner não lê .env, inicia serviços, provisiona administrador ou conecta ao banco. Exige AULA16_ALLOW_TEST_WRITES=SIM e AULA16_PROFILE_TEST_CONFIRMED=SIM, além da declaração JDBC exata. Prepare AULA16_ADMIN_LOGIN e AULA16_ADMIN_PASSWORD somente no ambiente privado do processo, sem credenciais em argumentos/arquivos versionados.

Depois da futura autorização e comprovação do destino:

```powershell
npm.cmd install --prefix target/aula16-postman-tools --no-save --ignore-scripts newman@6.2.3 ajv@6.12.6
$env:DB_TEST_URL = 'jdbc:postgresql://127.0.0.1:5432/instrumento2026_test'
$env:AULA16_ALLOW_TEST_WRITES = 'SIM'
$env:AULA16_PROFILE_TEST_CONFIRMED = 'SIM'
# Credenciais administrativas: ambiente privado já preparado.
node docs/aula16/EXECUTAR-NEWMAN.cjs
```

NEWMAN_MODULE_DIR permite localizar os módulos. O runner fixa o destino HTTP, as seis pastas e uma iteração. Não usa reporter que exporte bodies/headers/tokens nem imprime falhas brutas. target/aula16-newman-resultados.json guarda somente contadores, quantidade de falhas, IDs das fixtures e resultado da limpeza lógica. Saída diferente de zero exige revisão antes de repetir.

**Limpeza lógica não é exclusão física:** não existe DELETE de conta no contrato. A coleção bloqueia as contas criadas e inativa suas pessoas, mantendo registros/histórico no banco. IDs ficam no resumo para remoção física posterior somente se autorizada, pelo procedimento de fixtures do banco de teste. Não há busca ampla por IDs. Em abortos/expiração de credenciais, a limpeza pode falhar: revise as fixtures reportadas. O administrador fornecido não é bloqueado/inativado/removido; somente o refresh de seu login neste run é encerrado.

## Histórico: evidências da preparação e validação offline

Em 17/11/2026, encerramento 16:41:23 -03:00:

```powershell
.\mvnw.cmd -B '-Dtest=OpenApiDocumentationTest' '-Dsurefire.skipAfterFailureCount=1' '-Dspring.config.import=optional:file:./target/aula16-no-external-config.properties' '-Djacoco.destFile=target/aula16-openapi.exec' '-Djacoco.append=false' test > target/aula16-openapi.log 2>&1
```

**63 testes aprovados, zero falhas/erros/ignorados, BUILD SUCCESS, saída 0.** O slice confere ausência de DataSource, filtros reais para documentação e mocks dos serviços, incluindo contrato JSON/YAML e compatibilidade histórica. Nenhum PostgreSQL foi acessado.

Validação offline: **6 pastas, 40 requisições, 43 scripts compilados/simulados, 108 asserções sintéticas e zero requisições ao servidor**. Os CJS e o manifesto são válidos. A simulação não é JUnit nem assertion Newman real. Coleções e documentos históricos das aulas 7/10/12/13/14 ficaram sem diff.

Na preparação, estavam pendentes a execução real e a conferência de fixtures, realizadas na etapa abaixo. Continuam posteriores a revisão documental consolidada, avaliação das entregas arquiteturais/atividades e suíte completa/portão JaCoCo quando autorizada. Não houve commit/push.

Arquivos desta etapa: OpenApiConfig.java; OpenApiDocumentationTest.java; support/ContratoOpenApiSupport.java; este documento; README.md desta pasta; coleção Aula 16; NEWMAN.runner.json; EXECUTAR-NEWMAN.cjs; VALIDAR-POSTMAN.cjs. Nenhum controller, service, DTO, SecurityConfig ou migration foi alterado.

## Execução real autorizada — 17/11/2026

### Preflight e isolamento comprovado

Foram revisados este documento, o manifesto, o runner e os efeitos das 40 requisições. A coleção modifica somente as fixtures criadas por POST/201 e encerra as sessões originadas pelos próprios logins. Os IDs usados em alterações são capturados no run, com variáveis inicialmente limpas e sem sobrescrita por environment de maior precedência. O corpo que cita outra conta no logout global é uma verificação deliberada de isolamento: a API usa o subject autenticado e a outra conta continuou renovando.

Antes de iniciar qualquer servidor, consultas em BEGIN READ ONLY confirmaram:

| Verificação | Resultado |
|---|---|
| Banco | instrumento2026_test |
| Host/porta PostgreSQL | 127.0.0.1:5432 |
| Schema | public |
| ChangeSets | 44, com somente as migrations esperadas até 007 |
| Lock Liquibase | false |
| Pessoas / contas / sessões | 0 / 0 / 0 |
| Administradores utilizáveis | 0 |
| Porta HTTP 18016 | Livre |

A 007 preservou SHA-256 `6DA69A5EA246220E8509E826DC319FDCC7A28EB50EB2353A29A597D72505F55B`. Foram verificados os 44 changeSets nos arquivos e a ausência de overrides externos Maven/Java/configuração/DataSource. O caminho opcional neutro foi conferido como inexistente.

Como não existia administrador utilizável, a preparação criou uma **fixture administrativa exclusiva** pelo bootstrap existente, sem alterar código ou regras: login/e-mail/CPF próprios e senha aleatória gerada com crypto em memória. Não foi utilizada credencial administrativa preexistente ou real. Sua pessoa/conta permanecem no banco, conforme a tabela final; a coleção não as bloqueia ou remove. A senha não foi registrada em arquivo ou preservada para reutilização.

### Preparação e inicialização da API

As dependências Newman 6.2.3/AJV 6.12.6 foram instaladas somente em target, sem scripts de instalação. A validação offline passou novamente antes da execução. Foi feita compilação, **sem execução de testes**:

```powershell
.\mvnw.cmd -B '-DskipTests' test-compile dependency:build-classpath '-Dmdep.outputFile=target/aula16-newman-classpath.txt'
```

Preparação Maven: BUILD SUCCESS, saída 0, encerramento 16:56:37 -03:00. Isso não é execução JUnit nem suíte completa.

Um processo Java direto, com classpath de target/test-classes, target/classes e dependências, iniciou Instrumento2026Application. O classpath de teste disponibilizou application-test.properties. As opções explícitas foram:

```text
--spring.profiles.active=test
--server.address=127.0.0.1
--server.port=18016
--spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test
--spring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test
--spring.datasource.hikari.schema=public
--spring.liquibase.default-schema=public
--spring.config.import=optional:file:./target/aula16-no-external-config.properties
--spring.datasource.hikari.data-source-properties.ApplicationName=aula16-newman-0c6ad2602db4
--app.security.initial-admin.enabled=true
--app.security.jwt.expiration=PT30M
--app.security.refresh-token.idle-timeout=PT30M
--app.security.refresh-token.absolute-expiration=PT8H
--spring.jpa.show-sql=false
--logging.level.liquibase=INFO
--logging.level.org.hibernate.SQL=OFF
--logging.level.org.hibernate.orm.jdbc.bind=OFF
```

O application_name é um identificador efêmero para correlacionar conexões; o valor efetivo está no relatório de execução em target. Usuário/senha de teste, chave JWT efêmera e campos do administrador foram fornecidos somente no ambiente privado do processo. Variáveis Spring/DB/segurança herdadas foram filtradas antes da criação desse ambiente e substituídas pelos valores explícitos; a configuração privada .env não foi importada pelo Spring nem modificada. As credenciais de banco foram lidas em memória exclusivamente das chaves DB_TEST_*.

Evidências de inicialização, PID **29568**:

```text
16:59:36 — The following 1 profile is active: "test"
16:59:41 — Database is up to date, no changesets to execute
Run: 0 / Previously run: 44 / Total change sets: 44
Database JDBC URL [jdbc:postgresql://127.0.0.1:5432/instrumento2026_test]
16:59:45 — Tomcat started on port 18016 (http)
```

Get-NetTCPConnection confirmou que a porta pertencia ao PID iniciado. pg_stat_activity mostrou **10 conexões** com o application_name exclusivo da execução, todas no banco instrumento2026_test. A consulta também comprovou a fixture administrativa ativa antes de iniciar Newman. Portanto, a confirmação não se limitou a uma declaração de URL no manifesto.

### Runner, observação temporária e resultado real

O próprio EXECUTAR-NEWMAN.cjs foi executado uma única vez, com credenciais administrativas somente no environment privado e as declarações de destino habilitadas. Uma instrumentação temporária em target adicionou bail=true, contagem de status/eventos e coleta de segredos exclusivamente em memória/IPC para a auditoria de saída. Não houve alteração do runner versionável ou da coleção; o preload envolve newman.run sem trocar as requisições. Uma falha interromperia o run, sem repetição automática.

Comando de orquestração desta execução:

```powershell
node target/aula16-newman-orquestrar.cjs
```

O helper temporário iniciou Java, chamou o runner por um processo Node com preload de target/aula16-newman-observador.cjs, conferiu as conexões e encerrou seu próprio filho Java. Os arquivos são ignorados pelo Git e não integram os candidatos ao versionamento; as opções relevantes e os resultados estão documentados aqui para revisão.

Conclusão em **17/11/2026 às 16:59:52 -03:00**:

| Métrica real Newman | Total | Falhas/erros | Pendentes |
|---|---:|---:|---:|
| Requisições | 40 | 0 | 0 |
| Asserções | 108 | 0 | 0 |
| Falhas agregadas | 0 | — | — |
| Exceções de script/eventos anômalos | 0 | — | — |
| Respostas da limpeza lógica esperadas | 4 | 0 | 0 |

Runner e orquestração terminaram com código **0**. Não houve execução adicional da coleção.

| Status HTTP observado | Quantidade |
|---|---:|
| 200 | 18 |
| 201 | 4 |
| 204 | 6 |
| 400 | 1 |
| 401 | 10 |
| 403 | 1 |
| Total | 40 |

Passaram login/par, consulta autorizada, proibição administrativa do OPERADOR, R1 → R2 → R3, replay R1 com 401 e rejeição de R3, logout individual/idempotente/desconhecido, manutenção de outra família, global sem Bearer e com Bearer inválido, global da própria conta, preservação/renovação da outra conta, erros de entrada e limpeza lógica. As consultas protegidas após logout individual e global retornaram 200 com os access tokens anteriores, ainda válidos.

Os 108 resultados são agora **asserções Newman reais**, separados das 108 asserções sintéticas da simulação offline; não são somados. Esta execução não reexecutou os 63 testes OpenAPI, os 16 testes integrados ou a suíte completa.

### Segredos e encerramento

Os reporters de corpos/headers foram desabilitados. A instrumentação observou os tokens emitidos e seus hashes SHA-256, senhas das fixtures/administrador, senha de banco, chave JWT efêmera e hashes persistidos somente em memória. Antes de gravar as evidências, comparou esses valores com as saídas capturadas da API e runner: **zero ocorrências em ambos**. Os relatórios contêm somente contadores, IDs, estados e metadados de sessão, sem tokens, senhas ou hashes. Os scripts da coleção não produziram eventos de console.

Os logs finais são escritos com proteção por substituição caso surgisse algum segredo, mas a auditoria anterior à substituição encontrou zero vazamentos. Essa evidência cobre a saída capturada desta execução, não todos os destinos de logs possíveis em outros ambientes. A chave JWT e as senhas efêmeras não foram gravadas em arquivos versionáveis.

Foi encerrado **somente o processo Java 29568**, criado pela orquestração. Após o término, confirmou-se PID inexistente, porta 18016 livre e zero conexões PostgreSQL com o application_name dessa execução. Nenhum processo preexistente foi encerrado.

### Registros que permaneceram — sem exclusão física

As consultas finais, em novas conexões/transações somente leitura, mantiveram 44 changeSets e lock livre. Permaneceram exclusivamente as fixtures desta execução nas tabelas de pessoas/contas/sessões, que estavam vazias no preflight:

| Fixture | Pessoa ID | Conta ID | Conta | Pessoa | Perfis |
|---|---:|---:|---|---|---|
| Administrador de preparação | 9742 | 2511 | ATIVA | ATIVO | ADMINISTRADOR, OPERADOR |
| OPERADOR principal | 9743 | 2512 | BLOQUEADA | INATIVO | OPERADOR |
| Outra conta | 9744 | 2513 | BLOQUEADA | INATIVO | OPERADOR |

Total: **3 pessoas, 3 contas, 4 vínculos de perfil e 11 gerações de sessão em 7 famílias**. As 11 sessões estão revogadas; quatro também possuem utilizada_em. Os IDs de sessão e motivos efetivamente lidos, sem hashes:

| Conta | IDs de sessão | Motivo persistido |
|---:|---|---|
| 2511 | 70 | LOGOUT |
| 2512 | 72, 73, 74 | REUTILIZACAO |
| 2512 | 75 | LOGOUT |
| 2512 | 76, 77, 78, 79 | LOGOUT_GLOBAL |
| 2513 | 71, 80 | LOGOUT |

A persistência dessas revogações foi conferida depois de encerrar a aplicação, não apenas pela resposta HTTP. O replay preservou as três gerações encerradas; a outra conta somente recebeu LOGOUT na sua própria etapa final, após renovar com sucesso depois do global da conta principal.

**Nenhuma exclusão física foi feita.** O administrador de preparação continua ativo por desenho do fluxo; a coleção encerrou apenas sua sessão criada no run. Qualquer bloqueio adicional ou remoção física desses registros aguarda autorização específica, não está implícito nesta validação.

### Arquivos, evidências e pendências atuais

Nesta execução foi alterado somente **docs/aula16/postman.md** entre os documentos/fontes candidatos ao versionamento. Nenhum código de produção, migration, regra de negócio, coleção histórica, coleção Aula 16, manifesto ou runner versionável foi modificado.

Artefatos temporários em target, confirmados como ignorados pelo Git:

- aula16-newman-resultados.json — resumo do runner.
- aula16-newman-evidencias.json — destino/PID, status por requisição, auditoria de saída e fixtures finais, sem segredos.
- aula16-newman-api.log e aula16-newman-runner.log — logs conferidos.
- aula16-newman-preparacao.log, aula16-newman-classpath.txt e dependências locais.
- aula16-newman-orquestrar.cjs e aula16-newman-observador.cjs — instrumentação temporária.

**Validação real Postman/Newman aprovada.** Permanecem para revisão posterior o destino das fixtures retidas, a suíte completa/portão JaCoCo e o fechamento documental/entregas da Aula 16. Não houve acesso ao banco de desenvolvimento, commit ou push. Aguarda-se revisão antes de qualquer avanço.
