ALterações realizadas no projeto: Foi adicionado a entidade "Fornecedor", permitindo relacionar um fornecedor a instrumento

com isso, foi adicionados os seguintes campos a instrumento:
    - 'estoqueMinimo'
    - 'fornecedor'

Para representar essas alterações no banco de dados, foi criada a migration:
`003-fornecedor-e-estoque-minimo.yaml`

Foi utilizado o Liquibase para comparar a estrutura anterior do banco de dados com a nova estrutura gerada pelas entidades JPA.
O comando `liquibase:diff` gerou um rascunho contendo 7 changeSets.
O rascunho foi analisado antes da criação da migration definitiva.

Problemas encontrados no rascunho

 1. Alteração desnecessária da chave estrangeira de categoria
O rascunho gerado pelo Liquibase removia a chave estrangeira existente entre `instrumento` e `categoria_instrumento` e depois criava novamente essa relação com outro nome.
Essa alteração não era necessária, pois o relacionamento com categoria já existia e não fazia parte da evolução realizada nesta aula.
A remoção e recriação dessa chave estrangeira foram descartadas da migration final.

2. Criação de estoque_minimo diretamente como NOT NULL
O rascunho adicionava a coluna `estoque_minimo` diretamente com a restrição `NOT NULL`.
Isso poderia causar problemas em um banco que já possuísse instrumentos cadastrados, pois esses registros antigos não teriam valor para a nova coluna.
A migration foi revisada para realizar a alteração em três etapas:

1. adicionar `estoque_minimo` permitindo valores nulos temporariamente;
2. preencher os registros antigos com `estoque_minimo = 0`;
3. adicionar a restrição `NOT NULL`.
Dessa forma, os dados existentes são preservados durante a evolução do banco.

3. Nomes de constraints gerados automaticamente
O rascunho apresentou nomes de constraints gerados automaticamente, incluindo um nome automático para a chave estrangeira da categoria.
Na migration revisada foram utilizados nomes controlados e descritivos, como:
- `pk_fornecedor`
- `uk_fornecedor_cnpj`
- `ck_fornecedor_status`
- `ck_instrumento_estoque_minimo`
- `fk_instrumento_fornecedor`
Isso facilita a identificação e manutenção das constraints do banco de dados.

 Migration revisada
Após a análise do rascunho, foi criada a migration `003-fornecedor-e-estoque-minimo.yaml`.

Ela realiza:

- criação da tabela `fornecedor`;
- restrição de CNPJ único;
- validação do status do fornecedor;
- criação de `estoque_minimo`;
- atualização dos registros antigos com estoque mínimo igual a zero;
- aplicação de `NOT NULL` em `estoque_minimo`;
- validação para impedir estoque mínimo negativo;
- criação de `fornecedor_id` em `instrumento`;
- criação da chave estrangeira entre instrumento e fornecedor.

Teste de preservação de dados
Para validar a preservação dos dados, foi preparado um banco contendo somente as migrations `001` e `002`, representando a estrutura anterior à Aula 06.
Nesse banco foi cadastrado um instrumento antes da aplicação da migration `003`:

- código: `INT-ANTIGO-001`
- nome: `Violino Antigo`
- quantidade em estoque: `4`
- preço unitário: `1800.00`
Em seguida, a migration `003` foi aplicada.
Após a atualização, foi verificado que o instrumento continuava cadastrado com seus dados anteriores preservados.

Também foi verificado que:
- `estoque_minimo` recebeu o valor `0`;
- `fornecedor_id` permaneceu `NULL`, pois o relacionamento com fornecedor é opcional.
Assim, foi confirmado que a migration permite a evolução da estrutura do banco sem perda dos dados existentes.