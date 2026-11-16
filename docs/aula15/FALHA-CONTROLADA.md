# Aula 15 — Checkpoint 5: falha controlada e retorno aos limites oficiais

> Registro histórico que reutiliza a coleta de 671 casos. O TDD e a substituição
> posterior da coleta estão no [estado final](README.md#estado-final-da-aula-15) e em [TDD.md](TDD.md).

Data: 16/11/2026. Projeto: instrumento2026.
Referência consultada antes de qualquer alteração:
[Aula 15, seção 21, Checkpoint 5](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/15aula/15aula.md).

## Procedimento e adaptação ao projeto

O professor solicita elevar temporariamente o mínimo de branches para 0.90,
executar `verify`, observar a reprovação do `check` e retornar a 0.40.
No instrumento2026, a coleta válida do Checkpoint 4 contém 398 branches cobertos
de 442: 90,0452%. Logo, 90% não provoca falha. Esse limite foi executado e aprovado;
em seguida foi usado **0.91** para demonstrar a reprovação, mantendo o mesmo
procedimento de elevar o mínimo temporariamente, sem reduzir a cobertura.

Os valores foram aplicados por propriedade Maven na linha de comando. Não houve
edição temporária do POM: os mínimos oficiais de **0.70 para linhas e 0.40 para
branches** permaneceram no arquivo. Na validação final, o override de branches
foi removido. `prepare-agent`, `report` e `check`, escopo `BUNDLE` e métrica
`COVEREDRATIO` foram preservados; sem exclusões ou alteração em testes/produção.

## Escopo e isolamento

Foi reutilizado `target/jacoco.exec`, da execução aprovada de `clean verify` do
[Checkpoint 4](PORTAO-COBERTURA.md), em 16/11/2026 às 10:39:51 -03:00.
Não foi executado `clean`, pois apagaria a coleta que se pretende verificar.
As três chamadas usaram `-DskipTests`: nenhum teste foi reexecutado, nenhum contexto
Spring iniciado e nenhum banco acessado por esses comandos.

Os logs confirmam `Nothing to compile - all classes are up to date.` para produção
e testes, e `Tests are skipped.`. O relatório e o portão analisaram as mesmas
96 classes de produção. Os 671 testes aprovados pertencem ao Checkpoint 4 e não
são apresentados como nova execução do Checkpoint 5. `skipTests` impede a execução
da suíte; não desativa o JaCoCo `check`, cuja execução está comprovada nos logs.

Ambas as URLs foram explicitadas como `instrumento2026_test` por consistência com
o procedimento seguro anterior, embora não tenha havido necessidade de credenciais,
conexão SQL ou nova conferência de estado do banco nesta demonstração sem testes.
Não havia `MAVEN_ARGS`, `MAVEN_OPTS`, `JAVA_TOOL_OPTIONS` ou `JDK_JAVA_OPTIONS`
no ambiente que alterassem os comandos.

## Comandos efetivamente executados

PowerShell, na raiz do projeto. Em cada execução foi capturado `$LASTEXITCODE`
imediatamente após Maven. O primeiro comando reproduz o limite do exemplo oficial;
o segundo usa o mínimo adaptado; o terceiro valida os valores oficiais do POM.

```powershell
.\mvnw.cmd -B -DskipTests "-Djacoco.minimum.branch.coverage=0.90" "-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" "-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" verify > target/aula15-checkpoint5-limite90.log 2>&1
$runExit = $LASTEXITCODE

.\mvnw.cmd -B -DskipTests "-Djacoco.minimum.branch.coverage=0.91" "-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" "-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" verify > target/aula15-checkpoint5-falha91.log 2>&1
$failureExit = $LASTEXITCODE

.\mvnw.cmd -B -DskipTests "-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" "-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test" verify > target/aula15-checkpoint5-oficial.log 2>&1
$finalExit = $LASTEXITCODE
```

## Resultados e mensagem de reprovação

| Verificação | Mínimo de branches | Horário de conclusão (-03:00) | Código Maven | Resultado |
|---|---:|---|---:|---|
| Valor temporário da referência | 90% | 10:50:24 | 0 | BUILD SUCCESS |
| Falha controlada adaptada | 91% | 10:50:47 | 1 | BUILD FAILURE no goal check |
| Configuração oficial, sem override | 40% | 10:51:09 | 0 | BUILD SUCCESS |

O mínimo de linhas permaneceu em 70% em todas as chamadas. A reprovação apresentou:

```text
--- jacoco:0.8.16:check (report-coverage) ---
Rule violated for bundle instrumento2026: branches covered ratio is 0.90, but expected minimum is 0.91
BUILD FAILURE
Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.16:check (report-coverage) on project instrumento2026: Coverage checks have not been met.
```

O `0.90` na mensagem é a apresentação arredondada do JaCoCo, não uma alteração do
contador 398/442. O log aponta especificamente o mínimo de branches como motivo;
não houve recompilação, execução de teste, falha de banco ou mudança de coleta.

Sem o override, a validação final apresentou:

```text
--- jacoco:0.8.16:check (report-coverage) ---
Analyzed bundle 'instrumento2026' with 96 classes
All coverage checks have been met.
BUILD SUCCESS
```

O XML final conserva 1.233/1.274 linhas (96,78%) e 398/442 branches (90,05%).
Esses percentuais são reavaliação da coleta do Checkpoint 4, não nova medição.

## Integridade da configuração e da coleta

Hashes SHA-256 iguais antes e depois das três chamadas:

| Arquivo | SHA-256 |
|---|---|
| `pom.xml` | `22C09DCE2C856AA51517B39E9B655CFF58CF53F0FFD25C382C787EBA0C8BCF1E` |
| `target/jacoco.exec` | `A2DC45DD87BD493267CD1CEBF9025120DA2B233D5D3878EEB4285D17A3EA21A3` |

A restauração consistiu em encerrar os processos com override temporário e executar
o último comando sem `-Djacoco.minimum.branch.coverage`. Nenhum valor temporário
foi gravado no arquivo e não havia propriedade Maven de ambiente substituindo-o.

Os três logs, o relatório regenerado em `target/site/jacoco` e os JARs produzidos
por `verify` são artefatos locais ignorados pelo Git. Os relatórios Surefire existentes
continuam sendo da suíte do Checkpoint 4. O HTML não foi inspecionado graficamente.

## Arquivos e encerramento

Criado: `docs/aula15/FALHA-CONTROLADA.md`. Alterados: `docs/aula15/README.md` e
`docs/aula15/EVIDENCIAS.md`. Nenhuma alteração adicional em POM, fontes Java,
testes, configurações de aplicação ou migrations nesta etapa.

**Checkpoint 5 concluído**, com reprovação específica por cobertura e aprovação
posterior dos limites oficiais. Sem commit, tag ou push. Nenhuma atividade posterior
foi iniciada; depende de autorização do usuário.
