# Aula 15 — Checkpoint 4: portão de cobertura

> Registro histórico do Checkpoint 4, com 671 casos. As etapas posteriores foram
> concluídas conforme o [estado final da Aula 15](README.md#estado-final-da-aula-15).

Data: 16/11/2026. Projeto: instrumento2026.
Material consultado antes de alterar o POM:
[Aula 15 do professor Jefferson](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/15aula/15aula.md),
seções 16 e 21, Checkpoint 4.

## Exigência e limites adotados

O Checkpoint 4 pede `clean verify`, testes sem falhas, relatório HTML e aprovação
com linhas acima de 70% e branches acima de 40%. A configuração da seção 16
define esses mínimos com `BUNDLE` e `COVEREDRATIO`. Portanto, são os valores
explicitamente utilizados neste checkpoint, não metas novas escolhidas para o tema.
O número de testes da referência é adaptado à suíte existente do instrumento2026.

| Configuração | Valor |
|---|---|
| Versão JaCoCo | `0.8.16`, preservada |
| Escopo da regra | `BUNDLE`, conjunto da aplicação |
| Métrica | `COVEREDRATIO` |
| `jacoco.minimum.line.coverage` | `0.70` — mínimo de 70% das linhas |
| `jacoco.minimum.branch.coverage` | `0.40` — mínimo de 40% dos branches |
| Momento do portão | Fase `verify`, após `report` |

No [POM](../../pom.xml), a execution `prepare-jacoco-agent` foi preservada.
A execution existente `report-coverage` mantém `report` e agora inclui `check`
e as duas regras de mínimo. Não há exclusões, propriedades para ignorar falhas,
alteração de `argLine`, seleção reduzida de testes ou regra por pacote/classe.

A [base do Checkpoint 3](COBERTURA-INICIAL.md) foi 96,78% das linhas e 90,05%
dos branches. Os mínimos oficiais estão abaixo dessa base e oferecem apenas
proteção global contra regressões amplas; não congelam a cobertura atual.
Uma classe bem coberta pode compensar outra pouco exercitada. Não foi introduzida
uma política mais rigorosa nem realizado o aumento temporário do Checkpoint 5.

## Segurança do banco e preparação

Antes da execução, foi repetido o procedimento do Checkpoint 3:

- Conferência do profile `test`, do destino existente e da ausência de overrides
  dinâmicos de URL/conexões JDBC próprias nos testes.
- Consulta explícita por `psql -X -w -v ON_ERROR_STOP=1`, em `BEGIN READ ONLY`,
  confirmando `current_database()=instrumento2026_test`, usuário `instrumento_app`,
  servidor `127.0.0.1:5432` e schema `public`.
- 41 changeSets e lock Liquibase livre. Pessoas, contas, categorias, instrumentos,
  clientes, colaboradores, vendas e itens vazios; um fornecedor preexistente.
- Ausência de `MAVEN_ARGS`, `MAVEN_OPTS`, `JAVA_TOOL_OPTIONS` e `JDK_JAVA_OPTIONS`.
- Caminho opcional `target/aula15-no-external-config.properties` conferido ausente.

As credenciais de teste foram obtidas do `.env` somente em memória, sem imprimi-las.
As variáveis `DB_TEST_USERNAME` e `DB_TEST_PASSWORD` alimentaram DataSource;
`SPRING_LIQUIBASE_USER` e `SPRING_LIQUIBASE_PASSWORD` receberam os mesmos valores.
`INITIAL_ADMIN_ENABLED=false` foi definido no ambiente, sem propriedade JVM
global que interferisse nos testes isolados de configuração do bootstrap.
Todas essas variáveis foram restauradas em `finally`.

DataSource e Liquibase receberam explicitamente a mesma URL no comando. Durante
a suíte, uma consulta somente leitura em `pg_stat_activity` encontrou dez conexões
`PostgreSQL JDBC Driver` para `instrumento2026_test`, originadas de `127.0.0.1`.
Hibernate também registrou a URL explícita de teste. Nenhum comando escolheu o
banco de desenvolvimento como destino.

## Comando executado

PowerShell, na raiz do projeto, com as variáveis de ambiente acima preparadas.
O log foi inicialmente escrito em um arquivo temporário fora de `target`, pois
`clean` remove esse diretório, e copiado para `target/aula15-checkpoint4-verify.log`
após a execução:

```powershell
$runLog = Join-Path $env:TEMP ('instrumento2026-aula15-checkpoint4-' + [guid]::NewGuid() + '.log')
.\mvnw.cmd -B `
  "-Dspring.profiles.active=test" `
  "-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" `
  "-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" `
  "-Dspring.datasource.hikari.schema=public" `
  "-Dspring.liquibase.default-schema=public" `
  "-Dspring.config.import=optional:file:./target/aula15-no-external-config.properties" `
  clean verify > $runLog 2>&1
$gateExitCode = $LASTEXITCODE
Copy-Item -LiteralPath $runLog -Destination target/aula15-checkpoint4-verify.log
```

`clean` impede acumular a cobertura anterior: o relatório e o portão desta etapa
usam apenas a execução nova. Logs antigos em `target` são artefatos locais
descartáveis; os registros documentais dos checkpoints anteriores foram preservados.
Para repetir, é necessário reconferir destino, fixtures e credenciais em vez de
presumir que o estado do laboratório continua igual.

## Resultado da verificação normal

Conclusão em **16/11/2026 às 10:39:51 -03:00**, tempo Maven de **2 min 08 s**,
uma execução de `clean verify`, código de saída **0**:

```text
Tests run: 671, Failures: 0, Errors: 0, Skipped: 0
--- jacoco:0.8.16:report (report-coverage) ---
Analyzed bundle 'instrumento2026' with 96 classes
--- jacoco:0.8.16:check (report-coverage) ---
All coverage checks have been met.
BUILD SUCCESS
```

Os 54 relatórios XML Surefire foram conferidos: **671 aprovados, zero falhas,
erros e ignorados**. Os valores abaixo foram calculados pelos contadores agregados
do novo `target/site/jacoco/jacoco.xml`, com arredondamento a duas casas:

| Métrica | Cobertos / total | Resultado | Mínimo configurado | Portão |
|---|---:|---:|---:|---|
| Linhas | 1.233 / 1.274 | 96,78% | 70% | Atendido |
| Branches | 398 / 442 | 90,05% | 40% | Atendido |

Os contadores coincidem com a base do Checkpoint 3, embora tenham sido coletados
novamente após `clean`. O escopo é o bundle de produção, 96 classes analisadas,
usando os filtros padrão JaCoCo. Não é uma exigência de mínimo para cada classe
ou pacote. A medição não inclui código dos testes, SQL do PostgreSQL, YAML de
migrations ou execução Newman/Postman. Cobertura não comprova ausência de defeitos.

O resultado confirma o fluxo normal: compilação, suíte completa, empacotamento,
relatório e aplicação dos limites na fase `verify`. Não houve override dos mínimos,
`skip`, exclusão ou falha provocada para testar a reprovação do portão.

Após a suíte, consultas somente leitura confirmaram `instrumento2026_test`,
schema `public`, 41 changeSets, lock livre e as mesmas contagens iniciais em todas
as tabelas conferidas. Não se afirma igualdade de todo o conteúdo com base apenas
em contagens. Testes integrados fizeram suas escritas/rollbacks e limpezas no banco
de teste; sequências podem ter avançado. Nenhuma migration foi editada.

## Artefatos, arquivos e encerramento

- [HTML JaCoCo local](../../target/site/jacoco/index.html), gerado nesta execução.
- `target/site/jacoco/jacoco.xml` e `jacoco.csv`: métricas exportadas.
- `target/jacoco.exec`: coleta nova, sem acumular dados anteriores.
- `target/aula15-checkpoint4-verify.log`: saída de Maven, incluindo aprovação do `check`.
- `target/surefire-reports/TEST-*.xml`: resultados dos testes.

Esses artefatos são ignorados pelo Git. HTML/XML e links foram conferidos por
leitura; não houve inspeção gráfica em navegador. Avisos existentes do Mockito
não impediram testes, relatório ou portão e não foram ocultados com flags.

Arquivos alterados neste checkpoint: `pom.xml`, `docs/aula15/README.md` e
`docs/aula15/EVIDENCIAS.md`. Arquivo criado: este `PORTAO-COBERTURA.md`.
Fontes Java, testes, configurações de aplicação e migrations foram preservados.
A modificação de `ContaAcessoTest` que aparece no Git pertence ao Checkpoint 2;
seu hash SHA-256 permaneceu igual antes e depois desta execução.

**Checkpoint 4 concluído com build aprovado. Checkpoint 5 não iniciado:** a
demonstração de reprovação por limite temporário depende de autorização posterior.
Não houve commit, tag ou push.
