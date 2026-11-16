# Aula 15 — Evidências do Checkpoint 2

> Registro inicial do Checkpoint 2, seguido pela evolução das demais etapas.
> O estado definitivo está na seção final deste documento e no [índice da Aula 15](README.md).

Data: 11/11/2026. Projeto: instrumento2026.
Material: [Aula 15 do professor Jefferson](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/15aula/15aula.md),
seções 8 e 21, Checkpoint 2.

## Exigência e comparação anterior à alteração

O Checkpoint 2 solicita `ContaAcessoTest` e sua execução isolada. A seção 8
exemplifica criação normalizada, rejeição de login ausente, troca de hash válida,
administração de perfis e autenticação condicionada aos estados da conta/pessoa.
O exemplo da referência produz oito execuções; o total local depende dos casos existentes.

Antes de modificar arquivos, foram lidos o material oficial, a classe
[ContaAcessoTest](../../src/test/java/com/curso/instrumento2026/domain/ContaAcessoTest.java)
e o domínio [ContaAcesso](../../src/main/java/com/curso/instrumento2026/domain/ContaAcesso.java).
A classe já existia com sete métodos `@Test` e dois `@ParameterizedTest`.
Cada parametrizado usa `null`, vazio, espaço e tabulação: sete casos simples mais
oito parametrizados, totalizando 15 execuções esperadas.

| Regra comparada | Métodos existentes que a verificam | Situação encontrada e adaptação |
|---|---|---|
| Conta ativa, login normalizado, hash sem espaços externos e perfil OPERADOR | `deveCriarContaAtivaSomenteOperadorNormalizandoLogin` | Login, hash armazenado, estado e perfis já verificados. Entrada do hash passou a conter espaços externos e foi acrescentada a asserção de autenticação inicial |
| Login nulo, vazio ou branco rejeitado | `deveRejeitarLoginAusente` | Quatro entradas já cobertas; acrescentada conferência da mensagem `Login é obrigatório` |
| Troca válida de hash e rejeição de hash ausente | `deveSubstituirHashSemAlterarLoginPessoaOuPerfis`; `deveRejeitarHashAusenteSemDescartarHashAnterior` | Troca, rejeição e preservação do hash anterior já cobertas. Troca válida passou a usar entrada com espaços externos para verificar trim |
| Conceder/substituir perfis sem permitir conta sem perfil | `deveConcederPerfilSemDuplicarNemExporColecaoMutavel`; `deveValidarTodosOsPerfisAntesDeSubstituir` | Já coberto, incluindo coleção vazia/nula, conteúdo inválido, cópia defensiva e coleção imutável. Sem alteração |
| Autenticação depende dos estados da conta e da pessoa | `deveDependerDosEstadosDaContaEDaPessoa` | Estados ativo/bloqueado e pessoa ativa/inativa já cobertos. Acrescentado retorno à autenticação após reativar a conta com a pessoa ativa |

Os ajustes reutilizam métodos, fixtures e valores sintéticos existentes. Nenhum
método, classe ou argumento parametrizado foi adicionado; as contagens do inventário
permanecem válidas. Os outros dois métodos simples preservados verificam a exigência
de Pessoa/independência dos papéis comerciais e a ausência de hash na representação textual.

## Isolamento e banco

O teste instancia entidades diretamente. Não usa `@SpringBootTest`,
`@SpringJUnitConfig`, `@ActiveProfiles`, repositories, DataSource, EntityManager,
MockMvc ou acesso JDBC. As annotations JPA das entidades não iniciam persistência
ao construir objetos. Portanto, não há dependência de banco nesta execução e
nenhum destino de desenvolvimento ou teste foi utilizado.

O POM foi inspecionado antes da execução: o plugin Liquibase não possui execution
vinculada à fase `test`. A seleção usa o nome totalmente qualificado da única classe.
Não foi necessário iniciar Spring, consultar credenciais ou ativar o profile `test`.

## Comando executado

Na raiz do projeto, em PowerShell, usando o Maven Wrapper e Java 21:

```powershell
.\mvnw.cmd -B "-Dtest=com.curso.instrumento2026.domain.ContaAcessoTest" test 2>&1 | Tee-Object -FilePath target/aula15-checkpoint2-conta-acesso.log
$testExitCode = $LASTEXITCODE
Write-Output "MAVEN_EXIT_CODE=$testExitCode"
```

O processo retornou código 0. Não foi executado `clean`, `verify` ou a suíte completa.
Maven recompilou os 57 fontes de teste existentes, mas o Surefire executou apenas
`com.curso.instrumento2026.domain.ContaAcessoTest`. Compilar outras classes não
significa executar seus testes.

## Resultado comprovado nesta etapa

Conclusão registrada pelo Maven: **11/11/2026 às 11:22:02 -03:00**.

```text
Running com.curso.instrumento2026.domain.ContaAcessoTest
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
MAVEN_EXIT_CODE=0
```

| Medida | Resultado |
|---|---:|
| Classes executadas | 1 |
| Casos executados | 15 |
| Aprovados | 15 |
| Falhas | 0 |
| Erros | 0 |
| Ignorados | 0 |

O log informa 0,290 s para a classe e 10,594 s para o build inteiro. São tempos
desta execução, não estimativas gerais de desempenho ou comparação entre categorias.

Artefatos locais gerados em `target`, ignorados pelo Git:

- `aula15-checkpoint2-conta-acesso.log`: saída da execução isolada.
- `surefire-reports/TEST-com.curso.instrumento2026.domain.ContaAcessoTest.xml`:
  totais e casos individuais, conferidos após a execução.
- `surefire-reports/com.curso.instrumento2026.domain.ContaAcessoTest.txt`:
  resumo do Surefire.

Os demais relatórios antigos de `target/surefire-reports` não foram somados nem
tratados como novas execuções. Os 671 testes da Aula 14 continuam sendo históricos.

## Arquivos e limites

- Alterado: `src/test/java/com/curso/instrumento2026/domain/ContaAcessoTest.java`.
- Alterado: `docs/aula15/README.md`, para refletir o avanço ao Checkpoint 2.
- Criado: `docs/aula15/EVIDENCIAS.md`, este registro.

Sem alteração de produção, migrations, configurações, POM ou inventário do
Checkpoint 1. Os avisos de compilação sobre APIs depreciadas e operações unchecked
em outros testes não impediram o build; esses testes não foram executados.

Não há pendência impeditiva para o Checkpoint 2. Esta execução comprova regras de
domínio, não autenticação HTTP/JWT, persistência, cobertura ou aprovação da suíte
completa. Não houve execução de migrations, commit ou push.

**Encerramento histórico desta etapa: Checkpoint 2 concluído, sem iniciar o 3 naquele momento.**

## Continuidade — Checkpoint 3

Posteriormente autorizado e concluído em 11/11/2026. Evidências completas em
[COBERTURA-INICIAL.md](COBERTURA-INICIAL.md): banco de teste confirmado, primeira
tentativa com erro de credenciais Liquibase e segunda execução aprovada com 671
testes, zero falhas, erros e ignorados. JaCoCo registrou 96,78% das linhas e
90,05% dos branches, sem limites ou goal `check` naquela etapa.

## Continuidade — Checkpoint 4

Autorizado e concluído em 16/11/2026: [PORTAO-COBERTURA.md](PORTAO-COBERTURA.md).
`clean verify` com DataSource e Liquibase explicitamente em `instrumento2026_test`;
671 aprovados, zero falhas, erros ou ignorados, `BUILD SUCCESS` e código 0.
O goal `check` aprovou os mínimos oficiais globais de 70% das linhas e 40% dos
branches. Medição nova: 96,78% das linhas (1.233/1.274) e 90,05% dos branches
(398/442), 96 classes analisadas. Sem exclusões ou falha controlada.
Esse registro encerrou o Checkpoint 4 sem iniciar o seguinte naquele momento.

## Continuidade — Checkpoint 5

Concluído em 16/11/2026: [FALHA-CONTROLADA.md](FALHA-CONTROLADA.md).
Reutilizando a coleta do Checkpoint 4 e sem reexecutar testes, `verify` passou com
override de 90% de branches, falhou no goal `check` com 91% e voltou a passar
sem override, com 70% de linhas e 40% de branches do POM. Códigos Maven: 0, 1 e 0.
POM e `jacoco.exec` com SHA-256 idênticos antes/depois. Sem acesso ao banco,
alterações de testes/produção, commit ou push. Atividades posteriores não iniciadas.

## Atividade orientada — seções 24 e 25

Autorizada e concluída em 16/11/2026: [ATIVIDADE-ORIENTADA.md](ATIVIDADE-ORIENTADA.md).
Novo `domain/ItemVendaTest.java`, sem alterar produção. Isolado: 3 aprovados;
suíte completa: 674 aprovados em 55 classes, zero falhas, erros e ignorados,
ambas com BUILD SUCCESS e código 0. DataSource/Liquibase explicitamente em
`instrumento2026_test`, confirmado antes da suíte. Comparação da mesma classe,
com suíte completa: linhas 24/24 e branches 8/8 antes/depois (100% em ambos).
Ganho de confiança nas rejeições de vínculos nulos, sem aumento percentual.
Atende também ao teste novo escolhido por risco da seção 24, item 9.
A seção 26 ainda não havia sido iniciada naquele momento. Sem commit ou push.

## Fechamento — TDD e portão final

A seção 26 foi posteriormente autorizada e concluída; [TDD.md](TDD.md) preserva
RED (código 1, método inexistente), GREEN e REFACTOR (167 casos de domínio aprovados
em cada fase), snapshots do código e o oráculo dos cinco cenários novos.
A suíte completa de 16/11/2026 às 11:14:20 -03:00 aprovou 679 casos em 55 classes,
sem falhas, erros ou ignorados.

O primeiro check do TDD leu coleta antiga e não foi aceito como validação final.
A correção autorizada preservou a coleta antiga e copiou a nova para o caminho
padrão. Às 11:18:10 -03:00, report e check leram o mesmo arquivo novo e aprovaram
os mínimos oficiais (70% de linhas e 40% de branches), sem incompatibilidade.
Não houve nova execução de testes ou acesso ao banco nessa correção.

Cobertura final: 1.236/1.277 linhas (96,79%) e 398/442 branches (90,05%), em 96
classes de produção. Os resultados 671/674 das etapas anteriores são históricos
e não se somam aos 679 finais. Checkpoints 1–5 e seções 24–26 concluídos.

No fechamento documental, foram revisados links, candidatos ao commit e whitespace,
sem executar testes, Maven ou PostgreSQL. Relatórios e logs locais em target são
descartáveis e não serão versionados. Sem commit ou push; aguardando autorização.
