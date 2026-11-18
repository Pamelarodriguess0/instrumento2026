# Ponto D — integração HTTP e Checkpoint 3

**Situação atual em 17/11/2026: Checkpoint 3 validado no escopo do ciclo HTTP integrado, com 16 casos aprovados.** A correção do guard usou host(inet_server_addr()), sem enfraquecer as verificações de destino. Os 72 testes isolados e as duas interrupções anteriores permanecem históricos separados. A execução integrada aprovada e seus limites estão registrados ao final. OpenAPI, Postman e a verificação global continuam fora desta etapa.

Referência: [Aula 16 oficial](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/16aula/16aula.md), especialmente seções 15 (contratos), 16.4 (logout), 17 (limitação do access token), 19 (ciclo HTTP e Checkpoint 3) e ponto D da sequência de implementação. OpenAPI e Postman constam da sequência do professor, mas foram explicitamente adiados pelo usuário e não foram implementados aqui.

## Contratos conferidos antes da alteração e evidências da primeira tentativa

O projeto tinha POST /api/autenticacao/login público, com resposta 200 contendo quatro campos de access token. Não existiam endpoints de refresh/logout nem tratamento HTTP de RefreshTokenInvalidoException. A emissão com sid e o serviço transacional já existiam e haviam sido validados no Ponto C.

As regras HTTP existentes foram inspecionadas: Resource Server JWT, SessionCreationPolicy.STATELESS, CSRF desabilitado, consultas públicas por GET/HEAD, acesso administrativo restrito e demais APIs protegidas por perfis. ApiError inclui timestamp, status, error, message, path e fields; esse envelope foi preservado.

| Operação | Contrato implementado nesta etapa | Evidência atual |
|---|---|---|
| POST login | Público, 200, par de tokens com seis campos | Mapeamento e serialização aprovados com serviço mockado; login real HTTP não executado |
| POST refresh | Público, 200, mesmo contrato do login | Mapeamento e serialização aprovados com serviço mockado; renovação real HTTP não executada |
| Refresh ausente/desconhecido/expirado/revogado/replay | 401 genérico em ApiError | Handler implementado, mas os cenários parametrizados foram ignorados pelo fail-fast |
| POST logout | Público, 204 sem corpo; token desconhecido não é informado ao cliente | Mapeamento aprovado para token desconhecido, objeto vazio e corpo vazio, com serviço mockado; persistência não testada nesta etapa |
| POST logout-todos | Bearer válido obrigatório, 204 sem corpo, identidade exclusivamente do Principal | Teste interrompido por StackOverflowError; não declarar contrato validado |
| JSON malformado / login com campos inválidos | 400 no padrão existente | Aprovado no slice HTTP |

O par expõe accessToken, tokenType, expiresIn, expiresAt, refreshToken e refreshTokenExpiresAt. Os dois últimos são a credencial opaca e o limite absoluto da família, conforme o material. Não há HttpSession para guardar a sessão de refresh.

## Alterações implementadas

- `AutenticacaoService`: novo método autenticarComSessao retorna o par; autenticar foi mantido como adaptador interno que retorna o access token, preservando o uso existente do teste do Ponto C.
- `AutenticacaoController`: login usa o par; adicionados refresh, logout e logout-todos; mapping de resposta centralizado. Nenhuma nova anotação OpenAPI foi acrescentada e as anotações anteriores não foram revisadas nesta etapa.
- `TokenResponse`: dois campos de refresh adicionados; toString protege as duas credenciais.
- `RefreshTokenRequest`: DTO compartilhado por refresh/logout, com toString protegido. Não usa NotBlank: valor ausente ou em branco chega ao serviço, evitando converter a resposta genérica de refresh em erro de validação 400. Corpo ausente é aceito no binding; refresh o rejeita pelo serviço, logout o trata como tentativa vazia de encerramento. JSON malformado continua 400.
- `ApiExceptionHandler`: RefreshTokenInvalidoException recebe 401 com o envelope ApiError existente. O handler atua depois da saída do proxy do serviço, sem iniciar transação ou refazer a política noRollbackFor.
- `SecurityConfig`: permite somente POST público para login/refresh/logout; logout-todos possui regra authenticated antes da regra geral de perfis. Os demais caminhos e regras foram preservados. Um Bearer inválido, quando enviado a uma rota pública, ainda pode ser rejeitado pelo Resource Server antes do controller; permitAll não ignora a validação de uma credencial apresentada.
- `SessaoAutenticacaoService.encerrar`: transacional, consulta pelo hash com lock e revoga a família por LOGOUT; desconhecido, nulo ou em branco não revela existência. O repository preserva o primeiro motivo de linhas já revogadas.
- `SessaoAutenticacaoService.encerrarTodas`: transacional, busca a conta pelo login autenticado, revalida conta e pessoa ativas e revoga somente por conta.id. Login ausente/desconhecido/inativo usa CredenciaisInvalidasException, já mapeada para 401 genérico. Não recebe ID ou login arbitrário do corpo HTTP.
- Testes de segurança existentes: adicionada a dependência mockada de sessão aos dois slices; JwtAuthoritiesSecurityTest mantém autenticação em memória real e assinatura JWT real, com emissão do par simulada na fronteira de sessão. Essas adaptações ainda não foram executadas devido ao fail-fast.

Nenhuma denylist ou consulta de sessão foi adicionada à validação do Bearer. Logo, a implementação pretende manter o access token válido até expirar após logout, mas essa continuidade ainda precisa ser comprovada no ciclo HTTP integrado.

## Execução isolada e falha inesperada

Criado AutenticacaoHttpTest com controllers e filtros reais, mocks dos serviços e ausência de DataSource verificada. Impressão automática do MockMvc foi desabilitada nesse teste para não despejar credenciais de requisições/respostas em caso de erro. Os dados de teste são sintéticos.

Comando:

```powershell
.\mvnw.cmd -B '-Dtest=AutenticacaoHttpTest,JwtAuthoritiesSecurityTest,PoliticaHttpSecurityTest' '-Dsurefire.skipAfterFailureCount=1' '-Dspring.config.import=optional:file:./target/aula16-no-external-config.properties' '-Djacoco.destFile=target/aula16-pontoD-http-isolados.exec' '-Djacoco.append=false' test > target/aula16-pontoD-http-isolados.log 2>&1
```

Encerramento: **17/11/2026 16:09:40 -03:00**. Código **1**, `BUILD FAILURE`.

| Classe | Registros do Surefire | Aprovados | Falhas de asserção | Erros | Ignorados |
|---|---:|---:|---:|---:|---:|
| AutenticacaoHttpTest | 8 | 6 | 0 | 1 | 1 |
| JwtAuthoritiesSecurityTest | 6 | 0 | 0 | 0 | 6 |
| PoliticaHttpSecurityTest | 12 | 0 | 0 | 0 | 12 |
| Total reportado | 26 | 6 | 0 | 1 | 19 |

**Somente sete casos chegaram a executar: seis aprovações e um erro.** Os 26 registros não significam 26 casos executados; o fail-fast também impede a expansão completa de testes parametrizados. Nenhum dos dois slices anteriores foi validado nesta execução.

Os seis casos aprovados foram: contrato de login/refresh com serviços mockados (um caso), JSON malformado e login inválido (um caso), logout público (três variações) e proteção do toString dos DTOs (um caso).

Erro em `AutenticacaoHttpTest.logoutGlobalDeveExigirBearerEUsarApenasSubject`:

```text
java.lang.StackOverflowError
at org.springframework.aop.framework.JdkDynamicAopProxy.invoke(...)
at jdk.proxy2.$Proxy180.authenticate(...)
at org.springframework.aop.support.AopUtils.invokeJoinpointUsingReflection(...)
at org.springframework.aop.framework.JdkDynamicAopProxy.invoke(...)
at jdk.proxy2.$Proxy180.authenticate(...)
```

A pilha mostra delegação recursiva no proxy de AuthenticationManager. Hipótese principal: o novo slice importa a configuração real que expõe AuthenticationManager, mas mocka AutenticacaoService e não fornece a configuração de autenticação em memória que existe nos slices anteriores. O caminho de Bearer inválido pode delegar ao manager pai e atingir essa recursão. **A causa ainda precisa ser confirmada e corrigida no contexto isolado; não há evidência suficiente para atribuir o defeito à configuração de produção.** Nenhuma correção foi aplicada após a falha.

## Histórico: pendências registradas na interrupção inicial

1. Revisar o AuthenticationManager/providers do novo slice sem enfraquecer os filtros JWT e confirmar a causa da recursão.
2. Reexecutar os testes HTTP isolados autorizados; os cenários genéricos de refresh e os dois slices anteriores continuam sem aprovação.
3. Antes de qualquer teste integrado, reconfirmar DataSource e Liquibase em 127.0.0.1:5432/instrumento2026_test/public, neutralizar .env e conferir os 44 changeSets. Esse preflight integrado não foi realizado nesta etapa porque a execução parou antes dele.
4. Preparar e executar somente o ciclo HTTP necessário com PostgreSQL: login/par, rotações e replay, consulta em nova transação da revogação após 401, logout individual/idempotência, logout global restrito à própria conta e revalidação de identidade.
5. Comprovar via Bearer real que o access token anterior continua acessando uma rota protegida após logout até seu prazo de expiração.
6. Considerar na revisão futura os testes OpenAPI que dependem do controller e do antigo esquema de quatro campos. Eles não foram alterados nem executados por estarem fora do escopo autorizado.

As evidências transacionais do Ponto C continuam históricas e não substituem a validação HTTP dos novos métodos de logout. Concorrência entre logout global e uma renovação de outra geração não foi testada nem é declarada resolvida aqui.

## Arquivos desta etapa

Modificados:

- src/main/java/com/curso/instrumento2026/api/ApiExceptionHandler.java
- src/main/java/com/curso/instrumento2026/api/AutenticacaoController.java
- src/main/java/com/curso/instrumento2026/api/dto/TokenResponse.java
- src/main/java/com/curso/instrumento2026/config/SecurityConfig.java
- src/main/java/com/curso/instrumento2026/service/AutenticacaoService.java
- src/main/java/com/curso/instrumento2026/service/SessaoAutenticacaoService.java
- src/test/java/com/curso/instrumento2026/JwtAuthoritiesSecurityTest.java
- src/test/java/com/curso/instrumento2026/PoliticaHttpSecurityTest.java

Criados:

- src/main/java/com/curso/instrumento2026/api/dto/RefreshTokenRequest.java
- src/test/java/com/curso/instrumento2026/AutenticacaoHttpTest.java
- docs/aula16/api.md

Existem outras alterações das etapas anteriores no working tree; elas não foram refeitas. git diff --check não apontou erros. Não houve acesso ao banco, alteração de migrations, execução integrada ou da suíte completa, commit ou push. **Naquela interrupção, aguardava-se revisão para retomar; a correção isolada autorizada está registrada a seguir.**

## Diagnóstico comprovado e correção mínima do contexto isolado

### Evidência anterior a qualquer edição desta correção

Os componentes inspecionados foram AutenticacaoHttpTest, SecurityConfig, AutenticacaoService, ContaAcessoUserDetailsService e os contextos de JwtAuthoritiesSecurityTest e PoliticaHttpSecurityTest. O stack trace completo disponível no XML original do Surefire foi percorrido: 821 linhas, com 205 quadros authenticate do mesmo proxy, alternados com 204 ocorrências de cada quadro reflexivo principal. Não havia chamada repetida ao serviço de autenticação ou a um mock no ciclo:

```text
$Proxy180.authenticate
  -> JdkDynamicAopProxy.invoke
  -> AopUtils.invokeJoinpointUsingReflection
  -> Method.invoke
  -> $Proxy180.authenticate
  -> ...
```

A pilha comprova a recursão, mas, sozinha, não identifica a configuração que a produziu. Para confirmar o alvo, foi usada a skill ij-debugger no IntelliJ: inicialmente um logpoint; como a sessão curta encerrou antes da leitura de eventos, uma segunda sessão foi pausada antes da primeira requisição do método afetado para inspecionar o grafo de beans, sem alterar código ou credenciais. O primeiro lançamento diagnóstico reproduziu o mesmo StackOverflowError esperado; o segundo foi encerrado após a inspeção. Eles não são contados como testes aprovados da correção.

Valores observados no contexto original, antes da edição:

```text
UserDetailsService beans = 0
AuthenticationManager class = jdk.proxy1.$Proxy178
TargetSource class = org.springframework.aop.target.LazyInitTargetSource
TargetSource targetBeanName = authenticationManager
context.getBean("authenticationManager") == context.getBean(AuthenticationManager.class) = true
```

O número do proxy muda entre processos, mas o comportamento é o mesmo: o bean authenticationManager é um proxy lazy cujo alvo aponta para seu próprio nome. O serviço AutenticacaoService é mockado neste slice e não configura um provider; não havia UserDetailsService ou configuração de usuários em memória para construir o manager global. Nos dois slices anteriores existe um InMemoryUserDetailsManager; na aplicação completa existe ContaAcessoUserDetailsService.

O código primário da versão utilizada, Spring Security 7.0.6, confirma o caminho: [AuthenticationConfiguration](https://github.com/spring-projects/spring-security/blob/7.0.6/config/src/main/java/org/springframework/security/config/annotation/authentication/configuration/AuthenticationConfiguration.java) tenta construir o manager e, quando obtém null, cria um fallback lazy para o bean AuthenticationManager. Nesse slice, o único alvo é o próprio bean exposto por SecurityConfig. [ProviderManager](https://github.com/spring-projects/spring-security/blob/7.0.6/core/src/main/java/org/springframework/security/authentication/ProviderManager.java), após uma tentativa de autenticação sem sucesso, pode delegar ao parent; o Bearer inválido expõe o fallback autorreferente e a invocação do proxy se repete até esgotar a pilha.

**Causa confirmada: configuração incompleta exclusivamente do novo slice HTTP**, que importava SecurityConfig sem o componente de usuários presente nos outros contextos. Não foi identificado defeito de produção a corrigir nesta autorização. As evidências não autorizam afirmar que qualquer configuração arbitrária da aplicação inteira está coberta: o diagnóstico se refere ao contexto e ao caminho reproduzidos.

Os breakpoints criados pelo agente foram removidos; as três marcações preexistentes do usuário foram preservadas e a sessão diagnóstica foi encerrada. A pilha original também foi preservada em target/aula16-pontoD-stackoverflow-original.txt, antes da substituição dos relatórios Surefire pela execução final; o log inicial permanece em target/aula16-pontoD-http-isolados.log. Esses artefatos temporários não são candidatos ao commit.

### Correção aplicada

Somente AutenticacaoHttpTest.java foi alterado em código nesta correção: uma TestConfiguration local, explicitamente importada, fornece um InMemoryUserDetailsManager vazio. Não são necessários usuários nesse slice porque o caso de login verifica o contrato com AutenticacaoService mockado. A presença do bean permite ao Spring construir o manager global e elimina o fallback autorreferente.

Nenhuma configuração de produção, mock de autenticação Bearer, filtro desabilitado, usuário via @WithMockUser ou substituição do decoder foi introduzida. O teste de logout global continua enviando um JWT HS256 assinado pelo encoder real e validado pelo decoder/filtros reais, com emissor e prazo válidos. Os serviços de sessão permanecem mockados neste slice; isso não é evidência de persistência.

### Execução final e contratos observados

Executado somente o conjunto HTTP isolado relacionado ao Ponto D:

```powershell
.\mvnw.cmd -B '-Dtest=AutenticacaoHttpTest,JwtAuthoritiesSecurityTest,PoliticaHttpSecurityTest' '-Dsurefire.skipAfterFailureCount=1' '-Dspring.config.import=optional:file:./target/aula16-no-external-config.properties' '-Djacoco.destFile=target/aula16-pontoD-correcao-contexto.exec' '-Djacoco.append=false' test > target/aula16-pontoD-correcao-contexto.log 2>&1
```

Encerramento: **17/11/2026 16:19:42 -03:00**. Código de saída **0**, `BUILD SUCCESS`.

| Classe | Executados | Aprovados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|---:|
| AutenticacaoHttpTest | 13 | 13 | 0 | 0 | 0 |
| JwtAuthoritiesSecurityTest | 10 | 10 | 0 | 0 | 0 |
| PoliticaHttpSecurityTest | 49 | 49 | 0 | 0 | 0 |
| Total | 72 | 72 | 0 | 0 | 0 |

```text
Global AuthenticationManager configured with UserDetailsService bean with name usuariosFicticios
Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Não há StackOverflowError no log final. A expansão normal dos testes parametrizados explica a diferença entre os 26 registros da interrupção inicial e os 72 casos completos desta execução.

Evidências efetivamente aprovadas nesta correção:

- Login e refresh públicos: 200 com os mesmos seis campos e metadados, com serviços mockados; ausência de HttpSession.
- Refresh sem corpo, com objeto vazio, null, vazio, espaço ou valor desconhecido: 401 com mensagem genérica, timestamp, status, error, path e fields no ApiError.
- Logout individual sem Bearer: 204 sem corpo para token desconhecido, objeto vazio ou corpo vazio; não informa existência de token, no limite do serviço mockado.
- Logout global sem Bearer ou com Bearer inválido: 401, sem executar operação no serviço de sessão.
- Logout global com JWT assinado válido: 204 sem corpo; chama apenas encerrarTodas com o subject autenticado. ContaId/login de outra conta enviados no JSON não são usados.
- JSON malformado e login sem campos válidos: 400.
- JWT/authorities e política HTTP anteriores: os dois slices completos passaram, preservando restrições de perfis, respostas de acesso e autenticação real em memória no cenário de login anterior.
- toString dos DTOs continua sem expor access ou refresh token.

### Alcance atual e arquivos desta correção

**StackOverflowError resolvido e validação HTTP isolada aprovada.** Ainda não foram executados nesta etapa o ciclo HTTP com PostgreSQL, as consultas de revogação após erro HTTP nem a continuidade real do access token após logout. A validação transacional histórica do Ponto C não substitui esses testes dos endpoints. Portanto, **Checkpoint 3 ainda não está validado integralmente**.

Somente dois arquivos foram alterados nesta correção:

- src/test/java/com/curso/instrumento2026/AutenticacaoHttpTest.java
- docs/aula16/api.md

Não houve alteração de produção, migrations, configurações da aplicação, OpenAPI ou Postman; nenhum acesso ao PostgreSQL, teste integrado, suíte completa, commit ou push. Aguarda-se revisão antes da validação integrada do Checkpoint 3.

## Histórico: primeira tentativa integrada — interrupção na verificação inicial

### Contratos e segurança conferidos antes da execução

Foram relidos os contratos oficiais das seções 15 a 17 e o ciclo/Checkpoint 3 da seção 19. Foram conferidos AutenticacaoController, TokenResponse, RefreshTokenRequest, AutenticacaoService, SessaoAutenticacaoService, SecurityConfig, ApiExceptionHandler, os testes transacionais do Ponto C e o profile test. O repository e a política transacional permanecem os já implementados: lock pessimista, revogação de família e noRollbackFor restrito à exceção de negócio de refresh.

O preflight PostgreSQL foi feito com host, porta e banco explícitos, executando somente consultas em BEGIN READ ONLY. Conferiu-se também ausência de overrides Maven/Java, JSON de propriedades Spring e localizações alternativas de configuração/DataSource. O master inclui somente as sete migrations esperadas; os arquivos locais totalizam 44 changeSets. A migration 007 permanece com SHA-256 `6DA69A5EA246220E8509E826DC319FDCC7A28EB50EB2353A29A597D72505F55B`.

| Item | Antes e depois da tentativa integrada |
|---|---|
| Profile confirmado no log | test |
| DataSource explícito | jdbc:postgresql://127.0.0.1:5432/instrumento2026_test |
| Liquibase explícito | jdbc:postgresql://127.0.0.1:5432/instrumento2026_test |
| Destino real consultado | instrumento2026_test / 127.0.0.1:5432 / public |
| ChangeSets registrados | 44 |
| Lock do Liquibase | false |
| Sessões / contas / pessoas | 0 / 0 / 0 |

Na inicialização, o Liquibase informou banco atualizado, Run 0, Previously run 44, Total change sets 44. Nenhuma migration foi aplicada. O Hibernate inicializou com a configuração existente ddl-auto=validate.

A importação de .env pelo Spring foi neutralizada usando a localização opcional inexistente `target/aula16-no-external-config.properties`, cuja ausência foi conferida. Apenas as credenciais privadas de teste foram lidas em memória pelo processo para fornecer DB_TEST_USERNAME/PASSWORD e SPRING_LIQUIBASE_USER/PASSWORD; o ambiente anterior foi restaurado em finally. Não houve exposição desses valores em argumentos Maven, documentação ou saída. O administrador inicial foi desabilitado. Não houve acesso ao banco de desenvolvimento.

### Testes preparados

Criado `src/test/java/com/curso/instrumento2026/CicloTokenApiTest.java` com contexto completo, MockMvc com filtros habilitados, autenticação por senha, encoder/decoder JWT, serviços, JPA e PostgreSQL reais. Somente Clock é controlado, iniciando no instante corrente para manter o JWT compatível com o relógio real do decoder. Não há @Transactional na classe ou nos métodos de teste; fixtures, consultas posteriores e limpeza usam TransactionTemplate REQUIRES_NEW. A limpeza é restrita aos IDs das fixtures, usando a FK CASCADE para suas sessões.

Os cenários preparados cobrem:

- Login/par 200, rotação R1 para R2, hash independente, família, prazo absoluto e marcação de utilização.
- Replay 401 e consulta de toda a família revogada em nova transação, seguida de rejeição de R2.
- Seis formas de refresh ausente/vazio/desconhecido e dois limites de expiração, com ApiError genérico.
- JSON malformado em login/refresh/logout, retornando 400.
- Logout individual 204, repetição e desconhecido, isolamento de família e preservação de outra conta.
- Logout global 401 pelo filtro sem Bearer ou com Bearer inválido, 204 com JWT real, uso exclusivo do subject e preservação de outra conta.
- Revalidação de conta bloqueada e pessoa inativa no logout global, seguida da rejeição do refresh por CONTA_INATIVA.
- Uso do access token anterior em /api/contas-acesso/me após logout, ausência de HttpSession/JSESSIONID e proteção de representações textuais.
- Captura de saída para procurar tokens/hashes gerados sem imprimi-los; impressão automática do MockMvc desabilitada e logging SQL/bind desligado na execução.

São **16 casos parametrizados previstos**, mas **nenhum cenário HTTP chegou a executar** nesta tentativa. A preparação falhou antes da criação das fixtures e antes das chamadas MVC. Portanto, nenhuma das evidências acima deve ser considerada aprovada por este teste ainda. A verificação de não exposição de segredos gerados não tem valor probatório nesta tentativa, pois nenhum token foi emitido.

### Comando executado

Com as credenciais privadas no ambiente temporário:

```powershell
.\mvnw.cmd -B `
  '-Dtest=com.curso.instrumento2026.CicloTokenApiTest' `
  '-Dsurefire.skipAfterFailureCount=1' `
  '-Dspring.profiles.active=test' `
  '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.datasource.hikari.schema=public' `
  '-Dspring.liquibase.default-schema=public' `
  '-Dspring.config.import=optional:file:./target/aula16-no-external-config.properties' `
  '-Dapp.security.initial-admin.enabled=false' `
  '-Dapp.security.refresh-token.idle-timeout=PT30M' `
  '-Dapp.security.refresh-token.absolute-expiration=PT8H' `
  '-Dapp.security.jwt.expiration=PT30M' `
  '-Dlogging.level.liquibase=INFO' `
  '-Dlogging.level.org.hibernate.SQL=OFF' `
  '-Dlogging.level.org.hibernate.orm.jdbc.bind=OFF' `
  '-Djacoco.destFile=target/aula16-pontoD-integrado.exec' `
  '-Djacoco.append=false' test > target/aula16-pontoD-integrado.log 2>&1
```

Encerramento: **17/11/2026 16:29:55 -03:00**. Código de saída **1**, `BUILD FAILURE`.

```text
CicloTokenApiTest.preparar ... expected: <127.0.0.1> but was: <127.0.0.1/32>
Tests run: 8, Failures: 1, Errors: 0, Skipped: 7
BUILD FAILURE
```

São oito registros do Surefire: zero aprovações, uma falha em BeforeEach e sete ignorados pelo fail-fast. A expansão dos parametrizados foi interrompida; não são oito cenários HTTP executados nem os 16 casos preparados.

### Diagnóstico e conferência posterior

O guard de segurança do teste consulta `inet_server_addr()::text` e compara literalmente com `127.0.0.1`. O PostgreSQL representa esse inet convertido em texto como `127.0.0.1/32`. A falha está nessa asserção de representação do novo teste, não em evidência de conexão a outro banco. Para confirmar, uma consulta somente leitura, com o mesmo destino explícito, retornou:

```text
current_database: instrumento2026_test
host(inet_server_addr()): 127.0.0.1
inet_server_addr()::text: 127.0.0.1/32
inet_server_port: 5432
current_schema: public
```

A conferência posterior também confirmou 44 changeSets, lock livre e zero sessões, contas e pessoas, sem fixtures residuais. O contexto iniciou e validou o schema, mas a execução foi interrompida na preparação do primeiro caso.

Respeitando a instrução de interrupção por falha inesperada, **não foi aplicada correção após a falha, nem alterado código de produção**. A correção mínima proposta para revisão é usar `host(inet_server_addr())` na consulta do guard, mantendo a comparação exata com 127.0.0.1, a porta, o banco e o schema. Não há necessidade demonstrada de alterar configuração da aplicação ou banco.

### Estado atual e arquivos desta tentativa

- Criado: src/test/java/com/curso/instrumento2026/CicloTokenApiTest.java.
- Atualizado: docs/aula16/api.md.

Não houve alteração em produção, migrations ou configurações, OpenAPI/Postman, execução da suíte completa, commit ou push. Os 72 casos isolados e os 12 casos transacionais do Ponto C continuam resultados históricos separados e não substituem a validação integrada dos endpoints.

**Checkpoint 3 ainda pendente.** Nenhum status HTTP foi observado nesta tentativa integrada e nenhuma revogação via endpoint foi comprovada nela. A próxima ação é revisar/corrigir apenas a representação do host no guard do teste e retomar a execução autorizada após revisão do usuário.

## Correção mínima do guard e validação integrada aprovada

### Alteração autorizada e segurança do banco

Em autorização posterior, foi substituída **somente a expressão da consulta do endereço**, de `inet_server_addr()::text` para `host(inet_server_addr())`, em CicloTokenApiTest.preparar. A primeira expressão inclui a máscara /32 na representação textual do inet; a segunda fornece o host, preservando a comparação exata com 127.0.0.1. As verificações de porta 5432, banco instrumento2026_test, schema public, profile test, URLs explícitas de DataSource/Liquibase e importação neutra foram mantidas integralmente. Nenhum cenário ou asserção de comportamento foi removido.

Antes da repetição, o preflight somente leitura reconfirmou instrumento2026_test em 127.0.0.1:5432/public, 44 changeSets, lock livre e zero sessões/contas/pessoas. Foram conferidos novamente os 44 changeSets locais e a ausência de overrides alternativos de inicialização/DataSource. A localização opcional neutra foi conferida como inexistente. A execução passou explicitamente as duas URLs e ambos os schemas; a importação de .env pelo Spring permaneceu neutralizada. As credenciais de teste foram fornecidas pelo ambiente privado temporário e restauradas em finally, sem valores em argumentos ou documentação. O banco de desenvolvimento não foi acessado.

### Comando e resultado

Foi executada somente CicloTokenApiTest, uma vez após a correção:

```powershell
.\mvnw.cmd -B `
  '-Dtest=com.curso.instrumento2026.CicloTokenApiTest' `
  '-Dsurefire.skipAfterFailureCount=1' `
  '-Dspring.profiles.active=test' `
  '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.datasource.hikari.schema=public' `
  '-Dspring.liquibase.default-schema=public' `
  '-Dspring.config.import=optional:file:./target/aula16-no-external-config.properties' `
  '-Dapp.security.initial-admin.enabled=false' `
  '-Dapp.security.refresh-token.idle-timeout=PT30M' `
  '-Dapp.security.refresh-token.absolute-expiration=PT8H' `
  '-Dapp.security.jwt.expiration=PT30M' `
  '-Dlogging.level.liquibase=INFO' `
  '-Dlogging.level.org.hibernate.SQL=OFF' `
  '-Dlogging.level.org.hibernate.orm.jdbc.bind=OFF' `
  '-Djacoco.destFile=target/aula16-pontoD-integrado-host.exec' `
  '-Djacoco.append=false' test > target/aula16-pontoD-integrado-host.log 2>&1
```

Encerramento: **17/11/2026 16:35:02 -03:00**. Código **0**, `BUILD SUCCESS`. Tempo Maven: 21,556 segundos.

| Classe | Executados | Aprovados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|---:|
| CicloTokenApiTest | 16 | 16 | 0 | 0 | 0 |

```text
The following 1 profile is active: "test"
Database is up to date, no changesets to execute
Run:                          0
Previously run:              44
Total change sets:           44
Database JDBC URL [jdbc:postgresql://127.0.0.1:5432/instrumento2026_test]
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Não houve outra falha inesperada nem StackOverflowError. O log da tentativa anterior foi preservado, usando novo nome para log e coleta. O relatório Surefire final confirmou os 16 casos expandidos. Os testes isolados anteriores não foram reexecutados nem somados a esse total.

### Status HTTP e persistência efetivamente comprovados

| Cenário integrado | Resultado observado | Evidência após a resposta |
|---|---|---|
| Login com senha válida | 200, seis campos do par e metadados de 1800 segundos | Uma geração confirmada; hash SHA-256 calculado independentemente coincide com o persistido; JSON da linha não contém o refresh original |
| Refresh válido | 200, novos access e refresh | R1 utilizado e R2 disponível, mesma família e mesmo limite absoluto; sid preservado em JWT decodificado com componente real |
| Replay de R1 | 401 com mensagem genérica em ApiError | Nova transação encontra R1 e R2 revogados por REUTILIZACAO; tentar R2 também retorna 401 |
| Refresh sem corpo, objeto vazio, null, vazio, espaço ou desconhecido | Seis casos 401 com o mesmo ApiError genérico | Nenhuma geração criada; não há discriminação de existência na resposta |
| Inatividade e prazo absoluto no instante limite | Dois casos 401 | Motivo correspondente persistido, sem sucessor e sem marcação indevida de uso |
| JSON malformado em login/refresh/logout | Três casos 400 | Mensagem padronizada de JSON inválido |
| Logout individual com geração antiga utilizada, atual ou desconhecida | 204 sem corpo | R1 e R2 da família selecionada revogados por LOGOUT; repetição preserva motivo; segunda família da mesma conta e outra conta permanecem utilizáveis |
| Logout global sem Bearer / com Bearer inválido | 401 pelo filtro | Corpo vazio e WWW-Authenticate Bearer; nenhuma família revogada |
| Logout global com JWT real válido | 204 sem corpo | Duas famílias da própria conta revogadas por LOGOUT_GLOBAL; outra conta preservada e ainda consegue renovar |
| Corpo de logout global tenta indicar outra conta | 204 referente ao subject autenticado | Os valores de contaId/login enviados no JSON não redirecionam a revogação; conferência por IDs nas novas transações |
| Conta bloqueada / pessoa inativa após login | Dois casos 401 no logout global | ApiError de credenciais, sem WWW-Authenticate; a operação não revoga pela identidade inativa. Refresh subsequente é rejeitado com mensagem genérica e confirma CONTA_INATIVA |
| Access token anterior após logout individual e global | GET /api/contas-acesso/me retorna 200 | Identidade correta e token ainda dentro do prazo; não se afirma revogação imediata do JWT |

Os 16 casos não equivalem ao número de requisições: vários casos exercitam uma sequência completa e verificam múltiplas respostas. As rejeições genéricas de refresh conferem todo o envelope ApiError e a ausência de WWW-Authenticate. As rejeições de Bearer conferem o header e o corpo vazio do filtro, sem tratá-las como ApiError do controller.

### Commit real, isolamento, stateless e ausência de exposição

Não há transação envolvendo o método de teste. A preparação é confirmada antes do HTTP; cada serviço executa sua transação real dentro da chamada MockMvc. Depois da resposta 401 de replay, TransactionTemplate com REQUIRES_NEW abre outra transação e lê as duas gerações revogadas. Isso comprova que o tratamento HTTP não desfez a revogação. A mesma estratégia confirma os motivos de expiração, LOGOUT e LOGOUT_GLOBAL e a preservação das outras famílias/contas. As fixtures não são compartilhadas com testes anteriores e são removidas somente por seus próprios IDs.

Todas as requisições conferem ausência de HttpSession, cookie JSESSIONID e header Set-Cookie que o contenha. Os filtros de segurança permanecem ativos. O Bearer válido vem do login real com senha e é assinado/decodificado pelos componentes da aplicação; não há @WithMockUser nem autenticação Bearer simulada. O único mock é Clock para controlar as regras temporais de emissão/refresh; o decoder mantém sua validação real.

Após os cenários, as asserções de saída capturada verificaram que os access tokens, refresh tokens e hashes gerados durante cada caso não aparecem na saída do processo capturada pelo teste. Os erros de refresh e os toString dos DTOs/objeto interno também foram confrontados com esses valores, sem imprimi-los. SQL/binds e impressão automática de MockMvc ficaram desabilitados. Essa é evidência para a saída capturada nesta execução; não constitui auditoria de todos os destinos de logs possíveis em outros ambientes.

### Conferência final e alcance do Checkpoint 3

Uma conexão explícita somente leitura, após o build, confirmou novamente:

```text
Banco: instrumento2026_test
Host/porta: 127.0.0.1:5432
Schema: public
ChangeSets: 44
Liquibase locked: false
Sessões: 0 / contas: 0 / pessoas: 0
```

Portanto, a limpeza das fixtures foi confirmada e nenhuma migration foi aplicada. O Hibernate inicializou com ddl-auto=validate. **Checkpoint 3 validado no escopo autorizado do ciclo HTTP integrado**, somando esta evidência às validações históricas de domínio e persistência dos Checkpoints 1 e 2. As classes históricas não foram reexecutadas, respeitando a autorização para somente CicloTokenApiTest.

Não há pendência identificada nos cenários HTTP solicitados desta execução. Continuam fora do escopo: OpenAPI, Postman, suíte completa/portão de cobertura global, todos os destinos de logs possíveis e concorrência entre logout e operações de outras gerações; essas garantias não foram testadas aqui. A concorrência de duas renovações do mesmo token permanece uma evidência histórica do Ponto C, não uma nova execução nesta etapa.

Arquivos alterados exclusivamente nesta correção/repetição:

- src/test/java/com/curso/instrumento2026/CicloTokenApiTest.java — uma expressão SQL no guard.
- docs/aula16/api.md — histórico preservado e resultados atuais.

Os artefatos em target são temporários e não são candidatos ao versionamento. Não houve alteração de controllers, services, DTOs, SecurityConfig, migrations ou configurações; nenhuma execução da suíte completa, acesso ao banco de desenvolvimento, commit ou push. Aguarda-se revisão antes de avançar.
