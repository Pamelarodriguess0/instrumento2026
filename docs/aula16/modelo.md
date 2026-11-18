# Aula 16 — Ponto A: modelo e proposta de migração

> Registro histórico da preparação em 16/11/2026. O Checkpoint 1 foi validado
> posteriormente, com nove testes aprovados e 44 changeSets, conforme o [índice atualizado](README.md).

Data: 16/11/2026. Escopo: seções 9–10 e ponto A da seção 23.
Referência: [material oficial](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/16aula/16aula.md).
O esqueleto do material foi conferido também com a
[entidade da referência](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/src/main/java/com/curso/suporteos/domain/SessaoRefreshToken.java)
e o [changelog 007 da referência](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/src/main/resources/db/changelog/changes/007-sessoes-refresh-token.yaml).
Não houve cópia de services ou alteração de comportamento de autenticação.

## Modelo implementado

Nova [SessaoRefreshToken](../../src/main/java/com/curso/instrumento2026/domain/SessaoRefreshToken.java),
no pacote de domínio local, registra uma geração de refresh token:

| Campo Java / coluna | Tipo e regra de mapeamento |
|---|---|
| `id` / `id` | Long, BIGINT identity, chave primária |
| `conta` / `conta_acesso_id` | ManyToOne LAZY obrigatório para ContaAcesso; FK nomeada |
| `familiaId` / `familia_id` | String obrigatória, VARCHAR(36) |
| `tokenHash` / `token_hash` | String obrigatória, VARCHAR(64), unicidade nomeada |
| `criadaEm` / `criada_em` | Instant obrigatório |
| `ultimaAtividadeEm` / `ultima_atividade_em` | Instant obrigatório |
| `expiraEm` / `expira_em` | Instant obrigatório, posterior à criação |
| `utilizadaEm` / `utilizada_em` | Instant opcional |
| `revogadaEm` / `revogada_em` | Instant opcional |
| `motivoRevogacao` / `motivo_revogacao` | Enum STRING opcional, VARCHAR(30) |

O [enum](../../src/main/java/com/curso/instrumento2026/domain/MotivoRevogacaoRefreshToken.java)
contém LOGOUT, LOGOUT_GLOBAL, INATIVIDADE, EXPIRACAO_ABSOLUTA, REUTILIZACAO e CONTA_INATIVA.
Os cinco instantes usam `TIMESTAMP WITH TIME ZONE` na migration PostgreSQL.
Não há campo para armazenar a credencial original, serialização HTTP ou associação
em cascata JPA que persista/exclua uma ContaAcesso ao operar uma sessão.

Regras locais da entidade, coerentes com o código de referência:

- Construtor exige conta, família/hash não brancos e instantes obrigatórios;
  textos são aparados e expiração deve ser estritamente posterior à criação.
- `foiUtilizada` e `foiRevogada` observam os instantes persistidos, sem apagar registros.
- Uso é marcado uma única vez; uma segunda marcação é rejeitada e preserva a primeira.
- Revogação preserva o primeiro motivo/instante quando chamada novamente.
- Consultas temporais expiram exatamente no limite absoluto ou de inatividade;
  recebem Instant/Duration, sem ler relógio global.

Essas são operações de estado da própria entidade, não o caso de uso de rotação
ou revogação de família. Gerar hash/credencial, carregar conta, definir precedência
entre motivos e coordenar transações/locks pertencem às próximas etapas.

## Migration proposta, ainda não aplicada

Novo arquivo [007-sessoes-refresh-token.yaml](../../src/main/resources/db/changelog/changes/007-sessoes-refresh-token.yaml),
incluído ao final do [master](../../src/main/resources/db/changelog/db.changelog-master.yaml).
As migrations 001–006 não foram alteradas.

| ChangeSet | Responsabilidade |
|---|---|
| `007-01-create-sessao-refresh-token` | Tabela, PK, colunas obrigatórias, hash único e FK com ON DELETE CASCADE |
| `007-02-index-sessao-refresh-token` | Índices por família e por conta |
| `007-03-checks-sessao-refresh-token` | Expiração posterior à criação e valores válidos do motivo |

Nomes principais:

- PK `pk_sessao_refresh_token`.
- Unique `uk_sessao_refresh_token_hash`.
- FK `fk_sessao_refresh_token_conta`, referenciando `conta_acesso(id)`.
- Índices `idx_sessao_refresh_token_familia` e `idx_sessao_refresh_token_conta`.
- Checks `ck_sessao_refresh_token_expiracao` e `ck_sessao_refresh_token_motivo`.

O banco permite motivo nulo enquanto a geração não tiver revogação. Cada changeSet
tem rollback correspondente; nenhum rollback foi executado. Não se cria campo de
estado redundante, tabela de compras ou alteração nas tabelas comerciais.

## Adaptações e diferenças

1. Pacote `com.curso.instrumento2026.domain` e ContaAcesso locais. Colunas/FK explícitas
   evitam depender da inferência de nomes do trecho reduzido da aula.
2. Os fontes locais têm 41 changeSets; o arquivo 007 acrescenta três: **total previsto 44**.
   Os 36 citados na referência não são o total deste projeto e não foram copiados para o teste.
3. `ddl-auto=validate` e a configuração Liquibase existente foram preservados.
   Compatibilidade de tipos é proposta pelos mapeamentos; validação Hibernate real ainda pendente.
4. Unique nomeada por `@Table` segue o padrão local, alinhada à migration.
5. Em `revogar`, ambos os argumentos são validados antes de atribuir os campos.
   Isso evita estado parcialmente revogado se o motivo for nulo; os motivos e a
   preservação da primeira revogação seguem a referência.
6. Não foram acrescentadas políticas de tamanho/formato de credencial no domínio,
   ordenação adicional dos instantes ou precedência de motivos além do material.
   VARCHAR(36/64) limita armazenamento, mas não prova geração criptográfica ou formato do hash.

## Testes e resultado offline

[SessaoRefreshTokenTest](../../src/test/java/com/curso/instrumento2026/domain/SessaoRefreshTokenTest.java):
criação e associação, campos obrigatórios, expiração inválida, fronteiras temporais,
uso único, primeira revogação e rejeição sem estado parcial. Dados sintéticos e
instantes fixos. Não inicia Spring, Hibernate, Liquibase ou conexão JDBC.

```powershell
.\mvnw.cmd -B "-Dtest=com.curso.instrumento2026.domain.SessaoRefreshTokenTest" "-Djacoco.destFile=target/aula16-pontoA-unitarios.exec" "-Djacoco.append=false" test > target/aula16-pontoA-unitarios.log 2>&1
```

Conclusão: **16/11/2026 às 14:58:23 -03:00, código 0, BUILD SUCCESS**.
**9 casos aprovados, zero falhas, erros e ignorados.** São sete métodos `@Test`
e um parametrizado com dois casos. Não confundir esse resultado com suíte completa
ou aplicação da migration. Maven compilou também os testes de integração, sem executá-los.
Não foi executado `verify`, nem houve nova medição/validação global do portão JaCoCo.

Preparado, compilado e **não executado**:
[SessaoRefreshTokenPersistenciaTest](../../src/test/java/com/curso/instrumento2026/SessaoRefreshTokenPersistenciaTest.java).
Usa `@SpringBootTest`, profile `test`, transação por caso, EntityManager e JdbcTemplate.
Seus nove casos preparam evidência para:

- persistir/reler campos e associação LAZY, incluindo uso e revogação;
- 41 changeSets anteriores mais três novos, sem alteração das contagens históricas;
- hash duplicado, expiração inválida, motivo desconhecido e FK inexistente rejeitados;
- exclusão da conta removendo sessões sem remover a Pessoa;
- índices e conjunto de colunas, sem coluna da credencial original.

## Pendências do Checkpoint 1

Não foi acessado PostgreSQL, executado comando Liquibase ou iniciado contexto Spring.
O YAML foi revisado textualmente; compilação Java não valida/aplica o changelog.
Para concluir o Checkpoint 1, após revisão/autorização:

1. Confirmar credenciais/destino efetivo `instrumento2026_test` com o procedimento
   seguro da Aula 15; profile sozinho não impede override incorreto.
2. Aplicar 007 somente no destino autorizado e validar `ddl-auto=validate`.
3. Executar `PersistenciaJpaTest` junto do novo teste de persistência, verificando
   tipos, FKs, índices, constraints e os 44 changeSets efetivamente registrados.
4. Registrar resultados reais e eventuais diferenças, sem presumir aprovação.

Os logs/coleta unitários em `target` são ignorados pelo Git. Não foram implementados
repository de refresh, propriedades, emissão, rotação, endpoints ou logout. Login,
JWT, SecurityConfig e POM permanecem iguais ao marco da Aula 15. Sem branch nova,
commit ou push. **Ponto A preparado para revisão; Checkpoint 1 de banco pendente.**
