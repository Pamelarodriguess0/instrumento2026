Atividade- Aula04

Tema do projeto: Sistema para controle de instrumentos musicais de uma loja.
As entidades utilizadas nesta etapa foram:

- `CategoriaInstrumento`
- `Instrumento`
Uma categoria pode possuir vários instrumentos e cada instrumento pertence a uma categoria. Relacionamento um-para-muitos.

Configuração do ambiente
O projeto utiliza PostgreSQL e possui configurações separadas para os ambientes da aplicação.
Foram utilizados profiles para separar as configurações de desenvolvimento, teste e produção, evitando que os testes utilizem o mesmo banco de dados do ambiente de desenvolvimento.
As informações sensíveis de conexão são carregadas por meio de variáveis de ambiente, evitando armazenar senhas diretamente no código-fonte.

Versionamento do banco com Liquibase
A estrutura do banco de dados é controlada pelo Liquibase.
Foi criado um changelog mestre responsável por organizar as migrations: `db.changelog-master.yaml`

As migrations desta etapa foram:
- `001-create-categoria-instrumento.yaml`
- `002-create-instrumento.yaml`
Dessa forma, a estrutura do banco pode ser criada e atualizada de maneira controlada e reproduzível.

Relacionamento entre entidades:
O relacionamento entre `CategoriaInstrumento` e `Instrumento` é do tipo 1:N.
Uma categoria pode possuir vários instrumentos, enquanto cada instrumento possui uma única categoria.
No JPA, esse relacionamento é representado por `@OneToMany` em `CategoriaInstrumento` e `@ManyToOne` em `Instrumento`.
No banco de dados, a coluna `categoria_instrumento_id` da tabela `instrumento` funciona como chave estrangeira para a tabela `categoria_instrumento`.

Chave Primária (Primary key)
As tabelas possuem uma chave primária `id` para identificar cada registro de forma única.

Chave Estrangeira (Foreign Key)
A chave estrangeira entre `instrumento` e `categoria_instrumento` garante que um instrumento não seja associado a uma categoria inexistente.

Unique
O campo `codigo_instrumento` é único porque representa a identificação de negócio do instrumento. Dois instrumentos não devem possuir o mesmo código.

Not Null
Campos essenciais, como código, nome, quantidade em estoque, preço, data de cadastro, status e categoria, não podem ficar sem valor.
Essas restrições ajudam a impedir registros incompletos ou inválidos no banco de dados.

Check
Foram utilizadas regras de integridade para impedir valores inválidos, como quantidade de estoque negativa.
Além das validações realizadas no domínio Java, a validação no banco aumenta a proteção da integridade dos dados.

Teste de Persistencia
Foi criado o `PersistenciaJpaTest` para verificar a integração entre JPA e PostgreSQL.
O teste persiste uma categoria e um instrumento relacionados, limpa o contexto de persistência e realiza novamente a leitura do instrumento, verificando se os dados e o relacionamento foram armazenados corretamente.
Também foi realizado um teste de constraint para verificar que o banco rejeita uma tentativa de armazenamento de quantidade de estoque negativa.

Historico do Liquibase
O histórico das migrations pode ser verificado na tabela `databasechangelog`, criada e mantida pelo Liquibase.
Ela permite identificar quais changeSets já foram executados no banco e evita que a mesma migration seja aplicada novamente.


Execução dos testes
Após a implementação e configuração da persistência, os testes do projeto foram executados com Maven.
Resultado obtido:
- Testes executados: 14
- Falhas: 0
- Erros: 0
- Testes ignorados: 0
- Resultado final: `BUILD SUCCESS`

Histórico do Liquibase
A execução das migrations foi verificada por meio da tabela `databasechangelog` do Liquibase.
Foram encontrados registros referentes às migrations:

- `001-create-categoria-instrumento.yaml`
- `002-create-instrumento.yaml`