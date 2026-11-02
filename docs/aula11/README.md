# Aula 11 — Concorrência de estoque

Documentação do controle concorrente da loja de instrumentos musicais, baseada no [material do professor](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/11aula/11aula.md) e no código e testes de `instrumento2026`. Evidências registradas em 02/11/2026; horários em America/Sao_Paulo.

## Documentos

- [Concorrência e decisões técnicas](CONCORRENCIA.md): recurso protegido, invariantes, estratégia, implementação e limitações.
- [Atividade orientada](ATIVIDADE-ORIENTADA.md): linhas do tempo e interpretação do cenário obrigatório.
- [Testes e evidências](EVIDENCIAS.md): resultados reais, comandos e limites da validação.

## Correspondência com os entregáveis

As seções 19 a 21 do material pedem a caracterização da disputa no domínio, uma linha do tempo, justificativa da estratégia, implementação, teste concorrente, estado final, análise de deadlock e justificativa sobre changelog.

| Entregável | Local nesta entrega |
|---|---|
| Recurso disputado e regra da loja | CONCORRENCIA: problema e invariantes |
| Intercalação de duas transações | ATIVIDADE-ORIENTADA: diagramas |
| Estratégia escolhida e alternativa | CONCORRENCIA: escolha do bloqueio |
| Implementação e ordem de aquisição | CONCORRENCIA: finalização e movimentações |
| Teste concorrente e estado final | EVIDENCIAS: cenários e resultados |
| Riscos e deadlock | CONCORRENCIA: limites conhecidos |
| Necessidade de changelog | CONCORRENCIA: persistência e contrato |
| Explicação curta do resultado | ATIVIDADE-ORIENTADA: interpretação |

## Checkpoints implementados

| Checkpoint | Correspondência no projeto |
|---|---|
| 1 — código vulnerável | Análise da leitura e escrita concorrentes do estoque |
| 2 — EntityManager | Injeção pelo construtor de VendaService |
| 3 — finalização | Instrumentos bloqueados e recarregados por ID crescente |
| 4 — entradas e saídas | InstrumentoService coordena alterações pela linha do instrumento |
| 5 — OpenAPI | Descrições de finalização, entrada e saída atualizadas |
| 6 — teste concorrente | Duas vendas de quatro unidades sobre estoque cinco |

As investigações posteriores acrescentaram preservação de alterações pendentes, bloqueio da própria venda e testes de cadastro/status com saldo antigo. Essas adaptações estão descritas separadamente dos checkpoints originais.

A documentação e as exportações das aulas anteriores permanecem como registro histórico. O fluxo anterior da [Aula 10](../aula10/MODELAGEM.md) é sucedido pelo fluxo transacional descrito aqui. Nenhuma nova coleção Postman ou exportação OpenAPI está sendo apresentada como executada nesta etapa.
