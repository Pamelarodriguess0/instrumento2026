# Aula 14 — Plano de validação final controlada

**Histórico do plano 7A, com comandos corrigidos no 7K.** As execuções posteriores tiveram autorizações
próprias e foram concluídas, conforme o registro final abaixo. Este documento não concede nova autorização.
Não iniciar a aplicação, ativar bootstrap ou executar Newman antes de autorização específica.
Os [achados A1–A8](SEGURANCA.md#achados-e-ações-recomendadas) precisam orientar a próxima decisão.

## 1. Resolver pendências antes da suíte integral

Solicitar autorização para adaptar os testes históricos A1–A3, preservando todas as verificações de
negócio. Utilizar ADMINISTRADOR nos testes de cadastro de conta e nas exclusões comerciais; manter
filtros ativos nos testes de segurança e separar expectativas anônimas 401 e de operador 403.
Não converter falhas de negócio em expectativas genéricas de segurança. Confirmar que contextos de
fixtures são restaurados e que threads concorrentes recebem o contexto quando necessário.

Decidir expressamente como tratar A4, inativação da Pessoa do último administrador. Se for investigado,
primeiro usar regressão isolada, sem tocar no administrador de desenvolvimento. Não ampliar a política
silenciosamente. A limitação concorrente A5 deve permanecer declarada mesmo com testes aprovados.

## 2. Preparar isolamento e conferir conexões

Depois de autorização, usar PostgreSQL 17.6 local exclusivo e descartável com banco
`instrumento2026_test`. Não reutilizar dados reais nem presumir que profile test basta.
Identificar host, porta, database e usuário efetivos, sem imprimir senha; verificar o profile e a
configuração JDBC realmente consumida pela API. O runner exige esse nome de banco e localhost.
Se não for possível garantir identidade entre banco da API e banco da limpeza, **não executar Newman**.

Preferir instância/cluster descartável separado, sem conexões de desenvolvimento. Quando houver dados
anteriores, exigir snapshot e inventário anterior por tabela/IDs, incluindo conta e perfis do administrador.
Uma execução Newman normal não deve promover, bloquear ou excluir contas preexistentes; autoproteção
espera 400. Não executar testes paralelos à aplicação Newman. Confirmar procedimentos para falha abrupta,
pois o inventário de IDs do runner fica em memória.

## 3. Suíte JUnit PostgreSQL

Sem bootstrap e com todas as pendências de testes tratadas, executar:

```powershell
$env:INITIAL_ADMIN_ENABLED = 'false'
$env:DB_TEST_URL = 'jdbc:postgresql://127.0.0.1:55414/instrumento2026_test?currentSchema=public'
# Credenciais previamente fornecidas em memoria; caminho opcional abaixo deve estar ausente.
.\mvnw.cmd -B "-Dspring.profiles.active=test" "-Dspring.datasource.url=$env:DB_TEST_URL" "-Dspring.liquibase.url=$env:DB_TEST_URL" "-Dspring.liquibase.default-schema=public" "-Dspring.config.import=optional:file:./target/aula14-no-external-config.properties" test
```

Fornecer DB_TEST_URL, DB_TEST_USERNAME e DB_TEST_PASSWORD privadamente; não escrever valores reais
na documentação ou linha de comando. Verificar a importação do `.env` prevista no profile e possíveis
sobreposições. Registrar total, aprovados, falhas, erros, ignorados, BUILD e horário reais. Não prever
um total com base em 510 testes antigos mais execuções isoladas sobrepostas.

Conferir migrações históricas sem alterações e a contagem real de changeSets; a Aula 14 não cria migração.
Verificar regressões de estoque/vendas, concorrência e senha/status, além de consultas JPA administrativas.
Após o commit de cada teste transacional apropriado, consultar estados persistidos; mocks não substituem
essa evidência. Investigar falhas antes de modificar arquivos adicionais. Confirmar limpeza dos testes.

## 4. Bootstrap em laboratório exclusivo

Em banco descartável sem administrador, fornecer INITIAL_ADMIN_* apenas pelo ambiente privado, com CPF,
e-mail e login fictícios exclusivos e senha válida. Executar uma única instância com flag true; confirmar
Pessoa/conta/perfis persistidos, BCrypt, login e ausência de segredos em logs. Reiniciar uma vez para
verificar idempotência e depois desligar a flag. Não promover `operador.loja` por SQL.

Validar falha/rollback físico com cenário controlado, sem reaproveitar dados reais. Não alegar que testes
com transaction manager mock comprovaram rollback no PostgreSQL. Se existir administrador utilizável,
confirmar que não há nova criação; usar credenciais autorizadas, sem tentar contornar a proteção.

## 5. Newman normal

Usar o procedimento de [POSTMAN.md](POSTMAN.md#comando-de-execução-real--preparado-não-executado).
Iniciar API test na porta 18014 com bootstrap desabilitado, após confirmar a mesma conexão de limpeza.
Injetar credenciais administrativas por ambiente privado e executar o runner somente quando autorizado.
Ele realiza pastas 01–10; a pasta 11 não faz parte da execução normal. O bloqueio das fixtures usa API,
não o SQL auxiliar histórico da Aula 13. Preservar snapshots do administrador externo para comparar depois.

Registrar requisições, assertions, erros/falhas e horário reais do Newman. Chamadas auxiliares de CPF
podem aumentar o total HTTP em relação aos itens da coleção; não confundir 155 definições com execuções.
Verificar 401 sem Bearer, 403 para OPERADOR em administração, positivos comerciais/admin, troca de senha,
novo login, status e tokens antigos. Erros dos filtros não devem ser presumidos como ApiError.

Inspecionar resultado do finally e consulta de zero remanescentes por IDs. Confirmar que registros
preexistentes, sobretudo administrador, Pessoa, status e perfis, permanecem iguais ao inventário anterior.
Se houver falha, não repetir indiscriminadamente. Recuperar fixtures com inspeção restrita aos IDs ou
descartar o ambiente exclusivo autorizado. Executar novamente somente após limpeza confirmada.

## 6. Último administrador e interface

Opcionalmente autorizar um laboratório separado para a pasta 11. Não preparar esse cenário removendo
administradores do banco compartilhado. Seguir os pré-requisitos de POSTMAN para token administrativo
anterior de outra conta já rebaixada e único administrador fictício utilizável. Distinguir autoproteção
de proteção da contagem e consultar novamente o estado persistido após as negativas 400.

Se houver interface disponível, verificar manualmente Swagger com Bearer e permissões documentadas.
Registrar interface manual separadamente de HTTP/MockMvc/Newman. Encerrar API temporária e remover
credenciais da sessão; não exportar ambiente com tokens. Não alterar `.env` real como efeito colateral.

## 7. Critério de encerramento

Atualizar evidências apenas com resultados executados. Repetir revisão de segredos, git status/diff e
diff --check. Sem falhas obrigatórias abertas, limpeza comprovada e decisão sobre A4, solicitar auditoria
final. Nenhum commit, tag, push ou Aula 15 faz parte deste plano de execução autorizado agora.

## Registro final e procedimento corrigido — 7H.1 a 7K

A validação foi concluída em PostgreSQL 17.6, cluster `7704384549022663752`, destino
`127.0.0.1:55414/instrumento2026_test`, schema public. JUnit concluiu em 05/11/2026 às
09:12:29 -03:00: **671 aprovados, zero falhas, erros e ignorados, BUILD SUCCESS, código 0**.
Newman concluiu às 09:36:59 -03:00: **156 chamadas e 487 assertions, zero falhas, código 0**.
Pastas 01–10 executadas; pasta 11 não executada no runner comum. A validação estática conferiu
155 requisições, 163 scripts e 126 cenários históricos preservados. Totais sobrepostos não são somados.

O comando JUnit aprovado não continha a propriedade JVM global de desabilitação direta do bootstrap,
que interferia no teste isolado de configuração. Utilizou-se `INITIAL_ADMIN_ENABLED=false` no ambiente,
URLs explícitas, default-schema public, schema Hikari public e importação externa neutralizada.
Naquela suíte a URL não tinha query; o exemplo corrigido acima usa `currentSchema=public`, empregado
no startup HTTP validado. Não alterar arquivos permanentes para configurar uma nova execução.

Revisar sobreposições SPRING_*, DB_*, PG*, SPRING_APPLICATION_JSON e opções JVM/Maven herdadas.
Injetar credenciais somente no processo: DB_TEST_USERNAME/PASSWORD, SPRING_DATASOURCE_USERNAME/PASSWORD
e SPRING_LIQUIBASE_USER/PASSWORD devem corresponder ao usuário exclusivo do cluster temporário.
Não imprimir valores nem colocar senhas em argumentos. Capturar stdout/stderr e código nativo sem
converter avisos Mockito/Node em falhas adicionais.

Para a API, DataSource e Liquibase receberam a mesma URL com `currentSchema=public`; a configuração
`connection-init-sql` foi retirada após falha de binding no Hikari. O log Hibernate e a correlação de
dez conexões TCP com pg_stat_activity comprovaram o destino runtime. O runner psql não repassa os
parâmetros JDBC: utilizou-se `PGOPTIONS=-c search_path=public`, sem outros PG* conflitantes.
Ele não valida sozinho o cluster e mantém fallback para 5432: exigir URL explícita e conferência
externa antes de permitir a limpeza. Foi usado o runner diretamente por arquivo, sem `node -e`.

O bootstrap real criou a primeira conta. Recuperação autorizada de credencial e Newman ocorreram
na mesma sessão. A limpeza confirmou zero fixtures comerciais; contas 225/226 e Pessoas 639/640,
externas à coleção, permaneceram preservadas. Não repetir a suíte contra esse estado sem novo
planejamento devido aos documentos fixos das fixtures. A API foi encerrada; o cluster não foi
descartado. Foram confirmados 41 changeSets, lock liberado e preservação da instância original na porta 5432.

A1–A3 foram resolvidos pelos testes adaptados. A4 permanece como limitação conhecida de inativação
indireta de Pessoa; A5 mantém o risco concorrente da contagem. JWT anterior conserva authorities até
expirar. Não há prova de rollback físico do bootstrap nem de idempotência por reinicialização real:
os respectivos testes usam componentes simulados. Diagramas foram revisados textualmente, sem
atribuir renderização gráfica ou Swagger manual à execução HTTP.

A auditoria 7J solicitou alinhamento documental, realizado no 7K sem novas execuções.
O versionamento depende de autorização explícita. Evidências detalhadas: [EVIDENCIAS.md](EVIDENCIAS.md).
