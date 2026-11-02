# Testes e evidências — Aula 11

## Ambiente e origem

Execuções realizadas em 02/11/2026, Java 21, PostgreSQL 17.6 em `localhost:5432`, banco `instrumento2026_test`, profile `test`, isolamento observado `READ_COMMITTED`. As credenciais não fazem parte desta documentação.

Os resultados abaixo foram obtidos nas etapas de implementação, investigação e auditoria final desta sessão. As etapas documentais não reexecutaram testes. Os recortes preservam os resultados essenciais, sem copiar logs completos ou dados de configuração. `target/surefire-reports` é gerado, ignorado pelo Git e sobrescrito por novas execuções; os relatórios atuais podem corresponder a rodadas distintas.

## Histórico de execuções completas

| Marco | Resultado real |
|---|---|
| Aula 10 / análise inicial da Aula 11 | 345 testes aprovados |
| Checkpoints 2 a 5 | Suítes completas com 345 aprovados |
| Checkpoint 6 e auditoria inicial | 346 aprovados |
| Correção de alterações pendentes | 348 aprovados |
| Correção da finalização da mesma venda — execução anterior | 349 aprovados; término às 14:42:33 |
| Auditoria final de encerramento — execução mais recente | 351 aprovados; 02/11/2026 às 15:14:15 -03:00 |

Recorte da última execução completa disponível:

```text
Tests run: 351, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Execução completa mais recente: **02/11/2026 às 15:14:15 -03:00**, PostgreSQL **17.6**, profile **test**, **351 testes, 0 falhas, 0 erros e 0 ignorados — BUILD SUCCESS**. Ela inclui os dois testes de cadastro/status concorrentes adicionados depois da execução anterior de 349 testes. As demais linhas do histórico registram execuções anteriores e foram preservadas.

## Vendas concorrentes

Fonte: [VendaServiceTest](../../src/test/java/com/curso/instrumento2026/service/VendaServiceTest.java).

| Cenário | Execução e resultado |
|---|---|
| Duas vendas diferentes, cada uma com 4; estoque 5 | Teste obrigatório aprovado em cinco execuções isoladas. Um sucesso, uma falha com `Estoque insuficiente`, estados FINALIZADA/ABERTA e saldo 1. |
| Mesma venda de 4; estoque 10, antes da correção | Reproduzido em três execuções: duas aceitações, saldo 2, status FINALIZADA. O teste de contrato ficou vermelho, com uma falha de teste contendo três asserções divergentes. |
| Mesma venda, após bloqueio do status | Uma aceitação, uma rejeição por venda encerrada, saldo 6, status FINALIZADA. |
| Repetição após a correção | Três rodadas com os dois testes concorrentes: 2 testes aprovados em cada rodada, sem falhas ou erros. |
| Classe de vendas | 16 aprovados, incluindo rollback por item posterior sem saldo ou inativo e preservação do preço histórico. |

Recorte real da reprodução anterior:

```text
MESMA VENDA: sessoes=2, aceitas=2, resultados=[sucesso, sucesso], estoque=2, status=FINALIZADA
```

Recorte real após a correção, também presente na última suíte completa:

```text
MESMA VENDA: sessoes=2, aceitas=1, resultados=[sucesso, Somente vendas abertas podem ser alteradas], estoque=6, status=FINALIZADA
```

O teste da mesma venda confere sessões distintas com `pg_backend_pid()`. Seus limites são: latch de 10 segundos, Future de 15 segundos, transação de 10 segundos, `lock_timeout` de 5 segundos e `statement_timeout` de 8 segundos. O teste das vendas diferentes usa latch/Future de 10 segundos, sem a mesma configuração SQL explícita. Nenhum usa sleep.

## Movimentações e atributos pendentes

Fonte: [MovimentacaoEstoqueTransacionalTest](../../src/test/java/com/curso/instrumento2026/service/MovimentacaoEstoqueTransacionalTest.java).

| Teste | Resultado observado |
|---|---|
| `devePreservarMovimentacoesNomeEPrecoSemFlushManualAposCommit` | Saldo 5 + entrada 3 − saída 2 = 6 após commit; nome `Violino final`, preço `200.00`. Sem flush manual entre operações; nome e preço alterados antes e entre elas. |
| `deveCoordenarSaidasConcorrentesMesmoComOutrosAtributosPendentes` | Duas retiradas de 4 sobre saldo 5, ambas com nome/preço pendentes: um sucesso, uma falha por estoque insuficiente, saldo 1, nome `Violino concorrente`, preço `250.00`. |

Na correção, o conjunto específico de testes de estoque e vendas passou com 73 testes; a suíte completa então passou com 348. Os flush intermediários que contornavam o descarte de alterações em `Checkpoint5ServiceTest` e `Checkpoint8ApiTest` foram retirados.

## Cadastro/status com saldo antigo

Os dois testes adicionais são `devePreservarEstoqueConfirmadoAoAlterarCadastroComSaldoAntigo` e `devePreservarEstoqueConfirmadoAoAlterarStatusComSaldoAntigo`.

A transação cadastral carrega saldo cinco. Um latch libera a outra thread, que confirma uma entrada de três. Só depois do commit um segundo latch permite a alteração cadastral. O teste comprova que a entidade cadastral ainda contém cinco e é a mesma instância retornada pelo service. Após ambos os commits, uma nova consulta verifica saldo oito e os demais atributos.

As sessões PostgreSQL são distintas. Cada transação tem limite de 12 segundos; os comandos usam `lock_timeout = 5s` e `statement_timeout = 8s`. Latches aguardam até 10 segundos e Futures até 15 segundos, com encerramento explícito do executor. A preparação usa UUID; a limpeza exclui apenas os registros criados pelo teste.

Recortes reais de saída:

```text
CADASTRO CONCORRENTE: operacao=status, estoque=8, nome=Violino, preco=100.00, status=INATIVO
CADASTRO CONCORRENTE: operacao=cadastro, estoque=8, nome=Violino cadastral, preco=175.00, status=ATIVO
```

Também foram conferidos estoque mínimo (`0.000` ou `2.000`), categoria preservada e fornecedor nulo.

SQL observado na execução com logs habilitados:

```sql
update instrumento set quantidade_estoque=? where id=?
update instrumento set status=? where id=?
```

No outro cenário, a atualização cadastral foi:

```sql
update instrumento set estoque_minimo=?,nome_instrumento=?,preco_unitario=? where id=?
```

Nenhum desses UPDATEs cadastrais incluiu `quantidade_estoque`. Os dois testes passaram isoladamente; depois, a classe completa passou com quatro testes, concluída às 15:00:38:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Isso confirma a preservação do saldo nesses cenários com `@DynamicUpdate`, não uma garantia geral contra conflitos de cadastro. As limitações estão em [CONCORRENCIA](CONCORRENCIA.md).

## OpenAPI e regressão

No Checkpoint 5, `OpenApiDocumentationTest` passou com 51 testes. A suíte completa daquela etapa passou com 345. A classe continuou participando da execução anterior de 349 e da última suíte completa de 351, mantendo seus 51 testes aprovados.

Os testes consultam JSON e YAML gerados e verificam a equivalência dos contratos. As descrições novas são verificadas separadamente; somente essas três descrições são normalizadas na comparação com a exportação histórica da Aula 10. Respostas, exemplos, schemas e demais caminhos permanecem comparados. A exportação anterior não foi modificada.

## Comandos para reprodução

Estes são comandos usados nas execuções registradas; os números futuros devem vir de nova execução, não da soma dos relatórios locais.

```powershell
.\mvnw.cmd "-Dtest=VendaServiceTest" test
.\mvnw.cmd "-Dtest=VendaServiceTest#deveFinalizarMesmaVendaApenasUmaVezSobConcorrencia+deveImpedirVendaAcimaDoSaldoEmFinalizacoesConcorrentes" "-Dspring.jpa.show-sql=true" "-Dspring.jpa.properties.hibernate.format_sql=false" test
.\mvnw.cmd "-Dtest=MovimentacaoEstoqueTransacionalTest#devePreservarEstoqueConfirmadoAoAlterarCadastroComSaldoAntigo+devePreservarEstoqueConfirmadoAoAlterarStatusComSaldoAntigo" "-Dspring.jpa.show-sql=true" "-Dspring.jpa.properties.hibernate.format_sql=false" test
.\mvnw.cmd "-Dtest=MovimentacaoEstoqueTransacionalTest" test
.\mvnw.cmd "-Dtest=OpenApiDocumentationTest" test
.\mvnw.cmd test
```

A suíte completa da auditoria final consolidou a validação do conjunto após os dois testes cadastrais, com 351 aprovados. Não houve teste de carga, execução com duas instâncias, experimento manual em consoles ou remoção temporária de lock registrada. Nenhum desses resultados foi presumido.
