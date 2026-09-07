ATIVIDADE
 
Status HTTP
200 - OK
O status 200 é utilizado quando uma requisição é realizada com sucesso.
No projeto, ele é utilizado nas consultas, como ao buscar um instrumento por ID ou listar os instrumentos cadastrados.

201 - CREATED
O status 201 é utilizado quando um novo recurso é cadastrado com sucesso.
No projeto, ele é retornado no cadastro de categorias, fornecedores e instrumentos.

400 - BAD REQUEST
O status 400 é utilizado quando os dados enviados na requisição são inválidos ou quando o JSON está malformado.
Por exemplo, ocorre quando um instrumento é enviado com nome vazio, quantidade negativa ou preço inválido.

404 - NOT FOUND
O status 404 é utilizado quando o recurso solicitado não existe.
No projeto, ocorre ao tentar buscar um instrumento, categoria ou fornecedor com um ID que não está cadastrado.

409 - CONFLICT
O status 409 é utilizado quando a requisição entra em conflito com um dado que já existe.
No projeto, ocorre ao tentar cadastrar uma categoria com nome já utilizado, um fornecedor com CNPJ já cadastrado ou um instrumento com código duplicado.