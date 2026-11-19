# Aula 17 — persistência e concorrência

Referência: [material do professor, seções 11, 16 e 29](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/17aula/17aula.md).

**Registro histórico: Ponto B revalidado em 18/11/2026 às 19:36:44 -03:00, com
BUILD SUCCESS, código de saída Maven 0 e 72 testes aprovados, zero falhas,
erros ou ignorados.** Os 47 changeSets, o lock livre e as fixtures foram
conferidos antes e depois. A revalidação está registrada na seção final.

Na primeira execução, o Ponto B foi implementado e os relatórios Surefire
registraram 72 casos aprovados. A migration 008 aplicou três changeSets,
passando de 44 para 47. O problema de captura impediu registrar o resultado final
do Maven naquela tentativa; esse histórico permanece abaixo. As duas execuções
não são somadas para apresentar 144 testes distintos.

## O que foi implementado

- `008-01-create-protecao-autenticacao`: tabela com id BIGINT identity,
  chave_login VARCHAR(64) obrigatória e única, falhas_consecutivas INTEGER
  obrigatório com default zero, janela_iniciada_em e bloqueada_ate opcionais
  como TIMESTAMP WITH TIME ZONE. PK `pk_protecao_autenticacao`, unicidade
  `uk_protecao_autenticacao_chave` e check
  `ck_protecao_autenticacao_falhas` para contador não negativo.
- `008-02-create-evento-seguranca`: id BIGINT identity; tipo VARCHAR(40) e
  ocorrido_em TIMESTAMP WITH TIME ZONE obrigatórios; ator_conta_id e alvo_conta_id
  BIGINT, identificador_hash VARCHAR(64) e endereco_origem VARCHAR(45) opcionais.
  PK `pk_evento_seguranca` e check `ck_evento_seguranca_tipo` com os 12 tipos do
  professor. IDs históricos sem FK para contas. A tabela foi validada por SQL;
  ainda não há entidade, enum ou serviço Java de auditoria.
- `008-03-index-evento-seguranca`: índices `idx_evento_seguranca_tipo_data`,
  `idx_evento_seguranca_ator_data` e `idx_evento_seguranca_alvo_data`, cada um
  seguido de ocorrido_em DESC. Não foi criado índice redundante para a chave
  única da proteção.

O master inclui a 008 depois da 007. Os três changeSets têm rollback explícito;
os checks usam SQL compatível com a edição adotada do Liquibase. **Nenhum rollback
foi executado.** As migrations 001 a 007 permaneceram sem alterações.

`ProtecaoAutenticacaoRepository.inserirSeAusente()` executa o INSERT nativo com
`ON CONFLICT (chave_login) DO NOTHING`; retorna 1 na criação e 0 no conflito.
`buscarParaAtualizacao()` usa JPQL e PESSIMISTIC_WRITE. O chamador deve manter
upsert, consulta e alteração dentro da mesma transação. Os testes usam
TransactionTemplate; o serviço de proteção ainda não foi implementado.

As regras temporais e o mapeamento de `ProtecaoAutenticacao` foram preservados.
Suas assinaturas de arquivo foram comparadas antes/depois, assim como as das
propriedades e da configuração do Ponto A.

## Destino e preservação dos dados

Antes de executar, consultas psql com conexão explícita e modo somente leitura
confirmaram o destino. As assinaturas também coincidiram com a evidência final
da Aula 16, antes de iniciar o Maven.

| Verificação | Antes | Depois |
|---|---|---|
| Profile do comando | test | test |
| DataSource e Liquibase | jdbc:postgresql://127.0.0.1:5432/instrumento2026_test | Mesmo destino |
| Servidor / schema efetivos | 127.0.0.1:5432 / public | Mesmo destino |
| ChangeSets | 44 | 47 |
| Lock Liquibase | false | false |
| Pessoas / contas / vínculos de perfil / sessões | 3 / 3 / 4 / 11 | 3 / 3 / 4 / 11 |
| Linhas nas novas tabelas, após os testes | Não existiam | 0 proteções / 0 eventos |

Assinaturas agregadas das linhas completas, calculadas no PostgreSQL sem exibir
conteúdo sensível; os valores foram iguais antes e depois:

| Grupo preservado | Assinatura MD5 antes = depois |
|---|---|
| Pessoas 9742–9744 | `3e9130574fcec13f6a834dd1d9f4c0ae` |
| Contas 2511–2513 | `891c5c0318aafa150c6f85c1a896d758` |
| Quatro vínculos dessas contas | `30824b0c8fd0f4025f21e4e26f9c46f9` |
| Sessões 70–80 | `bfd54d6a8a71258dc1a5e15d9520c7c6` |
| 44 linhas anteriores do databasechangelog | `4ff1593d0b4915c956edac9b26b674c7` |

Essas assinaturas servem à comparação dos snapshots, não são prova de não repúdio.
Não são hashes individuais de senhas ou tokens. Os snapshots ficam em target.

As URLs e schemas foram impostos no comando para DataSource e Liquibase.
A importação Spring de .env foi substituída por um arquivo opcional inexistente,
cuja ausência foi conferida. As credenciais de teste foram lidas privadamente em
memória e fornecidas pelo ambiente temporário, restaurado em finally. Nenhuma
senha foi passada como argumento Maven. Overrides Maven/Java/Spring conflitantes
foram verificados antes do run. INITIAL_ADMIN_ENABLED=false ficou no ambiente,
sem propriedade global de bootstrap. SQL e bind logging ficaram desabilitados.

Não houve acesso ao banco de desenvolvimento. Os testes novos não alteram
pessoas, contas ou sessões. Os testes de schema/repository e a regressão de JPA
usam rollback de suas próprias transações. Os concorrentes usam chaves aleatórias
exclusivas e removem somente sua proteção por chave, depois de encerrar as threads.
Não houve TRUNCATE, limpeza global ou remoção das fixtures anteriores.

## Comando e resultados

Com o ambiente privado preparado e o preflight aprovado, foi executado uma única
vez o comando abaixo, com saída direcionada a target/aula17-ponto-b.log:

```powershell
.\mvnw.cmd -B `
  '-Dtest=ProtecaoAutenticacaoPersistenciaTest,ProtecaoAutenticacaoRepositoryTest,ProtecaoAutenticacaoConcorrenciaTest,SessaoRefreshTokenPersistenciaTest,PersistenciaJpaTest,ProtecaoAutenticacaoTest,ProtecaoAutenticacaoPropertiesTest' `
  '-Dsurefire.skipAfterFailureCount=1' `
  '-Dspring.profiles.active=test' `
  '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.datasource.hikari.schema=public' `
  '-Dspring.liquibase.default-schema=public' `
  '-Dspring.config.import=optional:file:./target/aula17-sem-config-externa.properties' `
  '-Dlogging.level.liquibase=INFO' `
  '-Dlogging.level.org.hibernate.SQL=OFF' `
  '-Dlogging.level.org.hibernate.orm.jdbc.bind=OFF' `
  '-Djacoco.destFile=target/aula17-ponto-b.exec' `
  '-Djacoco.append=false' test
```

| Classe | Executados e aprovados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| ProtecaoAutenticacaoPropertiesTest | 18 | 0 | 0 | 0 |
| ProtecaoAutenticacaoTest | 15 | 0 | 0 | 0 |
| PersistenciaJpaTest | 2 | 0 | 0 | 0 |
| ProtecaoAutenticacaoPersistenciaTest | 24 | 0 | 0 | 0 |
| ProtecaoAutenticacaoRepositoryTest | 2 | 0 | 0 | 0 |
| ProtecaoAutenticacaoConcorrenciaTest | 2 | 0 | 0 | 0 |
| SessaoRefreshTokenPersistenciaTest | 9 | 0 | 0 | 0 |
| **Total** | **72** | **0** | **0** | **0** |

Os sete XMLs Surefire têm atualização desta execução, entre 16:14:30 e 16:15:11
de 18/11/2026 (-03:00). A contagem não inclui relatórios de outras classes ou
execuções históricas. A opção fail-fast não ignorou nenhum teste.

O log registrou os três changeSets aplicados e a liberação do lock:

```text
Run:                          3
Previously run:              44
Total change sets:           47
Update command completed successfully.
Successfully released change log lock
```

O Hibernate inicializou com ddl-auto=validate e identificou PostgreSQL 17.6,
instrumento2026_test/public e isolamento READ_COMMITTED. A tabela de eventos,
sem entidade Java nesta etapa, foi conferida por metadados e operações SQL.

## Evidência concorrente

- `upsertsConcorrentesDevemCriarSomenteUmaProtecao`: duas conexões com PIDs
  diferentes; a primeira mantém sua inserção sem commit e a segunda tenta o
  mesmo upsert. pg_stat_activity e pg_blocking_pids confirmam a espera antes
  de liberar a primeira. Os retornos são 1 e 0; nova transação encontra uma
  única linha com contador zero.
- `lockDevePreservarDuasFalhasConcorrentesDeTresParaCinco`: preparação com três
  falhas confirmadas. A primeira transação bloqueia a linha, registra a quarta
  e faz flush; a segunda é observada aguardando o lock. Após liberar a primeira,
  a segunda registra a quinta. A leitura final em nova transação confirma cinco
  falhas, janela original e bloqueio até INICIO + 15 minutos. Somente a segunda
  chamada retorna true para ativação.

Não existe transação de teste externa nesses dois casos. A coordenação usa
latches, prazos finitos e consultas ao estado real do PostgreSQL, sem Thread.sleep.
Os XMLs registram aprovação dos dois métodos. Isso comprova os cenários descritos,
não todas as intercalações possíveis. A comprovação de um único evento persistido
de ativação continua para a etapa de auditoria: ainda não há gravação de eventos
pela aplicação.

## Problema na captura e limites — primeira tentativa

A primeira tentativa de chamar diretamente o script auxiliar foi recusada pela
ExecutionPolicy do PowerShell, antes de acessar banco ou iniciar Maven. A execução
foi feita em processo separado com ExecutionPolicy Bypass, sem mudar a política
persistente do sistema.

Durante a execução dos testes, o aviso do Mockito sobre autoanexação foi emitido
em stderr. O PowerShell 5, com ErrorActionPreference=Stop, tratou essa saída como
NativeCommandError ao capturá-la e encerrou o script auxiliar com código 1.
O arquivo que deveria guardar o código final do Maven não foi criado, e o log
capturado não contém a conclusão do build. Por isso **não se afirma BUILD SUCCESS
nem código Maven 0**. O código 1 observado é do script de captura, não uma falha
atribuída às asserções: os sete relatórios novos registram os 72 casos aprovados.
Não houve repetição automática ou correção de produção após esse problema.

A consulta final somente leitura confirmou 47 changeSets, lock livre, preservação
das fixtures e ausência de resíduos nas duas novas tabelas. A busca pelos valores
privados da senha de teste e JWT_SECRET não encontrou correspondências no log.

Schema, constraints, upsert e os cenários concorrentes tinham evidência aprovada.
Naquela tentativa permaneceu a limitação de captura da saída final do Maven; uma nova
execução precisa considerar que a 008 já está aplicada e deve corrigir somente
o tratamento de stderr do script auxiliar. Não há evidência de erro que exija
alterar código de produção. Não houve suíte global, Newman, report/check global
do JaCoCo, alteração do login, commit, push ou tag. Aguarda-se revisão antes do
Ponto C.

## Arquivos desta etapa

Criados:

- `src/main/resources/db/changelog/changes/008-auditoria-protecao-autenticacao.yaml`
- `src/main/java/com/curso/instrumento2026/repository/ProtecaoAutenticacaoRepository.java`
- `src/test/java/com/curso/instrumento2026/ProtecaoAutenticacaoPersistenciaTest.java`
- `src/test/java/com/curso/instrumento2026/repository/ProtecaoAutenticacaoRepositoryTest.java`
- `src/test/java/com/curso/instrumento2026/repository/ProtecaoAutenticacaoConcorrenciaTest.java`
- `src/test/java/com/curso/instrumento2026/support/ProtecaoAutenticacaoPersistenciaSupport.java`
- Este documento.

Modificados:

- `src/main/resources/db/changelog/db.changelog-master.yaml`
- `src/test/java/com/curso/instrumento2026/SessaoRefreshTokenPersistenciaTest.java`

O suporte compartilhado concentra as verificações de destino dos três testes
novos. A regressão da sessão mantém as três entradas da 007 e as 41 anteriores,
acrescentando três da 008 e o total 47. Os arquivos locais somam 47 changeSets;
os 39 do material correspondem ao projeto de referência.

Artefatos locais ignorados pelo Git: target/aula17-ponto-b-executar.ps1,
snapshots, assinaturas, resumo dos testes, log, coleta JaCoCo e relatórios Surefire.
Os arquivos pendentes do Ponto A foram preservados e não são mudanças novas do B.

## Revalidação focal — aprovada em 18/11/2026

A autorização desta etapa limitou-se à pendência da captura. Nenhum código Java,
teste, repository, migration, POM ou configuração foi alterado. As assinaturas
SHA-256 desses arquivos foram registradas antes e comparadas depois, sem diferenças.
Somente este documento foi atualizado entre os arquivos candidatos ao versionamento.

### Preflight e captura

Antes da nova execução, consultas psql somente leitura confirmaram novamente
instrumento2026_test, host 127.0.0.1, porta 5432 e schema public. Foram conferidos
os 47 changeSets, os três IDs previstos da 008 e o lock false. As assinaturas
das fixtures coincidiram com o estado final da primeira tentativa.

DataSource e Liquibase continuaram explicitamente direcionados à mesma URL de
teste e ao schema public. O profile foi test. A importação Spring de .env foi
neutralizada com o mesmo arquivo opcional inexistente, conferido antes da execução.
Credenciais permaneceram no ambiente privado temporário, restaurado em finally;
INITIAL_ADMIN_ENABLED=false ficou somente no ambiente, sem override global de
bootstrap. Não havia overrides Maven/Java/Spring conflitantes.

Foi executada uma única repetição das mesmas sete classes, com os mesmos argumentos
da seção de comando acima, alterando somente o destino da coleta para
`target/aula17-ponto-b-revalidacao.exec`. O comando terminou em `test`, sem clean,
verify, suíte global ou Newman.

A captura foi feita por `System.Diagnostics.ProcessStartInfo`, iniciando cmd.exe
com `/d /s /c`. O redirecionamento `> arquivo.log 2>&1` ficou dentro do comando
do processo filho. Assim, o PowerShell não recebeu stderr como ErrorRecord.
O processo usou `UseShellExecute=false` e `CreateNoWindow=true`; após
`WaitForExit()`, o script leu diretamente `Process.ExitCode` e o gravou em arquivo.
Não foi deduzido sucesso pela ausência de exceção ou apenas pelos XMLs.

O script auxiliar separado e os novos artefatos ficam em target, ignorado pelo
Git. O script e o log da primeira tentativa foram preservados. Nenhuma política
persistente de execução do PowerShell foi modificada.

### Resultado real

Encerramento: **18/11/2026 19:36:44 -03:00**. Duração Maven: **1 minuto e 7 segundos**.
**Código de saída Maven: 0. Resultado: BUILD SUCCESS.**

```text
Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Total time:  01:07 min
Finished at: 2026-11-18T19:36:44-03:00
```

| Classe | Executados e aprovados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|
| ProtecaoAutenticacaoPropertiesTest | 18 | 0 | 0 | 0 |
| ProtecaoAutenticacaoTest | 15 | 0 | 0 | 0 |
| PersistenciaJpaTest | 2 | 0 | 0 | 0 |
| ProtecaoAutenticacaoPersistenciaTest | 24 | 0 | 0 | 0 |
| ProtecaoAutenticacaoRepositoryTest | 2 | 0 | 0 | 0 |
| ProtecaoAutenticacaoConcorrenciaTest | 2 | 0 | 0 | 0 |
| SessaoRefreshTokenPersistenciaTest | 9 | 0 | 0 | 0 |
| **Total desta revalidação** | **72** | **0** | **0** | **0** |

Os sete XMLs foram atualizados nesta execução, entre 19:35:52 e 19:36:44 -03:00.
Os dois casos concorrentes passaram novamente, com as verificações de espera
no PostgreSQL e leitura final em nova transação. Não houve falha inesperada.
O aviso do Mockito não impediu a captura do resultado nem alterou o código de saída.

### Banco e dados depois da execução

O Liquibase reconheceu a migration já aplicada e não executou novos changeSets:

```text
Database is up to date, no changesets to execute
Run:                          0
Previously run:              47
Total change sets:           47
```

| Verificação | Antes | Depois |
|---|---|---|
| Banco / host / porta / schema | instrumento2026_test / 127.0.0.1 / 5432 / public | Igual |
| ChangeSets | 47 | 47 |
| Lock Liquibase | false | false |
| Pessoas / contas / vínculos / sessões | 3 / 3 / 4 / 11 | 3 / 3 / 4 / 11 |
| Proteções / eventos remanescentes | 0 / 0 | 0 / 0 |
| Assinaturas das quatro classes de fixtures | Iguais às registradas acima | Preservadas |
| Assinatura dos 44 changeSets anteriores | `4ff1593d0b4915c956edac9b26b674c7` | Igual |
| Assinatura de todos os 47 changeSets | `7ac0f241437b588666800dd0fadb805b` | Igual |

As pessoas 9742–9744, contas 2511–2513, seus quatro vínculos e sessões 70–80
permaneceram intactos. Não houve limpeza global ou remoção dessas fixtures.
Somente a limpeza restrita dos dados sintéticos já prevista nos testes foi usada.
A senha privada de teste e JWT_SECRET não foram encontrados no log.

Evidências locais desta execução:

- `target/aula17-ponto-b-revalidacao.log`
- `target/aula17-ponto-b-revalidacao-exit.txt`: valor 0
- `target/aula17-ponto-b-revalidacao-resultados.json`
- `target/aula17-ponto-b-revalidacao-antes.json` e `-depois.json`
- `target/aula17-ponto-b-revalidacao-changelog-antes.json` e `-changelog-depois.json`
- `target/aula17-ponto-b-revalidacao-fontes-antes.json`
- Relatórios Surefire das sete classes.

**A pendência de validação do Ponto B está resolvida.** A aprovação é focal e não
substitui a futura validação global da Aula 17. Nenhum serviço de proteção,
integração ao login, evento Java ou endpoint foi implementado nesta revalidação.
Não houve commit, push ou tag. Aguarda-se revisão antes do Ponto C.

## Ponto E — roteiro e evidências

O laboratório usa a pasta **11 — Proteção e auditoria da autenticação** da
[coleção de autenticação e sessões](../aula16/Instrumento%202026%20-%20Aula%2016.postman_collection.json).
O marco do `instrumento2026` é de 47 changeSets, incluindo os três da migration
008. O material do professor usa 39 no projeto de referência.

### Política e matriz de ameaças

Os valores padrão são cinco falhas em uma janela de 15 minutos e bloqueio
temporário por 15 minutos. No catálogo acadêmico, cinco tentativas permitem
corrigir erros de digitação e demonstrar o bloqueio em uma aula. A janela agrupa
erros próximos sem acumular falhas ocasionais ao longo do dia. O prazo limita
tentativas repetidas e permite ao operador voltar a entrar sem intervenção.
Em uso real, a escolha precisa considerar os horários de atendimento e o
impacto de bloquear um operador legítimo.

| Ameaça | Controle implementado | Evidência | Risco residual |
|---|---|---|---|
| Força bruta no login de um operador | Contador por login normalizado e bloqueio temporário | Quinta falha bloqueia; a senha correta também recebe 401 durante o bloqueio | Tentativas lentas podem se distribuir entre janelas |
| Reutilização de credenciais vazadas | Limite por login e trilha dos resultados | Eventos de sucesso e falha com campos restritos | Uma credencial válida pode funcionar na primeira tentativa |
| Pulverização de senhas entre várias contas | Contadores independentes e eventos de falha | Consulta filtrada e agregação por origem | Poucas tentativas por conta podem ficar abaixo do limite; limitação de volume na borda é um controle complementar |
| Enumeração de contas | Mesmo erro HTTP e correlação interna por hash | Login inexistente, senha incorreta e bloqueio usam o mesmo 401 | Diferenças de tempo podem continuar observáveis |
| Bloqueio abusivo de um operador | Bloqueio com expiração e evento de ativação | Testes da fronteira e da senha correta recusada | Um terceiro pode bloquear temporariamente uma conta conhecida |
| Injeção ou exposição de dados na auditoria | Campos estruturados, origem validada, DTO restrito e consulta administrativa | Testes de CR/LF, hash inválido, ausência de segredos e acesso do operador negado | O acesso ao banco pode permitir correlação por hash; origem e retenção exigem controle |
| Crescimento das tabelas por logins distintos | Diagnóstico por contagem de eventos e filtros | Consultas abaixo | O contador por login não limita o volume global; retenção e limites na borda ficam como propostas |

### Linha do tempo

| Horário ilustrativo | Tentativa com o mesmo login descartável | Resultado esperado |
|---|---|---|
| 10:00 a 10:03 | Falhas 1 a 4 | Quatro 401 e quatro `LOGIN_FALHA` |
| 10:04 | Falha 5 | 401, quinto `LOGIN_FALHA` e um `BLOQUEIO_TEMPORARIO_ATIVADO`; bloqueio até 10:19 |
| 10:05 | Senha correta | 401 e `LOGIN_RECUSADO_BLOQUEIO`; o contador continua em cinco |
| 10:19 | Senha correta em uma demonstração opcional | 200 com o par de tokens, estado limpo e `LOGIN_SUCESSO` |

Uma tentativa com outro identificador inexistente recebe o mesmo erro genérico
e gera `LOGIN_FALHA` em outro contador. Compare o status e a mensagem, que são
iguais. O timestamp varia entre respostas.

### Exercício de concorrência

`ProtecaoAutenticacaoConcorrenciaTest` verifica o lock no PostgreSQL.
`AutenticacaoJwtTest.falhasConcorrentesDevemAtivarBloqueioUmaVezERecusarSenhaCorreta`
começa com três falhas e coordena duas requisições simultâneas por uma barreira.
Cada requisição usa sua própria transação. O upsert assegura a existência de
uma única linha por hash e o lock pessimista serializa a verificação, a
atualização e o registro da ativação. A segunda transação lê o estado
confirmado pela primeira. O resultado esperado é cinco falhas, um evento de
ativação e a recusa subsequente da senha correta sem chamar o autenticador.

### Consultas somente leitura

No `psql`, defina `login_fixture` na sessão para o login descartável gerado
pela pasta de preparação. Repita com o sufixo `.inexistente` para examinar
o segundo cenário. Mantenha o valor fora das evidências compartilhadas. As
consultas usam o hash como filtro e exibem somente os campos necessários.

```sql
SELECT falhas_consecutivas, janela_iniciada_em, bloqueada_ate
FROM protecao_autenticacao
WHERE chave_login = encode(sha256(convert_to(lower(btrim(:'login_fixture')), 'UTF8')), 'hex');

SELECT id, tipo, ocorrido_em, ator_conta_id, alvo_conta_id, endereco_origem
FROM evento_seguranca
WHERE identificador_hash = encode(sha256(convert_to(lower(btrim(:'login_fixture')), 'UTF8')), 'hex')
ORDER BY ocorrido_em, id;

SELECT endereco_origem, count(*) AS falhas
FROM evento_seguranca
WHERE tipo = 'LOGIN_FALHA'
  AND identificador_hash = encode(sha256(convert_to(lower(btrim(:'login_fixture')), 'UTF8')), 'hex')
GROUP BY endereco_origem;
```

### Demonstração e validação final

1. Após autorização, confirme que a API aponta exclusivamente para
   `instrumento2026_test`, com 47 changeSets, lock livre e fixtures de
   referência preservadas. Configure `baseUrl`, `adminLogin` e
   `adminPassword` no ambiente privado do Postman.
2. Execute somente a pasta **01 — Preparação e contas próprias** para criar
   contas descartáveis novas e obter os Bearers. Em seguida, execute a pasta
   **11** separadamente e uma única vez. Ela usa `opLogin` para o bloqueio e
   `otherAccessToken` para demonstrar a negativa ao perfil operador.
3. Registre os sete 401 de login, os dois 200 de consulta administrativa e o
   403 do operador. Use as consultas para atribuir a ativação à fixture do
   cenário, pois a API também pode retornar eventos anteriores do mesmo tipo.
   Registre a linha do tempo e as contagens sem copiar credenciais ou hashes.
4. Registre o resultado dos testes focais e, depois de revisão e autorização,
   execute `clean verify` com o portão JaCoCo da Aula 15. Compare as
   assinaturas das fixtures e o estado do Liquibase antes e depois. O
   laboratório Postman e a validação global ficam pendentes nesta etapa.

Nesta preparação, 21 testes focais passaram. A comparação histórica foi
preservada e o OpenAPI confirmou os parâmetros `tipo`, `page` e `size`, a
resposta paginada, os 401/403 e a descrição do bloqueio no login. Os testes
também confirmaram rejeição de CR/LF e de hash inválido na entidade e
ausência de credenciais nos eventos consultados diretamente no banco.
As assinaturas das fixtures e dos 47 changeSets foram idênticas antes
e depois, com lock livre e sem resíduos em proteção ou auditoria.
O log local está em `target/aula17-ponto-e-focal.log`.

### Validação final em 19/11/2026

DataSource e Liquibase usaram exclusivamente
`jdbc:postgresql://127.0.0.1:5432/instrumento2026_test`, schema `public`, com
profile `test` e a importação externa apontada para o arquivo opcional ausente
`target/aula17-sem-config-externa.properties`. Antes do laboratório, a
conferência confirmou 47 changeSets, lock livre e as mesmas assinaturas das
3 pessoas, 3 contas, 4 vínculos de perfil e 11 sessões de referência.

#### Laboratório Postman

Newman 6.2.3 executou a coleção canônica contra a API local em
`127.0.0.1:18017`. A pasta 01 preparou duas contas operadoras e usou um
administrador descartável provisionado para esta execução. A pasta 11 rodou
em seguida, com **16 requisições no total, 26 asserções e zero falhas**.

| Ordem | Cenário | Resultado |
|---|---|---|
| Preparação | Login administrativo, criação de duas pessoas e duas contas, login da outra conta | 200, 201, 201, 201, 201, 200 |
| 01 a 05 | Cinco senhas incorretas na mesma conta descartável | 401 em cada tentativa |
| 06 | Senha correta durante o bloqueio | 401 |
| 07 | Login inexistente | 401 |
| 08 | Administrador filtra `LOGIN_FALHA` | 200, página com eventos do tipo solicitado |
| 09 | Administrador filtra `BLOQUEIO_TEMPORARIO_ATIVADO` | 200, página com eventos do tipo solicitado |
| 10 | Operador tenta consultar a auditoria | 403 |

Os sete 401 de login tiveram a mensagem `Login ou senha inválidos`. As
consultas SQL por fixture confirmaram contador **5**, primeira falha às
14:23:11.818867, quinta falha às 14:23:12.604823 e bloqueio até
14:38:12.604823 (UTC−03:00). Foram persistidos cinco `LOGIN_FALHA`, um
`BLOQUEIO_TEMPORARIO_ATIVADO` e um `LOGIN_RECUSADO_BLOQUEIO` para a conta
alvo, além de um `LOGIN_FALHA` para o identificador inexistente.

A limpeza usou somente os e-mails, IDs e chaves das fixtures descartáveis
desta execução. Após a limpeza, as tabelas de proteção e auditoria voltaram
a zero linhas, as assinaturas das fixtures de referência e dos 47
changeSets permaneceram idênticas e o lock do Liquibase estava livre.
Evidências sanitizadas em `target/aula17-final-postman-results.json` e
`target/aula17-final-postman-sql.json`.

#### Maven e JaCoCo

`.\mvnw.cmd clean verify` terminou às 14:25:12 (UTC−03:00) em 1 min 07 s.
Resultado **BUILD SUCCESS**, código de saída **0**. O Surefire executou
**841 testes, zero falhas, zero erros e zero ignorados**. O JaCoCo 0.8.16
analisou 113 classes e aprovou os limites configurados no projeto.

| Métrica | Cobertos / total | Cobertura | Mínimo do projeto |
|---|---:|---:|---:|
| Linhas | 1.561 / 1.609 | 97,02% | 70% |
| Branches | 561 / 614 | 91,37% | 40% |

Após o Maven, os snapshots de fixtures e do changelog coincidiram com os de
antes da execução. O destino permaneceu `instrumento2026_test` em
`127.0.0.1:5432/public`, com 47 changeSets, lock livre e zero linhas nas
tabelas de proteção e auditoria. A limpeza do laboratório removeu somente
14 eventos, 4 contadores, 3 contas e 3 pessoas descartáveis. As sessões
dessas contas foram removidas pela cascata existente.

Evidências locais em `target/aula17-final-maven.log`,
`target/aula17-final-maven-exit.txt`, `target/site/jacoco/index.html` e nos
snapshots `target/aula17-final-maven-antes.json` e
`target/aula17-final-maven-depois.json`. O Git permanece em revisão, sem
commit, push ou tag desta validação.
