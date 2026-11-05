# Aula 14 — Arquitetura e auditoria estática

> Atualização 7K, 05/11/2026: os achados do 7A são históricos. A1–A3 foram resolvidos no 7B;
> a validação final teve 671 testes JUnit e 156 chamadas/487 assertions Newman aprovados.
> Os riscos conhecidos abaixo permanecem, conforme o alcance final em EVIDENCIAS.

## Arquitetura observada

O login usa `AutenticacaoService`, `AuthenticationManager`, `ContaAcessoUserDetailsService` e
`PasswordEncoderConfig`. Conta ATIVA e Pessoa com status `ATIVO` são exigidas para novo login.
BCrypt, limite de 72 bytes UTF-8 e ausência de normalização da senha permanecem preservados.
`TokenService` emite JWT HS256 com perfis provenientes da autenticação. `SecurityConfig` converte
`perfis` para `ROLE_*`, mantém `sub` e aplica RBAC stateless sem filtro JWT manual.
O banco não é consultado novamente para atualizar roles/status a cada Bearer.

A [matriz HTTP](README.md#matriz-http-implementada) distingue autorização de sucesso de negócio.
Seis métodos administrativos têm proteção adicional por proxy Spring. A autoinvocação de `buscarPorId`
não é usada como barreira: o método administrativo externo já é protegido.

## Conformidade com os requisitos

| Requisito | Implementação/evidência | Situação |
|---|---|---|
| Dois perfis e authorities | SecurityConfig; JwtAuthoritiesSecurityTest | Atendido em testes isolados |
| Identidade pelo subject | Conversor de nome padrão; teste do sub | Atendido em testes isolados |
| Cadastros não públicos | Política HTTP; PoliticaHttpSecurityTest | Atendido; testes históricos adaptados e aprovados |
| Comercial para ambos | Regra geral /api/** e matriz | Atendido em testes isolados |
| Administração e DELETE restritos | Regras específicas antes da geral | Atendido em testes isolados |
| Defesa em profundidade | Seis métodos anotados; AutorizacaoServicesTest | Atendido por proxy testado |
| Gestão de perfis/status | Controller, DTOs e ContaAcessoService | Testes isolados e persistência por HTTP/Newman confirmados |
| Autoproteção/último administrador | Service e contarAdministradoresAtivos | Atendido nas operações de conta, com limites abaixo |
| Bootstrap desligado/idempotente | Properties, config, runner e service | Criação real confirmada; idempotência testada isoladamente |
| Sem novo changelog | Estrutura da Aula 12 reutilizada | Atendido; históricos preservados |
| OpenAPI coerente | Customizador e 60 testes OpenAPI | Atendido em MVC isolado |
| Postman adaptado | Coleção, validador e runner | Atendido: 156 chamadas/487 assertions, zero falhas |
| Matriz, diagramas e justificativa | README e ATIVIDADE-ORIENTADA | Atendido documentalmente |
| Suíte completa e evidências finais | Relatórios Surefire, Maven e Newman | 671 aprovados, BUILD SUCCESS; evidências finais registradas |
| Marco Git | Nenhum versionamento nesta etapa | Pendente; não autorizado |

## Invariantes e alcance

Administrador utilizável significa ADMINISTRADOR, conta `ATIVA` e Pessoa `ATIVO`, conforme a query do
repository. Alterar perfis impede autorremoção administrativa; alterar status impede autobloqueio.
Para outra conta utilizável, remover ADMINISTRADOR ou bloquear é negado quando a contagem é <= 1.
Essas falhas de negócio retornam 400, não 403. Perfis vazios/null/desconhecidos são inválidos.
Os dois endpoints administrativos retornam 200 com `ContaAcessoResponse`, sem hash.

Isso não constitui garantia global de existência de administrador:

- A contagem e a alteração não são serializadas. Duas transações podem observar a mesma contagem:
  limitação concorrente declarada no material, sem reprodução nova neste checkpoint.
- `PessoaService.alterarStatus` não consulta contas nem contagem administrativa. A política comercial
  permite a ambos os perfis alterar status de Pessoa. **Não há barreira para inativar a Pessoa do único
  administrador**, inclusive por OPERADOR. É conclusão da leitura, não experimento no PostgreSQL.
  Novo login seria impedido; JWTs anteriores manteriam validade.
- `@DynamicUpdate` preserva atualização seletiva, mas não resolve a corrida da contagem ou duas
  alterações simultâneas do mesmo atributo.
- Token anterior ao bloqueio/rebaixamento conserva claims até expirar. Não há revogação nesta aula.

Coordenar status de Pessoa/conta e serializar decisões exige decisão explícita de escopo; nada foi alterado.

## Administrador inicial

`application.properties` usa `${INITIAL_ADMIN_ENABLED:false}`. `.env.example` tem false e campos vazios.
O runner condicional chama outro bean transacional. Validação, normalização do login e BCrypt precedem
as escritas. O construtor de conta concede OPERADOR e o serviço acrescenta ADMINISTRADOR.
CPF/e-mail/login duplicados são recusados, sem promoção automática. Com administrador utilizável
existente, retorna vazio. O bootstrap não depende de autenticação nem expõe endpoint.

`AdministradorInicialProperties.toString()` protege os dados; logs do runner são mensagens fixas.
Exceções de validação não incluem valores de credenciais. Falhas de integridade escapam como erro
genérico para reverter a transação. Testes com transaction manager simulado verificam rollback
solicitado; **não comprovam rollback físico em PostgreSQL**. Campos vazios são aceitos quando desligado.
Idempotência é adequada a uma instância; não há coordenação distribuída de inicialização.

## Achados e ações recomendadas

A tabela preserva os achados do 7A. A1–A3 foram resolvidos posteriormente; as demais limitações
continuam classificadas conforme o alcance descrito na atualização final.

| ID / classificação | Arquivo e trecho | Consequência e recomendação |
|---|---|---|
| A1 — pendência obrigatória de testes | ContaAcessoApiTest: cadastroPublicoDeveRetornar201ComOperadorAtivoESomenteCamposDeResposta e outros cadastros sem Bearer | Esperam 201/400/404/409, mas filtro negará com 401. Adaptar testes de negócio para ADMINISTRADOR e preservar asserts; manter negativas separadas. Renomear referências a cadastro público. |
| A2 — pendência obrigatória de testes | ContaAcessoSecurityTest: bootstrapPessoaDeveSerPublicoSemCsrfComDemaisEscritasProtegidas e cadastroPublicoDeveValidarCamposSemLiberarOutrosMetodos | Expectativas anônimas 201/400 obsoletas. Verificar 401 sem identidade e manter validação do DTO com administrador autenticado. |
| A3 — pendência obrigatória de testes | Aula10Passo8ApiTest, Checkpoint8ApiTest, Checkpoint9ApiTest: addFilters=false, DELETEs, sem contexto administrativo | Desligar filtros não desliga PreAuthorize. Fornecer identidade de teste às operações administrativas sem desativar segurança por método. Não modificar InstrumentoApiTest/FornecedorDocumentoApiTest só por serem antigos; avaliar métodos realmente chamados. |
| A4 — alto, limitação identificada por leitura | PessoaService.alterarStatus, regra geral HTTP, query administrativa | Inativação da Pessoa pode eliminar o último administrador utilizável. Decidir tratamento e autorizar regressão isolada antes de modificar política. Não afirmar proteção global. |
| A5 — médio, limitação conhecida | Contagem sem locking | Decisão não atômica sob concorrência; coordenação futura já declarada no material. |
| A6 — médio, risco operacional | EXECUTAR-NEWMAN.cjs: conexões API e SQL independentes | Bancos distintos podem ter IDs coincidentes, atingindo dados antigos na limpeza. Confirmar identidade do banco da API e da limpeza; preferir ambiente descartável exclusivo. |
| A7 — baixo, recuperação operacional | IDs somente em memória e finally | Encerramento abrupto pode impedir limpeza e perder inventário. Prever snapshot/recuperação ou descarte do banco dedicado. |
| A8 — comportamento esperado | JWT autocontido sem revogação | Token antigo continua autorizado até exp. Não prometer bloqueio imediato de Bearer. |

A1–A3 são registros históricos do 7A, resolvidos no 7B e cobertos pela suíte final de 671 testes.
Não representam abertura de cadastros em produção. A4 não foi reproduzido em execução nesta etapa.
Nenhum código de produção, teste ou script de execução foi corrigido aqui.

## Limpeza Newman

O runner captura IDs positivos apenas de POST/201 de recursos conhecidos. Login administrativo não
gera ID de limpeza. Não há TRUNCATE, seleção por prefixo ou cadastro do administrador externo pelo
runner. Remove itens das vendas criadas, vendas, contas, vínculos e demais cadastros na ordem de FKs.
A FK dos perfis usa CASCADE; Pessoa é removida após a conta. Consulta os IDs ao final para confirmar ausência.

As alterações comerciais/status da coleção normal usam fixtures da execução. Chamadas de autoproteção
ao administrador externo esperam 400; resposta diferente exige inspeção. A pasta 11 é excluída do runner
comum. No 7I.4, a limpeza real confirmou zero fixtures remanescentes. A evidência não elimina os riscos
A6/A7 em outras execuções; ver o [procedimento final](VALIDACAO-FINAL.md).

## Riscos preservados e fora do escopo

HTTPS é necessário fora do laboratório. CSRF está desabilitado no Bearer stateless; reavaliar se houver
credencial enviada automaticamente por navegador. Não há refresh token, logout, revogação, limitação
de tentativas ou auditoria persistente. Cadastro de conta agora depende de administrador, fechando o
bootstrap público da Aula 12; não implementa comprovação independente de titularidade ou governança
contra administrador malicioso. OpenAPI/Swagger continuam desligados em prod.
Esta revisão não é teste de intrusão nem garantia absoluta de ausência de vulnerabilidades.

## Alcance final e limitações preservadas — 7J/7K

JUnit: 671 aprovados, zero falhas, erros e ignorados, BUILD SUCCESS e saída 0. Newman: 156 chamadas,
487 assertions, zero falhas e saída 0, pastas 01–10. A pasta 11 não foi executada no Newman comum;
a regra da contagem foi testada em contextos isolados. Validação estática: 155 requisições,
163 scripts e 126 cenários históricos preservados. Totais sobrepostos não são somados.

A6 foi mitigado nesta execução por cluster isolado comprovado, URLs explícitas, `currentSchema=public`
e correlação TCP/pg_stat_activity. O runner não comprova sozinho a identidade do cluster e mantém
fallback SQL para 5432; configuração explícita e verificação externa continuam obrigatórias.
Seu inventário de limpeza permanece em memória, sujeito a perda em encerramento abrupto.

A4 é limitação alta confirmada por leitura, sem novo experimento: a inativação indireta da Pessoa
pode retirar o último administrador utilizável. A5 é o risco médio conhecido de contagem concorrente
sem serialização. JWTs antigos mantêm authorities até expirar; não há revogação nesta aula.
Não se afirma proteção administrativa global nem ausência de corridas apenas porque os testes passaram.

A criação real do administrador foi observada, mas testes simulados não comprovam rollback físico
nem idempotência por reinicialização real. A limpeza confirmou zero fixtures comerciais; as duas
contas e duas Pessoas administrativas de preparação foram preservadas fora da coleção.
O cluster `7704384549022663752`, porta 55414, recebeu 41 changeSets, com lock liberado; o original
na porta 5432 foi preservado. Resultados completos em [EVIDENCIAS.md](EVIDENCIAS.md).
