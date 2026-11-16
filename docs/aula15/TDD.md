# Aula 15 — Seção 26: reposição do estoque mínimo com TDD

> Concluído: 679 testes aprovados e portão oficial validado sobre a coleta nova.
> A interrupção descrita abaixo é histórica e foi resolvida na seção
> [Correção autorizada](#correção-autorizada--portão-sobre-a-coleta-nova).

Data: 16/11/2026. Projeto: instrumento2026.
Referência: [seção 26 da Aula 15](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/15aula/15aula.md).
Evidências sequenciais substituem commits intermediários, como permitido no material.

## Regra nova e oráculo

`Instrumento.calcularQuantidadeParaReposicao()` retorna um `BigDecimal` com escala
zero: quantas unidades inteiras faltam para atingir o mínimo, nunca negativo.
Uma diferença fracionária é arredondada para cima. O cálculo não movimenta estoque,
não considera status e não cria endpoint, campo persistido ou processo de compra.

O método não existia antes desta atividade. Os testes usam resultados literais
da tabela abaixo, definidos pelos exemplos comerciais, sem calcular o esperado
copiando a fórmula de produção. `BigDecimal.equals` verifica também a escala zero.

| Estoque | Mínimo | Resultado esperado |
|---:|---:|---:|
| 2 | 5.000 | 3 |
| 5 | 5.000 | 0 |
| 7 | 5.000 | 0 |
| 2 | 5.500 | 4 |
| 0 | 0.000 | 0 |

## RED — registrado antes da implementação

Primeiro foi acrescentado somente o método de teste
`InstrumentoTest.deveCalcularQuantidadeParaReposicaoQuandoEstoqueEstaAbaixoDoMinimo`.
Ele constrói estoque 2 e mínimo `5.000`, chama a operação ainda inexistente e
espera `new BigDecimal("3")`. O fonte de produção ainda não foi alterado nesta fase.

```powershell
.\mvnw.cmd -B "-Dtest=InstrumentoTest#deveCalcularQuantidadeParaReposicaoQuandoEstoqueEstaAbaixoDoMinimo" "-Djacoco.destFile=target/aula15-tdd/red.exec" test > target/aula15-tdd/red.log 2>&1
$redExit = $LASTEXITCODE
```

Conclusão: **16/11/2026, 11:11:05 -03:00; código 1; BUILD FAILURE**.

```text
COMPILATION ERROR
cannot find symbol
  symbol: method calcularQuantidadeParaReposicao()
  location: variable instrumento of type com.curso.instrumento2026.domain.Instrumento
Failed to execute goal ... maven-compiler-plugin:3.14.1:testCompile
```

É RED por ausência da operação, conforme a proposta aprovada. **Não é falha de
asserção:** nenhum caso JUnit foi executado, porque a compilação de teste não passou.
Não houve contexto Spring, acesso ao banco ou falha de infraestrutura.

Antes de implementar, foram preservados `target/aula15-tdd/red/Instrumento.java`,
`red/InstrumentoTest.java`, `red.log` e `red-exit.txt`. Este registro foi escrito
antes de iniciar GREEN. Os arquivos de `target` são locais e ignorados pelo Git;
o trecho de erro acima conserva a evidência no documento versionável.

## GREEN — implementação mínima e domínio aprovado

Depois de preservar RED, foi implementado o método abaixo e acrescentado um teste
parametrizado com as outras quatro linhas da tabela, sem repetir o primeiro caso:

```java
public BigDecimal calcularQuantidadeParaReposicao() {
    BigDecimal estoqueAtual = BigDecimal.valueOf(quantidadeEstoque);
    if (estoqueAtual.compareTo(estoqueMinimo) >= 0) {
        return BigDecimal.ZERO;
    }
    return estoqueMinimo.subtract(estoqueAtual).setScale(0, RoundingMode.CEILING);
}
```

```powershell
.\mvnw.cmd -B "-Dtest=com.curso.instrumento2026.domain.*Test" "-Djacoco.destFile=target/aula15-tdd/green.exec" "-Djacoco.append=false" test > target/aula15-tdd/green.log 2>&1
$greenExit = $LASTEXITCODE
```

Conclusão: **16/11/2026, 11:12:01 -03:00; código 0; BUILD SUCCESS**.
Foram **167 casos de domínio aprovados, zero falhas, erros e ignorados**, incluindo
os cinco casos novos de reposição. Não houve Spring ou acesso ao banco.
O snapshot `target/aula15-tdd/green/` contém produção e teste antes da refatoração;
`green.log` e `green-exit.txt` registram a execução. A suíte completa fica reservada
à validação integrada final, após conferir o banco, conforme a sequência autorizada.

## REFACTOR — mesma regra, menos bifurcação

Somente depois de GREEN foi removida a comparação condicional. A versão final é:

```java
public BigDecimal calcularQuantidadeParaReposicao() {
    return estoqueMinimo.subtract(BigDecimal.valueOf(quantidadeEstoque))
            .max(BigDecimal.ZERO)
            .setScale(0, RoundingMode.CEILING);
}
```

Limitar a diferença a zero antes do arredondamento preserva os casos de saldo
igual/superior ao mínimo. `CEILING` garante unidades inteiras suficientes, inclusive
quando o mínimo tem fração. Nenhuma regra de validação ou movimentação foi alterada.
Os mesmos testes foram mantidos, sem trocar os resultados esperados para acomodar
a refatoração. Snapshots em `green/` e `refactor/` e `target/aula15-tdd/refactor.diff`
preservam o antes/depois; ambos os códigos também estão transcritos neste documento.

```powershell
.\mvnw.cmd -B "-Dtest=com.curso.instrumento2026.domain.*Test" "-Djacoco.destFile=target/aula15-tdd/refactor.exec" "-Djacoco.append=false" test > target/aula15-tdd/refactor.log 2>&1
$refactorExit = $LASTEXITCODE
```

Conclusão: **16/11/2026, 11:12:45 -03:00; código 0; BUILD SUCCESS**.
Novamente **167 casos de domínio aprovados, zero falhas, erros e ignorados**.
Os cinco casos de reposição estão incluídos nesse total. As execuções GREEN e
REFACTOR são sobrepostas e não devem ser somadas como casos distintos.

## Preparação da validação integrada

Antes da suíte, consultas por psql, com host/porta/banco explícitos e `BEGIN READ ONLY`,
confirmaram `instrumento2026_test`, servidor `127.0.0.1:5432`, schema `public`,
41 changeSets e lock livre. Pessoas, contas, categorias, instrumentos e vendas
estavam vazios; um fornecedor preexistente foi preservado. Não foi usado o banco dev.

O POM foi conferido com mínimos oficiais LINE 0.70 e BRANCH 0.40. Não houve
override desses mínimos, exclusões ou alteração em `prepare-agent`, `report` e `check`.
As coletas de domínio e a coleta integrada são arquivos distintos, com `append=false`:
os resultados parciais não entram na cobertura final. Não foi executado `clean`,
para preservar os snapshots; a coleta final usa um arquivo novo exclusivo.

Credenciais de teste foram lidas de `.env` somente em memória e usadas nas variáveis
`DB_TEST_USERNAME`, `DB_TEST_PASSWORD`, `SPRING_LIQUIBASE_USER` e
`SPRING_LIQUIBASE_PASSWORD`. `INITIAL_ADMIN_ENABLED=false` no ambiente evita bootstrap.
Valores anteriores restaurados em `finally`; sem imprimir ou passar senhas por argumentos.
O caminho opcional de importação foi conferido ausente; não havia overrides nos
ambientes `MAVEN_ARGS`, `MAVEN_OPTS`, `JAVA_TOOL_OPTIONS` e `JDK_JAVA_OPTIONS`.

```powershell
.\mvnw.cmd -B `
  "-Djacoco.destFile=target/aula15-tdd/final.exec" `
  "-Djacoco.dataFile=target/aula15-tdd/final.exec" `
  "-Djacoco.append=false" `
  "-Dspring.profiles.active=test" `
  "-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" `
  "-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" `
  "-Dspring.datasource.hikari.schema=public" `
  "-Dspring.liquibase.default-schema=public" `
  "-Dspring.config.import=optional:file:./target/aula15-no-external-config.properties" `
  verify > target/aula15-tdd/final.log 2>&1
$finalExit = $LASTEXITCODE
```

## Resultado integrado e interrupção por divergência no portão

Conclusão: **16/11/2026, 11:14:20 -03:00; 679 casos aprovados, zero falhas,
erros ou ignorados**, tempo Maven 51,818 s. O processo retornou código 0 e
`BUILD SUCCESS`, porém a aprovação do portão **não é considerada válida para a
coleta nova**, pelos seguintes trechos reais:

```text
--- jacoco:0.8.16:report (report-coverage) ---
Loading execution data file .../target/aula15-tdd/final.exec
--- jacoco:0.8.16:check (report-coverage) ---
Loading execution data file .../target/jacoco.exec
Classes in bundle 'instrumento2026' do not match with execution data.
Execution data for class com/curso/instrumento2026/domain/Instrumento does not match.
All coverage checks have been met.
```

O diagnóstico foi confirmado por leitura do descritor `META-INF/maven/plugin.xml`
do plugin JaCoCo 0.8.16 instalado. No goal `report`, `dataFile` aceita a propriedade
`jacoco.dataFile`; no goal `check`, o parâmetro usa o caminho padrão e não está
vinculado a essa propriedade. Assim, o argumento adotado redirecionou apenas o
relatório, deixando o portão ler a coleta antiga do Checkpoint 4. Não houve erro
de teste ou banco; houve divergência na entrada do portão.

O relatório HTML/XML novo, que leu `final.exec`, registrou:

| Métrica | Cobertos / total | Percentual |
|---|---:|---:|
| Linhas globais | 1.236 / 1.277 | 96,79% |
| Branches globais | 398 / 442 | 90,05% |
| Linhas de Instrumento | 109 / 117 | 93,16% |
| Branches de Instrumento | 26 / 38 | 68,42% |

O novo método tem suas três linhas executadas. A refatoração usa operações da
biblioteca Java e não acrescenta branches próprios contabilizados na entidade.
O relatório mede a suíte completa sobre 96 classes de produção; não representa
teste de desempenho, mutação ou validação de todos os caminhos possíveis.

**Interrupção:** conforme a instrução do usuário para falha inesperada, não houve
correção nem nova execução após esse diagnóstico. RED, GREEN, REFACTOR e os testes
integrados possuem evidências, mas a validação final do portão ficou **pendente**.
Uma correção possível, mediante autorização, é preservar a coleta padrão antiga,
colocar a coleta nova em `target/jacoco.exec` e reaplicar `verify -DskipTests`,
confirmando que `report` e `check` leem os mesmos dados, sem aviso de incompatibilidade.
Isso dispensa alteração do POM e nova execução de banco.

## Arquivos e limites desta execução

Alterados somente `src/main/java/com/curso/instrumento2026/domain/Instrumento.java`
e `src/test/java/com/curso/instrumento2026/domain/InstrumentoTest.java`;
criado `docs/aula15/TDD.md`. O POM foi preservado, SHA-256
`22C09DCE2C856AA51517B39E9B655CFF58CF53F0FFD25C382C787EBA0C8BCF1E`.
Não foram criados endpoints, migrations, campos ou fluxos de compra. As demais
regras de estoque e o README principal não foram alterados.

Logs, snapshots e códigos de saída de cada fase estão em `target/aula15-tdd/`;
esses arquivos são ignorados pelo Git, enquanto os trechos e os dois códigos de
produção acima preservam as evidências no documento. Não houve commit ou push.
Esse registro encerra a execução interrompida. A correção posteriormente autorizada
e o resultado definitivo constam abaixo.

## Correção autorizada — portão sobre a coleta nova

Em 16/11/2026, o usuário autorizou exclusivamente corrigir os arquivos de coleta e
reaplicar o JaCoCo, sem executar testes ou acessar PostgreSQL. Não houve alteração
em Java, testes, POM, migrations, configuração ou outro documento nesta correção.

### Identificação dos caminhos e integridade

| Goal | Caminho na execução integrada anterior | Caminho na correção |
|---|---|---|
| `prepare-agent` | `C:\Projetos\instrumento2026\target\aula15-tdd\final.exec`, `append=false`, confirmado no `argLine` do log | Não executado; não houve JVM de testes |
| `report` | `C:\Projetos\instrumento2026\target\aula15-tdd\final.exec` | `C:\Projetos\instrumento2026\target\jacoco.exec` |
| `check` | `C:\Projetos\instrumento2026\target\jacoco.exec`, então antigo | `C:\Projetos\instrumento2026\target\jacoco.exec`, agora cópia da coleta nova |

A origem da coleta nova foi confirmada por `final.log`: agente gravando no caminho
exclusivo com `append=false`, suíte inteira com 679 aprovados e conclusão às
11:14:20 -03:00. A soma dos 55 XMLs Surefire também registrou 679 testes, zero
falhas, erros e ignorados. Isso identifica a execução anterior, não testes novos.
Um arquivo `.exec` não armazena por si só a quantidade de testes; a associação é
comprovada pelos caminhos, pelo log da execução e pelos resultados Surefire.

Antes da substituição, a coleta nova foi validada por leitura real com JaCoCo:

```powershell
.\mvnw.cmd -B "-Djacoco.dataFile=target/aula15-tdd/final.exec" jacoco:report@report-coverage > target/aula15-tdd/coleta-nova-conferencia.log 2>&1
```

Código 0, 96 classes analisadas e nenhum aviso de incompatibilidade ou coleta ausente.
Não havia overrides de ambiente em `MAVEN_ARGS`, `MAVEN_OPTS`, `JAVA_TOOL_OPTIONS`
ou `JDK_JAVA_OPTIONS`. Em seguida foram feitas cópias, sem apagar os originais:

```powershell
Copy-Item -LiteralPath target/jacoco.exec -Destination target/aula15-tdd/jacoco-antigo-checkpoint4.exec
Copy-Item -LiteralPath target/aula15-tdd/final.exec -Destination target/jacoco.exec
```

O caminho do backup foi conferido ausente antes de criá-lo. Hashes SHA-256 foram
comparados após cada cópia e novamente após a validação:

| Arquivo(s) | SHA-256 |
|---|---|
| Coleta antiga preservada em `target/aula15-tdd/jacoco-antigo-checkpoint4.exec` | `A2DC45DD87BD493267CD1CEBF9025120DA2B233D5D3878EEB4285D17A3EA21A3` |
| Coleta nova original `target/aula15-tdd/final.exec` e cópia `target/jacoco.exec` | `EDA174FB26AD68BE1CE4D2D4EC9C4633D6CF18B1468645212954506D9A6D973C` |

### Comando final e limites efetivamente aplicados

```powershell
.\mvnw.cmd -B jacoco:report@report-coverage jacoco:check@report-coverage > target/aula15-tdd/portao-corrigido.log 2>&1
$gateExit = $LASTEXITCODE
```

O sufixo `@report-coverage` seleciona a execution existente do POM, incluindo suas
regras de `check`: `BUNDLE`, `COVEREDRATIO`, `LINE` mínimo **0.70** e `BRANCH`
mínimo **0.40**. Não foi usado override de mínimo nem flag para ignorar o portão.
Foram invocados somente os dois goals JaCoCo: nenhuma fase de compilação, teste,
empacotamento ou startup Spring. O POM mantém o mesmo SHA-256 registrado acima.

Conclusão: **16/11/2026, 11:18:10 -03:00; código 0; BUILD SUCCESS**:

```text
--- jacoco:0.8.16:report (report-coverage) ---
Loading execution data file C:\Projetos\instrumento2026\target\jacoco.exec
Analyzed bundle 'instrumento2026' with 96 classes
--- jacoco:0.8.16:check (report-coverage) ---
Loading execution data file C:\Projetos\instrumento2026\target\jacoco.exec
Analyzed bundle 'instrumento2026' with 96 classes
All coverage checks have been met.
BUILD SUCCESS
```

Não existem mensagens `does not match`, `do not match` ou de goal ignorado no log
corrigido. Relatório e portão leram exatamente o mesmo arquivo novo, com identidade
confirmada por hash. O XML final mantém 1.236/1.277 linhas (**96,79%**) e 398/442
branches (**90,05%**), acima dos limites oficiais. O HTML foi regenerado; não houve
inspeção gráfica em navegador nem medição adicional por novos testes.

**Resultado definitivo: o portão foi efetivamente validado sobre a coleta da suíte
completa de 679 testes. A pendência técnica registrada na interrupção foi resolvida.**
O único documento alterado nesta correção é `docs/aula15/TDD.md`; backups, logs,
arquivo padrão e relatório regenerado ficam em `target`, ignorados pelo Git.
Nenhum acesso ao PostgreSQL, teste, commit ou push foi realizado nesta correção.
