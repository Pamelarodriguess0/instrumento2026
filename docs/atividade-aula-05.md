Atividade de transferencia - Aula 05
Nesta atividade foram utilizadas principalmente as entidades:

- `CategoriaInstrumento`
- `Instrumento`
O objetivo foi implementar a camada de acesso a dados e a camada de serviços, separando as responsabilidades da aplicação.

Foram criados os repositories:

- `CategoriaInstrumentoRepository`
- `InstrumentoRepository`
Os repositories utilizam Spring Data JPA e são responsáveis pelo acesso aos dados persistidos no banco.
Por meio deles, a aplicação pode realizar operações de consulta e persistência sem precisar escrever manualmente todo o código de acesso ao banco.

Consulta pro chave de negocio
Para `Instrumento`, o código do instrumento é utilizado como chave de negócio.
Foi implementada uma consulta que permite localizar um instrumento pelo seu `codigoInstrumento`.
Essa consulta é importante porque o código identifica o instrumento dentro das regras do sistema, enquanto o `id` é utilizado principalmente como identificador técnico no banco de dados.

Consulta pelo relacionamento
Também foi implementada uma consulta utilizando o relacionamento entre `Instrumento` e `CategoriaInstrumento`.
Dessa forma, é possível recuperar instrumentos associados a uma determinada categoria.
O relacionamento é do tipo 1:N:
- uma categoria pode possuir vários instrumentos;
- cada instrumento pertence a uma categoria.

Camada de serviço
Foram criados serviços para representar e coordenar os casos de uso da aplicação, entre eles:
- `CategoriaInstrumentoService`
- `InstrumentoService`
A camada de serviço utiliza os repositories para acessar os dados e coordena as regras necessárias para realizar as operações do sistema.

Cadastro Transacional
O cadastro de instrumento é realizado dentro de uma transação.
O uso de `@Transactional` garante que as operações relacionadas ao caso de uso sejam tratadas como uma única unidade.
Caso alguma operação necessária ao cadastro falhe, a transação pode realizar rollback, evitando que apenas uma parte da operação seja gravada no banco.

Teste de falha e rollback
Foi realizado teste provocando uma falha durante uma operação transacional.
O objetivo foi verificar que, quando ocorre uma exceção durante a transação, as alterações realizadas naquele caso de uso não permanecem parcialmente gravadas no banco.
Dessa forma, o teste demonstra o funcionamento do rollback e a preservação da consistência dos dados.

Responsabilidades das regras
As regras da aplicação foram separadas de acordo com sua responsabilidade.

Domínio
No domínio ficam regras que pertencem ao próprio objeto.
Exemplos em `Instrumento`:
- impedir quantidade de estoque inválida;
- impedir preço inválido;
- adicionar estoque;
- retirar estoque;
- ativar e inativar um instrumento.
Essas regras fazem parte do comportamento do próprio instrumento e não dependem diretamente do acesso ao banco.

Serviço
Na camada de serviço ficam regras que coordenam um caso de uso e que podem depender de consultas ou operações de persistência.
Exemplos:
- verificar se um código de instrumento já está cadastrado;
- buscar uma categoria necessária para cadastrar um instrumento;
- coordenar o cadastro dentro de uma transação;
- lançar exceções quando um recurso não é encontrado ou quando existe duplicidade.

Banco de dados
No banco ficam regras de integridade que devem ser garantidas independentemente da aplicação Java.
Exemplos:

- `PRIMARY KEY`;
- `FOREIGN KEY`;
- `UNIQUE`;
- `NOT NULL`;
- `CHECK`.
Essas constraints ajudam a impedir que dados inválidos sejam armazenados mesmo que uma operação não passe pelas validações da camada de domínio ou serviço.

Exceções da Aplicação
Foram criadas exceções específicas para representar falhas esperadas dos casos de uso:
- `RecursoNaoEncontradoException`
- `RecursoDuplicadoException`
Nesta etapa, essas exceções representam erros da aplicação sem definir códigos HTTP. O tratamento HTTP pertence à camada de API e será realizado posteriormente.

Testes
Foram utilizados testes da camada de serviço para verificar o comportamento dos casos de uso, incluindo operações bem-sucedidas e situações de falha.
Os testes ajudam a confirmar a separação das responsabilidades entre domínio, serviço, repository e banco de dados.

Evidencias da atividade
Os testes da camada de serviço foram executados com sucesso.
No `InstrumentoServiceTest` foram verificados:
- cadastro de instrumento com categoria válida;
- falha ao cadastrar instrumento com categoria inexistente;
- lançamento de `RecursoNaoEncontradoException`;
- confirmação de que o instrumento não permanece salvo após a falha.

O teste de rollback verifica o código `INT-ROLLBACK-001` no repository após a exceção e confirma que ele não foi persistido.