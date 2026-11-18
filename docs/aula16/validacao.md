# Aula 16 — validação global

**Estado atual em 18/11/2026: BUILD SUCCESS, 771 testes aprovados, zero falhas/erros/ignorados.** O clean verify revisado foi executado uma única vez nesta autorização. JaCoCo report/check usaram a mesma coleta nova: LINE 96,85% e BRANCH 90,83%, com aprovação dos mínimos de 70%/40%. As fixtures Postman e o databasechangelog foram preservados integralmente. A execução aprovada está na seção final; o histórico abaixo permanece registrado.

**Primeira execução global em 17/11/2026: BUILD FAILURE por duas asserções de teste.** A suíte completa foi executada uma única vez: 771 casos, 769 aprovados, duas falhas, zero erros e zero ignorados. O Maven não alcançou report/check do JaCoCo. Não há cobertura final ou aprovação do portão nessa tentativa.

**Correções direcionadas em 17/11/2026: as duas pendências foram corrigidas/validadas com 22 testes aprovados.** Naquela etapa, a suíte completa não foi repetida. A correção do teste de login, a invocação segura de bootstrap e os resultados estão registrados adiante. A nova execução global foi autorizada posteriormente e está documentada na seção final.

Referência: [Aula 16 oficial](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/16aula/16aula.md), seções 23/E, 24 e 31. Os checkpoints anteriores e a execução real Newman continuam evidências históricas separadas; não substituem um clean verify aprovado.

## Git e preflight

Branch main; HEAD `ebe61ba2b57c7fcec8a8a0dad67a7447422786c2`, commit/tag de origem da Aula 15. As alterações acumuladas da Aula 16 estavam no working tree, sem commit: configurações/propriedades de refresh, modelo/migration 007, repository, emissão/renovação/logout, integração HTTP, OpenAPI, testes e docs/aula16. O inventário completo foi salvo fora de target antes do clean. Nenhum arquivo de produção, teste, migration ou configuração foi alterado nesta validação.

O pom.xml foi conferido antes da execução:

- JaCoCo 0.8.16, prepare-agent preservado.
- Execução report-coverage, fase verify, goals report e check.
- Regra BUNDLE, COVEREDRATIO de LINE mínimo 0.70 e BRANCH mínimo 0.40.
- Sem opções novas de exclusão, skip, tolerância a falhas ou coleta alternativa.

SHA-256 do POM antes/depois: `22C09DCE2C856AA51517B39E9B655CFF58CF53F0FFD25C382C787EBA0C8BCF1E`.

Consultas somente leitura confirmaram DataSource/Liquibase direcionados explicitamente a instrumento2026_test, host 127.0.0.1, porta 5432 e schema public, com 44 changeSets e lock false. A importação Spring de .env foi neutralizada pela localização opcional inexistente target/aula16-no-external-config.properties; sua ausência foi conferida antes da execução. Não existiam overrides Maven/Java ou configurações alternativas de DataSource identificadas no preflight. As credenciais de teste foram lidas privadamente em memória e fornecidas pelo ambiente temporário do processo, restaurado em finally. Não houve credenciais em argumentos, documentação ou saídas apresentadas.

### Compatibilidade e proteção das fixtures Postman

Antes do run havia três pessoas (9742–9744), três contas (2511–2513), quatro vínculos de perfil e 11 sessões (70–80), exatamente como registrado em [postman.md](postman.md). As limpezas da suíte foram revisadas: utilizam IDs criados pelos próprios testes, identificadores exclusivos ou rollback de transação; não foi encontrada limpeza global que atingisse esses registros. Os CPFs das fixtures existentes foram comparados, sem divulgá-los, com os literais de 11 dígitos usados nos testes: zero colisões.

O bootstrap global ficou desabilitado; os testes unitários desse componente usam serviço mockado. Os testes novos de ciclo e transação criam contas próprias e limpam somente essas contas/pessoas. Testes de repository/endpoints trabalham com suas próprias famílias e IDs. Não foi removida nenhuma fixture remanescente do Postman.

Foram registradas assinaturas do conteúdo integral das linhas protegidas de pessoa, conta_acesso, conta_acesso_perfil e sessao_refresh_token, além do databasechangelog. As assinaturas incluem todos os campos das linhas sem expor seu conteúdo; não são os hashes de senha/refresh. A comparação posterior está registrada abaixo.

Foi identificada no preflight uma asserção histórica de AutenticacaoJwtTest que ainda exigia o formato de login da Aula 13 (quatro campos e claims sem sid). Isso não representa risco de banco, mas impede o contrato novo de passar nesse teste. Não foi alterada; a execução global solicitada foi feita para registrar o resultado real, sem ignorar a incompatibilidade.

## Comando único executado

Credenciais temporárias em DB_TEST_USERNAME/PASSWORD, SPRING_DATASOURCE_USERNAME/PASSWORD e SPRING_LIQUIBASE_USER/PASSWORD, sem valores registrados. INITIAL_ADMIN_ENABLED=false e durações padrão de refresh no ambiente do processo. O comando também passou a propriedade de sistema de bootstrap abaixo; ela causou uma das falhas descritas adiante.

```powershell
.\mvnw.cmd -B `
  '-Dspring.profiles.active=test' `
  '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.datasource.hikari.schema=public' `
  '-Dspring.liquibase.default-schema=public' `
  '-Dspring.config.import=optional:file:./target/aula16-no-external-config.properties' `
  '-Dapp.security.initial-admin.enabled=false' `
  '-Dspring.test.mockmvc.print=NONE' `
  '-Dlogging.level.liquibase=INFO' `
  '-Dlogging.level.org.hibernate.SQL=OFF' `
  '-Dlogging.level.org.hibernate.orm.jdbc.bind=OFF' clean verify
```

A impressão automática do MockMvc foi desabilitada para evitar despejar bodies com credenciais durante falhas; filtros e testes continuaram ativos. Não houve -Dtest, skipTests, maven.test.skip, skipAfterFailureCount, jacoco.skip ou testFailureIgnore. Todas as classes da suíte foram executadas. Nenhum Postman/Newman foi executado novamente.

O stdout/stderr foi direcionado inicialmente para um diretório temporário fora de target, evitando que o clean removesse o log aberto. Antes do clean foram preservados os artefatos anteriores aula16-* e o inventário Git. Diretório local desta execução:

```text
C:\Users\pr472\AppData\Local\Temp\instrumento2026-aula16-global-693e46877f654d2a9899f0c309b504aa
```

O log final também foi copiado para target/aula16-validacao-global.log, e a comparação das fixtures para target/aula16-validacao-global-fixtures.json; são artefatos ignorados pelo Git. As evidências anteriores do Newman foram preservadas no diretório temporário antes de o clean remover target.

## Resultado Maven e diagnóstico

Encerramento: **17/11/2026 19:34:22 -03:00**. Duração Maven: **1 minuto e 47 segundos**. Código de saída **1**.

| Métrica | Resultado |
|---|---:|
| Classes de teste com XML Surefire | 64 |
| Casos executados | 771 |
| Aprovados | 769 |
| Falhas de asserção | 2 |
| Erros | 0 |
| Ignorados | 0 |

Os totais foram conferidos pela soma dos 64 XMLs, além do resumo Maven:

```text
Tests run: 771, Failures: 2, Errors: 0, Skipped: 0
BUILD FAILURE
```

### 1. Asserção histórica do login

Arquivo: src/test/java/com/curso/instrumento2026/AutenticacaoJwtTest.java, método loginDeveRetornarBearerComClaimsControladosPeloServidorEIdentificarMe, linha 83.

```text
JSON path "$.length()" expected:<4> but was:<6>
```

O endpoint entrega os seis campos de TokenResponse da Aula 16, já comprovados no Checkpoint 3. O teste antigo ainda exige quatro. A linha 88 do mesmo método também exige exatamente as claims antigas, sem sid; essa asserção não foi alcançada porque a primeira já falhou. Não deve ser contada como terceira falha executada, mas é outra adaptação necessária nesse mesmo cenário.

Diagnóstico: teste histórico ainda não adaptado ao contrato novo; não há evidência de defeito de produção nessa falha. Uma futura adaptação deve conferir os seis campos e sid sem perder as verificações existentes de assinatura, issuer, subject, jti, perfis e prazos. Nenhuma adaptação foi feita nesta etapa.

### 2. Propriedade de sistema interferindo no teste isolado de bootstrap

Arquivo: src/test/java/com/curso/instrumento2026/config/AdministradorInicialConfigTest.java, método propriedadesDaAplicacaoDevemUsarVariaveisExternasEDefaultFalse, linha 61.

```text
expected: <true> but was: <false>
```

O teste adiciona application.properties como ResourcePropertySource ao final e fornece INITIAL_ADMIN_ENABLED=true ao seu ApplicationContextRunner, esperando resolver o placeholder app.security.initial-admin.enabled=${INITIAL_ADMIN_ENABLED:false}. O comando global, porém, passou diretamente -Dapp.security.initial-admin.enabled=false como propriedade de sistema. Essa chave final tem precedência sobre o valor do arquivo: o teste obtém false antes mesmo de resolver o placeholder que queria exercitar.

Diagnóstico confirmado pelo comando, configuração do teste e asserção: interferência da invocação global no teste isolado, não falha demonstrada do bootstrap de produção nem influência da fixture administrativa existente. O contexto desse teste usa AdministradorInicialService mockado e não escreve no PostgreSQL.

Na retomada, deve-se revisar a forma de desabilitar bootstrap global sem impedir esse teste de configurar seu próprio contexto. INITIAL_ADMIN_ENABLED=false no ambiente já é uma proteção disponível, mas qualquer ajuste e nova execução aguardam autorização; não se deve ignorar o teste ou enfraquecer a proteção do destino.

Depois do resultado, nenhuma correção de código, configuração ou teste foi aplicada e a suíte não foi repetida.

## JaCoCo — coleta nova, sem relatório ou gate executado

prepare-agent foi executado e indicou:

```text
destfile=C:\Projetos\instrumento2026\target\jacoco.exec
```

A coleta nova foi criada nesta execução, após clean, e tem **828.001 bytes**, última escrita em 19:34:22 -03:00. SHA-256: `4E269020435A96066D7390AB0B00F4B990E35A9E7210F192DCEDA81D34D57B0F`.

O POM mantém os caminhos padrão: report e check usariam target/jacoco.exec. Não houve sobreposição de arquivos antigos ou propriedade customizada de dataFile/destFile. Entretanto, **nenhum dos dois goals executou**: a falha em Surefire interrompeu o lifecycle antes de verify.

| Item | Situação desta tentativa |
|---|---|
| prepare-agent | Executado |
| Coleta nova | Criada em target/jacoco.exec |
| Relatório HTML/XML | Não gerado; target/site/jacoco/index.html e jacoco.xml ausentes |
| Cobertura final de linhas/branches | Não apurada por relatório nesta tentativa |
| check dos mínimos 70% / 40% | Não executado |
| All coverage checks have been met | Ausente |
| Avisos de incompatibilidade de classes | Não apareceram no log; report/check não executados, portanto isso não comprova compatibilidade dos relatórios |

Não foi executado JaCoCo separadamente após a falha, nem reaproveitada a cobertura histórica da Aula 15 como se fosse atual. Não é correto declarar o portão aprovado ou apresentar um percentual final da Aula 16 com esta execução.

## Banco e fixtures após a suíte

Consultas posteriores, explícitas e somente leitura, confirmaram:

| Verificação | Resultado final |
|---|---|
| Banco / host / porta / schema | instrumento2026_test / 127.0.0.1 / 5432 / public |
| ChangeSets | 44 |
| Lock Liquibase | false |
| Pessoas / contas / sessões totais | 3 / 3 / 11 |
| Assinatura das três pessoas protegidas | Igual à anterior |
| Assinatura das três contas protegidas | Igual à anterior |
| Assinatura dos vínculos de perfil | Igual à anterior |
| Assinatura das 11 sessões protegidas | Igual à anterior |
| Assinatura dos registros databasechangelog | Igual à anterior |

As pessoas 9742–9744, contas 2511–2513 e sessões 70–80 do Postman foram preservadas integralmente, não apenas suas contagens. Permanecem administrador ATIVA/ATIVO e dois operadores BLOQUEADA/INATIVO; todas as 11 sessões continuam revogadas, com os mesmos motivos e instantes. Não houve exclusão física desses registros nem fixture adicional residual da suíte. O banco de desenvolvimento não foi acessado.

## Separação das evidências e pendências

| Evidência | Estado |
|---|---|
| Checkpoint 3: 16 testes HTTP integrados anteriores | Histórico aprovado |
| OpenAPI: 63 testes isolados anteriores | Histórico aprovado |
| Newman: 40 requisições / 108 asserções anteriores | Histórico aprovado, não reexecutado |
| Validação global desta etapa | 771 testes, duas falhas, BUILD FAILURE |
| Portão JaCoCo global | Ainda pendente |

Pendências para a retomada e o fechamento documental:

1. Autorizar a adaptação do teste histórico de login ao par e sid, preservando sua força.
2. Revisar a invocação/proteção de bootstrap para não contaminar o teste isolado de propriedades.
3. Somente depois de nova autorização, repetir clean verify e obter relatório/check sobre a mesma coleta nova, percentuais reais e mensagem de aprovação do portão.
4. Consolidar as entregas/documentação final e decidir o destino das fixtures mantidas, sem exclusão implícita.

Criado somente este documento entre os candidatos ao versionamento. Houve artefatos temporários de auditoria e build, sem alteração de produção, testes, POM, migrations, configurações ou README principal. git diff --check não apontou erros. Nenhum commit/push ou avanço para o fechamento foi realizado. **Validação global não concluída; aguarda revisão.**

## Correção autorizada das duas pendências

### Login: atualização exclusiva das asserções históricas

O contrato real foi conferido em TokenResponse e TokenService: o login entrega seis campos e o JWT emitido pela sessão inclui sid. O cenário histórico AutenticacaoJwtTest.loginDeveRetornarBearerComClaimsControladosPeloServidorEIdentificarMe foi atualizado para:

- Exigir exatamente os seis campos, incluindo refresh não vazio e prazo de refresh em string UTC interpretável como Instant.
- Exigir exatamente as seis claims originais mais sid (iss, sub, iat, exp, jti, perfis e sid: sete no total).
- Validar sid como UUID e conferir sua correspondência com familia_id da sessão persistida para a pessoa da própria fixture.
- Conferir que o limite absoluto de refresh ocorre depois da emissão do JWT.

Permaneceram as verificações de assinatura/decodificação reais, HS256, issuer, subject, jti UUID, perfis, duração de 1800 segundos, expiração e ausência de senha/hash na resposta. Nenhum método de teste ou cenário foi removido; as alterações estão restritas às asserções desse método e à leitura necessária para correlacionar sid à sua família. Services, controllers e DTOs não foram alterados.

### Bootstrap: correção da invocação, sem alterar teste ou produção

A causa da segunda falha foi confirmada pela comparação entre o comando histórico e AdministradorInicialConfigTest. A propriedade de sistema app.security.initial-admin.enabled=false selecionava diretamente o valor final e tinha precedência sobre o application.properties acrescentado ao final das fontes do ApplicationContextRunner. Assim, INITIAL_ADMIN_ENABLED=true do cenário não conseguia exercitar o placeholder do arquivo.

A correção consistiu em **não passar -Dapp.security.initial-admin.enabled=false**. INITIAL_ADMIN_ENABLED=false foi mantido somente no ambiente privado do processo, sem editar .env/configuração persistida. No contexto normal de teste, o placeholder mantém o bootstrap desabilitado. No cenário isolado, withPropertyValues(INITIAL_ADMIN_ENABLED=true) tem precedência sobre o ambiente e pode habilitar sua própria configuração.

Antes de executar, confirmou-se que AdministradorInicialConfigTest não usa SpringBootTest, DataSource, repository ou serviço real: constrói ApplicationContextRunner somente com a configuração/runner e AdministradorInicialService mockado. ApplicationContextRunner não executa automaticamente ApplicationRunner; os cenários que chamam o runner manualmente usam o mock. As mensagens de “administrador criado” dessa execução são parte da verificação de logs do mock, não provisionamento no banco. Nenhum arquivo de bootstrap foi alterado.

### Segurança e preservação das fixtures

Preflight somente leitura confirmou novamente instrumento2026_test em 127.0.0.1:5432/public, 44 changeSets e lock false. A importação de .env no Spring foi neutralizada com a localização opcional inexistente; usuário/senha de DataSource e Liquibase foram fornecidos pelo ambiente privado, sem argumentos sensíveis. Overrides de inicialização/configuração e APP_SECURITY_INITIAL_ADMIN_ENABLED foram verificados como ausentes.

AutenticacaoJwtTest usa PostgreSQL e limpa somente os IDs criados por ele. Seus identificadores não colidem com as fixtures remanescentes; o bootstrap global estava desabilitado por INITIAL_ADMIN_ENABLED=false. O teste isolado de bootstrap não acessou banco.

As assinaturas do estado integral das pessoas 9742–9744, contas 2511–2513, quatro vínculos de perfil e sessões 70–80 foram registradas antes dos testes e comparadas depois: **todas iguais**. O databasechangelog também permaneceu igual. Totais finais: três pessoas, três contas, quatro vínculos e 11 sessões; 44 changeSets, lock livre. Nenhuma fixture Postman foi excluída ou modificada, e nenhum registro adicional permaneceu.

### Comandos direcionados e resultados

Cada classe foi executada uma vez, separadamente, com INITIAL_ADMIN_ENABLED=false no ambiente do processo. Abaixo, a base comum de argumentos (não executar a suíte nesta etapa):

```text
-Dspring.profiles.active=test
-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test
-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test
-Dspring.datasource.hikari.schema=public
-Dspring.liquibase.default-schema=public
-Dspring.config.import=optional:file:./target/aula16-no-external-config.properties
```

1. `mvnw.cmd -B -Dtest=AutenticacaoJwtTest` com a base acima, `-Dspring.test.mockmvc.print=NONE`, logging Liquibase INFO e SQL/binds OFF, `-Djacoco.destFile=target/aula16-correcoes-jwt.exec -Djacoco.append=false test`. Log: target/aula16-correcoes-jwt.log. Encerramento **17/11/2026 19:48:05 -03:00**, código **0**.
2. `mvnw.cmd -B -Dtest=AdministradorInicialConfigTest` com a mesma base, `-Djacoco.destFile=target/aula16-correcoes-bootstrap.exec -Djacoco.append=false test`. Log: target/aula16-correcoes-bootstrap.log. Encerramento **17/11/2026 19:48:55 -03:00**, código **0**. Os argumentos de destino não fazem essa classe conectar ao banco: o contexto continua isolado.

| Classe | Executados | Aprovados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|---:|
| AutenticacaoJwtTest | 16 | 16 | 0 | 0 | 0 |
| AdministradorInicialConfigTest | 6 | 6 | 0 | 0 | 0 |
| Total direcionado | 22 | 22 | 0 | 0 | 0 |

Ambos terminaram com BUILD SUCCESS. Não houve falha inesperada. Os 22 casos não são somados aos 771 da tentativa global para formar um resultado global aprovado. A coleta histórica dessa tentativa global não foi substituída: os testes direcionados usam arquivos JaCoCo separados, sem executar report/check.

### Comando revisado preparado após as correções de 17/11/2026

Depois de nova autorização e do preflight de destino/fixtures, preparar privadamente DB_TEST_USERNAME/PASSWORD e os correspondentes SPRING_DATASOURCE_USERNAME/PASSWORD e SPRING_LIQUIBASE_USER/PASSWORD. Conferir ausência da chave final app.security.initial-admin.enabled em propriedades de sistema/MAVEN_ARGS/MAVEN_OPTS/JAVA_TOOL_OPTIONS e de overrides alternativos. Manter o caminho opcional neutro inexistente.

```powershell
$initialAdminAntes = $env:INITIAL_ADMIN_ENABLED
try {
    $env:INITIAL_ADMIN_ENABLED = 'false'
    .\mvnw.cmd -B `
      '-Dspring.profiles.active=test' `
      '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
      '-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
      '-Dspring.datasource.hikari.schema=public' `
      '-Dspring.liquibase.default-schema=public' `
      '-Dspring.config.import=optional:file:./target/aula16-no-external-config.properties' `
      '-Dspring.test.mockmvc.print=NONE' `
      '-Dlogging.level.liquibase=INFO' `
      '-Dlogging.level.org.hibernate.SQL=OFF' `
      '-Dlogging.level.org.hibernate.orm.jdbc.bind=OFF' clean verify
} finally {
    $env:INITIAL_ADMIN_ENABLED = $initialAdminAntes
}
```

Não há a propriedade de sistema conflitante, seleção de classes, skips ou tolerância a falhas. Também não há override de dataFile/destFile: após clean, prepare-agent, report e check usarão a coleta padrão nova, conforme o POM. Se redirecionar stdout/stderr durante clean, usar arquivo fora de target para não manter um arquivo aberto no diretório a remover.

### Histórico: estado após as correções direcionadas

**As duas pendências estão resolvidas no escopo das classes afetadas.** Isso ainda não comprova que o próximo clean verify completo passará nem aprova o portão JaCoCo. Permanecem pendentes a execução global autorizada, os percentuais finais e a mensagem de aprovação do check.

Arquivos modificados nesta correção: src/test/java/com/curso/instrumento2026/AutenticacaoJwtTest.java e este documento. AdministradorInicialConfigTest e todo o código de produção permanecem sem alteração. POM manteve o SHA-256 registrado antes; não houve edição de migrations, configurações, README principal, execução Newman, suíte completa, commit ou push. git diff --check sem erros. Aguarda-se revisão antes de repetir a suíte completa.

## Segunda validação global — aprovada em 18/11/2026

### Preflight completo e preservação de evidências

A autorização desta etapa limitou-se ao código já implementado e ao comando revisado acima. O Git continuava com as alterações acumuladas da Aula 16, sem staging/commit; o inventário foi registrado antes do run. Nenhuma alteração de fonte foi feita para esta execução.

Antes de executar, foram conferidos POM, application-test.properties, comando documentado, variáveis de ambiente e arquivos .mvn. As únicas variáveis de infraestrutura relevantes já presentes eram JAVA_HOME e MAVEN_HOME; não havia MAVEN_ARGS, MAVEN_OPTS, JAVA_TOOL_OPTIONS, JDK_JAVA_OPTIONS, SPRING_APPLICATION_JSON, localizações alternativas de configuração/DataSource nem APP_SECURITY_INITIAL_ADMIN_ENABLED. Não havia .mvn/maven.config ou .mvn/jvm.config impondo propriedades adicionais.

O POM permaneceu com JaCoCo 0.8.16, prepare-agent e report/check em verify; limites BUNDLE/COVEREDRATIO de LINE 0.70 e BRANCH 0.40, sem nova exclusão. SHA-256 antes/depois: `22C09DCE2C856AA51517B39E9B655CFF58CF53F0FFD25C382C787EBA0C8BCF1E`.

Consultas somente leitura confirmaram:

| Verificação anterior | Resultado |
|---|---|
| Banco / host / porta / schema | instrumento2026_test / 127.0.0.1 / 5432 / public |
| ChangeSets registrados e contados nos arquivos | 44 |
| Lock Liquibase | false |
| Pessoas / contas / perfis / sessões | 3 / 3 / 4 / 11 |
| IDs das fixtures protegidas | Pessoas 9742–9744, contas 2511–2513, sessões 70–80 |
| Assinaturas comparadas com o estado histórico após as correções | Todas iguais |
| Colisões de CPF entre fixtures protegidas e literais dos testes | 0 |

As limpezas dos testes continuam restritas às próprias fixtures ou rollback; não houve mudança de código desde a revisão anterior que ampliasse seu alcance. Foram registradas novas assinaturas integrais das pessoas, contas, perfis, sessões e databasechangelog imediatamente antes deste run, para comparação posterior.

O destino foi imposto nas URLs do DataSource **e** Liquibase; schemas public explícitos. A importação de .env no Spring foi neutralizada pela localização opcional inexistente target/aula16-no-external-config.properties, cuja ausência foi conferida. As credenciais foram lidas privadamente em memória e fornecidas somente pelo ambiente temporário do processo; o ambiente anterior foi restaurado em finally. Nenhuma senha foi passada em argumento Maven ou registrada na documentação.

INITIAL_ADMIN_ENABLED=false ficou no ambiente do processo. **Não foi definido -Dapp.security.initial-admin.enabled=false.** Assim, o contexto normal mantém bootstrap desabilitado, enquanto o teste isolado conserva sua própria configuração explícita com serviço mockado.

Os artefatos aula16-* anteriores, a coleta global anterior e os relatórios Surefire anteriores foram copiados antes do clean para um diretório fora de target. Nesse mesmo diretório foram registrados inventário Git, assinaturas do banco e SHA-256 dos arquivos de src/POM:

```text
C:\Users\pr472\AppData\Local\Temp\instrumento2026-aula16-global-final-25f132684e1641a78dca40dee6af25c4
```

O log do novo Maven foi aberto nesse diretório, evitando conflito de remoção de arquivo aberto durante clean. As evidências da primeira execução e do Newman não foram reutilizadas como resultados atuais nem descartadas.

### Execução única e resultado Maven

Foi executado exatamente o comando revisado documentado acima: clean verify completo, profile test, URLs/schemas explícitos, importação neutra e bootstrap somente via ambiente. Não houve -Dtest, skipTests, maven.test.skip, tolerância a falhas, seleção de classes, skip do JaCoCo ou override de destFile/dataFile.

Encerramento: **18/11/2026 08:31:25 -03:00**. Duração Maven: **3 minutos e 50 segundos**. Código de saída **0**.

| Métrica desta execução | Resultado |
|---|---:|
| Classes de teste com XML Surefire | 64 |
| Testes executados | 771 |
| Aprovados | 771 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 0 |

Os totais foram conferidos somando os 64 relatórios XML gerados após clean, além do resumo Maven:

```text
Tests run: 771, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

As duas classes anteriormente afetadas passaram dentro desta suíte completa, sem propriedade conflitante ou testes ignorados. A execução não exigiu alteração de código/configuração e não foi repetida nesta autorização. O empacotamento e repackage Spring Boot também foram concluídos antes de verify.

### JaCoCo: mesma coleta nova, relatório e portão oficial

O clean eliminou a coleta anterior do diretório de build. prepare-agent apontou para target/jacoco.exec. A coleta desta execução foi criada em **18/11/2026 08:28:03 -03:00**, com última escrita **08:31:16 -03:00**, tamanho **827.506 bytes**, SHA-256:

```text
5EBC0D3BBB23C198316D5DC60A144AD51AD9822F0A52321CC229B2CD40468837
```

O log comprova os dois goals usando esse mesmo caminho:

```text
--- jacoco:0.8.16:report (report-coverage) @ instrumento2026 ---
Loading execution data file C:\Projetos\instrumento2026\target\jacoco.exec
Analyzed bundle 'instrumento2026' with 104 classes

--- jacoco:0.8.16:check (report-coverage) @ instrumento2026 ---
Loading execution data file C:\Projetos\instrumento2026\target\jacoco.exec
Analyzed bundle 'instrumento2026' with 104 classes
All coverage checks have been met.
```

Não houve aviso de incompatibilidade de classes nos dados de execução. Os relatórios HTML e XML foram gerados em target/site/jacoco/index.html e target/site/jacoco/jacoco.xml. Os valores abaixo vêm dos contadores raiz desse XML, não de média de percentuais de classes:

| Contador | Cobertos | Não cobertos | Total | Cobertura | Limite oficial | Resultado |
|---|---:|---:|---:|---:|---:|---|
| LINE | 1.416 | 46 | 1.462 | 96,85% | 70% | Aprovado |
| BRANCH | 505 | 51 | 556 | 90,83% | 40% | Aprovado |

Escopo medido: bundle instrumento2026, **104 classes de produção analisadas** em target/classes, conforme o escopo padrão do report Maven; não inclui classes de teste. Não foram introduzidas exclusões ou alterado o escopo para melhorar os números. Os percentuais são arredondados para duas casas; o check avalia os ratios dos contadores completos.

### Conferência posterior de banco, fontes e fixtures

As mesmas consultas somente leitura, após o Maven, confirmaram instrumento2026_test em 127.0.0.1:5432/public, **44 changeSets e lock false**. Nenhum changeSet novo foi aplicado; a assinatura integral de databasechangelog permaneceu igual.

| Conjunto protegido | Antes | Depois | Comparação integral |
|---|---:|---:|---|
| Pessoas 9742–9744 | 3 | 3 | Assinatura igual |
| Contas 2511–2513 | 3 | 3 | Assinatura igual |
| Vínculos de perfil dessas contas | 4 | 4 | Assinatura igual |
| Sessões 70–80 | 11 | 11 | Assinatura igual |
| databasechangelog | 44 | 44 | Assinatura igual |

Os totais dessas tabelas também continuam 3 pessoas, 3 contas, 4 vínculos e 11 sessões: nenhum registro adicional de teste permaneceu. As fixtures Postman não foram excluídas ou alteradas: administrador ativo, operadores bloqueados/pessoas inativas e as mesmas 11 sessões revogadas, com motivos e instantes preservados. Não houve acesso ao banco de desenvolvimento.

A comparação SHA-256 de todos os arquivos de src e do POM antes/depois registrou **zero alterações**. Não houve mudança em código de produção, testes, migrations ou configurações. Os snapshots antes/depois e a comparação estão no arquivo de auditoria temporário; nenhum conteúdo de senha/hash das linhas foi divulgado.

### Evidências finais desta autorização e limites

- Log Maven: diretório temporário acima/clean-verify.log, também copiado para target/aula16-validacao-global-final.log.
- Resultado consolidado: diretório temporário acima/resultado-final.json e target/aula16-validacao-global-final.json.
- Snapshots antes/depois do banco e assinaturas de fontes/POM: diretório temporário acima.
- Relatórios novos: target/surefire-reports e target/site/jacoco.

Os artefatos de build/auditoria são temporários, fora dos candidatos ao commit. A única alteração documental desta etapa é este arquivo. A tabela histórica de 771 testes com duas falhas permanece como primeira tentativa; os 22 testes direcionados continuam resultados intermediários, e os 40 requests/108 assertions Newman continuam uma execução anterior, não repetida ou somada à suíte atual.

**Validação global do escopo implementado aprovada**, incluindo o portão oficial JaCoCo. Esse resultado não é fechamento documental nem autorização de versionamento: nenhuma outra entrega foi implementada/revisada nesta etapa, README principal não foi atualizado, fixtures não foram removidas e não houve Newman, commit ou push. Aguarda-se revisão antes de avançar.
