# Atividade de transferência — Aula 08

Projeto: **instrumento2026**, controle de instrumentos musicais de uma loja.

Referência: [Aula 08 do professor Jefferson Passerini](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/08aula/08aula.md), especialmente Checkpoints 1 a 9 e seções 13 a 20.

## Adaptação ao tema

| Material | Projeto |
|---|---|
| GrupoProduto | CategoriaInstrumento |
| Produto | Instrumento |
| codigoBarras | codigoInstrumento |
| descricao | nomeInstrumento |
| valorUnitario | precoUnitario |
| saldoEstoque | quantidadeEstoque, inteiro |
| grupoId / grupoNome | categoriaId / categoriaNome |

O preço deve ser **maior que zero**, preservando domínio e constraint existentes. O mínimo de estoque é decimal, mas entradas e saídas representam unidades inteiras. Frações são rejeitadas sem truncamento. A mensagem de saldo insuficiente é `Estoque insuficiente`.

## Contrato HTTP

As rotas abaixo têm prefixo `/api`. Saídas de categoria e instrumento são seus DTOs Response, nunca entidades JPA. `PaginaResponse` contém DTOs em `conteudo`.

| Método | URI | Entrada | Saída | Sucesso | Erros previstos |
|---|---|---|---|---|---|
| POST | /categorias | nome | CategoriaInstrumentoResponse + Location | 201 | 400, 409 |
| GET | /categorias/{id} | ID | CategoriaInstrumentoResponse | 200 | 404 |
| GET | /categorias | filtros e Pageable | PaginaResponse | 200 | 400 |
| PUT | /categorias/{id} | nome completo | CategoriaInstrumentoResponse | 200 | 400, 404, 409 |
| PUT | /categorias/{id}/status | status | CategoriaInstrumentoResponse | 200 | 400, 404 |
| DELETE | /categorias/{id} | ID | sem corpo | 204 | 404, 409 |
| POST | /instrumentos | InstrumentoRequest | InstrumentoResponse + Location | 201 | 400, 404, 409 |
| GET | /instrumentos/{id} | ID | InstrumentoResponse | 200 | 404 |
| GET | /instrumentos | filtros e Pageable | PaginaResponse | 200 | 400 |
| PUT | /instrumentos/{id} | InstrumentoAtualizacaoRequest | InstrumentoResponse | 200 | 400, 404 |
| PUT | /instrumentos/{id}/status | status | InstrumentoResponse | 200 | 400, 404 |
| POST | /instrumentos/{id}/estoque/entradas | quantidade | InstrumentoResponse | 200 | 400, 404 |
| POST | /instrumentos/{id}/estoque/saidas | quantidade | InstrumentoResponse | 200 | 400, 404 |
| DELETE | /instrumentos/{id} | ID | sem corpo | 204 | 404 |

Também permanecem POST, GET por ID e GET de listagem de `/fornecedores`, além de GET `/health`. Fornecedores continuam com listagem em array: o incremento da aula cobre categoria e instrumento. Não existe DELETE de fornecedor.

Exemplo de atualização completa dos dados editáveis (substituir IDs):

```json
{
  "nomeInstrumento": "Violino profissional",
  "precoUnitario": 2500.00,
  "estoqueMinimo": 3.000,
  "categoriaId": 1,
  "fornecedorId": null
}
```

Código, quantidade em estoque, data de cadastro e status não integram esse DTO. O fornecedor é opcional e `null` remove a referência. Categoria é obrigatória. Nome não pode ser branco; nomes de categoria são aparados e únicos ignorando caixa e espaços externos. Limites: categoria 120 caracteres; nome do instrumento 150.

Status: `{"status":"ATIVO"}` ou `{"status":"INATIVO"}`. Movimentação: `{"quantidade":3}`. Zero, negativo, nulo, fração e overflow são rejeitados. Saída superior ao saldo não modifica o estoque.

## Semântica e regras

- PUT substitui todos os dados editáveis e é idempotente: repetir o mesmo corpo mantém o estado final. A atualização de status também é idempotente.
- POST de estoque não é idempotente: repetir uma entrada de 3 soma mais 3. O domínio realiza a movimentação; o service coordena a transação.
- DELETE remove fisicamente o registro e retorna 204 sem corpo. É idempotente quanto ao efeito, mesmo que a repetição retorne 404.
- Inativar mantém registro e referências e permite reativação. É a alternativa quando uma categoria ainda possui instrumentos.
- 400 representa entrada/regra inválida; 404 indica recurso inexistente; 409 sinaliza nome/código duplicado ou categoria em uso.
- As associações são verificadas antes de alterar dados. A troca de categoria valida o código no destino antes de remover da origem, atualiza os dois lados e não faz nada quando o destino é o mesmo objeto.
- A coleção rejeita objetos distintos com o mesmo código. O mesmo objeto pode ser adicionado novamente sem duplicação.
- Operações de escrita usam `@Transactional`. Entidades carregadas na transação são atualizadas por dirty checking; não exigem `save` adicional. Exclusões executam `flush` dentro do service.
- Constraints continuam sendo a proteção definitiva contra concorrência. Não foram adicionados locking ou histórico de movimentações, extensões opcionais da aula.

## Consultas, paginação e ordenação

Filtros de categoria: `nome` (trecho, ignorando caixa) e `status`.

Filtros de instrumento: `nomeInstrumento` (trecho, ignorando caixa), `status`, `categoriaId`, `fornecedorId` e `abaixoEstoqueMinimo`. Este último só restringe quando `true`, usando comparação estrita entre colunas (`quantidadeEstoque < estoqueMinimo`). Igualdade não significa estoque abaixo do mínimo. Ausente ou `false` não restringe.

Specifications combinam condições com AND; o PostgreSQL executa filtragem, ordenação e paginação. Não há filtragem de listas em memória. Estoque inteiro é convertido para decimal na comparação com o mínimo.

`page` começa em 0; `size` padrão 20 e máximo 100 nas rotas HTTP. Os services recebem Pageable; o limite é aplicado pelo resolvedor web do Spring. Ordenação padrão `id,asc`. Para múltiplas ordens, repetir `sort`.

Campos permitidos:

- Categoria: `id`, `nome`, `status`.
- Instrumento: `id`, `codigoInstrumento`, `nomeInstrumento`, `quantidadeEstoque`, `precoUnitario`, `estoqueMinimo`, `dataCadastro`, `status`.

Caminhos arbitrários como `fornecedor.razaoSocial` são rejeitados com 400.

```text
GET /api/categorias?nome=cordas&status=ATIVO&page=0&size=10&sort=nome,asc
GET /api/instrumentos?nomeInstrumento=violino&status=ATIVO&categoriaId=1&abaixoEstoqueMinimo=true&page=0&size=5&sort=status,asc&sort=nomeInstrumento,asc
```

O contrato anterior de array mudou para objeto nas duas pesquisas:

```json
{
  "conteudo": [],
  "pagina": 0,
  "tamanho": 20,
  "totalElementos": 0,
  "totalPaginas": 0,
  "primeira": true,
  "ultima": true
}
```

O EntityGraph de instrumento carrega categoria e fornecedor nas consultas por ID, listagem anterior e pesquisa paginada. `open-in-view=false` permanece; `ddl-auto=validate` também.

Erros usam ApiError: `timestamp`, `status`, `error`, `message`, `path`, `fields`. Validação de DTO preenche campos; regras de domínio retornam mapa vazio. Handlers anteriores preservam sua representação existente. Não há conversão genérica de toda exceção para 400.

## Migrações

Arquivos existentes preservados: 001 categoria, 002 instrumento, 003 fornecedor/estoque mínimo e 004 unicidade normalizada. Total esperado: **18 changeSets**. Nenhuma migration executada foi reescrita nesta finalização.

004 cria `uk_categoria_instrumento_nome_normalizado` sobre `LOWER(BTRIM(nome))`, com rollback por DROP INDEX. As consultas de nomes usam a mesma expressão, incluindo registros antigos com espaços. Antes de aplicar em outro banco com dados, verificar duplicidades sem apagá-las automaticamente:

```sql
SELECT LOWER(BTRIM(nome)), COUNT(*)
FROM categoria_instrumento
GROUP BY LOWER(BTRIM(nome))
HAVING COUNT(*) > 1;
```

## Coleção Postman

Arquivo atualizado: [Instrumento 2026 - Aula 07.postman_collection.json](Instrumento%202026%20-%20Aula%2007.postman_collection.json). Nome interno: **Instrumento 2026 - Aula 08**. O caminho antigo foi mantido para não quebrar referências.

Importar e executar as cinco pastas em ordem, com `baseUrl=http://localhost:8080` e aplicação no profile dev em banco de aula. São **48 requisições**: preparação, consultas, alterações, status, movimentações, filtros/páginas, exemplos inválidos/conflitos e exclusões finais. Variáveis capturam IDs e geram código/CNPJ exclusivos por execução. Não há credenciais. O fornecedor criado permanece porque não existe operação de exclusão correspondente. Não executar em produção.

Para iniciar:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

As requisições têm códigos esperados nos nomes, descrições e scripts. Incluem repetição de PUT, repetição de entrada, conferência de saldo após rejeição, paginação, ordenação inválida, categoria em uso e corpo vazio na exclusão. As exclusões vêm depois de todas as operações que dependem dos IDs.

## Auditoria final dos Checkpoints 1 a 9

| Checkpoint | Resultado | Evidência no projeto |
|---|---|---|
| 1 — Domínio | Conforme na adaptação | Alterações específicas, regras monetárias/estoque, categoria bidirecional, duplicidade no destino, fornecedor opcional; testes de domínio sem Spring |
| 2 — Unicidade | Conforme | Índice funcional e rollback, inclusão no master, testes PostgreSQL de caixa/espaços e histórico de 18 changeSets |
| 3 — DTOs | Conforme na adaptação | Atualização sem campos controlados, status obrigatório, movimentação positiva; preço estritamente positivo |
| 4 — Repositories | Conforme | JpaSpecificationExecutor, consultas auxiliares e EntityGraph; teste de leitura após detach |
| 5 — Services | Conforme | Transações, dirty checking, pré-checagem de categoria em uso, exclusões com flush, estoque delegado ao domínio |
| 6 — Filtros | Conforme | Dois filtros de categoria, cinco de instrumento; integração JPA com registros compatíveis e incompatíveis |
| 7 — Página | Conforme | PaginaResponse, metadados, padrão 20/máximo 100, lista de campos permitidos e teste do resolvedor real |
| 8 — Controllers | Conforme | Rotas completas, DTOs, listagens paginadas, testes HTTP 200/201/204/400/404/409 |
| 9 — Erros | Conforme | IllegalArgumentException 400, RecursoEmUsoException 409, preservação dos handlers anteriores |

Diferenças deliberadas: estoque inteiro, preço positivo, nomes do tema e texto `Estoque insuficiente`; manutenção dos métodos listar dos services, sem uso deles nas pesquisas HTTP. A normalização adicional do repository protege compatibilidade com dados antigos. Após revisão externa, definirCategoria delega à troca completa, a remoção unilateral tem acesso de pacote e a inserção interna é restrita ao domínio, conforme a seção 3.3. O construtor que recebe categoria também inicializa os dois lados. Não há requisito funcional obrigatório dos nove checkpoints identificado como pendente nesta revisão.

Limites da evidência: testes transacionais com flush/clear demonstram escrita e releitura, mas não são um ensaio de concorrência nem de commit/rollback em transações independentes. definirCategoria e adicionarInstrumento permanecem públicos por compatibilidade com cadastro e testes; a primeira operação coordena os dois lados e a segunda rejeita instrumento de outra categoria. removerInstrumento não tem consumidores fora do domínio e agora é interno ao pacote. Publicação, commit e tag dependem de autorização do usuário e não foram feitos.

## Testes e evidências

Fontes de evidência: `src/test/java/com/curso/instrumento2026` e relatórios locais `target/surefire-reports`. O diretório target é gerado e não é versionado.

| Área | Classes principais |
|---|---|
| Domínio | CategoriaInstrumentoTest, InstrumentoTest, CorrecaoAula08Test |
| DTO | InstrumentoAtualizacaoRequestTest |
| Persistência | PersistenciaJpaTest, NormalizacaoCategoriaTest |
| Repository | RepositoryCheckpoint4Test |
| Services | CategoriaInstrumentoServiceTest, InstrumentoServiceTest, Checkpoint5ServiceTest |
| Filtros/páginas | PesquisaCheckpoint6Test, PaginacaoCheckpoint7Test |
| HTTP | InstrumentoApiTest, Checkpoint8ApiTest, Checkpoint9ApiTest |

Existem mais de dez testes novos, incluindo sucesso e falha, saldo preservado, conflito de exclusão, ID inexistente, status inválido, filtros combinados, páginas e ordenação inválida. O teste novo desta finalização verifica os 18 changeSets no PostgreSQL, sem reaplicar ou modificar migrações.

### Execução final — 15/10/2026

```powershell
.\mvnw.cmd test
git diff --check
git status --short
```

Resultado da suíte: **83 testes, 0 falhas, 0 erros, 0 ignorados — BUILD SUCCESS**, concluída às 14:07:37 (America/Sao_Paulo). O teste de histórico confirmou os **18 changeSets** aplicados. Git diff --check sem erros; git status mostra as alterações da Aula 08 ainda sem commit. Não há diferenças nos arquivos de migration; `.env` não é rastreado.

A coleção foi executada contra HTTP real na porta 18088 e PostgreSQL 17.6, banco `instrumento2026_test`: **48 requisições, 85 verificações, PASS**. Foi usado um executor temporário Node com fetch/vm que lê o JSON e executa o subconjunto de API `pm` usado pela coleção; **não foi executada a interface Postman nem o Newman**. A sintaxe dos scripts e o JSON também foram validados. Scripts auxiliares ficaram em target, fora dos entregáveis.

Comando usado para a instância local de validação, encerrada ao final:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.arguments=--server.port=18088 --spring.config.additional-location=optional:file:./src/test/resources/"
```

A primeira tentativa de iniciar pelo goal run não carregou a configuração em src/test/resources; a indicação explícita acima resolveu, sem alteração do código ou das configurações. A suíte Maven utiliza o profile test normalmente. Não houve bloqueio restante de ambiente.

Resultados HTTP observados e verificados nos scripts (campos resumidos; não representam respostas integrais):

| Cenário | Status observado | Evidência |
|---|---|---|
| Cadastros de categoria, fornecedor e instrumento | 201 | IDs capturados nas variáveis |
| Alteração e repetição do PUT | 200 | quantidadeEstoque continua 5 |
| Entrada de 3, repetida | 200 / 200 | quantidadeEstoque 8 e depois 11 |
| Saída de 2 | 200 | quantidadeEstoque 9 |
| Pesquisa combinada | 200 | totalElementos 1 |
| Consulta padrão / size=200 | 200 | tamanho 20 / 100 |
| Dados inválidos, fração, saldo insuficiente, ordenação inválida | 400 | ApiError com status, message, path, timestamp |
| Consulta após rejeições | 200 | quantidadeEstoque continua 9 |
| Nome/código duplicado e categoria em uso | 409 | ApiError; categoria mantida até a exclusão final |
| ID inexistente e consulta após exclusão | 404 | ApiError |
| Exclusões finais | 204 | corpo vazio |

As categorias e o instrumento criados pela coleção foram excluídos pelas requisições finais. Um fornecedor de teste permanece, pois não existe DELETE para fornecedor; nenhum dado anterior foi apagado. O banco dev não foi usado nessa validação. Importação visual no Postman não é evidência disponível nesta sessão; a coleção está pronta para importação.

## Conferência dos entregáveis

Revisão de encapsulamento em 15/10/2026, às 14:17:06: `AssociacaoInstrumentoTest` acrescenta cinco testes de chamadas isoladas, preservação em erro, inicialização bidirecional e remoção interna. Suíte completa reexecutada: **88 testes, 0 falhas, 0 erros, 0 ignorados — BUILD SUCCESS**. Todos os testes anteriores foram preservados.

Contrato das rotas e justificativa de PUT/POST/DELETE e 400/404/409: documentados acima. Código de domínio, aplicação e API: implementado. Migração necessária: 004 existente e preservada. Testes novos: acima do mínimo. Coleção Postman: atualizada. A atividade orientada A–E e a transferência para o tema estão contempladas. O envio ao GitHub permanece aguardando autorização.
