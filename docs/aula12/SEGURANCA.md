# Segurança — Aula 12

[Índice da aula](README.md) · [Evidências](EVIDENCIAS.md) · [Postman](POSTMAN.md)

## Responsabilidades e separação do domínio

Identificação informa um login; autenticação verifica sua credencial; autorização decide o acesso permitido. Nesta versão, todas as contas autenticadas têm o mesmo acesso comercial. Perfil de acesso e função de Colaborador são conceitos distintos.

| Componente | Finalidade implementada |
|---|---|
| [ContaAcesso](../../src/main/java/com/curso/instrumento2026/domain/ContaAcesso.java) | Credencial ligada obrigatoriamente a uma Pessoa; login, hash, status e conjunto de perfis. Inicia ATIVA e OPERADOR. `podeAutenticar()` exige conta ATIVA e Pessoa com `Status.ATIVO`. Usa `@DynamicUpdate` para atualizar somente atributos alterados. |
| [PerfilAcesso](../../src/main/java/com/curso/instrumento2026/domain/PerfilAcesso.java) | Enum OPERADOR/ADMINISTRADOR, convertido em authorities `ROLE_OPERADOR`/`ROLE_ADMINISTRADOR`; não deriva de função comercial. |
| [StatusContaAcesso](../../src/main/java/com/curso/instrumento2026/domain/StatusContaAcesso.java) | Enum ATIVA/BLOQUEADA. É independente do status da Pessoa. |
| [ContaAcessoRepository](../../src/main/java/com/curso/instrumento2026/repository/ContaAcessoRepository.java) | Persistência e consultas por login/Pessoa. `findByLoginIgnoreCase` e `findOneById` carregam Pessoa e perfis via EntityGraph. |
| [ContaAcessoService](../../src/main/java/com/curso/instrumento2026/service/ContaAcessoService.java) | Cadastro, consultas, troca de senha e alteração de status em transações. Valida e codifica senhas; traduz os dois conflitos conhecidos de unicidade. |
| [ContaAcessoUserDetailsService](../../src/main/java/com/curso/instrumento2026/service/ContaAcessoUserDetailsService.java) | Carrega a conta persistida em transação de leitura, normaliza login e produz UserDetails com hash, authorities e condição de habilitação. Login ausente gera mensagem genérica. |
| [PasswordEncoderConfig](../../src/main/java/com/curso/instrumento2026/config/PasswordEncoderConfig.java) | Disponibiliza um decorador do encoder de `PasswordEncoderFactories.createDelegatingPasswordEncoder()`: rejeita acima de 72 bytes UTF-8 em `matches`, delega a verificação válida e preserva `encode`/`upgradeEncoding`. Novas senhas continuam BCrypt. |
| [SecurityConfig](../../src/main/java/com/curso/instrumento2026/config/SecurityConfig.java) | HTTP Basic, STATELESS, exceções públicas por método e autenticação para as demais requisições. |
| [ContaAcessoController](../../src/main/java/com/curso/instrumento2026/api/ContaAcessoController.java) | Expõe somente cadastro, consulta própria e troca da própria senha. Usa DTOs e mapper; não retorna a entidade. |

Pessoa, Cliente e Colaborador não receberam campos de senha. A associação JPA com Pessoa é `@OneToOne`, obrigatória e LAZY; perfis são valores em `@ElementCollection` EAGER com `EnumType.STRING`. O EntityGraph evita depender de sessão aberta no mapeamento das consultas. Não há tabela de entidade PerfilAcesso.

## Persistência e migração 006

Fonte: [006-contas-acesso.yaml](../../src/main/resources/db/changelog/changes/006-contas-acesso.yaml), incluída pelo [master](../../src/main/resources/db/changelog/db.changelog-master.yaml). Os changeSets `006-01-create-conta-acesso` e `006-02-create-conta-acesso-perfil` elevam o total de 39 para 41.

| Tabela/coluna | Tipo e restrição |
|---|---|
| `conta_acesso.id` | BIGINT identity, PK `pk_conta_acesso`, não nulo |
| `conta_acesso.pessoa_id` | BIGINT, não nulo, UNIQUE `uk_conta_acesso_pessoa` |
| `conta_acesso.login` | VARCHAR(80), não nulo |
| `conta_acesso.senha_hash` | VARCHAR(255), não nulo; recebe apenas hash no fluxo do serviço |
| `conta_acesso.status` | VARCHAR(20), não nulo; CHECK `ck_conta_acesso_status`: ATIVA/BLOQUEADA |
| `conta_acesso_perfil.conta_acesso_id` | BIGINT, não nulo |
| `conta_acesso_perfil.perfil` | VARCHAR(30), não nulo; CHECK `ck_conta_acesso_perfil`: OPERADOR/ADMINISTRADOR |

`fk_conta_acesso_pessoa` referencia `pessoa(id)` com ON DELETE RESTRICT. A tabela de perfis tem PK composta `pk_conta_acesso_perfil` em `(conta_acesso_id, perfil)`, impedindo repetição do mesmo perfil. Sua FK `fk_conta_acesso_perfil_conta` usa ON DELETE CASCADE: excluir uma conta remove seus perfis, não a Pessoa.

O índice único funcional `uk_conta_acesso_login_lower` usa `LOWER(BTRIM(login))`. `Operador`, `operador` e suas variantes com espaços externos disputam a mesma identidade. A aplicação aplica `trim()` e lowercase com `Locale.ROOT`, exige login de 3 a 80 caracteres e aceita letras ASCII, números, ponto, hífen e sublinhado. As consultas JPQL com `lower(trim(...))` seguem essa normalização.

As pré-consultas melhoram as mensagens, mas não garantem unicidade concorrente. `saveAndFlush` força a verificação pelo banco; o serviço reconhece SQLSTATE 23505 e os nomes dos dois constraints/índices de unicidade, traduzindo para `RecursoDuplicadoException`. Outras violações não são mascaradas como login duplicado. Os testes concorrentes exercitam duas transações que ultrapassam as pré-consultas.

O rollback estrutural remove primeiro `conta_acesso_perfil` e depois `conta_acesso`, na ordem reversa dos changeSets. Houve validação/geração do SQL de rollback, sem execução destrutiva sobre dados existentes. A exigência de pelo menos um perfil é da aplicação; a estrutura SQL não obriga toda conta a possuir uma linha de perfil. O banco também não verifica se uma string em `senha_hash` é BCrypt: essa responsabilidade é do serviço.

## Senhas e DTOs

O serviço rejeita senha nula, em branco, com menos de **12 pontos de código Unicode** (`codePointCount`) ou acima de **72 bytes UTF-8**. Pontos de código não equivalem necessariamente a caracteres visuais/graphemes. O DTO usa Bean Validation com tamanho em unidades da String Java; a validação efetiva Unicode/bytes também ocorre no serviço.

Não há trim, lowercase ou normalização Unicode da senha. Espaços e caixa fazem parte da credencial. Exemplos comprovados incluem 36 caracteres `é` (72 bytes) aceitos e 37 rejeitados, além da contagem de caracteres suplementares. O limite evita equivalências decorrentes do limite efetivo do BCrypt.

O encoder delegante gera `{bcrypt}` seguido do hash. Salt aleatório faz senhas iguais produzirem hashes diferentes. O custo aumenta o trabalho por tentativa; o projeto usa o padrão do encoder, sem configurar um custo próprio. Hash não é criptografia reversível. A verificação usa `matches(senha, hash)`, nunca igualdade entre duas chamadas de `encode`.

Desde o Checkpoint 7B, o bean PasswordEncoder também mede os bytes UTF-8 durante a verificação, inclusive no HTTP Basic. Para entrada nula ou acima de 72 bytes, `matches` retorna `false` antes de chamar o delegate. Essa guarda não aplica trim ou normalização e não altera os hashes existentes. O mínimo Unicode e as regras de cadastro/troca continuam no serviço; não foram transferidos para a SecurityFilterChain.

Na troca, a senha atual precisa corresponder ao hash e a nova precisa atender às regras e ser diferente da atual. Após commit, a anterior deixa de autenticar. Não há troca de senha de terceiros: o controller obtém o login de `Principal.getName()`.

[ContaAcessoRequest](../../src/main/java/com/curso/instrumento2026/api/dto/ContaAcessoRequest.java) aceita `pessoaId`, `login`, `senha`. [AlterarSenhaContaRequest](../../src/main/java/com/curso/instrumento2026/api/dto/AlterarSenhaContaRequest.java) aceita `senhaAtual` e `novaSenha`. Os campos secretos são WRITE_ONLY no JSON/OpenAPI e seus `toString()` os ocultam. O cadastro não recebe perfil ou status; campos adicionais enviados não concedem ADMINISTRADOR nem alteram o estado inicial.

[ContaAcessoResponse](../../src/main/java/com/curso/instrumento2026/api/dto/ContaAcessoResponse.java) e o [mapper](../../src/main/java/com/curso/instrumento2026/api/mapper/ContaAcessoMapper.java) retornam somente `id`, `pessoaId`, `nomePessoa`, `login`, `status`, `perfis`. Não há senha ou hash em resposta. O DTO interno [StatusContaAcessoRequest](../../src/main/java/com/curso/instrumento2026/api/dto/StatusContaAcessoRequest.java) não implica existência de endpoint de status.

## Autenticação e rotas reais

O cliente envia `Authorization: Basic base64(login:senha)`. O filtro consulta UserDetails e o provider usa o PasswordEncoder existente. Conta BLOQUEADA ou Pessoa INATIVO produz UserDetails desabilitado; ambas impedem autenticação. Ativar a conta não reativa a Pessoa. O cadastro de conta para Pessoa inativa também é rejeitado.

STATELESS impede armazenar autenticação em sessão. Cada requisição apresenta a credencial, permitindo observar bloqueio e mudança de status nas requisições seguintes. Os testes verificam ausência de sessão e cookie JSESSIONID; isso não demonstra revogação de uma requisição que já foi autenticada e está em andamento.

| Métodos | Caminhos | Regra |
|---|---|---|
| GET e HEAD | `/api/health` | Público |
| GET e HEAD | `/v3/api-docs`, `/v3/api-docs/**`, `/v3/api-docs.yaml`, `/swagger-ui.html`, `/swagger-ui/**` | Público quando o recurso está habilitado |
| POST | `/api/pessoas`, `/api/contas-acesso` | Bootstrap público provisório |
| Demais requisições | Incluindo `/api/pessoas` fora do POST público, `/api/clientes`, `/api/colaboradores`, `/api/categorias`, `/api/fornecedores`, `/api/instrumentos`, `/api/vendas` e subrotas | Exigem autenticação |
| GET / PUT | `/api/contas-acesso/me` / `/api/contas-acesso/me/senha` | Exigem autenticação; identidade vem do Principal |

As liberações são por método: tornar o POST de Pessoa público não libera listagem ou alteração anônima. `anyRequest().authenticated()` também cobre rotas não listadas. Estar autenticado não cria um endpoint inexistente. Rotas por ID, troca de senha de terceiros e administração de contas não foram expostas.

## Endpoints e códigos HTTP

| Operação | Entrada | Sucesso e erros |
|---|---|---|
| POST `/api/contas-acesso` | ContaAcessoRequest | 201 com ContaAcessoResponse; 400 por validação/Pessoa inativa; 404 por Pessoa inexistente; 409 por login ou Pessoa duplicados |
| GET `/api/contas-acesso/me` | Principal autenticado | 200 com ContaAcessoResponse; 401 por ausência/falha de autenticação; 404 se a consulta do serviço não localizar a conta |
| PUT `/api/contas-acesso/me/senha` | Principal e AlterarSenhaContaRequest | 204 sem corpo; 400 por senha atual incorreta/nova inválida; 401 pelos filtros; 404 se a consulta do serviço não localizar a conta |

O 404 do cadastro foi observado no teste de Pessoa inexistente. Os 404 de `/me` e troca estão declarados no controller e são compatíveis com o serviço; **não foram apresentados como cenários HTTP normais reproduzidos**, pois uma conta ausente tende a ser recusada antes pelo filtro. A evidência observada e a documentação declarada estão discriminadas em [EVIDENCIAS](EVIDENCIAS.md).

Erros de negócio/validação são tratados pelo [ApiExceptionHandler](../../src/main/java/com/curso/instrumento2026/api/ApiExceptionHandler.java), com ApiError. O 401 ocorre antes do controller; os testes observaram corpo vazio e, nos casos verificados, desafio `WWW-Authenticate: Basic`. Não existe contrato ApiError para esses filtros. Não foi criada regra de autorização por perfil para produzir 403 administrativo. A liberação pública não dispensa o processamento de um cabeçalho Basic inválido eventualmente enviado pelo cliente.

## OpenAPI e Swagger

[OpenApiConfig](../../src/main/java/com/curso/instrumento2026/config/OpenApiConfig.java) publica `basicAuth`, `type: http`, `scheme: basic` e uma exigência global de segurança. As três operações públicas (GET health, POST Pessoa e POST ContaAcesso) têm `@SecurityRequirements` vazio, gerando `security: []`. As demais herdam a exigência global. Isso documenta a política; quem a executa é a SecurityFilterChain.

Os metadados e o customizador `quantidadeInteiraDoItemVenda` foram preservados. Os testes de compatibilidade permitem apenas adições explícitas de conta/segurança e as três descrições concorrentes da Aula 11, mantendo métodos, operationId, parâmetros, respostas e schemas históricos. A [exportação da Aula 10](../aula10/openapi.yaml) não foi regravada.

O schema é consumido pela UI do Swagger. Página, configuração, JSON e YAML foram verificados por MockMvc; o fluxo visual Authorize/logout ainda não foi executado manualmente. Em [application-prod.properties](../../src/main/resources/application-prod.properties), `springdoc.api-docs.enabled=false` e `springdoc.swagger-ui.enabled=false` continuam definidos; liberar a URL no filtro não habilita o recurso desativado.

## Defeitos reproduzidos e corrigidos nos Checkpoints 7A/7B

### Equivalência de senha excedente na autenticação

O experimento isolado do encoder da auditoria não bastava como prova HTTP. [ContaAcessoAutenticacaoLimiteDiagnosticoTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoAutenticacaoLimiteDiagnosticoTest.java) cadastrou contas reais no PostgreSQL e chamou GET `/api/contas-acesso/me` com filtros ativos. Tanto para ASCII quanto para UTF-8 multibyte, a senha correta de 72 bytes retornou 200, a de 73 bytes com os mesmos primeiros 72 retornou indevidamente 200, e uma incorreta retornou 401.

A causa era a equivalência na verificação BCrypt sem a guarda de tamanho na autenticação; as validações de gravação do serviço não cobriam esse caminho. O decorador em PasswordEncoderConfig passou a rejeitar o excesso antes da delegação. Após a correção, os dois casos retornaram **200 / 401 / 401**, mantendo as expectativas originais. Não houve mudança de endpoint, hash, login ou conteúdo da senha.

### Perda de atualização entre senha e bloqueio

[ContaAcessoAtualizacaoConcorrenteDiagnosticoTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoAtualizacaoConcorrenteDiagnosticoTest.java) abriu duas transações REQUIRES_NEW, em conexões e IDs de transação PostgreSQL distintos. Ambas carregaram a conta antes do primeiro commit; latches controlaram a ordem, sem sleeps. Os services reais participaram dessas transações, reencontrando o estado antigo no contexto JPA. A leitura final ocorreu em outra transação após ambos os commits.

Antes da correção, o UPDATE da entidade incluía atributos que aquela operação não pretendia alterar: senha → bloqueio restaurava o hash antigo; bloqueio → senha restaurava ATIVA. Isso podia perder a troca de senha ou desfazer o bloqueio.

`@DynamicUpdate` fez o Hibernate incluir somente os atributos alterados. `alterarSenha` modifica o hash e `alterarStatus` modifica o status, sem sobrescrever o outro campo com o estado antigo. Nas duas ordens, a conta terminou **BLOQUEADA**, com hash correspondente à nova senha e rejeitando a antiga. Os checks do hash persistido não foram apresentados como novas tentativas HTTP de uma conta bloqueada.

As regras dos services e a migração 006 permaneceram intactas. Não foi necessário adicionar locks ou coluna de versão para os dois cenários exercitados. **Limitação:** `@DynamicUpdate` não serializa duas alterações do mesmo atributo nem torna a validação da senha atual atômica entre duas trocas simultâneas. Esses casos não foram investigados pelos diagnósticos; eventual controle adicional exige cenário e validação próprios.

Os quatro diagnósticos passaram sem enfraquecer suas expectativas. Resultados e datas antes/depois estão em [EVIDENCIAS](EVIDENCIAS.md).

## Riscos didáticos e limitações que permanecem

| Limitação | Consequência e limite atual |
|---|---|
| Bootstrap público com pessoaId | Ter conhecimento de uma Pessoa ativa ainda sem conta permite tentar vinculá-la a uma credencial sem provar titularidade. O perfil inicial fixo não corrige esse risco. |
| Basic em HTTP local | Base64 não protege segredo em trânsito; HTTPS é necessário em produção. Não houve teste de implantação TLS. |
| CSRF desabilitado | Simplificação didática para cliente que envia Authorization explicitamente. Navegadores podem reenviar Basic automaticamente; STATELESS não elimina CSRF. É necessário reavaliar antes de uso autenticado por navegador. |
| Sem JWT ou expiração de token | Usa-se a senha em cada requisição Basic; não há emissão/renovação de tokens nem logout de sessão implementado. |
| Sem diferenciação por perfil | OPERADOR e ADMINISTRADOR têm o mesmo acesso autenticado; não há administração protegida por role. |
| Sem endpoint de bloqueio | Serviço/domínio suportam mudança de status; o Postman depende de preparação SQL restrita ao banco local. Não é uma API administrativa. |
| Sem rate limiting, MFA, recuperação e auditoria de acesso | Ataques de tentativa repetida e comprometimento de credencial não têm esses controles nesta aula. |
| Integridade parcial no banco | CHECKs limitam valores, mas não obrigam existência de perfil nem validam o conteúdo criptográfico do hash. Escritas SQL externas podem violar invariantes da aplicação. |
| Requisições em andamento | Os testes comprovam bloqueio na próxima requisição; não demonstram cancelamento instantâneo de chamadas já autenticadas. |
| Concorrência sobre o mesmo atributo | Atualização seletiva preservou hash/status em campos distintos. Duas trocas simultâneas de senha ou alterações conflitantes do mesmo status não estão necessariamente protegidas e não foram reproduzidas nessa correção. |
| Documentação desativada em prod | Comportamento definido em propriedades; a implantação prod completa não foi exercitada nesta entrega. |

Nenhuma dessas limitações é apresentada como corrigida ou como ataque explorado em produção. O laboratório e os testes cobrem os cenários descritos em [EVIDENCIAS](EVIDENCIAS.md).

Melhorias futuras incluem investigar concorrência sobre o mesmo atributo e avaliar locking/versionamento conforme a garantia desejada, além de substituir o bootstrap público e implementar os controles de produção já indicados acima. JWT, autorização administrativa, rate limiting e MFA não foram implementados como parte das correções 7B. As validações manuais de Postman, Swagger, TLS e comportamento em navegador continuam pendentes; a atualização documental 7C não as executou.
