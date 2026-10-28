# Atividade orientada — Aula 10

## Enunciado e origem das evidências

A [atividade orientada do professor, seção 23](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/10aula/10aula.md) pede uma venda com dois produtos e quantidades diferentes, saldos antes/depois, alteração do preço atual, consulta do histórico, finalização e tentativa de remoção de item. Esta entrega usa instrumentos musicais e acrescenta a exclusão recusada do instrumento vendido, conforme solicitado.

Os resultados abaixo são de execuções reais no PostgreSQL de desenvolvimento, com API em `http://localhost:18080`, perfil `dev`. Os IDs pertencem ao cenário criado pela [coleção da Aula 10](<Instrumento 2026 - Aula 10.postman_collection.json>), execução `mvscfrmf-9d8544831e3d`. Não são parâmetros fixos recomendados para uma nova execução.

| Fonte | Execução e resultado |
|---|---|
| Execução Newman da coleção da Aula 10; resultados essenciais transcritos abaixo | Início 2026-10-28T16:43:26.459Z; término 2026-10-28T16:43:30.145Z; 40 requisições principais, 3 consultas auxiliares de CPF, 111 asserções aprovadas, zero falhas. |
| Execução HTTP complementar da remoção após finalizar; resposta e conferências transcritas na seção 5 | Concluída em 2026-10-28T16:52:29.646Z; 7 chamadas HTTP: consulta da venda, dois instrumentos, tentativa de remoção, nova consulta da venda e dos dois instrumentos. Resposta `400`; venda e saldos comparados integralmente antes/depois. |

A coleção anterior havia removido um item apenas na venda aberta. A remoção após finalizar ainda não estava comprovada por uma execução HTTP nesses relatórios; foi executada nesta etapa antes da elaboração deste documento. A execução complementar primeiro conferiu que a venda atual era idêntica à registrada no relatório Newman, evitando operar sobre outra venda.

Os relatórios completos ficam em `target`, ignorado pelo Git e sujeito à limpeza do build. Os JSONs essenciais estão preservados neste documento. Foram apenas formatados; os recortes e agrupamentos são identificados explicitamente. Não houve alteração de código ou criação de outra venda nesta etapa. A API temporária foi usada exclusivamente para conferir o cenário de teste já criado.

## 1. Saldos iniciais

Requisições `16A` e `16B`: `GET /api/instrumentos/25` e `GET /api/instrumentos/26`, ambas com **HTTP 200**. Os itens da venda já estavam montados, mas a venda ainda estava aberta: incluir, remover e reincluir itens não baixou o estoque.

Recortes das duas respostas, agrupados editorialmente em um array; a API retornou cada instrumento separadamente:

```json
[
  {
    "id": 25,
    "codigoInstrumento": "A10-A-mvscfrmf-9d8544831e3d",
    "quantidadeEstoque": 10,
    "precoUnitario": 100
  },
  {
    "id": 26,
    "codigoInstrumento": "A10-B-mvscfrmf-9d8544831e3d",
    "quantidadeEstoque": 12,
    "precoUnitario": 250
  }
]
```

## 2. Abertura e inclusão de quantidades diferentes

Requisição `11`: `POST /api/vendas`, **HTTP 201**, com o corpo efetivamente enviado:

```json
{
  "clienteId": 3,
  "vendedorId": 3
}
```

Resposta da abertura:

```json
{
  "id": 4,
  "clienteId": 3,
  "clienteNome": "Aula10 Cliente mvscfrmf-9d8544831e3d",
  "vendedorId": 3,
  "vendedorNome": "Aula10 Vendedor mvscfrmf-9d8544831e3d",
  "dataVenda": "2026-10-28T13:43:27.6437689",
  "status": "ABERTA",
  "itens": [],
  "total": 0
}
```

Requisição `12`: `POST /api/vendas/4/itens`, **HTTP 200**:

```json
{
  "instrumentoId": 25,
  "quantidade": 2
}
```

Requisição `13`: mesmo endpoint, **HTTP 200**:

```json
{
  "instrumentoId": 26,
  "quantidade": 3
}
```

Foram incluídas **2 unidades do instrumento 25** e **3 unidades do instrumento 26**. A execução também removeu B enquanto a venda estava aberta (`14`, HTTP 200) e o reincluiu (`15`, HTTP 200); por isso, o item B que permanece no histórico tem ID 8. O item A tem ID 6.

## 3. Alteração do preço atual e consulta do histórico

Requisição `17`: `PUT /api/instrumentos/25`, **HTTP 200**. O PUT exige também os outros campos editáveis, que foram preservados:

```json
{
  "nomeInstrumento": "Violino A Aula10 mvscfrmf-9d8544831e3d",
  "precoUnitario": 175,
  "estoqueMinimo": 1,
  "categoriaId": 22,
  "fornecedorId": 10
}
```

Recorte da resposta do instrumento, confirmando preço atual **175** e saldo ainda **10**:

```json
{
  "id": 25,
  "codigoInstrumento": "A10-A-mvscfrmf-9d8544831e3d",
  "quantidadeEstoque": 10,
  "precoUnitario": 175
}
```

Requisição `18`: `GET /api/vendas/4`, **HTTP 200**. Resposta integral: o item A mantém preço **100**, subtotal **200**; o item B mantém preço **250**, subtotal **750**; total **950**.

```json
{
  "id": 4,
  "clienteId": 3,
  "clienteNome": "Aula10 Cliente mvscfrmf-9d8544831e3d",
  "vendedorId": 3,
  "vendedorNome": "Aula10 Vendedor mvscfrmf-9d8544831e3d",
  "dataVenda": "2026-10-28T13:43:27.643769",
  "status": "ABERTA",
  "itens": [
    {
      "id": 6,
      "instrumentoId": 25,
      "instrumentoNome": "Violino A Aula10 mvscfrmf-9d8544831e3d",
      "quantidade": 2,
      "valorUnitario": 100,
      "subtotal": 200
    },
    {
      "id": 8,
      "instrumentoId": 26,
      "instrumentoNome": "Violino B Aula10 mvscfrmf-9d8544831e3d",
      "quantidade": 3,
      "valorUnitario": 250,
      "subtotal": 750
    }
  ],
  "total": 950
}
```

## 4. Finalização e saldos finais

Requisição `19`: `POST /api/vendas/4/finalizacao`, sem corpo, **HTTP 200**. Resposta integral:

```json
{
  "id": 4,
  "clienteId": 3,
  "clienteNome": "Aula10 Cliente mvscfrmf-9d8544831e3d",
  "vendedorId": 3,
  "vendedorNome": "Aula10 Vendedor mvscfrmf-9d8544831e3d",
  "dataVenda": "2026-10-28T13:43:27.643769",
  "status": "FINALIZADA",
  "itens": [
    {
      "id": 6,
      "instrumentoId": 25,
      "instrumentoNome": "Violino A Aula10 mvscfrmf-9d8544831e3d",
      "quantidade": 2,
      "valorUnitario": 100,
      "subtotal": 200
    },
    {
      "id": 8,
      "instrumentoId": 26,
      "instrumentoNome": "Violino B Aula10 mvscfrmf-9d8544831e3d",
      "quantidade": 3,
      "valorUnitario": 250,
      "subtotal": 750
    }
  ],
  "total": 950
}
```

A requisição `20` repetiu a finalização e recebeu **HTTP 400**, sem nova baixa. Na sequência, `21A` e `21B` consultaram os instrumentos: **HTTP 200** em ambos. Recortes dessas respostas, agrupados editorialmente:

```json
[
  {
    "id": 25,
    "codigoInstrumento": "A10-A-mvscfrmf-9d8544831e3d",
    "quantidadeEstoque": 8,
    "precoUnitario": 175
  },
  {
    "id": 26,
    "codigoInstrumento": "A10-B-mvscfrmf-9d8544831e3d",
    "quantidadeEstoque": 9,
    "precoUnitario": 250
  }
]
```

| Instrumento | Saldo inicial | Quantidade vendida | Saldo final | Preço atual | Preço histórico |
|---|---:|---:|---:|---:|---:|
| 25 — A | 10 | 2 | 8 | 175,00 | 100,00 |
| 26 — B | 12 | 3 | 9 | 250,00 | 250,00 |

## 5. Remoção recusada após finalizar

Execução complementar: `DELETE /api/vendas/4/itens/6`, sem corpo, **HTTP 400**. Resposta integral:

```json
{
  "timestamp": "2026-10-28T16:52:29.594546100Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Somente vendas abertas podem ser alteradas",
  "path": "/api/vendas/4/itens/6",
  "fields": {}
}
```

O código identifica a tentativa como alteração de venda encerrada. A consulta posterior `GET /api/vendas/4` retornou **HTTP 200** e foi comparada integralmente com a consulta anterior: continuou `FINALIZADA`, com os itens **6 e 8**, quantidades **2 e 3**, e total **950**. As releituras de `/api/instrumentos/25` e `/api/instrumentos/26` também retornaram **200** e respostas idênticas às anteriores, com saldos **8 e 9**. A requisição recusada não removeu item nem movimentou estoque.

## 6. Exclusão recusada do instrumento vendido

Requisição `22` do Newman: `DELETE /api/instrumentos/25`, sem corpo, **HTTP 409**. Resposta integral:

```json
{
  "timestamp": "2026-10-28T16:43:28.838088100Z",
  "status": 409,
  "error": "Conflict",
  "message": "Instrumento não pode ser excluído porque está vinculado a uma venda",
  "path": "/api/instrumentos/25",
  "fields": {}
}
```

A requisição `23` consultou novamente o instrumento e recebeu **HTTP 200**, com saldo **8**. A requisição `24` consultou a venda e recebeu **HTTP 200**; a asserção comparou a resposta integral com a capturada na finalização e passou. O instrumento e o histórico permaneceram preservados.

## 7. Evidência adicional da atomicidade

O fluxo Newman demonstra a baixa correta dos dois instrumentos em uma finalização bem-sucedida. A prova de rollback vem de um teste integrado separado: [VendaServiceTest.deveFazerRollbackDaPrimeiraBaixaSeSegundoItemNaoTemSaldo](../../src/test/java/com/curso/instrumento2026/service/VendaServiceTest.java).

O teste inclui dois itens, força insuficiência no segundo e relê o banco fora da transação que falhou, verificando saldos originais e venda aberta. O resultado Surefire consultado na elaboração desta atividade registrou 14 testes da classe, zero falhas, erros ou ignorados. A execução da suíte completa utilizada como evidência registrou `BUILD SUCCESS` em 28/10/2026 às 13:33:57 −03:00. Não foi uma nova execução nesta etapa, e o cenário desse teste é distinto da venda 4 documentada acima.

## Explicação — oito linhas

```text
A venda aberta permite montar e remover itens sem reservar nem baixar estoque.
Os instrumentos 25 e 26 participaram com quantidades diferentes: duas e três unidades.
O preço do item é copiado na inclusão e permanece independente do preço atual do instrumento.
Assim, mudar A de 100 para 175 manteve seu preço histórico em 100 e o total da venda em 950.
A finalização coordenou as duas baixas na mesma transação e mudou a venda para FINALIZADA.
Se uma baixa falhar, o rollback preserva os saldos anteriores e a venda aberta, conforme teste integrado.
Após finalizar, remover o item retornou 400, preservando os dois itens e os saldos 8 e 9.
Excluir o instrumento vendido retornou 409, mantendo o instrumento e o histórico comercial.
```

Para repetir o cenário com novos IDs, use as [instruções da coleção](POSTMAN.md). A tentativa complementar de remoção deve usar um item retornado por uma venda própria já finalizada; não reutilize os IDs deste registro em outro banco.
