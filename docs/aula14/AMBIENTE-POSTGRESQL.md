# Aula 14 — Diagnóstico do ambiente PostgreSQL

> Atualização 7K, 05/11/2026: o diagnóstico 7C abaixo é histórico. O isolamento foi posteriormente
> comprovado e a validação concluída: 671 testes JUnit, BUILD SUCCESS, saída 0; Newman com 156
> chamadas e 487 assertions, zero falhas, saída 0. Consulte o registro final deste documento.

Checkpoint 7C, 04/11/2026. **Somente inspeção e preparação documental.** Não houve conexão PostgreSQL,
consulta SQL, migração, teste de integração, aplicação iniciada, bootstrap ou Newman. Nenhum banco,
credencial ou `.env` foi alterado. Os comandos deste documento são propostas futuras, não executadas.
Referência: [Aula 14 do professor](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/14aula/14aula.md).

## Configuração atual

| Arquivo | Uso | Destino e credenciais |
|---|---|---|
| src/main/resources/application.properties | Base; profile padrão none | Sem datasource base; changelog master, JPA validate e INITIAL_ADMIN_ENABLED com padrão false |
| src/main/resources/application-dev.properties | dev | DB_DEV_URL/USERNAME/PASSWORD; URL padrão local instrumento2026_dev |
| src/test/resources/application-test.properties | test | DB_TEST_URL/USERNAME/PASSWORD; URL padrão local instrumento2026_test |
| src/main/resources/application-prod.properties | prod | DB_URL/USERNAME/PASSWORD; sem fallback de URL |
| src/main/resources/application-schema-reference.properties | schema-reference | DB_REFERENCE_URL/USERNAME/PASSWORD; Liquibase desligado, ddl-auto=create |
| Plugin Liquibase Maven no pom.xml | Objetivos explícitos | DB_DIFF_* e DB_REFERENCE_*; distinto do Liquibase Spring |

dev e test importam `optional:file:./.env[.properties]`, relativo ao diretório de execução. Os padrões
dev/test usam localhost:5432 e usuário `instrumento_app`. O profile test fica nos recursos de teste:
para `spring-boot:run`, sua ativação isolada não garante que esse arquivo esteja no classpath normal.
Por isso os comandos futuros incluem explicitamente o arquivo por `spring.config.additional-location`.

As **27 classes @SpringBootTest** inspecionadas ativam `@ActiveProfiles("test")`. Obtêm o DataSource
Spring Boot via JPA/repositories, JdbcTemplate e TransactionTemplate, inclusive em testes concorrentes.
Não foram encontradas conexões manuais DriverManager, URL JDBC PostgreSQL própria em testes,
Testcontainers, H2 ou criação automática de banco descartável. Contextos MVC/unitários isolados não
representam essa conexão; vários verificam explicitamente ausência de DataSource.

A leitura privada do `.env` encontrou nomes DB_DEV_*, DB_TEST_*, DB_REFERENCE_* e DB_DIFF_*.
Comparação somente em memória, sem divulgar valores: dev/test têm URLs e nomes de banco distintos,
mas mesmo host/porta configurados e mesmo nome de usuário. Essas duas URLs não possuem parâmetros
de query. Não foram encontrados nomes de sobreposição DB_*/SPRING_* ou opções JVM relevantes no processo
desta inspeção, nem variáveis PG*. Isso não informa argumentos de outra sessão, IDE, JVM ou execução futura.
A flag local não foi identificada como habilitada; nenhum runner de bootstrap foi executado.

## Isolamento e riscos

Há **separação de configuração e nomes de banco**, não comprovação de isolamento operacional.
Sem acessar o servidor, não se pode confirmar existência/conteúdo dos bancos, versão efetiva, cluster,
privilégios ou search_path. O nome instrumento2026_test não garante banco vazio ou descartável.
PostgreSQL 17.6 é o ambiente declarado anteriormente; não foi consultado novamente nesta etapa.

- DB_TEST_URL pode apontar incorretamente para desenvolvimento: o profile não valida seu destino.
- SPRING_DATASOURCE_*, SPRING_APPLICATION_JSON, argumentos JVM/Maven/aplicação e arquivos externos podem
  sobrepor o datasource. Conferir profiles simultâneos, includes/groups e configuração do IDE.
- SPRING_LIQUIBASE_URL/USER/PASSWORD podem enviar migrações a destino diferente do DataSource principal.
- O mesmo usuário está configurado para dev/test; não foram verificados privilégios em cada banco.
- Uma API antiga na porta 18014 pode estar conectada a outro banco. Health/login/porta não comprovam destino.
- Não usar prod ou schema-reference na validação. schema-reference desliga Liquibase, mas ddl-auto=create
  pode recriar estrutura no destino errado.
- Transação de teste não evita migração de inicialização. Testes sem transação externa/concorrentes
  confirmam escritas e depois limpam dados. Fixtures com CPFs e códigos fixos podem colidir com dados prévios.

## Liquibase e dependência de dados

O starter Liquibase está presente e test não o desabilita nem configura conexão própria. O master inclui
001 a 006. Na configuração normal, Liquibase usa o DataSource Spring e executa changeSets pendentes
durante a inicialização do contexto, antes do primeiro método de teste. `ddl-auto=validate` não impede
essas escritas. Até um teste de contexto pode migrar um banco vazio.

A contagem histórica esperada é 41 changeSets, sem migração nova da Aula 14. Ela não foi consultada
no servidor. O plugin Maven não possui execução ligada ao ciclo de testes; não executar liquibase:*.
DB_DIFF_* e DB_REFERENCE_* do plugin não substituem DB_TEST_* automaticamente.

Os testes inspecionados criam Pessoas, contas, participantes, cadastros e vendas próprios. Não há carga
`@Sql` ou script de dados de desenvolvimento exigido nos testes. Banco vazio com estrutura criada pelos
changelogs é uma estratégia viável, ainda não comprovada por execução real autorizada.
As expectativas históricas foram adaptadas no 7B: 237 testes isolados aprovados; integração apenas compilada.
Os achados de testes dos documentos do 7A representam aquele momento, não falhas já reproduzidas no banco.

Usar a ordem **suíte completa sem bootstrap; depois bootstrap; depois Newman**. Um administrador criado
antes pode colidir com CPFs fixos das fixtures. Não executar suíte e Newman em paralelo nem repetir suíte
contra estado desconhecido após provisionamento. Não pressupor limpeza integral sem evidências.

## Estratégia descartável recomendada

Após autorização, disponibilizar instância/cluster PostgreSQL 17.6 exclusivo para validação, em porta
local distinta, com banco `instrumento2026_test` e usuário exclusivo, sem credenciais ou privilégios de
desenvolvimento. Esse nome de banco é exigido pelo runner. Não basta outro schema no banco de desenvolvimento.
Não foram criados container, instância, usuário ou banco.

Usar sessão privada limpa, uma única URL JDBC sem credenciais embutidas e schema public uniforme.
O banco de desenvolvimento não deve existir nessa instância descartável. Havendo dados anteriores,
exigir inventário/snapshot autorizado antes das escritas; preferir ambiente sem dados preexistentes.
Provisionamento, recuperação e descarte da infraestrutura exigem autorizações separadas.

## Como comprovar o destino da API e da limpeza

`EXECUTAR-NEWMAN.cjs` lê DB_TEST_* **do processo**, não do `.env`. A API test importa `.env`.
O runner restringe HTTP a localhost:18014 e SQL a localhost/instrumento2026_test, mas não consulta o
DataSource da API. A mesma pasta ou porta não comprova o mesmo banco.

O runner extrai host/porta/database para psql, descartando parâmetros JDBC como currentSchema/SSL.
PGOPTIONS/PGSERVICE e configurações de usuário/banco podem alterar search_path. Mesmo banco com schemas
distintos pode fazer a limpeza atingir tabelas erradas.

Após autorização para conexão e inicialização:

1. Injetar a mesma URL/credenciais em Maven/API e runner. Fixar explicitamente a URL do DataSource da API.
2. Identificar o processo da porta 18014, profile test, configuração efetiva do pool e ausência de
   conexão Liquibase separada. Não imprimir senha nem expor endpoint de metadados.
3. Inspecionar o DataSource real em runtime, por exemplo por depurador local, obtendo metadados JDBC e
   a consulta abaixo na **conexão da própria API**. Psql avulso não comprova o destino da API.
4. Executar a mesma consulta por psql com os parâmetros do runner. Comparar banco, servidor, usuário
   e schemas com a instância exclusiva identificada. Exigir schema public uniforme.
5. Opcionalmente correlacionar ApplicationName exclusivo da API com pg_stat_activity, se houver
   visibilidade. Nome de aplicação/versão iguais, isoladamente, não bastam; não ampliar privilégios sem autorização.
6. Sem correspondência comprovada, **não executar Newman nem limpeza**.

Consulta somente leitura preparada, **não executada**:

```sql
SELECT current_database() AS banco,
       current_user AS usuario,
       current_schema() AS schema_atual,
       current_schemas(false) AS schemas_visiveis,
       inet_server_addr() AS endereco_servidor,
       inet_server_port() AS porta_servidor,
       pg_postmaster_start_time() AS inicio_instancia,
       current_setting('server_version') AS versao;
```

Endereço pode variar em interfaces IPv4/IPv6; avaliar os resultados junto com a configuração e identidade
da instância descartável, sem aceitar coincidência parcial como prova de isolamento.

A limpeza captura IDs apenas de POST/201 e remove fixtures por esses IDs, sem TRUNCATE ou prefixos.
Não inclui o administrador externo. Porém, se os destinos divergirem, IDs podem coincidir com registros
preexistentes no banco de limpeza. Encerramento abrupto pode perder inventário em memória/finally.
Isso reforça a necessidade de ambiente exclusivo, confirmação prévia e recuperação planejada.

`VALIDAR-POSTMAN.cjs` apenas lê arquivos/schema, compila scripts e usa respostas fictícias. Não conecta
ao PostgreSQL nem chama Newman. Ele não comprova datasource, privilégios ou limpeza real.

## Variáveis necessárias, sem valores reais

| Finalidade | Nomes e placeholders |
|---|---|
| Datasource exclusivo | DB_TEST_URL=`jdbc:postgresql://localhost:<PORTA_EXCLUSIVA>/instrumento2026_test`; DB_TEST_USERNAME=`<USUARIO_EXCLUSIVO>`; DB_TEST_PASSWORD=`<SEGREDO_PRIVADO>` |
| Bootstrap posterior | INITIAL_ADMIN_NAME, INITIAL_ADMIN_EMAIL, INITIAL_ADMIN_CPF, INITIAL_ADMIN_LOGIN e INITIAL_ADMIN_PASSWORD: valores fictícios válidos/exclusivos pelo ambiente privado |
| Flag | INITIAL_ADMIN_ENABLED=false normalmente; ativação somente no passo autorizado |
| Newman | AULA14_ADMIN_LOGIN=`<LOGIN_FICTICIO_PROVISIONADO>`; AULA14_ADMIN_PASSWORD=`<SEGREDO_PRIVADO>`; NEWMAN_BASE_URL=`http://localhost:18014`; AULA14_ALLOW_TEST_WRITES=SIM somente após autorização |
| Ferramentas | NEWMAN_MODULE_DIR=`<PASTA_NODE_MODULES>`; PSQL_PATH=`<CAMINHO_PSQL>`; POSTMAN_SCHEMA_PATH=`<SCHEMA_V2_1>` se necessário |
| Sobreposições a revisar | SPRING_DATASOURCE_*, SPRING_LIQUIBASE_*, SPRING_APPLICATION_JSON, SPRING_PROFILES_*, SPRING_CONFIG_*, JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS, MAVEN_OPTS, PGOPTIONS, PGSERVICE e demais PG* |

Não colocar senha em URL, argumentos Maven, coleção, relatório ou histórico de shell. A chave didática
JWT do profile test é exclusiva de testes; não é necessário gerar ou ler chave real para essa validação.

## Comandos futuros — não executados

Somente após autorização e preparação privada de DB_TEST_*, com URL conferida. Não são comandos para
criar bancos/usuários. Pressupõem infraestrutura exclusiva autorizada. Não ativar dev/prod/schema-reference.

Conferência SQL somente leitura, também sujeita a autorização de conexão. `-W` pede senha interativamente
e `-X` evita psqlrc local:

```powershell
& '<CAMINHO_PSQL>' -X -W -v ON_ERROR_STOP=1 -h localhost -p '<PORTA_EXCLUSIVA>' -U '<USUARIO_EXCLUSIVO>' -d instrumento2026_test -c 'SELECT current_database(), current_user, current_schema(), current_schemas(false), inet_server_addr(), inet_server_port(), pg_postmaster_start_time();'
```

Suíte completa: **pode executar Liquibase e gravar fixtures**. Credenciais virão de DB_TEST_* privado.
O argumento de URL não deve conter senha ou parâmetros alternativos de schema:

```powershell
$env:INITIAL_ADMIN_ENABLED = 'false'
$env:DB_TEST_URL = 'jdbc:postgresql://127.0.0.1:55414/instrumento2026_test?currentSchema=public'
.\mvnw.cmd -B "-Dspring.profiles.active=test" "-Dspring.datasource.url=$env:DB_TEST_URL" "-Dspring.liquibase.url=$env:DB_TEST_URL" "-Dspring.liquibase.default-schema=public" "-Dspring.config.import=optional:file:./target/aula14-no-external-config.properties" test
```

Bootstrap, **depois da suíte e sob autorização específica**. INITIAL_ADMIN_* já deverão estar injetadas
privadamente. Essa inicialização pode executar Liquibase antes do runner:

```powershell
.\mvnw.cmd -B "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=18014 --spring.config.additional-location=file:./src/test/resources/application-test.properties --spring.config.import=optional:file:./target/aula14-no-external-config.properties --spring.datasource.url=$env:DB_TEST_URL --spring.liquibase.url=$env:DB_TEST_URL --spring.liquibase.default-schema=public --app.security.initial-admin.enabled=true" spring-boot:run
```

Confirmar criação e interromper a aplicação. Idempotência com flag ativa requer execução controlada
separada. Para uso normal/Newman, iniciar com flag explicitamente false:

```powershell
$env:INITIAL_ADMIN_ENABLED = 'false'
.\mvnw.cmd -B "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=18014 --spring.config.additional-location=file:./src/test/resources/application-test.properties --spring.config.import=optional:file:./target/aula14-no-external-config.properties --spring.datasource.url=$env:DB_TEST_URL --spring.liquibase.url=$env:DB_TEST_URL --spring.liquibase.default-schema=public" spring-boot:run
```

Após comprovar identidade do DataSource, outra sessão privada com os mesmos DB_TEST_* e as credenciais
Newman autorizadas poderá executar:

```powershell
node docs/aula14/EXECUTAR-NEWMAN.cjs
```

AULA14_ALLOW_TEST_WRITES=SIM é trava operacional, não substitui autorização. O finally faz DELETEs;
a autorização Newman precisa incluir criação, alteração, exclusão e conferência no banco identificado.
A pasta 11, último administrador, fica fora do runner comum e exige laboratório específico.
Depois confirmar ausência das fixtures e preservação do administrador, encerrar API e remover segredos
da sessão privada, sem modificar `.env` ou apagar registros de desenvolvimento.

## Parecer histórico do Checkpoint 7C

**No 7C, o isolamento real não estava confirmado e não se autorizava avançar.** Havia configuração de teste separada,
mas não comprovação do destino runtime, permissões ou estado do PostgreSQL.
Exigem autorização: disponibilizar infraestrutura descartável, conectar para consultas, migrar/testar,
iniciar API, provisionar administrador, executar Newman/limpeza e recuperar/descartar o ambiente.
Nenhuma dessas operações havia sido realizada no 7C. O estado posterior comprovado está abaixo.

## Isolamento efetivo e encerramento da validação — 7K

O diagnóstico original não comprovava isolamento. Posteriormente foi criado e verificado um cluster
nativo independente: PostgreSQL 17.6, identificador `7704384549022663752`, somente em
`127.0.0.1:55414`, banco `instrumento2026_test`, schema public. O servidor foi relançado manualmente
em PowerShell independente, fora de Job Objects. A instância original na porta 5432 e seus quatro
arquivos de configuração comparados foram preservados.

JUnit: **671 aprovados**, zero falhas, erros e ignorados, BUILD SUCCESS, saída 0, em
05/11/2026 às 09:12:29 -03:00. Newman: **156 chamadas HTTP e 487 assertions**, zero falhas,
saída 0, às 09:36:59 -03:00. Pastas 01–10 executadas; **pasta 11 não executada** no Newman comum.
Validação estática: 155 requisições, 163 scripts e 126 cenários históricos preservados.
Liquibase: 41 changeSets registrados, 12 tabelas e lock liberado.

Na API, DataSource e Liquibase utilizaram a mesma URL com `currentSchema=public`; log Hibernate e
correlação de dez conexões TCP/pg_stat_activity comprovaram o destino runtime. O runner não repassa
parâmetros JDBC ao psql: o schema SQL foi fixado por `PGOPTIONS=-c search_path=public`, sem outros
PG* conflitantes. Ele não comprova sozinho a identidade do cluster e conserva fallback para 5432;
destino explícito e verificação prévia continuam obrigatórios.

Para JUnit, usar `INITIAL_ADMIN_ENABLED=false` no ambiente, sem propriedade JVM global conflitante
com o teste isolado de habilitação. A suíte aprovada usou URL explícita sem query e schema Hikari
public; `currentSchema=public` foi empregado no startup HTTP. Os exemplos corrigidos acima também
usam esse parâmetro, sem representar literalmente o comando histórico da suíte. Neutralizar a
importação de .env e garantir que o caminho opcional indicado esteja ausente. Credenciais só no
ambiente do processo, com DataSource e Liquibase correspondentes. Não usar connection-init-sql.

A limpeza Newman confirmou zero fixtures e zero registros comerciais remanescentes. Contas 225/226
e Pessoas 639/640 de preparação permaneceram externas ao inventário. A API foi encerrada; cluster
preservado. Nova escrita, recuperação ou descarte exige autorização própria. Não repetir a suíte
após essa preparação sem reavaliar suas fixtures fixas.

Mantêm-se os limites de inativação indireta de Pessoa, contagem administrativa sem serialização,
JWTs anteriores até expirar e inventário de limpeza em memória. Não se afirma rollback físico do
bootstrap nem reinicialização real idempotente. Evidências: [EVIDENCIAS.md](EVIDENCIAS.md).
