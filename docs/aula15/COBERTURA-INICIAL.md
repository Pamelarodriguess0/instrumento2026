# Aula 15 — Checkpoint 3: medição inicial sem portão

> Baseline histórica anterior ao portão e ao TDD. Pendências e arquivos citados
> refletem essa execução; o [estado final](README.md#estado-final-da-aula-15) a sucede.

Registro em 11/11/2026. Projeto: instrumento2026.
Material: [Aula 15 do professor Jefferson](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/15aula/15aula.md),
seção 21, Checkpoint 3, e seção 16 para configuração do JaCoCo.

## Exigência e configuração mínima

O Checkpoint 3 exige medir antes de escolher limites, registrar linhas e branches
e explicar a diferença entre os percentuais. Não se aplica ainda o portão do
Checkpoint 4 nem as métricas obtidas no suporteos2026.

Antes das alterações, foram conferidos o POM, o inventário, as annotations de teste,
o profile `test` e as orientações históricas de PostgreSQL da Aula 14.
O [pom.xml](../../pom.xml) não tinha JaCoCo, limites ou customização de `argLine`.
Os testes com contexto completo usam `@ActiveProfiles("test")`; não foram encontrados
overrides dinâmicos de URL ou conexões JDBC próprias nos testes.

Foi acrescentado exclusivamente:

- Propriedade `jacoco.version=0.8.16`, versão indicada no material.
- Plugin `org.jacoco:jacoco-maven-plugin` com `prepare-agent`.
- Execution `report-coverage`, com `report` na fase `verify`.

Nesta medição o relatório foi solicitado explicitamente após `test`, por
`jacoco:report`. Não foi necessário executar `verify`. Não há `check`, regras,
limites mínimos ou exclusões adicionais. Surefire, sua seleção padrão de testes,
dependências e demais plugins foram preservados. O agente forneceu `argLine`
automaticamente; não foi introduzida substituição manual dessa propriedade.

## Segurança do banco antes da suíte

As credenciais de teste existentes foram lidas de `.env` apenas em memória.
Nenhuma senha foi passada na linha de comando, publicada ou gravada em documento.
O destino configurado no `.env` era `localhost:5432/instrumento2026_test`;
o banco de desenvolvimento tem outro nome, `instrumento2026_dev`.
O laboratório histórico da porta 55414 não estava ativo e não foi iniciado.

O cliente `C:\Program Files\PostgreSQL\17\bin\psql.exe` conectou com host,
porta, usuário e banco explícitos, `-X -w -v ON_ERROR_STOP=1` e senha temporária em
`PGPASSWORD`. Foram executadas consultas em transação `BEGIN READ ONLY`:

```sql
SELECT current_database(), current_user, inet_server_addr(), inet_server_port(), current_schema();
SELECT count(*) FROM public.databasechangelog;
SELECT locked FROM public.databasechangeloglock;
```

Resultado confirmado antes da medição:

| Item | Valor |
|---|---|
| Banco | `instrumento2026_test` |
| Usuário | `instrumento_app` |
| Servidor | `127.0.0.1:5432` |
| Schema | `public` |
| Tabelas no schema public | 12 |
| ChangeSets registrados | 41 |
| Lock Liquibase | `false` |
| Pessoas, contas, categorias, instrumentos, clientes, colaboradores, vendas e itens | Zero registros em cada tabela |
| Fornecedores | Um registro preexistente |

O teste foi autorizado somente após a confirmação do destino. Para impedir que
o `.env` ou defaults redirecionassem o runtime, o comando fixou ambas as URLs e
os schemas e substituiu a importação externa por um caminho opcional cuja ausência
foi verificada. Não havia `MAVEN_ARGS`, `MAVEN_OPTS`, `JAVA_TOOL_OPTIONS` ou
`JDK_JAVA_OPTIONS` configurados na sessão. O bootstrap foi desabilitado somente
pela variável de ambiente `INITIAL_ADMIN_ENABLED=false`, preservando o teste
isolado que verifica sua habilitação.

## Comando efetivo da medição aprovada

PowerShell, na raiz do projeto. Antes do comando, as variáveis abaixo receberam as
credenciais de teste já existentes, sem exibi-las:

- `DB_TEST_USERNAME` e `DB_TEST_PASSWORD` para DataSource.
- `SPRING_LIQUIBASE_USER` e `SPRING_LIQUIBASE_PASSWORD` com as mesmas credenciais.
- `INITIAL_ADMIN_ENABLED=false`.

Os valores anteriores dessas variáveis foram restaurados em `finally`.

```powershell
.\mvnw.cmd -B `
  "-Dspring.profiles.active=test" `
  "-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" `
  "-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" `
  "-Dspring.datasource.hikari.schema=public" `
  "-Dspring.liquibase.default-schema=public" `
  "-Dspring.config.import=optional:file:./target/aula15-no-external-config.properties" `
  test jacoco:report > target/aula15-checkpoint3-medicao.log 2>&1
$coverageExitCode = $LASTEXITCODE
```

Não criar o arquivo opcional indicado. Para uma repetição futura, conferir novamente
destino, credenciais e fixtures, e separar dados de cobertura anteriores antes de
executar; o JaCoCo pode acumular dados existentes. A medição aprovada começou sem
`target/jacoco.exec`, separando o arquivo da tentativa com erro.

## Tentativas e resultados reais

Uma primeira chamada de ferramenta expirou na revisão automática antes de executar
qualquer alteração ou medição; a ausência de alterações e artefatos foi conferida
antes da nova chamada.

A primeira execução Maven efetiva terminou em `BUILD FAILURE`, código 1:
671 casos reportados, 410 aprovados, zero falhas de asserção, 261 erros e zero
ignorados. O comando havia fixado uma URL própria para Liquibase, mas não suas
credenciais; os contextos com banco falharam com autenticação SCRAM sem senha.
Os resultados não foram utilizados como baseline e o goal de relatório não chegou
a executar. Nenhum teste foi alterado para contornar a falha.

Após passar também as credenciais de Liquibase pelo ambiente, a segunda execução
terminou em **11/11/2026 às 19:39:06 -03:00**:

```text
Tests run: 671, Failures: 0, Errors: 0, Skipped: 0
Analyzed bundle 'instrumento2026' with 96 classes
BUILD SUCCESS
MAVEN_EXIT_CODE=0
```

| Resultado da execução válida | Valor |
|---|---:|
| Classes de teste | 54 |
| Casos executados/aprovados | 671 / 671 |
| Falhas / erros / ignorados | 0 / 0 / 0 |
| Tempo total informado pelo Maven | 1 min 29 s |

As duas tentativas não são somadas como uma suíte. Os 54 XMLs Surefire foram
conferidos após a execução válida. A contagem de casos coincide com o inventário
histórico, agora comprovada também com a instrumentação JaCoCo.

Durante a suíte, Hibernate registrou a URL explícita de `instrumento2026_test`.
Uma consulta somente leitura em `pg_stat_activity` encontrou dez conexões
`PostgreSQL JDBC Driver`, originadas de `127.0.0.1`, nesse banco. O teste sem JDBC
que inicializa metadados Hibernate também registra URL `undefined/unknown`;
isso não representa uma conexão com outro banco.

Depois da suíte, consultas somente leitura confirmaram novamente o mesmo banco,
41 changeSets, lock livre e as mesmas contagens de registros anteriores.
Nenhum changelog foi alterado nem novo changeSet registrado. Os testes fizeram
suas escritas/rollbacks e limpezas no banco de teste; sequências podem avançar.
A comparação de contagens não é uma auditoria de igualdade de todas as linhas.
O banco de desenvolvimento não foi escolhido como destino de nenhum comando.

## Cobertura inicial e escopo medido

Valores calculados a partir dos contadores agregados de
`target/site/jacoco/jacoco.xml`, exclusivamente da execução válida:

| Métrica | Cobertos | Não cobertos | Total | Cobertura |
|---|---:|---:|---:|---:|
| Linhas (`LINE`) | 1.233 | 41 | 1.274 | **96,78%** |
| Branches (`BRANCH`) | 398 | 44 | 442 | **90,05%** |

Fórmula: `cobertos / (cobertos + não cobertos) × 100`, arredondada a duas casas.
O HTML apresenta percentuais inteiros; para evitar confundir instruções com linhas,
os percentuais de linhas acima vieram especificamente do contador `LINE` no XML.

O relatório analisa 96 classes com instruções, em dez pacotes de produção, a partir
de `target/classes`, dos fontes de `src/main/java` e do arquivo `target/jacoco.exec`.
Inclui domínio, services, controllers, DTOs, mappers, validadores, configuração,
exceções e classe da aplicação, sujeitos aos filtros padrão do JaCoCo. Não foram
encontradas classes compiladas sem fonte correspondente. O número de fontes Java
não equivale ao número de classes com instruções contabilizadas pelo JaCoCo.

A coleta reúne os testes unitários, contextos reduzidos, MockMvc, persistência e
concorrência executados nesta JVM de testes. Não mede cobertura dos próprios testes,
do PostgreSQL, das migrations YAML ou de Newman/Postman. Não inclui servidor
implantado, carga, interface gráfica ou mutação.

Linhas e branches têm denominadores distintos. Uma linha pode ser alcançada sem
que os dois resultados de uma decisão sejam exercitados; expressões com curto-circuito
também deixam alternativas não visitadas. Nesta execução, restam 41 linhas e 44
branches não cobertos, e a proporção de alternativas de decisão não percorridas é
maior. Percentual alto não comprova qualidade dos oráculos, todas as combinações
de condições, ausência de defeitos ou todos os escalonamentos concorrentes.

## Artefatos e limitações

- [Relatório HTML local](../../target/site/jacoco/index.html).
- `target/site/jacoco/jacoco.xml` e `jacoco.csv`: contadores exportados.
- `target/jacoco.exec`: dados da execução válida.
- `target/aula15-checkpoint3-medicao.log`: log do build aprovado.
- `target/aula15-checkpoint3-tentativa1.log` e `.exec`: tentativa com erro, separada.

Todos permanecem em `target`, ignorados pelo Git. O HTML foi gerado e seu total
conferido por leitura; não houve inspeção gráfica em navegador. Os avisos de
instrumentação dinâmica do Mockito não impediram a suíte nem o JaCoCo; nenhuma
flag foi acrescentada para ocultá-los.

Arquivos desta etapa: `pom.xml`, `docs/aula15/README.md`,
`docs/aula15/EVIDENCIAS.md` e este documento novo. A alteração de `ContaAcessoTest`
que já estava no workspace pertence ao Checkpoint 2 e foi preservada.

Não há limite escolhido nem aprovação por cobertura: o build passou pelos testes
e pela geração do relatório. Para o Checkpoint 4, resta avaliar e justificar os
limites a partir desta base e, quando autorizado, configurar/verificar o portão.
**Checkpoint 4 não iniciado. Sem commit, tag ou push.**
