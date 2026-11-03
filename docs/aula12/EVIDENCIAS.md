# Testes e evidências — Aula 12

[Índice da aula](README.md) · [Segurança](SEGURANCA.md) · [Laboratório Postman](POSTMAN.md)

## Origem e limites dos registros

Consolidação documental em **03/11/2026**, atualizada no Checkpoint 8B com as evidências da validação pré-commit 8A. **Não foram reexecutados JUnit ou Newman nas etapas documentais 5B, 7C e 8B.** Java 21, Spring Boot 4.0.7, Spring Security 7.0.6 e PostgreSQL 17.6; execuções de integração no banco `instrumento2026_test`, profile `test`.

Os recortes essenciais são preservados neste documento. Logs e relatórios em `target` são arquivos locais gerados, ignorados pelo Git e sujeitos a limpeza/sobrescrita. Sua presença local não os transforma em entregáveis versionados. Não foram copiadas credenciais, cabeçalhos Authorization ou capturas de tela.

## Validação final mais recente — Checkpoint 8A

Execuções realizadas em **03/11/2026**, após as correções 7B, no PostgreSQL **17.6**, profile **test**. Horários em America/Sao_Paulo, offset -03:00.

| Execução | Horário de conclusão | Total | Aprovados | Falhas | Erros | Ignorados | Resultado |
|---|---|---:|---:|---:|---:|---:|---|
| Suíte JUnit completa | **10:44:40** | **480** | **480** | **0** | **0** | **0** | **BUILD SUCCESS** |
| Newman — requisições | **10:48:41** | **29** | **29** | **0** | **0** | — | Sucesso |
| Newman — assertions | **10:48:41** | **75** | **75** | **0** | **0** | — | Sucesso |

Recorte real do Maven:

```text
Tests run: 480, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
Finished at: 2026-11-03T10:44:40-03:00
```

Fonte local: `target/aula12-checkpoint8a-suite-completa.log`. Os quatro diagnósticos permaneceram ativos, com as expectativas originais, e passaram dentro desses 480 testes.

O Newman **6.2.3** executou novamente a coleção inalterada contra a aplicação local na porta **18012**, em duas fases: pastas 01–02 com **12 requisições e 32 assertions**, preparação SQL restrita à conta fictícia e pastas 03–07 com **17 requisições e 43 assertions**. Ambas as fases terminaram sem falhas ou erros. O registro `2026-11-03T13:48:41.025Z` corresponde a **03/11/2026 às 10:48:41 -03:00**.

Foram confirmados HTTP Basic válido (200), acesso anônimo/senha incorreta (401), troca da própria senha (204), rejeição da senha anterior (401), nova senha aceita (200), conta bloqueada (401) e Pessoa inativa (401). A reativação da Pessoa também restaurou sua autenticação (200). A conta de bloqueio havia autenticado antes da preparação SQL, e o banco confirmou **BLOQUEADA/ATIVO** antes do cenário negativo.

Fontes locais: `target/aula12-checkpoint8a-newman.log` e `target/aula12-checkpoint8a-newman-resultados.json`. Esses resultados posteriores às correções substituem, como validação mais recente, a situação descrita durante o 7C; a execução anterior 5A permanece preservada abaixo.

### Limpeza dos dados temporários

O runner removeu somente os dados próprios capturados nessa execução, com confirmação da transação. A consulta posterior ao PostgreSQL confirmou ausência de fixtures Postman remanescentes:

```text
LIMPEZA_SQL_8A: BEGIN; DELETE 3; DELETE 4; COMMIT; exit=0
contas_postman_restantes|0
pessoas_postman_restantes|0
```

Foram removidas **três contas e quatro Pessoas** temporárias. A aplicação temporária foi encerrada. Não houve alteração de código, testes, documentos ou coleção durante a validação 8A; os registros gerados ficaram em `target`.

Cada execução tem seus próprios totais. Não se somam os 480 testes de 8A aos 480 de 7B, nem as 29 requisições/75 assertions atuais aos resultados da execução 5A. As fases do Newman acima compõem somente a execução 8A; JUnit e Newman são verificações distintas.

README e POSTMAN mantêm os registros datados dos checkpoints anteriores, enquanto SEGURANCA descreve as correções e limitações que continuam vigentes. A evidência 8A acrescenta a confirmação posterior às correções, sem substituir por nova execução os resultados históricos desses documentos. Não houve validação manual pela interface gráfica do Postman ou do Swagger.

## Execuções anteriores preservadas — Checkpoint 7B

Horários de término registrados pelo Maven, em America/Sao_Paulo, offset -03:00.

| Execução | Data e horário | Total | Aprovados | Falhas | Erros | Ignorados | Resultado |
|---|---|---:|---:|---:|---:|---:|---|
| Quatro diagnósticos após correção | 03/11/2026 10:24:50 | 4 | 4 | 0 | 0 | 0 | BUILD SUCCESS |
| Regressão relacionada, 7B | 03/11/2026 10:27:49 | 350 | 350 | 0 | 0 | 0 | BUILD SUCCESS |
| Suíte completa, 7B | **03/11/2026 10:29:23** | **480** | **480** | **0** | **0** | **0** | **BUILD SUCCESS** |

Recortes reais:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

Tests run: 350, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

Tests run: 480, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Fontes locais: `target/aula12-checkpoint7b-diagnosticos.log`, `target/aula12-checkpoint7b-regressao.log` e `target/aula12-checkpoint7b-suite-completa.log`. A suíte inclui os 476 casos anteriores e os quatro diagnósticos, sem alteração de suas expectativas. As seleções de 4 e 350 e os 56 testes OpenAPI estão incluídos no total de 480; não devem ser somados a ele. A regressão de 350 inclui também os testes concorrentes existentes de contas, vendas e estoque.

Comandos dessas execuções:

```powershell
.\mvnw.cmd -B "-Dspring.profiles.active=test" "-Dtest=ContaAcessoAutenticacaoLimiteDiagnosticoTest,ContaAcessoAtualizacaoConcorrenteDiagnosticoTest" test
.\mvnw.cmd -B "-Dspring.profiles.active=test" "-Dtest=Aula10Passo8ApiTest,Checkpoint8ApiTest,Checkpoint9ApiTest,FornecedorDocumentoApiTest,InstrumentoApiTest,ContaAcessoApiTest,ContaAcessoSecurityTest,ContaAcessoUserDetailsServiceTest,OpenApiDocumentationTest,ContaAcessoTest,ContaAcessoPersistenciaTest,MigracaoAula10Test,PersistenciaJpaTest,RepositoriesPasso6Test,RepositoryCheckpoint4Test,PessoasEPapeisServiceTest,ClienteTest,ColaboradorTest,PessoaTest,ContaAcessoServiceTest,ContaAcessoDtoTest,ContaAcessoServiceUnitTest,ContaAcessoServiceConcorrenciaTest,ContaAcessoAutenticacaoLimiteDiagnosticoTest,ContaAcessoAtualizacaoConcorrenteDiagnosticoTest,VendaServiceTest,MovimentacaoEstoqueTransacionalTest,Checkpoint5ServiceTest,InstrumentoServiceTest,InstrumentoExclusaoTest" test
.\mvnw.cmd -B "-Dspring.profiles.active=test" test
```

## Execuções anteriores preservadas — Checkpoint 4E

Horários em America/Sao_Paulo, offset -03:00. Os horários JUnit abaixo são os de término registrados pelo Maven.

| Execução | Data e horário | Total | Aprovados | Falhas | Erros | Ignorados | Resultado |
|---|---|---:|---:|---:|---:|---:|---|
| OpenAPI, Checkpoint 4E | 02/11/2026 19:49:25 | 56 | 56 | 0 | 0 | 0 | BUILD SUCCESS |
| Regressão relacionada, 4E | 02/11/2026 19:50:31 | 307 | 307 | 0 | 0 | 0 | BUILD SUCCESS |
| Suíte completa, 4E | 02/11/2026 19:51:36 | 476 | 476 | 0 | 0 | 0 | BUILD SUCCESS |

Recortes reais:

```text
Tests run: 56, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

Tests run: 307, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

Tests run: 476, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Fontes locais: `target/aula12-checkpoint4e-openapi.log`, `target/aula12-checkpoint4e-regressao.log` e `target/aula12-checkpoint4e-suite-completa.log`. Os 56 testes OpenAPI e os 307 da seleção de regressão já estão incluídos nos 476; não são parcelas adicionais.

Comandos das execuções históricas 4E:

```powershell
.\mvnw.cmd -B "-Dspring.profiles.active=test" "-Dtest=OpenApiDocumentationTest" test
.\mvnw.cmd -B "-Dspring.profiles.active=test" "-Dtest=Aula10Passo8ApiTest,Checkpoint8ApiTest,Checkpoint9ApiTest,FornecedorDocumentoApiTest,InstrumentoApiTest,ContaAcessoApiTest,ContaAcessoSecurityTest,ContaAcessoUserDetailsServiceTest,OpenApiDocumentationTest,ContaAcessoTest,ContaAcessoPersistenciaTest,MigracaoAula10Test,PersistenciaJpaTest,RepositoriesPasso6Test,RepositoryCheckpoint4Test,PessoasEPapeisServiceTest,ClienteTest,ColaboradorTest,PessoaTest,ContaAcessoServiceTest,ContaAcessoDtoTest,ContaAcessoServiceUnitTest,ContaAcessoServiceConcorrenciaTest" test
.\mvnw.cmd -B "-Dspring.profiles.active=test" test
```

## Testes e comportamentos demonstrados

| Fonte | Evidência e limites |
|---|---|
| [ContaAcessoTest](../../src/test/java/com/curso/instrumento2026/domain/ContaAcessoTest.java) | 15 testes de domínio: estado inicial, perfis, dependência da Pessoa e invariantes. |
| [ContaAcessoPersistenciaTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoPersistenciaTest.java) | 17 testes de mapeamento, consultas e restrições no PostgreSQL. |
| [ContaAcessoServiceTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoServiceTest.java) | 32 testes: cadastro, normalização de login, senhas Unicode/UTF-8, BCrypt/salt, troca com commit, bloqueio/desbloqueio e Pessoa inativa. |
| [ContaAcessoServiceUnitTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoServiceUnitTest.java) | 3 testes das traduções de constraints conhecidas e preservação de outras falhas. |
| [ContaAcessoServiceConcorrenciaTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoServiceConcorrenciaTest.java) | 2 cenários parametrizados: mesma Pessoa ou mesmo login; duas transações passam pelas pré-consultas e o banco aceita somente um cadastro. |
| [ContaAcessoDtoTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoDtoTest.java) | 4 testes de entrada/saída, campos WRITE_ONLY e representações sem segredos. |
| [ContaAcessoUserDetailsServiceTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoUserDetailsServiceTest.java) | 3 testes de normalização, authorities e erro genérico para login ausente. |
| [ContaAcessoSecurityTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoSecurityTest.java) | 21 testes com filtros ativos: rotas públicas, comerciais anônimas, senha incorreta, conta bloqueada, Pessoa inativa, ausência de sessão e documentação acessível. |
| [ContaAcessoApiTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoApiTest.java) | 23 testes com filtros ativos: DTOs, cadastro público, 400/404/409, ausência de escalada, identidade via Principal, troca própria, hash persistido e preservação do contrato histórico. |
| [OpenApiDocumentationTest](../../src/test/java/com/curso/instrumento2026/OpenApiDocumentationTest.java) | 56 testes: Basic global, três exceções públicas, DTOs/respostas, schemas sem hash e compatibilidade com Aula 10/descrições da Aula 11. |
| [ContaAcessoAutenticacaoLimiteDiagnosticoTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoAutenticacaoLimiteDiagnosticoTest.java) | 2 casos parametrizados com filtros ativos, senha cadastrada de 72 bytes ASCII/multibyte, variante de 73 bytes e senha incorreta. |
| [ContaAcessoAtualizacaoConcorrenteDiagnosticoTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoAtualizacaoConcorrenteDiagnosticoTest.java) | 2 casos parametrizados com transações independentes: senha → bloqueio e bloqueio → senha; leitura do estado persistido após os commits. |

Exemplos verificáveis no código: `deveAceitarExatamente72BytesMultibyteEExigir12CaracteresUnicode`, `autenticacaoNaoDeveCriarSessaoNemPersistirEntreRequisicoes`, `trocaValidaDeveRetornar204ERevogarSenhaAnteriorAposCommit` e `deveArbitrarUnicidadeNoBancoAposAmbasPreConsultas`. Os testes de serviço/API consultam o estado persistido após as transações nos cenários que afirmam commit/rollback. Não se deduz teste de carga, pentest ou validação em produção desses resultados.

## Adaptação dos testes históricos

As 50 falhas da suíte do Checkpoint 4C recebiam 401 antes dos controllers, em requisições comerciais sem autenticação. No 4D foi alterado somente `@AutoConfigureMockMvc(addFilters = false)` nas cinco classes abaixo, conforme a orientação do professor. Os asserts e códigos HTTP comerciais não foram trocados por 401, e não se removeu cenário algum.

| Classe | Resultado individual após adaptação |
|---|---|
| [Aula10Passo8ApiTest](../../src/test/java/com/curso/instrumento2026/Aula10Passo8ApiTest.java) | 22 aprovados |
| [Checkpoint8ApiTest](../../src/test/java/com/curso/instrumento2026/Checkpoint8ApiTest.java) | 10 aprovados |
| [Checkpoint9ApiTest](../../src/test/java/com/curso/instrumento2026/Checkpoint9ApiTest.java) | 10 aprovados |
| [FornecedorDocumentoApiTest](../../src/test/java/com/curso/instrumento2026/FornecedorDocumentoApiTest.java) | 6 aprovados |
| [InstrumentoApiTest](../../src/test/java/com/curso/instrumento2026/InstrumentoApiTest.java) | 5 aprovados |

Cada execução individual terminou com zero falhas, erros e ignorados e BUILD SUCCESS. O parâmetro isola esses testes de controller; não desativa segurança global. `ContaAcessoSecurityTest`, `ContaAcessoApiTest` e `OpenApiDocumentationTest` mantêm filtros ativos.

## Histórico das expectativas corrigidas

| Marco anterior | Data e horário (-03:00) | Resultado e interpretação |
|---|---|---|
| Encerramento Aula 11 | 02/11/2026 15:14:15 | 351 aprovados |
| 4B, regressão final | 02/11/2026 19:16:38 | 249 executados, 247 aprovados, 2 falhas, zero erros/ignorados; BUILD FAILURE. Expectativas antigas de ausência do endpoint de conta e igualdade integral com OpenAPI da Aula 10. |
| 4C, dois testes corrigidos | 02/11/2026 19:20:23 | 2 aprovados, zero falhas/erros/ignorados; BUILD SUCCESS. Cadastro vazio passou a exigir 400; comparação histórica passou a permitir adições explícitas. |
| 4C, regressão | 02/11/2026 19:21:22 | 249 aprovados, zero falhas/erros/ignorados; BUILD SUCCESS. |
| 4C, suíte completa | 02/11/2026 19:22:22 | 471 executados, 421 aprovados, 50 falhas, zero erros/ignorados; BUILD FAILURE por autenticação ausente nos cinco testes históricos. |
| 4D, regressão | 02/11/2026 19:38:43 | 302 aprovados, zero falhas/erros/ignorados; BUILD SUCCESS. |
| 4D, suíte completa | 02/11/2026 19:39:43 | 471 aprovados, zero falhas/erros/ignorados; BUILD SUCCESS. |
| 4E, suíte anterior | 02/11/2026 19:51:36 | 476 aprovados; cinco novos casos OpenAPI acrescentados ao total anterior. |
| 6, auditoria final antes dos diagnósticos | 03/11/2026 09:57:21 | 476 aprovados, zero falhas/erros/ignorados; BUILD SUCCESS. Não abrangia os quatro casos acrescentados no 7A. |

Os resultados anteriores são históricos, não pendências ainda presentes na suíte mais recente.

## Reprodução e correção dos dois defeitos — 7A/7B

Na auditoria, o comportamento do encoder no limite foi observado isoladamente e a perda de atualização ainda era hipótese. O Checkpoint 7A criou testes HTTP/concorrentes mantendo as expectativas de segurança; ambos os defeitos foram então reproduzidos. No 7B foram alterados somente PasswordEncoderConfig e ContaAcesso, preservando os testes.

| Diagnóstico antes da correção | Data e horário (-03:00) | Total | Aprovados | Falhas | Erros | Ignorados | Resultado |
|---|---|---:|---:|---:|---:|---:|---|
| HTTP Basic, 7A | 03/11/2026 10:18:30 | 2 | 0 | 2 | 0 | 0 | BUILD FAILURE |
| Senha/status concorrentes, 7A | 03/11/2026 10:19:39 | 2 | 0 | 2 | 0 | 0 | BUILD FAILURE |

Fontes: `target/aula12-checkpoint7a-http.log` e `target/aula12-checkpoint7a-concorrencia.log`. Essas falhas foram evidência do defeito; as expectativas não foram trocadas para aceitar o resultado incorreto.

### Quatro cenários, antes e depois

| Cenário | Resultado observado no 7A | Resultado após correção no 7B |
|---|---|---|
| HTTP Basic, ASCII | Correta de 72 bytes: 200; mesmo prefixo com 73: **200 indevido**; incorreta: 401 | Correta: **200**; excedente: **401**; incorreta: **401** |
| HTTP Basic, multibyte UTF-8 | Correta de 72 bytes: 200; mesmo prefixo com 73: **200 indevido**; incorreta: 401 | Correta: **200**; excedente: **401**; incorreta: **401** |
| Commit senha → bloqueio | BLOQUEADA; hash da senha antiga restaurado, nova perdida | **BLOQUEADA**, hash corresponde à **nova**, antiga inválida |
| Commit bloqueio → senha | **ATIVA indevida**; hash corresponde à nova | **BLOQUEADA**, hash corresponde à **nova**, antiga inválida |

O teste HTTP usa contas cadastradas, PostgreSQL e filtros ativos em GET `/api/contas-acesso/me`; não depende da inferência a partir de `matches` isolado. Ele verifica os comprimentos UTF-8 antes das chamadas: 72 e 73 bytes. A senha multibyte válida tem 36 pontos de código `é`.

No teste concorrente, duas threads executam transações REQUIRES_NEW em READ_COMMITTED, com conexões e IDs de transação distintos, latches e timeouts. Ambas carregam o estado inicial antes do primeiro commit. Cada serviço real participa da transação da tarefa e reencontra o estado gerenciado antigo. Os commits são ordenados para testar as duas intercalações; não há sleeps. Uma terceira transação consulta status/hash depois dos dois commits. A correspondência do hash é verificada pelo encoder; não se afirma que foi feito login HTTP com a conta bloqueada.

A correção do Basic mede UTF-8 em `PasswordEncoder.matches` e retorna `false` acima de 72 bytes antes do delegate. `encode` e `upgradeEncoding` continuam delegados; preservam BCrypt e `{bcrypt}`, sem trim, normalização ou exposição de credenciais.

A correção concorrente é `@DynamicUpdate`: o UPDATE inclui somente campos alterados, evitando restaurar hash/status antigos ao modificar o outro atributo. As regras de alterarSenha/alterarStatus, a SecurityFilterChain e a migração 006 não mudaram. Os dois casos passaram com BLOQUEADA e nova senha persistida em ambas as ordens.

**Limitação preservada:** atualização seletiva não serializa duas alterações concorrentes da própria senha ou do mesmo status, nem torna toda validação da senha atual atômica. Essas hipóteses continuam sem reprodução específica e exigem investigação antes de escolher um controle adicional. Os defeitos entre campos distintos foram corrigidos; os riscos didáticos de bootstrap, transporte, CSRF e ausência de administração/JWT permanecem conforme [SEGURANCA](SEGURANCA.md).

## HTTP observado e contrato documentado

| Código | Observação efetiva |
|---|---|
| 200 | Health, OpenAPI, consultas autenticadas e alterações de status de Pessoa nos cenários exercitados |
| 201 | Cadastro público válido de Pessoa/ContaAcesso |
| 204 | Troca válida de senha, sem corpo |
| 400 | Campos ausentes, senha curta/acima de 72 bytes, senha atual incorreta, reutilização de senha, Pessoa inativa no cadastro e JSON malformado |
| 401 | Recurso protegido anônimo, senha incorreta, login inexistente, conta bloqueada, Pessoa inativa e, após 7B, senha acima de 72 bytes na autenticação |
| 404 | Cadastro de conta com Pessoa inexistente; rotas administrativas/por ID não implementadas, com cliente autenticado |
| 409 | Login duplicado ou segunda conta da mesma Pessoa |
| 302 | Entrada pública `/swagger-ui.html` redirecionando para a UI |

Não se afirma que todo código se aplica a toda operação. Nos testes de autenticação verificados, 401 teve corpo vazio; o desafio Basic foi conferido nos casos correspondentes. O handler comercial não foi aplicado aos filtros. Não houve cenário de autorização administrativa por perfil que comprovasse 403.

GET `/me` e PUT `/me/senha` declaram 404 para conta não localizada pelo serviço, mas esse caso HTTP não foi reproduzido como fluxo normal: sem conta, o filtro normalmente retorna 401 antes do controller. O 404 de Pessoa inexistente no cadastro foi efetivamente testado em JUnit, não na coleção Newman.

## Liquibase

A migração 006 acrescenta dois changeSets, totalizando 41. O Checkpoint 2 aplicou os dois sobre os 39 anteriores, preservou os históricos e registrou 19 testes de migração aprovados. Os testes de persistência posteriores conferem constraints e relacionamentos. Houve validação e prévia de SQL de rollback; **não houve rollback destrutivo executado**. O Checkpoint 5B apenas conferiu o changelog e os registros anteriores; não reaplicou migrações nem fez nova consulta ao catálogo.

## Execução Newman anterior preservada — Checkpoint 5A

Fonte local: `target/aula12-postman-resultados.json`. Execução automatizada registrada no 5A com **Newman 6.2.3**, PostgreSQL **17.6**, profile **test**, aplicação local na porta **18012**. Registro `2026-11-03T12:39:59.956Z`, equivalente a **03/11/2026 09:39:59 -03:00**.

Essa execução pertence ao Checkpoint 5A, **anterior às correções 7B**. **Até a consolidação documental do Checkpoint 7C**, a coleção não havia sido reexecutada depois dessas correções; essa era a situação histórica então registrada. O Checkpoint 8A posteriormente a reexecutou com sucesso, conforme a seção de validação final acima. A coleção não contém os quatro novos diagnósticos JUnit. Os resultados de 29 requisições/75 assertions desta seção são preservados exclusivamente como evidência da execução 5A, separados dos resultados JUnit e Newman posteriores.

| Fase | Requisições aprovadas | Assertions aprovadas | Falhas |
|---|---:|---:|---:|
| Pastas 01–02: infraestrutura e preparação | 12 | 32 | 0 |
| Preparação intermediária | SQL executado | Estado BLOQUEADA/ATIVO confirmado | 0 |
| Pastas 03–07: cenários de acesso e erro | 17 | 43 | 0 |
| Total HTTP/assertions | **29** | **75** | **0** |

O runner capturou IDs/logins gerados, preparou somente a conta própria de bloqueio e executou as demais pastas com as variáveis atualizadas. A conta havia autenticado antes do bloqueio; a Pessoa de inativação foi preparada por API e restaurada depois. A troca atualizou `authPassword` e comprovou rejeição da anterior. O JSON foi validado com Ajv 6 contra o schema oficial Collection v2.1.0; sete pastas, 29 requisições e 31 scripts sintaticamente válidos.

A primeira tentativa do runner auxiliar parou na preparação SQL porque não transportava as variáveis entre fases. O transporte foi corrigido; os totais acima pertencem à execução completa posterior. Os dados próprios da execução final e da tentativa inicial foram removidos, e a aplicação temporária foi encerrada. Não se afirma que o Postman gráfico executa automaticamente SQL: a pausa e a preparação manual necessárias estão em [POSTMAN](POSTMAN.md).

## Validações manuais ainda não registradas

- Importação e execução pelo Postman gráfico e inspeção de Authorization no Postman Console.
- Interação com Authorize/logout no Swagger pelo navegador.
- Implantação com HTTPS, configuração prod completa e análise de CSRF em navegadores.
- Testes de carga, limitação de tentativas, MFA e autorização administrativa; esses controles não foram implementados.

Não há capturas de tela nem evidência manual inventada. Nesta etapa documental são verificadas referências, caminhos, consistência de números e `git diff --check`; os resultados dessas verificações não substituem uma nova suíte de testes.
