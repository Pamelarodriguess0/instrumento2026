# Ponto C — serviço, rotação e validação transacional

**Estado atual (17/11/2026): validação transacional autorizada concluída, com 12 casos integrados aprovados.** A incompatibilidade inicial de sid e sua correção estão preservadas como histórico. Os resultados atuais e os limites da evidência estão na seção final. Endpoints e revisão das demais integrações continuam fora desta etapa.

Referência: [Aula 16, seções 6 a 8 e 12 a 14](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/16aula/16aula.md).

## Escopo implementado, ainda não aprovado

- `SessaoAutenticacaoService`: geração com SecureRandom (32 bytes), Base64 URL sem padding, persistência somente do SHA-256 hexadecimal, família por autenticação, rotação e preservação do limite absoluto.
- Consulta bloqueante existente; revalidação de conta/pessoa e reconstrução de autoridades com os perfis carregados na transação.
- Rejeição de ausentes, desconhecidos, revogados, utilizados e expirados. Reutilização, expiração e conta inativa revogam a família antes da exceção genérica.
- `TokensAutenticacao`: transporte interno do par, com toString protegido.
- `RefreshTokenInvalidoException`: mensagem genérica sem token ou hash.
- `TokenService`: sobrecarga para o claim sid; preserva jti, emissor, perfis e cálculo do prazo do access token. A incompatibilidade inicial foi corrigida na execução isolada registrada ao final deste documento.
- `AutenticacaoService`: inicia uma sessão após a autenticação. Retorna ainda somente o access token ao controller existente. A entrega HTTP do par e a adaptação final desse contrato estão pendentes da etapa de endpoints. Neste estado intermediário, o refresh gerado pelo login não é entregue ao cliente.

Nenhum controller, configuração de segurança, migration ou contrato OpenAPI foi alterado nesta etapa.

## Histórico: execução isolada e interrupção inicial

Executado em 17/11/2026, encerramento 15:41:40 -03:00:

```powershell
.\mvnw.cmd -B '-Dtest=SessaoAutenticacaoServiceTest,TokenServiceTest' '-Djacoco.destFile=target/aula16-pontoC-unitarios.exec' '-Djacoco.append=false' test > target/aula16-pontoC-unitarios.log 2>&1
```

Código de saída: **1**, `BUILD FAILURE`.

| Classe | Executados | Aprovados | Falhas de asserção | Erros | Ignorados |
|---|---:|---:|---:|---:|---:|
| SessaoAutenticacaoServiceTest | 8 | 8 | 0 | 0 | 0 |
| TokenServiceTest | 1 | 0 | 0 | 1 | 0 |
| Total | 9 | 8 | 0 | 1 | 0 |

Erro relevante, sem credenciais:

```text
java.lang.IllegalArgumentException: value cannot be null
at org.springframework.security.oauth2.jwt.JwtClaimsSet$Builder.claim(JwtClaimsSet.java:167)
at com.curso.instrumento2026.service.TokenService.emitir(TokenService.java:40)
at com.curso.instrumento2026.service.TokenService.emitir(TokenService.java:29)
```

A assinatura preexistente `emitir(Authentication)` delega à nova sobrecarga com identificador nulo, que foi passado diretamente a `claim`. O builder não aceita esse valor. A compilação passou; a falha é de execução unitária, sem Spring e sem banco. A correção precisa tratar explicitamente a compatibilidade dessa assinatura e acrescentar evidência de assinatura/decodificação com sid. Não foi aplicada após a falha, respeitando a interrupção solicitada.

Os oito casos aprovados verificam formato/aleatoriedade/hash e proteção do toString; rotação com família, prazo e autoridades; ausência/desconhecimento; quatro motivos de rejeição (incluindo os instantes exatos de expiração); e preservação de sessão já revogada. São mocks: não comprovam transações, SQL nem assinatura JWT nesse caminho.

## Histórico: pendências identificadas antes da validação transacional

`renovar` usa noRollbackFor somente para RefreshTokenInvalidoException. As rejeições previstas ocorrem antes de marcar utilização ou emitir sucessor. Após uma revogação em massa, a execução lança imediatamente a exceção; não reutiliza as entidades desanexadas pelo repository. Outras falhas de execução mantêm o rollback padrão. Essa análise de código **não substitui a prova integrada**.

Após a correção isolada registrada abaixo, foram identificadas as seguintes pendências (a situação atual está detalhada na seção de validação transacional):

1. Confirmar explicitamente DataSource e Liquibase em instrumento2026_test; nenhuma conexão foi feita nesta etapa.
2. Testar a rotação real e o armazenamento, revalidação de conta/pessoa e perfis entre transações.
3. Comprovar em nova transação que o replay preserva a revogação após a exceção.
4. Comprovar rollback de utilização/sucessor diante de falha inesperada de assinatura ou persistência.
5. Adaptar os testes isolados de segurança que importam AutenticacaoService, considerando sua nova dependência. Não foram executados nem declarados compatíveis.
6. Validar a integração completa da sessão com o JWT; a emissão isolada com assinatura e decodificação reais já foi comprovada na correção abaixo.

O lock pessimista foi mantido; não houve teste concorrente nesta etapa. Não se afirma garantia de serialização da família em todas as corridas possíveis. Não há logout, renovação HTTP, denylist nem consulta de sessão por requisição de access token.

**Situação naquela interrupção: Ponto C incompleto; revisão e retomada necessárias antes dos endpoints.** A suíte completa não foi executada. Não houve commit ou push. Naquele momento, os 44 changeSets eram o resultado histórico da etapa anterior, não uma verificação nova.

## Correção autorizada da compatibilidade de sid

Em 17/11/2026, a emissão foi centralizada em um método privado. A assinatura antiga `emitir(Authentication)` usa esse caminho sem acrescentar sid. A sobrecarga pública `emitir(Authentication, String)` rejeita identificador nulo, vazio ou composto somente de espaços antes de invocar o encoder; para identificador válido, acrescenta exatamente o valor recebido. Não foi imposta nova exigência de formato UUID ao contrato do TokenService.

O teste existente foi parametrizado para os dois modos de emissão, evitando duplicação. Ambos assinam e decodificam JWT com HS256 e validam emissor, subject, issuedAt com truncamento a segundos, expiresAt, duração de 1800 segundos, jti UUID distinto entre emissões, perfis filtrados/ordenados e ausência de credenciais. O modo antigo comprova ausência da chave sid; o novo comprova presença e valor. Quatro casos adicionais rejeitam sid nulo, vazio, espaço e tabulação, sem chamar o encoder.

Comando executado, sem Spring ou PostgreSQL:

```powershell
.\mvnw.cmd -B '-Dtest=TokenServiceTest,SessaoAutenticacaoServiceTest' '-Djacoco.destFile=target/aula16-pontoC-correcao-sid.exec' '-Djacoco.append=false' test > target/aula16-pontoC-correcao-sid.log 2>&1
```

Encerramento: **17/11/2026 15:46:03 -03:00**. Código de saída **0**, `BUILD SUCCESS`.

| Classe | Executados | Aprovados | Falhas | Erros | Ignorados |
|---|---:|---:|---:|---:|---:|
| TokenServiceTest | 6 | 6 | 0 | 0 | 0 |
| SessaoAutenticacaoServiceTest | 8 | 8 | 0 | 0 | 0 |
| Total | 14 | 14 | 0 | 0 | 0 |

Saída relevante:

```text
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Somente TokenService.java, TokenServiceTest.java e este documento foram alterados nesta correção. O log e a coleta da execução anterior foram preservados; os novos artefatos ficam em target e não são candidatos ao versionamento. Os avisos de instrumentação dinâmica do Mockito/Byte Buddy não causaram falhas.

**A incompatibilidade de sid foi resolvida e validada isoladamente.** Aquela execução não validou transações, revogação persistida, concorrência, integração HTTP ou a suíte completa. Nenhum acesso ao banco, migration, configuração, endpoint, commit ou push foi realizado naquela correção. A autorização transacional foi concedida posteriormente, para a etapa abaixo.

## Validação transacional com PostgreSQL

### Segurança do destino e preflight

O material foi relido nas regras de emissão, hash, família, rotação, locking e noRollbackFor (seções 6 a 8 e 12 a 14). Foram inspecionados o serviço, o repository, o login interno, os testes existentes, o master e a migration 007.

Antes dos testes, uma conexão explicitamente direcionada ao banco de teste executou somente consultas em `BEGIN READ ONLY`. Resultado:

| Item | Valor confirmado |
|---|---|
| Profile da execução | test |
| DataSource e Liquibase | jdbc:postgresql://127.0.0.1:5432/instrumento2026_test |
| Servidor efetivo, via PostgreSQL | 127.0.0.1:5432 |
| Banco e schema efetivos | instrumento2026_test / public |
| ChangeSets registrados antes/depois | 44 / 44 |
| ChangeSets aplicados nestas execuções | 0 |
| Lock do Liquibase antes/depois | false / false |
| Sessões, contas e pessoas antes/depois | 0, 0, 0 / 0, 0, 0 |

Os arquivos locais contêm 44 changeSets e apenas as sete migrations esperadas. A 007 continua com seus três changeSets, SHA-256 do arquivo `6DA69A5EA246220E8509E826DC319FDCC7A28EB50EB2353A29A597D72505F55B`. Nenhuma migration foi alterada. O Liquibase verificou os changelogs e informou:

```text
Database is up to date, no changesets to execute
Run:                          0
Previously run:              44
Total change sets:           44
```

O Hibernate inicializou com `ddl-auto=validate`, preservado na configuração existente. O log confirmou o profile test e a URL JDBC acima.

Foi verificada a ausência de overrides de inicialização Maven/Java, JSON de propriedades Spring, localizações externas e caminhos alternativos de DataSource. A importação de `.env` pelo Spring foi neutralizada com `spring.config.import=optional:file:./target/aula16-no-external-config.properties`, cuja ausência foi conferida. Somente as credenciais de teste foram lidas privadamente pelo processo para alimentar `DB_TEST_USERNAME`, `DB_TEST_PASSWORD`, `SPRING_LIQUIBASE_USER` e `SPRING_LIQUIBASE_PASSWORD`; os valores anteriores do ambiente foram restaurados em finally. Nenhuma credencial foi passada em argumento Maven ou registrada nesta documentação. O administrador inicial foi desabilitado explicitamente; SQL e bind logging foram desabilitados para esta execução. Não houve conexão ao banco de desenvolvimento.

### Estrutura dos testes e transações reais

Criado `src/test/java/com/curso/instrumento2026/service/SessaoAutenticacaoTransacionalTest.java`, com Spring Boot e PostgreSQL reais. Não há `@Transactional` na classe nem nos métodos de teste. Cada chamada ao serviço passa pelo proxy transacional; a preparação, limpeza e as consultas posteriores usam `TransactionTemplate` com `REQUIRES_NEW` e timeout de 20 segundos. O teste verifica que não há transação externa ativa, além dos valores explícitos de profile, URLs e schemas.

Repository, EntityManager, banco, autenticação por senha e assinatura JWT são reais. Apenas Clock é substituído para controlar o tempo; TokenService é um spy que normalmente chama a implementação real, permitindo injetar as falhas e coordenar a concorrência nos casos específicos. O banco nunca foi substituído por mocks. Nenhum token ou hash foi impresso nas evidências; as asserções que comparam segredos usam resultados booleanos para evitar exposição em diagnósticos.

O noRollbackFor de renovar continua restrito a `RefreshTokenInvalidoException` (classe final), não a RuntimeException ou Exception em geral. As rejeições previstas precedem a criação do sucessor. O comportamento dessa distinção foi comprovado pelos testes de replay e pelos dois testes de rollback a seguir.

| Caso | Evidência observada após a chamada |
|---|---|
| Login real | AutenticacaoService autentica a senha sintética e confirma a primeira sessão; nova transação encontra uma geração cujo familiaId corresponde ao sid do JWT assinado |
| Hash e rotação | SHA-256 calculado independentemente corresponde ao valor persistido; JSON da linha não contém o refresh original; R1 é usado e R2 nasce na mesma família com o mesmo limite absoluto |
| Replay R1 depois de R2 | A exceção escapa do proxy; nova transação encontra as duas gerações revogadas por REUTILIZACAO; R2 também é rejeitado |
| Falha de assinatura | O spy força flush real de utilizada_em e lança JwtEncodingException; nova transação encontra somente R1, ainda não utilizado e não revogado; após retirar a falha, R1 pode renovar |
| Falha antes do commit | Um callback transacional força flush, observa duas gerações e o uso de R1, então lança IllegalStateException; nova transação encontra somente R1, sem uso nem revogação |
| Inatividade no limite exato | Com Clock em criação + 1800 segundos, rejeição e motivo INATIVIDADE persistido, sem sucessor |
| Prazo absoluto no limite exato | Com Clock em criação + 28800 segundos, rejeição e motivo EXPIRACAO_ABSOLUTA persistido, sem sucessor |
| Sessão já revogada | Revogação preparada diretamente no domínio, sem implementar logout; renovação é rejeitada e o primeiro motivo LOGOUT é preservado |
| Conta bloqueada / pessoa inativa | Duas variações alteram a fixture em transação confirmada depois da emissão; a renovação relê o estado, rejeita e persiste CONTA_INATIVA |
| Perfis atualizados | Perfil OPERADOR é substituído por ADMINISTRADOR em nova transação; o JWT renovado é assinado/decodificado e contém somente o perfil atual, com o mesmo sid |
| Concorrência controlada | Duas chamadas ao proxy, em threads e transações independentes: primeira retém o lock, segunda é observada aguardando lock no PostgreSQL; um sucesso, uma rejeição e exatamente um sucessor; ao final, todas as gerações estão revogadas por replay |

São **12 casos distintos**: os motivos de rejeição geram três casos e a revalidação de conta/pessoa gera dois. As fixtures são removidas por seus próprios IDs em transação confirmada; a FK CASCADE remove suas sessões e o teste remove a pessoa vinculada. Não se executa limpeza global.

### Concorrência: alcance da prova

Na versão final do teste, a primeira renovação é pausada na emissão do JWT, depois da consulta bloqueante, mantendo a transação aberta. A segunda chamada é iniciada nesse intervalo. Consultas a `pg_stat_activity` verificam `wait_event_type='Lock'`, consulta à tabela de sessões e `pg_blocking_pids(pid)` não vazio. A primeira só é liberada após observar esse bloqueio. As esperas usam latches e prazos finitos; não há Thread.sleep.

Após os commits, uma nova transação comprova duas linhas no total (R1 e um único sucessor), ambas revogadas por REUTILIZACAO. Isso confirma o comportamento previsto para **duas renovações do mesmo token**, no PostgreSQL READ COMMITTED usado nesta execução. Não é prova de todas as intercalações possíveis, de carga elevada, de renovações sobre gerações distintas da família ou de corridas com logout/global; essas garantias não são afirmadas.

### Comandos e resultados

Com as credenciais de teste no ambiente privado e restauradas ao final, a execução final foi:

```powershell
.\mvnw.cmd -B `
  '-Dtest=com.curso.instrumento2026.service.SessaoAutenticacaoTransacionalTest' `
  '-Dsurefire.skipAfterFailureCount=1' `
  '-Dspring.profiles.active=test' `
  '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.liquibase.url=jdbc:postgresql://127.0.0.1:5432/instrumento2026_test' `
  '-Dspring.datasource.hikari.schema=public' `
  '-Dspring.liquibase.default-schema=public' `
  '-Dspring.config.import=optional:file:./target/aula16-no-external-config.properties' `
  '-Dapp.security.initial-admin.enabled=false' `
  '-Dapp.security.refresh-token.idle-timeout=PT30M' `
  '-Dapp.security.refresh-token.absolute-expiration=PT8H' `
  '-Dlogging.level.liquibase=INFO' `
  '-Dlogging.level.org.hibernate.SQL=OFF' `
  '-Dlogging.level.org.hibernate.orm.jdbc.bind=OFF' `
  '-Djacoco.destFile=target/aula16-pontoC-transacional-lock.exec' `
  '-Djacoco.append=false' test > target/aula16-pontoC-transacional-lock.log 2>&1
```

| Execução em 17/11/2026 | Encerramento (-03:00) | Executados | Aprovados | Falhas | Erros | Ignorados | Saída |
|---|---|---:|---:|---:|---:|---:|---|
| Inicial, com duas chamadas concorrentes sem observar a espera no servidor | 15:55:23 | 12 | 12 | 0 | 0 | 0 | 0 / BUILD SUCCESS |
| Final, após reforçar a evidência de contenção do lock | 15:56:35 | 12 | 12 | 0 | 0 | 0 | 0 / BUILD SUCCESS |

A execução inicial usou os mesmos argumentos, com os arquivos `target/aula16-pontoC-transacional.log` e `target/aula16-pontoC-transacional.exec`. Foi repetida apenas esta classe após reforçar o teste concorrente. São 24 execuções de casos, **12 casos distintos**, não 24 testes novos. A evidência final é:

```text
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Os 14 testes isolados da correção anterior permanecem um resultado histórico separado, não foram reexecutados aqui. Não foi gerado relatório global de cobertura, nem executado o portão JaCoCo ou a suíte completa nesta etapa.

### Resultado e pendências atuais

**Validação transacional do Ponto C aprovada e pronta para revisão**, para os cenários acima. Não houve falha inesperada. Houve apenas as falhas sintéticas esperadas pelos testes, cujos rollbacks foram confirmados em novas transações.

Continuam fora desta evidência: respostas HTTP, entrega do par no login, novos endpoints, logout, OpenAPI, Postman, compatibilidade dos testes de segurança que importam o login e validação global da suíte. O login interno atual cria a sessão, mas o controller ainda entrega somente o access token; essa limitação já existia na implementação parcial e não foi alterada aqui. Também não houve teste específico de violação de constraint durante uma renovação; a falha tardia usada para demonstrar rollback é um callback sintético antes do commit, após SQL real, e não uma falha de infraestrutura ou de constraint.

Nesta etapa foram alterados somente este documento e o novo teste integrado. Não houve alteração em código de produção, migrations ou configurações persistidas, nem commit ou push. A execução do Liquibase não aplicou migrations. A consulta final, somente leitura, confirmou novamente destino, 44 changeSets e ausência das fixtures. Aguarda-se revisão antes de qualquer avanço.

## Linha do tempo — seção 26

Exemplo didático com os prazos configurados: inatividade de 30 minutos e limite
absoluto de oito horas desde o login. Os horários abaixo ilustram a regra; não
são uma nova execução nem horários extraídos dos testes. R1, R2 e R3 representam
refresh tokens da mesma família; os valores das credenciais não são exibidos.

```text
09:00 login       09:20 renovação     09:40 renovação     10:10 inatividade
    R1 ----------------> R2 ----------------> R3 ------------X
    ativa                ativa                ativa          nenhuma ativa
                         R1 utilizada         R2 utilizada   R3 expirada

09:00 ------------------------------------------------------ 17:00
      início da família                          limite absoluto fixo
```

- De 09:00 até antes de 09:20, somente R1 pode renovar; sem uso, venceria às 09:30.
- Às 09:20, R1 é marcada como utilizada e nasce R2, com inatividade até 09:50.
- Às 09:40, R2 é marcada como utilizada e nasce R3, com inatividade até 10:10.
- Às 10:10, R3 já não pode renovar: o instante exato do vencimento é inválido.
  Sem outra renovação, nenhuma geração está ativa. A expiração decorre do tempo;
  ao tentar renovar, o serviço registra a revogação por INATIVIDADE.
- O limite absoluto continua sendo 17:00 para todas as gerações. Não se espera
  até esse horário para rejeitar R3, que já expirou por inatividade.

Para separar os dois limites, considere uma continuação alternativa: R3 é usada
antes de 10:10 e as sucessoras são renovadas sempre antes de 30 minutos. Se a
última geração nascer às 16:50, sua janela de inatividade chegaria a 17:20, mas
ela só pode renovar antes de 17:00. Exatamente às 17:00, o limite absoluto
impede a renovação e a tentativa registra EXPIRACAO_ABSOLUTA. Nenhuma renovação
prolonga a família além das oito horas originais.

O replay é outro desvio da linha principal: se R1 for reapresentada às 09:45,
o serviço reconhece seu uso anterior, responde com o erro genérico de refresh
inválido e revoga R1, R2 e R3 por REUTILIZACAO. R3 perde a possibilidade de renovar
mesmo antes das 10:10, pois a família está comprometida. A prova transacional
desse comportamento está registrada acima; as requisições HTTP e a demonstração
com duas renovações estão em [api.md](api.md) e [postman.md](postman.md).

## Alternativa para revogação imediata do access token

Uma alternativa seria consultar, em cada requisição autenticada, um registro de
famílias revogadas identificado pelo claim sid. O logout gravaria a família nesse
registro e a validação rejeitaria também os access tokens associados. As entradas
precisariam permanecer até expirar o último JWT que pudesse usá-las.

O custo é acrescentar estado e uma consulta ao banco ou serviço compartilhado
por requisição, com latência, carga, disponibilidade e sincronização entre
instâncias. Um cache com dados desatualizados poderia atrasar a revogação;
garantir efeito imediato exigiria uma estratégia consistente de leitura e
invalidação. Também seria necessário definir o comportamento se o registro
ficasse indisponível.

**Esta é somente uma proposta arquitetural, não uma funcionalidade implementada.**
O projeto mantém a validação local do JWT: logout impede novas renovações, mas
não invalida imediatamente um access token já emitido. Os testes HTTP e a
execução Newman registrados demonstram esse limite, sem novas execuções nesta
complementação documental.
