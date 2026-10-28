# Postman — Aula 10

Importe [Instrumento 2026 - Aula 10.postman_collection.json](<Instrumento 2026 - Aula 10.postman_collection.json>) no Postman por **Import → Files**. A coleção das aulas anteriores permanece independente e não precisa ser executada antes.

## Preparar e executar

1. Disponibilize o PostgreSQL de desenvolvimento e preencha o `.env` local conforme `.env.example`, sem versionar senhas.
2. Na raiz do projeto, inicie a API: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` (Windows: `.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev`).
3. Na coleção importada, confira **Variables → baseUrl**. O padrão é `http://localhost:8080`. Use seu ambiente de desenvolvimento.
4. Abra **Run collection**, mantenha as seis pastas e todas as 40 requisições selecionadas, na ordem original. Execute sequencialmente, começando em **00 - Iniciar execução**. Não é necessário configurar IDs ou documentos.
5. Confira a aba de resultados: os casos negativos (`400` e `409`) devem passar, pois são resultados esperados. Uma resposta com código diferente do esperado interrompe o encadeamento. Corrija a causa e reinicie pela requisição 00, que cria um cenário novo.

Não execute duas instâncias do Runner simultaneamente sobre a mesma coleção: elas compartilham as variáveis. Para inspecionar os registros depois da execução, mantenha os valores capturados; para repetir o cenário completo, comece novamente em 00. Não execute isoladamente as requisições de alteração, remoção ou finalização.

## Dados e variáveis

A inicialização gera um identificador de execução com horário e UUID, usado nos nomes, códigos dos instrumentos, e-mails e matrícula. CPF e CNPJ são gerados com dígitos verificadores válidos; isso não comprova existência nem titularidade. Os três CPFs gerados são consultados antes do cadastro e regenerados se já existirem. Os CPFs fixos usados pelos testes anteriores são excluídos da geração.

Todas as variáveis do cenário têm prefixo `a10_`. A requisição 00 limpa os IDs anteriores; respostas `201` capturam os novos IDs de pessoas, papéis, categoria, fornecedor e vendas. Inclusões de itens capturam seus IDs, inclusive após a reinclusão. As requisições posteriores usam somente esses valores. Não cadastre IDs manualmente nem reutilize identificadores de outros dados.

Os saldos iniciais são A = 10 e B = 12; os preços iniciais são A = 100,00 e B = 250,00. A primeira venda inclui A × 2 e B × 3. Após remover e reincluir B, o preço atual de A muda para 175,00, enquanto o item mantém 100,00 e o total da venda continua 950,00. A finalização deixa saldos A = 8 e B = 9. A segunda tentativa de finalizar retorna `400`, sem nova baixa. A segunda venda inclui B × 1 e é cancelada sem alterar os saldos.

## Organização e verificações

| Pasta | Cenário |
|---|---|
| 01 | Health textual `OK`; pessoas; CPF inválido `400`; CPF e e-mail duplicados `409`; cliente e vendedor |
| 02 | Categoria, fornecedor com CNPJ válido e dois instrumentos exclusivos |
| 03 | Venda aberta, quantidades diferentes, remoção/reinclusão, estoque intacto e snapshot de preço |
| 04 | Finalização, baixa única, repetição `400`, exclusão de vendido `409` e histórico integral preservado |
| 05 | Segunda venda com item, cancelamento e consulta dos saldos preservados |
| 06 | Filtros de pessoa, cliente, colaborador e vendas; período; duas páginas; resumo sem itens/total |

Os scripts verificam códigos HTTP, `Location`, IDs, documentos retornados, participantes, estados, quantidades de itens, preços, subtotais, total, estoque e envelope paginado. As consultas são filtradas por dados da execução, sem depender da quantidade de registros já existente no banco.

São 40 requisições principais e três consultas auxiliares de unicidade de CPF na inicialização (pode haver consultas adicionais se ocorrer colisão). Um fluxo normal executa **43 chamadas HTTP e 111 asserções**.

## Executar com Newman

Com Newman disponível, execute na raiz do projeto:

```powershell
newman run "docs/aula10/Instrumento 2026 - Aula 10.postman_collection.json" --env-var "baseUrl=http://localhost:8080" --bail
```

Não há credenciais embutidas nem necessidade de ambiente Postman separado. `baseUrl` pode ser sobrescrita por `--env-var`; os IDs `a10_` vêm da própria coleção.

## Dados deixados no desenvolvimento

A coleção cria duas pessoas, um cliente, um colaborador, uma categoria, um fornecedor, dois instrumentos e duas vendas. Os registros permanecem para conferência; a API não oferece exclusão de todos esses recursos. A única tentativa de excluir instrumento é feita sobre o A criado nesta execução e deve ser recusada com `409`. A remoção de item afeta exclusivamente a venda aberta criada pela coleção.

Não há limpeza por SQL, exclusão de dados anteriores ou desativação de cadastros compartilhados. Cada nova execução cria outro conjunto, sem reaproveitar os anteriores. Os relatórios do Runner/Newman podem conter os documentos sintéticos enviados: mantenha relatórios locais em `target`, ignorado pelo Git.
