# Concorrência e decisões técnicas — Aula 11

## Problema e invariantes do domínio

O recurso disputado é `Instrumento.quantidadeEstoque`: unidades inteiras de um instrumento de catálogo da loja. Duas vendas diferentes podem referenciar o mesmo instrumento. Abrir uma venda ou incluir um item não reserva estoque; o consumo ocorre na finalização.

Uma transação atômica reverte todas as baixas quando um item falha. Isso não impede duas transações de lerem saldo cinco e calcularem, separadamente, saldo um após retirar quatro. Sem coordenação, ambas poderiam ser confirmadas e uma escrita sobrescrever a outra. Essa atualização perdida venderia oito unidades com apenas cinco disponíveis.

O isolamento observado no ambiente é `READ_COMMITTED`, sem alteração global nesta aula. MVCC permite leituras de versões confirmadas; não torna a sequência de leitura, decisão e escrita exclusiva por si só.

As regras protegidas são: não retirar mais que o saldo disponível; confirmar todas as baixas da venda juntas; finalizar a mesma venda apenas uma vez; preservar o preço histórico de cada item. O saldo permanece `int`. A quantidade do item é `BigDecimal`, mas o domínio exige valor positivo, inteiro e representável por `int`.

## Escolha do bloqueio

`PESSIMISTIC_WRITE` coordena a disputa no PostgreSQL, compartilhado pelas instâncias da aplicação. O bloqueio permanece até o commit ou rollback. A transação que aguarda precisa decidir com o saldo confirmado pela anterior, e não com a entidade desatualizada no contexto de persistência.

Locking otimista com versão seria uma alternativa para conflitos pouco frequentes, mas exigiria definir o tratamento dos conflitos de atualização e eventual repetição. A implementação segue a estratégia pessimista da aula, sem `@Version` e sem retry automático. `synchronized` não coordenaria aplicações em JVMs diferentes.

## Finalização de venda

Fonte: [VendaService](../../src/main/java/com/curso/instrumento2026/service/VendaService.java).

O fluxo atual de `finalizar` permanece dentro de `@Transactional`:

1. Consulta somente `v.status` por ID, usando `PESSIMISTIC_WRITE` na consulta JPQL.
2. Rejeita venda inexistente ou status diferente de `ABERTA`, com as mensagens existentes.
3. Carrega o restante pelo repository com `EntityGraph` e valida os participantes.
4. Cria uma lista dos itens ordenada pelo ID crescente do instrumento, sem reorganizar a coleção persistida.
5. Para cada instrumento, chama `refresh` com `PESSIMISTIC_WRITE`, confere se está ativo e retira a quantidade inteira.
6. Finaliza a venda e confirma a unidade de trabalho. Exceção em qualquer baixa reverte a transação.

A ordenação define uma sequência comum para disputar vários instrumentos. Vendas com itens recebidos em ordens inversas passam a adquirir esses bloqueios na mesma ordem, reduzindo ciclos de espera. A ordem local é **venda → instrumentos por ID crescente**.

`ItemVenda.valorUnitario` não é reescrito pela releitura do preço do instrumento. O subtotal continua usando o preço capturado na inclusão do item. As validações de pessoas ativas, função do vendedor, instrumento ativo, quantidade e estoque foram preservadas, inclusive `Estoque insuficiente`.

## Mesma venda finalizada simultaneamente

A investigação reproduziu duas aceitações e saldo dois sobre estoque dez e uma única venda de quatro. Bloquear somente os instrumentos não bastava: cada transação já tinha uma venda gerenciada como `ABERTA`. A segunda aguardava o instrumento, via saldo seis e fazia outra baixa.

A correção consulta o status diretamente no banco com bloqueio antes de validá-lo. O SQL observado foi:

```sql
select v1_0.status from venda v1_0 where v1_0.id=? for no key update of v1_0
```

Essa projeção retorna um valor atual do banco; ela não depende de atualizar o status da entidade que já estava no primeiro nível de cache. A segunda tentativa lê `FINALIZADA` após a espera e recebe `Somente vendas abertas podem ser alteradas` antes de baixar estoque.

O bloqueio inicial não utiliza o `EntityGraph`. Evita também `refresh` na venda, pois `Venda.itens` possui `cascade = ALL`, incluindo refresh em cascata. Os instrumentos continuam bloqueados individualmente na ordem definida. O SQL exibido é a tradução do provedor neste PostgreSQL, não uma nova consulta nativa no código.

## Movimentações manuais e alterações pendentes

Fonte: [InstrumentoService](../../src/main/java/com/curso/instrumento2026/service/InstrumentoService.java).

`receberEstoque` e `retirarEstoque` são transacionais e chamam `atualizarEstoqueComBloqueio` antes da movimentação. O helper executa:

1. `entityManager.lock(instrumento, PESSIMISTIC_WRITE)`.
2. Verificação de alterações pendentes por `entityManager.unwrap(Session.class).isDirty()`.
3. `flush` apenas quando o contexto está alterado, já sob o bloqueio do instrumento.
4. `refresh(instrumento, PESSIMISTIC_WRITE)`.

Somente depois disso ocorre a entrada ou saída no domínio. A conversão rejeita quantidade nula, fracionária ou fora do intervalo de `int`; o domínio rejeita quantidade não positiva, saldo insuficiente e overflow de entrada com `Math.addExact`.

A versão anterior executava refresh diretamente a cada chamada. Em uma mesma transação, saldo cinco, entrada três e saída dois podiam terminar em três: o segundo refresh descartava a entrada pendente. Os `flush` intermediários adicionados aos testes no Checkpoint 4 apenas contornavam a perda. Foram removidos depois da correção no serviço; o flush final usado para conferir persistência no teste antigo continua existindo.

O novo teste verifica, sem flush manual entre operações, saldo seis após commit, além de nome e preço alterados antes e entre as movimentações. Flush não equivale a commit: as escritas sincronizadas ainda são revertidas se a transação falhar.

## DynamicUpdate e cadastro concorrente

[Instrumento](../../src/main/java/com/curso/instrumento2026/domain/Instrumento.java) usa `@DynamicUpdate`. Isso é necessário à estratégia de sincronizar atributos pendentes sem incluir no UPDATE um saldo antigo que não foi alterado localmente. A anotação modifica a geração do SQL, não o schema.

`alterar` atribui nome, preço, estoque mínimo, categoria e fornecedor. `alterarStatus` atribui status. Esses métodos não atribuem quantidade em estoque. Nos testes executados, uma transação carregou saldo cinco; outra confirmou uma entrada e saldo oito; só então a primeira alterou cadastro ou status e confirmou, ainda contendo saldo cinco no seu contexto.

O banco manteve saldo oito. O UPDATE cadastral incluiu apenas nome, preço e estoque mínimo efetivamente modificados; o de status incluiu somente status. Os testes mantiveram categoria e fornecedor, sem comprovar trocas concorrentes dessas associações. Evidências e SQL estão em [EVIDENCIAS](EVIDENCIAS.md).

## Persistência e contrato

Não há alteração de tabela, coluna, índice, constraint ou dado de referência; não foi criado changelog. O master Liquibase e as migrações existentes permanecem inalterados.

As descrições OpenAPI de finalização, entrada e saída informam a coordenação implementada. Rotas, códigos HTTP, exemplos e schemas foram preservados. O teste de documentação valida as três descrições e compara o restante com [a exportação histórica da Aula 10](../aula10/openapi.yaml), que não foi regravada. O documento gerado em runtime reflete as anotações atuais, enquanto o arquivo anterior continua representando sua aula de origem.

## Limitações e riscos residuais

- A ordem dos instrumentos reduz deadlocks, mas não elimina ciclos em transações externas que combinem múltiplas vendas ou operações em outra ordem. Não foi executado ensaio de deadlock com múltiplos instrumentos.
- Os bloqueios duram até o término da transação externa, se houver. Não há política nova de timeout ou retry em produção; os limites explícitos de alguns testes não são configuração da API.
- `Session.isDirty()` e `flush` abrangem o contexto inteiro. Entidades pendentes de outras operações também podem ser sincronizadas; a aquisição de locks dessas escritas deve ser considerada em composições maiores. A solução depende do Hibernate.
- O helper manual preserva alterações pendentes; o loop de instrumentos da finalização continua usando refresh direto. Não se comprovou uma composição arbitrária de movimentações, edições pendentes e finalização na mesma transação externa.
- O bloqueio da venda foi acrescentado à finalização. Não demonstra exclusão mútua com edição de itens ou cancelamento concorrentes.
- `@DynamicUpdate` não é controle de versão: duas edições do mesmo atributo ainda podem sobrescrever uma à outra. A entidade carregada não passa a enxergar automaticamente o saldo confirmado por outra transação. SQL externo, merge de entidades desatualizadas e outros caminhos de escrita não estão comprovados pelos cenários de cadastro.
- Os testes usam uma aplicação e um PostgreSQL real. O lock é no banco, mas não foi executado teste com duas instâncias da aplicação. Não foram medidos throughput ou latência sob carga.
- Alguns testes usam apenas timeout de latch/Future e try-with-resources no executor; o close pode aguardar tarefas bloqueadas além do timeout do Future. Os cenários mais recentes incluem limites SQL e encerramento explícito. Não ocorreu travamento nas execuções registradas.
- VendaServiceTest reutiliza identificadores fixos e limpeza por IDs na execução sequencial. Não se assume segurança para duas execuções simultâneas dessa classe contra o mesmo banco. Os testes novos de movimentação usam códigos UUID e limpeza dos próprios registros.

O experimento opcional em dois consoles SQL e a remoção temporária do lock na atividade orientada não foram executados nesta entrega; nenhuma dessas evidências é apresentada como obtida.
