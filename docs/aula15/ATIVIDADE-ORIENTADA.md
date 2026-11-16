# Aula 15 — Atividade orientada e teste escolhido por risco

> Registro das seções 24–25, anterior ao TDD, com 674 casos. A seção 26 foi
> concluída posteriormente: [TDD.md](TDD.md) e [estado final](README.md#estado-final-da-aula-15).

Data: 16/11/2026. Projeto: instrumento2026.
Referência: [seções 24 e 25 da Aula 15](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/15aula/15aula.md).

## Escolha anterior ao teste

A seção 24, item 9, pede um teste novo selecionado por risco. A seção 25 pede uma
classe de domínio sem teste unitário dedicado, cenários válido/inválido, execução
isolada e completa, comparação de cobertura e justificativa de até dez linhas.
O mesmo entregável pode atender aos dois itens: o material não exige testes distintos.

Foram examinados `InstrumentoTest`, `CategoriaInstrumentoTest`, `AssociacaoInstrumentoTest`,
`CorrecaoAula08Test` e `VendaTest`, além dos respectivos fontes de domínio.
Estoque, overflow, troca de categoria, preservação de associações, preço histórico,
quantidade e arredondamento já têm cenários. Duplicá-los não foi o objetivo.

A classe escolhida foi [ItemVenda](../../src/main/java/com/curso/instrumento2026/domain/ItemVenda.java),
que era exercitada por `VendaTest`, mas ainda não tinha `ItemVendaTest` dedicado.
A busca por `new ItemVenda` nos testes encontrou apenas quatro construções diretas
em `VendaTest.itemDeveValidarPrecoEArredondarSubtotal`, todas com vínculos válidos.
O teste `VendaTest.deveRejeitarInstrumentoNulo` observa a validação de `Venda.adicionarItem`,
que rejeita o argumento antes de chamar o construtor de `ItemVenda`.

**Invariante escolhida:** todo item criado pelo construtor de domínio exige uma
venda e um instrumento. A regra já estava implementada antes da atividade:
`Objects.requireNonNull(venda, "Venda é obrigatória")` e
`Objects.requireNonNull(instrumento, "Instrumento é obrigatório")`.
O construtor protegido sem argumentos do JPA não faz parte desse contrato de criação.

### Justificativa do risco — cinco linhas

1. Um item sem venda perde o vínculo com a operação comercial que o originou.
2. Um item sem instrumento perde a identificação do produto vendido.
3. Aceitar esses estados pode causar falhas tardias no fluxo comercial ou na persistência.
4. Os testes anteriores não protegiam as duas rejeições no próprio construtor de ItemVenda.
5. O teste novo fornece feedback unitário imediato sem depender de Spring ou de constraints SQL.

Não foi estimada uma probabilidade numérica: trata-se de risco de integridade do
estado em memória. O caminho atual via `Venda` reduz a exposição; a defesa local
também importa para futuras chamadas dentro do pacote de domínio.

## Teste acrescentado e oráculo

Novo arquivo: [ItemVendaTest.java](../../src/test/java/com/curso/instrumento2026/domain/ItemVendaTest.java).

| Método | Cenário e resultado esperado |
|---|---|
| `deveCriarItemVinculadoAVendaEAoInstrumentoInformados` | Válido: o construtor mantém exatamente as referências fornecidas |
| `deveRejeitarItemSemVendaNaConstrucao` | Inválido: venda nula gera `NullPointerException` com a mensagem da regra |
| `deveRejeitarItemSemInstrumentoNaConstrucao` | Inválido: instrumento nulo gera `NullPointerException` com a mensagem da regra |

Os argumentos restantes são válidos para isolar o motivo da rejeição. As datas são
fixas e os dados fictícios. O oráculo é a obrigatoriedade dos dois vínculos no
contrato existente, não um cálculo que copia a implementação. O cenário válido
é o controle positivo dessa fronteira; não repete cálculo monetário ou estados de venda.

São três métodos `@Test`, sem mocks, contexto Spring, JPA ativo ou banco. Não foram
alterados produção, regras, migrations, configurações ou POM. Isso é caracterização
de regra existente, **não TDD**. A seção 26 continua pendente e não foi iniciada.

## Medição inicial e critérios de comparação

Antes de criar o teste, foram lidos os contadores de `ItemVenda` no XML existente
e preservados o XML e o arquivo de coleta em `target/aula15-atividade-risco/antes/`.
A origem é a suíte completa aprovada do Checkpoint 4, de 16/11/2026 às 10:39:51
-03:00, posteriormente reavaliada no Checkpoint 5 sem alterar sua coleta.
Não se afirma uma nova execução anterior nesta atividade, nem se refazem checkpoints.

A coleta inicial já tinha 24/24 linhas e 8/8 branches de `ItemVenda` cobertos.
O fato de a classe ter cobertura total não prova que as rejeições eram verificadas.

A comparação usa, antes e depois, JaCoCo 0.8.16, o nome de classe
`com/curso/instrumento2026/domain/ItemVenda`, os contadores `LINE` e `BRANCH`,
mesmo código de produção e suíte completa padrão. A única ampliação é a classe de
teste nova: 671 casos antes, 674 depois. Não se compara coleta isolada com completa.
Não foram introduzidas exclusões ou mudanças nos filtros padrão do JaCoCo.

## Execuções e banco seguro

Antes da suíte com banco, consultas `BEGIN READ ONLY`, por psql com destino explícito,
confirmaram `instrumento2026_test`, `127.0.0.1:5432`, schema `public`, 41 changeSets e
lock livre. Pessoas, contas, categorias, instrumentos e vendas estavam vazios;
havia um fornecedor preexistente. Nenhum banco de desenvolvimento foi utilizado.

Credenciais existentes foram carregadas de `.env` somente em memória para
`DB_TEST_USERNAME`, `DB_TEST_PASSWORD`, `SPRING_LIQUIBASE_USER` e
`SPRING_LIQUIBASE_PASSWORD`; `INITIAL_ADMIN_ENABLED=false`. Valores anteriores
restaurados em `finally`, sem publicar senhas. O caminho opcional de importação
foi conferido ausente; não havia overrides Maven/JVM de ambiente.

Primeiro, somente a nova classe:

```powershell
.\mvnw.cmd -B "-Dtest=com.curso.instrumento2026.domain.ItemVendaTest" "-Djacoco.destFile=target/aula15-atividade-risco/isolado.exec" "-Djacoco.append=false" test > target/aula15-atividade-risco/isolado.log 2>&1
```

Depois, a suíte inteira e relatório, sem repetir `clean verify` ou as demonstrações
dos checkpoints. O arquivo de coleta é exclusivo e não acumula dados anteriores:

```powershell
.\mvnw.cmd -B `
  "-Djacoco.destFile=target/aula15-atividade-risco/depois.exec" `
  "-Djacoco.dataFile=target/aula15-atividade-risco/depois.exec" `
  "-Djacoco.append=false" `
  "-Dspring.profiles.active=test" `
  "-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" `
  "-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" `
  "-Dspring.datasource.hikari.schema=public" `
  "-Dspring.liquibase.default-schema=public" `
  "-Dspring.config.import=optional:file:./target/aula15-no-external-config.properties" `
  test jacoco:report > target/aula15-atividade-risco/suite.log 2>&1
```

`$LASTEXITCODE` foi capturado após cada chamada: **0 nas duas**. O log confirma
`append=false` e a leitura de `depois.exec` pelo goal de relatório. A coleta isolada
não participa da comparação. DataSource e Liquibase receberam a mesma URL explícita.
Durante a suíte, Hibernate registrou essa URL e `pg_stat_activity` mostrou 40 conexões
JDBC em `instrumento2026_test`. Depois, as contagens conferidas, os 41 changeSets e
o lock livre foram preservados. Contagens não comprovam igualdade de todo conteúdo;
sequências podem avançar com os testes.

## Resultados e cobertura antes/depois

| Execução | Conclusão em 16/11/2026 (-03:00) | Aprovados | Falhas | Erros | Ignorados | Build |
|---|---|---:|---:|---:|---:|---|
| `ItemVendaTest` isolado | 11:01:58 | 3 | 0 | 0 | 0 | SUCCESS |
| Suíte completa, 55 classes | 11:03:19 | 674 | 0 | 0 | 0 | SUCCESS |

A execução isolada está incluída na suíte: não somar as linhas como testes distintos.
Os 55 XMLs Surefire foram conferidos após a suíte. O relatório analisou 96 classes
de produção. A contagem atual é 398 métodos (328 `@Test` e 70 parametrizados);
o inventário do Checkpoint 1 permanece como registro histórico de 54 classes.

| ItemVenda | Antes, suíte completa | Depois, suíte completa | Diferença |
|---|---:|---:|---:|
| Linhas | 24/24 — 100% | 24/24 — 100% | 0 pontos percentuais |
| Branches | 8/8 — 100% | 8/8 — 100% | 0 pontos percentuais |

Os contadores foram extraídos dos XMLs salvos em `antes/jacoco.xml` e
`depois/jacoco.xml`, relativos à mesma classe. A cobertura global também permaneceu
em 1.233/1.274 linhas (96,78%) e 398/442 branches (90,05%).

**A confiança aumentou apesar do percentual constante:** remover as validações
de vínculos faria os novos casos negativos falharem por ausência da exceção.
Isso é uma análise do poder de detecção; não foi executada mutação nem removida
qualquer validação. `Objects.requireNonNull` fica na biblioteca Java: seus caminhos
internos não se tornam branches da classe ItemVenda no relatório. As linhas do
construtor já eram percorridas com argumentos válidos nos testes existentes.

## Artefatos, limites e atendimento

Em `target/aula15-atividade-risco/`, ignorado pelo Git: snapshots `antes/` e `depois/`,
coletas `isolado.exec`/`depois.exec` e logs `isolado.log`/`suite.log`.
O [HTML local](../../target/site/jacoco/index.html) foi regenerado para a coleta completa
desta atividade, sem inspeção gráfica em navegador. O `target/jacoco.exec` anterior
foi preservado; o relatório novo usa explicitamente o arquivo `depois.exec`.

O teste não comprova FKs, persistência, autorização ou que o item esteja inserido
na coleção de Venda; observa somente o contrato de criação e seus vínculos.
Não amplia artificialmente cobertura nem altera o portão. A atividade não inclui
execução de `check`, pois o objeto aqui é o teste escolhido e sua medição comparável.

Atende à **seção 24, item 9** com um novo teste de domínio motivado por risco, e
à **seção 25** com classe antes sem teste dedicado, regra já existente, cenários
válido/inválidos, execução isolada/completa, comparação e justificativa de cinco linhas.
Não são necessários dois testes separados para cumprir essas entregas sobrepostas.

Criado o teste e este documento; atualizados README e EVIDENCIAS da Aula 15.
Checkpoints 1–5 não refeitos. Sem commit, tag ou push. **Seção 26 não iniciada.**
