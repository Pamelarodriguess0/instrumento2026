# Aula 14 — Plano da instância PostgreSQL descartável

> Atualização 7K, 05/11/2026: o plano 7D abaixo é histórico. A infraestrutura foi posteriormente
> criada e a validação concluída. O registro final distingue os resultados reais das propostas.

Checkpoint 7D, 04/11/2026. **Plano, não execução de infraestrutura.** Complementa o
[diagnóstico de conexões](AMBIENTE-POSTGRESQL.md). As únicas consultas executadas foram de arquivos,
registro de programas, serviços, processos, portas e versões/ajuda de executáveis.
Nenhum comando initdb de criação, pg_ctl start, SQL, migração, teste de integração, aplicação ou Newman
foi executado. Nenhuma credencial, `.env`, instalação ou serviço foi alterado.

## Infraestrutura encontrada

Conferência de serviços/portas em **04/11/2026 às 14:45:52 -03:00**:

| Item | Resultado observado |
|---|---|
| docker e docker-compose no PATH | Não encontrados |
| Docker Desktop/CLI/Compose em caminhos padrão e plugin do usuário | Não encontrados |
| Programas instalados nos registros consultados | PostgreSQL 17, versão 17.6-1; nenhum Docker encontrado |
| Serviços/processos Docker consultados | Nenhum encontrado |
| Versão Docker/Compose e engine | Não obtidas: não há ferramenta/engine disponível identificado nesta sessão |
| PostgreSQL existente | Serviço postgresql-x64-17 em Running |
| Porta existente 5432 | Listening, PID 7132, interfaces IPv4/IPv6 |
| Portas propostas 55414 e 18014 | Sem listeners na consulta; não estão reservadas |
| initdb, pg_ctl e psql | Executáveis existentes em C:\Program Files\PostgreSQL\17\bin; todos reportaram 17.6 |
| Projeto | Nenhum Dockerfile, Compose ou configuração de container equivalente encontrado; sem Testcontainers |

A ausência nos locais consultados não prova ausência de qualquer instalação portátil em todo o computador.
Entretanto, Docker/Compose não estão utilizáveis pelos comandos/localizações inspecionados. Não foi
tentado instalar, iniciar Docker Desktop/daemon ou trocar contextos Docker. Não há versão a afirmar.

## Estratégia recomendada neste computador

Reutilizar **somente os binários já instalados**, criando posteriormente um cluster independente em
diretório temporário novo e exclusivo. Isso não altera a instalação, configurações, dados ou serviço
PostgreSQL existentes. A existência dos executáveis/versões foi confirmada; criação e inicialização
continuam não testadas e exigem autorização.

- Dados: diretório novo sob TEMP, com identificador aleatório exclusivo, fora da instalação e do repositório.
  Não usar target para PGDATA: um Maven clean poderia apagar o cluster em execução.
- Escuta: somente 127.0.0.1:55414, sem usar a porta 5432 ou registrar serviço Windows.
- Banco: instrumento2026_test, nome exigido pelo runner atual.
- Usuários: administrador exclusivo do cluster temporário e usuário de aplicação exclusivo, sem superuser,
  CREATEDB ou CREATEROLE. Não reutilizar credenciais do desenvolvimento.
- Autenticação: SCRAM, senha do administrador solicitada interativamente; senha da aplicação definida
  por mecanismo privado e fornecida ao Maven/API/Newman somente no ambiente da sessão.
- Logs e dados fora do Git. Não limpar TEMP ou apagar arquivos enquanto esse cluster estiver ativo.
  Parada, recuperação e descarte somente após identificação inequívoca e autorização específica.

Rodar sob usuário comum com permissão no diretório temporário. Não elevar privilégios silenciosamente,
não registrar serviço e nunca executar comandos de manutenção no diretório da instalação existente.

## Opção Docker, apenas se vier a ficar disponível

Não é a opção imediata, pois exigiria instalação/ativação não autorizada nesta etapa. Se Docker for
disponibilizado posteriormente por decisão separada, usar engine/contexto local confirmado, PostgreSQL
17.6 com imagem/digest verificados, bind 127.0.0.1:55414:5432 e banco/usuário exclusivos.
Credenciais viriam de variáveis privadas, sem valores no Compose ou em arquivos versionados.
Usar volume novo com nome exclusivo por execução, sem external volume ou bind de diretórios existentes.
Não presumir que um volume com nome conhecido está vazio; bloquear se já existir. Remoção do volume
exigiria autorização posterior, restrita a esse recurso. Nada disso foi criado ou validado neste checkpoint.

## Bloqueios antes de criar infraestrutura

Não há como comprovar a identidade operacional de um servidor que ainda não foi criado. Separar duas
autorizações: criar a infraestrutura exclusiva, depois validar sua identidade por leitura antes de
permitir migrações ou escritas da aplicação.

Antes da criação, parar se qualquer condição falhar:

1. Porta 55414 ocupada; não reaproveitar automaticamente o listener existente nem cair para 5432.
2. Diretório calculado já existe ou não está sob a raiz temporária esperada; não inicializar nem limpar.
3. Binários não são os executáveis 17.6 identificados, faltam permissões ou há exigência de instalação/elevação.
4. Variáveis de conexão ambíguas: PGDATA, PGHOSTADDR, PGPORT, PGOPTIONS, PGSERVICE/PGSERVICEFILE,
   profiles adicionais, SPRING_APPLICATION_JSON, parâmetros de IDE/JVM ou configurações externas conflitantes.
5. Senhas ausentes, iguais às credenciais de desenvolvimento, ou com valores reais em arquivos versionáveis.
6. Identidade, inventário e proteção da instalação/porta 5432 não puderem ser estabelecidos.

As portas devem ser conferidas novamente no momento da execução. Ausência de listener agora não garante
disponibilidade futura. Usar sempre -D explícito nos comandos nativos, nunca depender de PGDATA herdado.

## Comandos propostos para o cluster nativo — NÃO EXECUTADOS

São exemplos sujeitos a autorização e substituição de placeholders. Nenhum deles foi invocado nesta etapa.
Não há comandos de exclusão de diretórios, DROP DATABASE, restart de serviço ou limpeza do desenvolvimento.

Escolha de diretório e portas, em uma futura sessão privada:

```powershell
$pgBin = 'C:\Program Files\PostgreSQL\17\bin'
$labRoot = Join-Path ([IO.Path]::GetTempPath()) ('instrumento2026-aula14-' + [guid]::NewGuid().ToString('N'))
$labData = Join-Path $labRoot 'data'
if (Test-Path -LiteralPath $labRoot) { throw 'Diretorio do laboratorio ja existe.' }
if (Get-NetTCPConnection -State Listen -LocalPort 55414 -ErrorAction SilentlyContinue) { throw 'Porta 55414 ocupada.' }
```

Somente depois da aprovação para **criar** o cluster: conferir o caminho absoluto calculado, criar a
raiz nova e inicializar. --pwprompt não publica senha nos argumentos ou em arquivo:

```powershell
New-Item -ItemType Directory -Path $labRoot -ErrorAction Stop
& "$pgBin\initdb.exe" -D $labData --username='<ADMIN_CLUSTER_EXCLUSIVO>' --auth=scram-sha-256 --pwprompt --encoding=UTF8 --locale=C
if ($LASTEXITCODE -ne 0) { throw 'Inicializacao falhou; nao iniciar servidor.' }
# O relancamento efetivamente utilizado foi em PowerShell aberto manualmente no Windows:
& "$pgBin\postgres.exe" -D $labData -h 127.0.0.1 -p 55414
```

Isso inicia um processo próprio, não um serviço Windows. Não usar register, restart, diretório de dados
existente ou `Start-Service`. Se um lançamento exigir Start-Process, utilizar WindowStyle Hidden.

Criar usuário/banco também é escrita de infraestrutura e exige autorização. Em sessão psql interativa
conectada explicitamente ao cluster novo, os passos planejados são:

```powershell
& "$pgBin\psql.exe" -X -W -h 127.0.0.1 -p 55414 -U '<ADMIN_CLUSTER_EXCLUSIVO>' -d postgres
```

```sql
CREATE ROLE <USUARIO_APP_EXCLUSIVO> LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE;
```

Definir sua senha pelo comando interativo `\password <USUARIO_APP_EXCLUSIVO>`, sem literal SQL com segredo.
Depois, ainda sob autorização de criação:

```sql
CREATE DATABASE instrumento2026_test OWNER <USUARIO_APP_EXCLUSIVO>;
```

Placeholders precisam ser substituídos por identificadores válidos exclusivos. Não usar os nomes ou
senhas do cluster de desenvolvimento. No 7D, a criação ainda não havia sido executada nem validada empiricamente.

## Comprovação antes de migrações e escritas da aplicação

Depois da infraestrutura autorizada, mas antes de Spring/Liquibase/testes:

1. pg_ctl -D do diretório exclusivo deve identificar o processo novo; porta/PID devem corresponder a ele.
2. Com administrador do **cluster temporário**, consultar data_directory e system_identifier e conferir
   o diretório exclusivo. Não consultar/controlar o serviço existente com esse administrador.
3. Com usuário da aplicação, confirmar banco, usuário, servidor, schema public e ausência de tabelas
   comerciais antes da primeira migração. O novo cluster não deve conter os bancos de desenvolvimento.
4. Identificar todas as fontes efetivas de DataSource e Liquibase. Parar diante de qualquer divergência.

Consultas preparadas, não executadas:

```sql
-- Administrador exclusivo do cluster temporário, com privilégios próprios desse cluster:
SHOW data_directory;
SELECT system_identifier FROM pg_control_system();

-- Usuário de aplicação no banco instrumento2026_test:
SELECT current_database(), current_user, current_schema(), current_schemas(false),
       inet_server_addr(), inet_server_port(), pg_postmaster_start_time();
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
```

Antes da API iniciar, a comprovação possível é de configuração e infraestrutura, não do DataSource runtime.
A inicialização da aplicação fica autorizada somente após essas verificações. Não existe endpoint público
de diagnóstico e nenhum será criado. Arquivos de configuração/versão isoladamente não provam destino.

## DataSource, Liquibase e ordem da validação

Uma sessão privada fornecerá os seguintes valores locais, sem mudar `.env`:

| Variável | Valor planejado |
|---|---|
| DB_TEST_URL | jdbc:postgresql://127.0.0.1:55414/instrumento2026_test?currentSchema=public |
| DB_TEST_USERNAME | <USUARIO_APP_EXCLUSIVO> |
| DB_TEST_PASSWORD | <SEGREDO_PRIVADO_DA_APLICACAO> |
| SPRING_DATASOURCE_USERNAME/PASSWORD | Mesmos valores privados de DB_TEST_USERNAME/PASSWORD |
| SPRING_LIQUIBASE_USER/PASSWORD | Mesmos valores privados de DB_TEST_USERNAME/PASSWORD |
| INITIAL_ADMIN_ENABLED | false normalmente |
| INITIAL_ADMIN_NAME/EMAIL/CPF/LOGIN/PASSWORD | Dados fictícios exclusivos; senha privada; somente na fase autorizada |
| AULA14_ADMIN_LOGIN/PASSWORD | Login e senha privados do administrador fictício provisionado |
| NEWMAN_BASE_URL | http://localhost:18014 |
| AULA14_ALLOW_TEST_WRITES | SIM somente na execução Newman autorizada |
| NEWMAN_MODULE_DIR; PSQL_PATH | Caminhos locais das ferramentas já disponíveis |

A senha deve vir de entrada privada/cofre para a memória da sessão, não de atribuição literal no histórico.
URLs não devem conter senhas ou schema alternativo. Conferir ausência de overrides conflitantes antes de
usar os comandos abaixo. Liquibase e DataSource recebem explicitamente a mesma URL; suas credenciais vêm
das variáveis privadas acima. Não usar dev, prod ou schema-reference junto de test.

### 1. Suíte completa antes do bootstrap

```powershell
$env:INITIAL_ADMIN_ENABLED = 'false'
.\mvnw.cmd -B "-Dspring.profiles.active=test" "-Dspring.datasource.url=$env:DB_TEST_URL" "-Dspring.liquibase.url=$env:DB_TEST_URL" "-Dspring.liquibase.default-schema=public" "-Dspring.config.import=optional:file:./target/aula14-no-external-config.properties" test
```

Esse comando escreve: inicializa o contexto, executa migrações pendentes e testes de integração. Exige
autorização própria. Registrar totais reais, conferir migrações/estado persistido e limpeza antes de seguir.
Não criar administrador antes: CPFs fixos das fixtures podem colidir com o provisionamento.

### 2. Bootstrap temporário, somente após a suíte

Fornecer INITIAL_ADMIN_* privadamente, então iniciar uma única instância com flag true:

```powershell
.\mvnw.cmd -B "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=18014 --spring.config.additional-location=file:./src/test/resources/application-test.properties --spring.config.import=optional:file:./target/aula14-no-external-config.properties --spring.datasource.url=$env:DB_TEST_URL --spring.liquibase.url=$env:DB_TEST_URL --spring.liquibase.default-schema=public --app.security.initial-admin.enabled=true" spring-boot:run
```

Confirmar Pessoa/conta/perfis e login sem divulgar segredo. Parar a API. Testar idempotência somente sob
autorização de execução adicional. Para uso normal, repetir a inicialização com a flag final explicitamente
false. Não deixar INITIAL_ADMIN_ENABLED=true na sessão; não alterar `.env` nem promover conta existente.

### 3. API e Newman

Iniciar a API test com os mesmos argumentos de URL/schema, **flag false**. Identificar processo da
porta 18014. Antes de Newman, conferir no DataSource real, por inspeção local confiável, a conexão efetiva
e uma consulta somente leitura com `pg_backend_pid()`, banco, usuário, schema e identificação do servidor.
Correlacionar esse backend no pg_stat_activity do cluster temporário já identificado pelo administrador.
Usar somente a permissão do administrador desse laboratório, não conceder privilégios no desenvolvimento.

Executar a mesma conferência na conexão SQL do runner, garantindo 127.0.0.1:55414, instrumento2026_test,
usuário exclusivo e schema public. O runner não importa `.env`: DB_TEST_* devem ser iguais nos dois
processos. Uma versão, porta HTTP ou login bem-sucedido isolados não bastam como prova.

Só depois dessa comprovação e da autorização de escritas/limpeza:

```powershell
node docs/aula14/EXECUTAR-NEWMAN.cjs
```

Executar pastas 01–10; não preparar pasta 11 no mesmo ambiente com administrador preexistente.
Confirmar limpeza por IDs capturados e preservação do administrador externo à coleção. Encerrar a API
temporária e posteriormente parar apenas o processo do diretório exclusivo. Recuperação/descarte requer
autorização separada; não há comando de remoção neste plano.

## Riscos restantes e pontos de parada

- Não foram comprovados privilégios do Windows, inicialização nativa ou compatibilidade de locale; falha
  não autoriza instalar programas, elevar permissões ou reutilizar a instância local.
- Porta livre pode ser ocupada depois; diretório temporário pode sofrer limpeza externa. Não executar
  utilitários de limpeza enquanto o laboratório estiver ativo.
- Sessões com overrides ou PGOPTIONS podem enviar conexão/schema ao lugar errado. Qualquer ambiguidade
  bloqueia início da aplicação e Newman.
- Liquibase escreve durante startup; não usar a aplicação como primeiro teste de destino incerto.
- Tokens não são revogados imediatamente; contagem administrativa e inativação de Pessoa mantêm os
  limites conhecidos. Criar outro cluster não corrige esses comportamentos.
- Falha abrupta pode impedir finally do Newman. Preservar inventário e planejar recuperação do laboratório.

## Autorizações necessárias

Permanecem não autorizados: criar diretório/cluster, iniciar processo PostgreSQL, conectar por SQL,
criar usuário/banco, migrar/testar, iniciar API, executar bootstrap, Newman/limpeza e descartar recursos.
Instalar/ativar Docker ou outro programa exigiria decisão adicional, mas não é necessário à estratégia
nativa proposta. Essas restrições descrevem o momento do 7D; as execuções posteriores tiveram
autorizações próprias e estão registradas abaixo. Não há autorização implícita para novas operações.

## Infraestrutura utilizada e resultados — atualização 7K

Foi criado um cluster nativo independente, PostgreSQL **17.6**, identificador **7704384549022663752**,
escutando somente em **127.0.0.1:55414**, banco **instrumento2026_test**, schema **public**.
A instância original na porta 5432 e seus arquivos de configuração comparados foram preservados.
O relançamento efetivo ocorreu em PowerShell manual independente, confirmado fora de Job Objects.
Não repetir Initialize ou Start no cluster existente. O exemplo de criação acima não autoriza recriá-lo.

O lançamento antigo por pg_ctl ficou aguardando canais herdados; o controlador terminou após parada
controlada. O auxiliar foi corrigido com redirecionamento e espera limitada, sem executar Start
novamente nessa correção. O servidor foi então iniciado manualmente e sua identidade confirmada por SQL.

JUnit concluiu em **05/11/2026 às 09:12:29 -03:00**: **671 aprovados**, zero falhas, erros e
ignorados, BUILD SUCCESS e código 0. Newman concluiu às **09:36:59 -03:00**: **156 chamadas HTTP,
487 assertions, zero falhas e código 0**. Pastas 01–10 executadas; **pasta 11 não executada** no
Newman comum. Validação estática: 155 requisições, 163 scripts e 126 cenários históricos preservados.
Liquibase: **41 changeSets**, 12 tabelas e lock liberado.

DataSource e Liquibase da API receberam URL explícita com `currentSchema=public`. Hibernate e a
correlação de dez conexões TCP com pg_stat_activity comprovaram o destino runtime. O runner extrai
host/porta/banco e não repassa currentSchema ao psql; utilizou-se `PGOPTIONS=-c search_path=public`,
sem outros PG* conflitantes. Ele não comprova sozinho o cluster e mantém fallback para 5432:
exigir configuração explícita e comparação externa antes da limpeza.

A suíte aprovada utilizou URL sem query e schema Hikari public. Os exemplos corrigidos usam o
parâmetro JDBC empregado na API e `INITIAL_ADMIN_ENABLED=false` no ambiente. Não usar propriedade
JVM global para desabilitar o bootstrap nos testes: ela interferiu no teste isolado de configuração.
Neutralizar importação de .env e garantir ausência do caminho opcional indicado. Credenciais devem
ficar somente em memória; não usar connection-init-sql, cuja aplicação tardia falhou no Hikari.

A limpeza confirmou **zero fixtures Newman e zero registros comerciais remanescentes**. Contas
225/226 e Pessoas 639/640 de preparação permaneceram preservadas fora do inventário da coleção.
A API foi encerrada; o cluster não foi descartado. Nova escrita, recuperação ou descarte exige
autorização. Não repetir a suíte contra esse estado sem planejamento das fixtures.

Continuam conhecidos: inativação indireta da Pessoa do último administrador, contagem concorrente
não serializada, authorities de JWTs antigos até expirar e inventário de limpeza em memória.
Não há evidência de rollback físico do bootstrap ou idempotência por reinicialização real.
Resultados e procedimento completo: [EVIDENCIAS.md](EVIDENCIAS.md) e [VALIDACAO-FINAL.md](VALIDACAO-FINAL.md).
