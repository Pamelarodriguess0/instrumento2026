# Aula 15 — Checkpoint 1: inventário e estratégia de testes

> Registro histórico do Checkpoint 1. Contagens e pendências referem-se àquela etapa;
> consulte o [estado final da Aula 15](README.md#estado-final-da-aula-15) para o fechamento.

Data: 11/11/2026. Projeto: instrumento2026.
Fonte: [seção 21, Checkpoint 1, da Aula 15](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/15aula/15aula.md).
Índice e atendimento das exigências: [README.md](README.md).

## Exigência e método de análise

O checkpoint exige um inventário e a classificação de pelo menos um teste de
cada nível, com seis informações: unidade observada, infraestrutura, evidência,
custo esperado, defeito detectável e limite da detecção.

O inventário abaixo foi reconferido por leitura dos arquivos Java, das annotations
e das dependências utilizadas. A classificação considera a fronteira exercitada,
não apenas o nome ou o pacote. Segurança é também uma finalidade transversal:
um teste de domínio pode proteger uma regra de segurança sem validar filtros HTTP.
Cada classe aparece uma única vez no inventário; a matriz posterior reutiliza
exemplos para explicar os níveis, sem somar novamente seus testes.

O comando de listagem proposto na aula pode ser adaptado para incluir também
`Instrumento2026ApplicationTests.java`, cujo nome termina em `Tests`:

```powershell
Get-ChildItem src/test/java -Recurse -File | Where-Object { $_.Name -match 'Tests?\.java$' }
```

A contagem estática considera métodos anotados com `@Test` e `@ParameterizedTest`.
Não confundir métodos parametrizados com quantidade de execuções.

## Inventário completo

**54 classes, 325 métodos `@Test` e 70 métodos `@ParameterizedTest`: 395 métodos.**
Nas tabelas, T significa `@Test`; P significa `@ParameterizedTest`.
Os arquivos de apoio `AdminFixtureSupport`, `ContratoOpenApiSupport` e
`JwtTestSupport` não são classes de teste e não entram nessa contagem.

### Domínio — unitários, sem Spring ou banco

| Classe | T | P |
|---|---:|---:|
| [AssociacaoInstrumentoTest](../../src/test/java/com/curso/instrumento2026/domain/AssociacaoInstrumentoTest.java) | 5 | 0 |
| [CategoriaInstrumentoTest](../../src/test/java/com/curso/instrumento2026/domain/CategoriaInstrumentoTest.java) | 3 | 0 |
| [ClienteTest](../../src/test/java/com/curso/instrumento2026/domain/ClienteTest.java) | 3 | 1 |
| [ColaboradorTest](../../src/test/java/com/curso/instrumento2026/domain/ColaboradorTest.java) | 3 | 2 |
| [ContaAcessoTest](../../src/test/java/com/curso/instrumento2026/domain/ContaAcessoTest.java) | 7 | 2 |
| [CorrecaoAula08Test](../../src/test/java/com/curso/instrumento2026/domain/CorrecaoAula08Test.java) | 8 | 0 |
| [DocumentoFiscalTest](../../src/test/java/com/curso/instrumento2026/domain/DocumentoFiscalTest.java) | 0 | 6 |
| [FornecedorTest](../../src/test/java/com/curso/instrumento2026/domain/FornecedorTest.java) | 1 | 1 |
| [InstrumentoTest](../../src/test/java/com/curso/instrumento2026/domain/InstrumentoTest.java) | 11 | 0 |
| [PessoaTest](../../src/test/java/com/curso/instrumento2026/domain/PessoaTest.java) | 4 | 3 |
| [VendaTest](../../src/test/java/com/curso/instrumento2026/domain/VendaTest.java) | 10 | 2 |

`VendaTest` também exercita `ItemVenda`, inclusive subtotal e validações. A ausência
de uma classe `ItemVendaTest` não demonstra ausência de testes desse objeto.
`ContaAcessoTest` é unitário de domínio, com finalidade adicional de segurança.

### Services unitários — instância direta, sem contexto Spring

| Classe | T | P | Colaboradores e alcance |
|---|---:|---:|---|
| [ContaAcessoServiceUnitTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoServiceUnitTest.java) | 1 | 1 | Mockito; tradução de falhas de integridade simuladas |
| [ContaAcessoUserDetailsServiceTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoUserDetailsServiceTest.java) | 3 | 0 | Repository simulado; carregamento da identidade |
| [InstrumentoExclusaoTest](../../src/test/java/com/curso/instrumento2026/service/InstrumentoExclusaoTest.java) | 2 | 1 | Mockito; tratamento de constraints na exclusão |
| [TokenServiceTest](../../src/test/java/com/curso/instrumento2026/service/TokenServiceTest.java) | 1 | 0 | Encoder/decoder JWT reais em memória e relógio fixo; sem banco |

### Integração com PostgreSQL — persistência e aplicação

Todas as classes desta tabela usam `@SpringBootTest` e `@ActiveProfiles("test")`.

| Classe | T | P | Fronteira principal |
|---|---:|---:|---|
| [PersistenciaJpaTest](../../src/test/java/com/curso/instrumento2026/PersistenciaJpaTest.java) | 2 | 0 | JPA e constraint de estoque por SQL direto |
| [MigracaoAula10Test](../../src/test/java/com/curso/instrumento2026/MigracaoAula10Test.java) | 11 | 2 | ChangeSets, mapeamentos e restrições de pessoas/vendas |
| [ContaAcessoPersistenciaTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoPersistenciaTest.java) | 8 | 2 | Mapeamentos, índices e constraints de contas/perfis |
| [NormalizacaoCategoriaTest](../../src/test/java/com/curso/instrumento2026/NormalizacaoCategoriaTest.java) | 7 | 0 | Normalização e unicidade persistida |
| [PaginacaoCheckpoint7Test](../../src/test/java/com/curso/instrumento2026/PaginacaoCheckpoint7Test.java) | 5 | 0 | Paginação e consultas |
| [PesquisaCheckpoint6Test](../../src/test/java/com/curso/instrumento2026/PesquisaCheckpoint6Test.java) | 2 | 0 | Pesquisa e filtros |
| [RepositoriesPasso6Test](../../src/test/java/com/curso/instrumento2026/RepositoriesPasso6Test.java) | 7 | 0 | Consultas e grafos de relacionamentos |
| [RepositoryCheckpoint4Test](../../src/test/java/com/curso/instrumento2026/RepositoryCheckpoint4Test.java) | 3 | 0 | Consultas de categoria/instrumento |
| [CategoriaInstrumentoServiceTest](../../src/test/java/com/curso/instrumento2026/service/CategoriaInstrumentoServiceTest.java) | 1 | 0 | Service, repository e cadastro persistido |
| [Checkpoint5ServiceTest](../../src/test/java/com/curso/instrumento2026/service/Checkpoint5ServiceTest.java) | 12 | 0 | Atualização, associações, exclusão e estoque |
| [ContaAcessoServiceTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoServiceTest.java) | 13 | 4 | Contas, credenciais e persistência |
| [InstrumentoServiceTest](../../src/test/java/com/curso/instrumento2026/service/InstrumentoServiceTest.java) | 2 | 0 | Cadastro e rejeição de categoria inexistente |
| [PessoasEPapeisServiceTest](../../src/test/java/com/curso/instrumento2026/service/PessoasEPapeisServiceTest.java) | 10 | 0 | Pessoas, clientes e colaboradores |
| [VendaServiceTest](../../src/test/java/com/curso/instrumento2026/service/VendaServiceTest.java) | 12 | 1 | Venda, rollback e finalizações concorrentes |
| [MovimentacaoEstoqueTransacionalTest](../../src/test/java/com/curso/instrumento2026/service/MovimentacaoEstoqueTransacionalTest.java) | 4 | 0 | Commits e movimentações concorrentes |
| [ContaAcessoServiceConcorrenciaTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoServiceConcorrenciaTest.java) | 0 | 1 | Criação concorrente de contas |
| [ContaAcessoAtualizacaoConcorrenteDiagnosticoTest](../../src/test/java/com/curso/instrumento2026/service/ContaAcessoAtualizacaoConcorrenteDiagnosticoTest.java) | 0 | 1 | Atualizações concorrentes de conta |
| [Instrumento2026ApplicationTests](../../src/test/java/com/curso/instrumento2026/Instrumento2026ApplicationTests.java) | 1 | 0 | Inicialização do contexto; não contém asserções de regra comercial |

`InstrumentoServiceTest` e `CategoriaInstrumentoServiceTest` são integração,
embora estejam no mesmo pacote dos services unitários. Um mock de exceção SQL em
`InstrumentoExclusaoTest` não comprova que a constraint existe no PostgreSQL.

O [profile test](../../src/test/resources/application-test.properties) aponta por padrão
para `instrumento2026_test`, com substituição por variáveis de ambiente.
A [configuração principal](../../src/main/resources/application.properties) usa
`ddl-auto=validate` e o [changelog master](../../src/main/resources/db/changelog/db.changelog-master.yaml),
que inclui os arquivos 001–006. Uma execução futura desse contexto pode aplicar
Liquibase; nenhuma foi iniciada neste checkpoint. Disponibilidade, destino efetivo
das variáveis e estado atual do banco não foram verificados.

### API/MockMvc — comportamento HTTP e contrato

| Classe | T | P | Infraestrutura e limite |
|---|---:|---:|---|
| [InstrumentoApiTest](../../src/test/java/com/curso/instrumento2026/InstrumentoApiTest.java) | 5 | 0 | Spring completo, PostgreSQL; filtros desligados |
| [Checkpoint8ApiTest](../../src/test/java/com/curso/instrumento2026/Checkpoint8ApiTest.java) | 10 | 0 | Spring completo, PostgreSQL; filtros desligados |
| [Checkpoint9ApiTest](../../src/test/java/com/curso/instrumento2026/Checkpoint9ApiTest.java) | 10 | 0 | Spring completo, PostgreSQL; filtros desligados |
| [Aula10Passo8ApiTest](../../src/test/java/com/curso/instrumento2026/Aula10Passo8ApiTest.java) | 17 | 1 | Spring completo, PostgreSQL; filtros desligados |
| [FornecedorDocumentoApiTest](../../src/test/java/com/curso/instrumento2026/FornecedorDocumentoApiTest.java) | 3 | 1 | Spring completo, PostgreSQL; filtros desligados |
| [ContaAcessoApiTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoApiTest.java) | 21 | 1 | Spring completo, PostgreSQL e filtros ativos; também segurança |
| [OpenApiDocumentationTest](../../src/test/java/com/curso/instrumento2026/OpenApiDocumentationTest.java) | 18 | 4 | `@WebMvcTest`, Springdoc real e colaboradores simulados; sem banco |

As seis classes com Spring completo usam o profile `test`. Nas cinco classes
históricas indicadas, `@AutoConfigureMockMvc(addFilters = false)` exclui a cadeia
de filtros: suas asserções HTTP não são evidência de autenticação/autorização.
Elas foram preservadas e complementadas pelos testes de segurança existentes.
MockMvc não abre uma porta TCP e não valida implantação ou rede.

### Segurança — autenticação, autorização e bootstrap

| Classe | T | P | Nível e infraestrutura |
|---|---:|---:|---|
| [AutenticacaoJwtTest](../../src/test/java/com/curso/instrumento2026/AutenticacaoJwtTest.java) | 9 | 2 | Componente integrado: Spring completo, MockMvc, filtros e PostgreSQL, profile `test` |
| [ContaAcessoSecurityTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoSecurityTest.java) | 10 | 2 | Componente integrado: Spring completo, MockMvc, filtros e PostgreSQL, profile `test` |
| [ContaAcessoAutenticacaoLimiteDiagnosticoTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoAutenticacaoLimiteDiagnosticoTest.java) | 0 | 1 | Componente integrado: limite de senha no login, MockMvc e PostgreSQL, profile `test` |
| [PoliticaHttpSecurityTest](../../src/test/java/com/curso/instrumento2026/PoliticaHttpSecurityTest.java) | 2 | 10 | `@WebMvcTest`, controllers/filtros reais, services comerciais simulados; sem banco |
| [JwtAuthoritiesSecurityTest](../../src/test/java/com/curso/instrumento2026/JwtAuthoritiesSecurityTest.java) | 5 | 1 | `@WebMvcTest`, conversão de claims e autorização; sem banco |
| [ContaAcessoAdministracaoTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoAdministracaoTest.java) | 7 | 8 | `@WebMvcTest`, administração HTTP com colaboradores simulados; sem banco |
| [AutorizacaoServicesTest](../../src/test/java/com/curso/instrumento2026/service/AutorizacaoServicesTest.java) | 11 | 5 | `@SpringJUnitConfig`, proxies reais, repositories e transaction manager simulados |
| [AdministradorInicialServiceTest](../../src/test/java/com/curso/instrumento2026/service/AdministradorInicialServiceTest.java) | 8 | 3 | `@SpringJUnitConfig`, bootstrap/proxies com persistência e transações simuladas |

Os dois últimos são integração de componentes Spring em contexto reduzido,
sem PostgreSQL. Verificar chamada a `rollback` em um transaction manager simulado
não comprova rollback físico no banco. A categoria segurança também inclui, por
finalidade, `ContaAcessoTest`, `TokenServiceTest`, `ContaAcessoUserDetailsServiceTest`
e testes de configuração já classificados em suas fronteiras principais.

### Apoio: DTOs, validação, configuração e consulta sem JDBC

| Classe | T | P | Fronteira |
|---|---:|---:|---|
| [ContaAcessoDtoTest](../../src/test/java/com/curso/instrumento2026/ContaAcessoDtoTest.java) | 4 | 0 | DTOs em memória, sem contexto Spring |
| [InstrumentoAtualizacaoRequestTest](../../src/test/java/com/curso/instrumento2026/InstrumentoAtualizacaoRequestTest.java) | 1 | 0 | Request/validação sem contexto Spring |
| [DocumentoFiscalValidationTest](../../src/test/java/com/curso/instrumento2026/api/validation/DocumentoFiscalValidationTest.java) | 6 | 0 | Validadores de documentos, sem contexto Spring |
| [AdministradorInicialConfigTest](../../src/test/java/com/curso/instrumento2026/config/AdministradorInicialConfigTest.java) | 6 | 0 | Configuração de bootstrap sem contexto Spring |
| [JwtConfigTest](../../src/test/java/com/curso/instrumento2026/config/JwtConfigTest.java) | 9 | 1 | Configuração JWT sem contexto Spring |
| [ContaAcessoConsultaAdministradoresTest](../../src/test/java/com/curso/instrumento2026/repository/ContaAcessoConsultaAdministradoresTest.java) | 1 | 0 | Metadados Hibernate e compilação semântica JPQL, sem Spring/JDBC |

O último usa Hibernate real e um provedor de conexão simulado. É validação de
integração com o ORM sem banco, não prova de execução SQL ou de resultado da consulta.
Seu custo inclui inicializar metadados/SessionFactory, mesmo sem rede.

## Alcance, evidência e custo por nível

Custos são **qualitativos e relativos**, inferidos da infraestrutura:
baixo para objetos/mocks em memória; intermediário para contexto reduzido, MVC ou
metadados ORM; alto para contexto completo com banco; alto com coordenação adicional
para concorrência. Não foram medidos tempos, consumo de recursos ou desempenho.
Criptografia, cache de contexto e condições do ambiente podem mudar a ordem real.

| Nível e exemplo existente | Unidade observada | Infraestrutura carregada | Evidência | Custo esperado | Defeito que pode detectar | Defeito que não detecta |
|---|---|---|---|---|---|---|
| Domínio: `ContaAcessoTest` | Conta, pessoa e perfis em memória | JUnit e objetos reais | Normalização, rejeição de entradas e transições de estado | Baixo; sem contexto/rede | Conta bloqueada considerada autenticável pela regra local | Filtro aceitando JWT inválido |
| Aplicação unitária: `InstrumentoExclusaoTest` | Tratamento de erro de exclusão no service | JUnit, Mockito e exceções simuladas | Tradução seletiva da constraint de histórico | Baixo; dependências substituídas | Service converter indevidamente qualquer falha de integridade | FK ausente ou SQL incorreto no PostgreSQL |
| Aplicação integrada: `InstrumentoServiceTest` | Cadastro via service/repository | Spring completo, JPA, PostgreSQL e Liquibase, profile `test` | Cadastro persistido e rejeição de categoria inexistente | Alto; contexto e banco | Instrumento cadastrado sem resolver categoria válida | Contrato JSON incorreto no controller |
| Persistência: `PersistenciaJpaTest` | Mapeamento instrumento/categoria e restrição SQL | Spring completo, Hibernate, PostgreSQL/Liquibase | Reler após flush/clear e rejeitar INSERT com estoque negativo | Alto; contexto e SQL real | Constraint de estoque ausente | API retornando status HTTP errado |
| Componente/API: `ContaAcessoApiTest` | Requisição interna até aplicação/banco | MockMvc, Spring completo, filtros, JPA e PostgreSQL | Status, JSON e comportamentos da conta via HTTP | Alto; cadeia ampla com banco | Contrato de cadastro/consulta/troca de senha incorreto | Falha de proxy reverso ou TLS na implantação |
| Contrato: `OpenApiDocumentationTest` | Documento publicado pelo Springdoc | MockMvc e contexto MVC; colaboradores simulados | Rotas, schemas e declarações do contrato | Intermediário; gera contrato sem banco | Rota/schema desaparecer da documentação | Service real descumprir uma regra de negócio |
| Segurança HTTP: `ContaAcessoSecurityTest` | Login, identidade e decisões de acesso | MockMvc, filtros, Spring completo e PostgreSQL | Credenciais, estados da conta/pessoa, acessos 401/403 | Alto; autenticação integrada e banco | Login aceito para pessoa inativa | Segurança da infraestrutura externa ou resistência a carga |
| Segurança por método: `AutorizacaoServicesTest` | Entrada por proxies dos services | Spring reduzido, method security, repositories/transações simulados | Negativa antes do repository e proteção administrativa | Intermediário; proxies sem banco | Método administrativo aceitar OPERADOR | Rollback físico falhar no PostgreSQL |
| Concorrência: `VendaServiceTest` | Finalizações e disputa por estoque | Spring completo, PostgreSQL, transações, threads, latches e timeouts | Efeitos confirmados/revertidos sob cenários simultâneos coordenados | Alto, com coordenação e limpeza adicionais | Duas finalizações consumirem estoque além do disponível | Todas as intercalações possíveis ou capacidade sob carga |

## Estratégia para aproveitar a suíte

Para uma regra em memória, o ponto inicial é o teste de domínio correspondente.
Para decisão de service dependente de um colaborador, um teste isolado localiza
o comportamento assumindo o contrato simulado. SQL, constraints, dirty checking,
commit e locks exigem a camada integrada. Alterações de DTO/controller pedem API
ou contrato; mudanças de política pedem filtros e/ou proxies conforme a fronteira.
Essa orientação descreve a escolha futura dos testes; nenhum foi executado agora.

O par `ContaAcessoServiceUnitTest`/`ContaAcessoServiceTest` já permite discutir
isolamento versus integração sem criar exemplos novos. Para instrumentos,
`InstrumentoExclusaoTest` e `InstrumentoServiceTest` têm alcances complementares,
mas não exercitam exatamente os mesmos cenários. Categorias já possuem integração;
ampliar testes isolados pode ser uma oportunidade posterior, não entrega desta etapa.

Os testes de banco combinam rollback por teste, flush/clear e, nos cenários que
precisam de commits reais, transações próprias e limpeza explícita. Uma transação
do método de teste não abrange automaticamente outras threads. Os testes concorrentes
existentes coordenam threads e limitam esperas; isso não equivale a teste de desempenho.

Não duplicar `ContaAcessoTest` ou criar classes somente para espelhar o suporteos2026.
O tema local usa `Instrumento`, `CategoriaInstrumento` e estoque inteiro. A identificação
de futuras lacunas deve considerar risco e asserções, sem inferir qualidade só pela contagem.

## Evidências disponíveis e limites desta etapa

Os relatórios locais `target/surefire-reports/TEST-*.xml` conferidos na auditoria
anterior registram 671 execuções em 54 classes, com zero falhas, erros e ignorados,
em 05/11/2026. A [documentação da Aula 14](../aula14/README.md) também registra esse
resultado. São evidências históricas: não foram produzidas nem reexecutadas na Aula 15.
Os relatórios em `target` são locais e não são entregáveis deste checkpoint.

Os 395 métodos atuais não equivalem a 395 execuções, pois os 70 métodos parametrizados
geram múltiplos casos. Não se transfere para este projeto o total de testes da referência.
Não há nova evidência de suíte verde, duração ou cobertura. JaCoCo, medição e portão
pertencem a etapas posteriores e não foram configurados.

## Validação documental

Foram conferidos os links locais dos dois documentos, a correspondência das 54
classes com os fontes, os totais T/P e o escopo das alterações. A checagem de
whitespace com `git diff --no-index --check` foi aplicada a cada arquivo novo.
Essa conferência não compila nem executa o projeto.

**Encerramento no Checkpoint 1. O Checkpoint 2 depende de autorização do usuário.**
