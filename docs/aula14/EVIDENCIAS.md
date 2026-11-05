# Aula 14 — Evidências e limites da validação

> Atualização 7K, 05/11/2026: os registros do 7A abaixo são históricos. A suíte integral e o
> Newman foram concluídos posteriormente, conforme as seções finais. Nada foi reexecutado no 7K.

## Estado e origem

Checkpoint 7A em 04/11/2026. Branch `main`; HEAD
`d7aaf93a353a66aa6309efece2f3c6ef2ec3fe96`; tag anterior `aula-13-autenticacao-jwt`.
Aula 14 não versionada. Logs em `target` são locais e ignorados, não entregáveis contendo dados reais.
Os resultados abaixo foram conferidos nos logs existentes; não foram todos reexecutados nesta etapa.

## Histórico de testes isolados da Aula 14

Todas as execuções desta tabela registram **zero falhas, erros e ignorados, BUILD SUCCESS**.
Horários em -03:00, todos em 04/11/2026. Totais são próprios de cada execução e se sobrepõem.
**Não somar as linhas como uma suíte única.**

| Etapa | Aprovados | Conclusão | Log local |
|---|---:|---|---|
| 1 | 22 | 08:50:42 | target/aula14-checkpoint1-direcionados.log |
| 2 | 71 | 08:58:21 | target/aula14-checkpoint2-direcionados.log |
| 3A | 105 | 10:36:36 | target/aula14-checkpoint3a-direcionados.log |
| 3B | 138 | 10:55:35 | target/aula14-checkpoint3b-direcionados.log |
| 4 | 173 | 11:11:19 | target/aula14-checkpoint4-direcionados.log |
| 5 — OpenAPI | 60 | 13:43:33 | target/aula14-checkpoint5-openapi.log |
| 5 — regressão, inclui OpenAPI | 233 | 13:44:36 | target/aula14-checkpoint5-regressao.log |
| 6 — API, política e proxies | 107 | 13:55:40 | target/aula14-checkpoint6-isolados.log |

No Checkpoint 6: 25 testes em `ContaAcessoAdministracaoTest`, 49 em `PoliticaHttpSecurityTest` e 33 em
`service/AutorizacaoServicesTest`. Usam repositórios simulados/contextos isolados; não provam persistência
nem rollback físico PostgreSQL. Os 60 testes OpenAPI usam geração real Springdoc em contexto MVC sem
DataSource e preservam o contrato histórico com diferenças explicitamente reconhecidas.

## Verificações executadas no Checkpoint 7A

Compilação de produção e testes, sem executar testes ou inicializar a aplicação:

```powershell
.\mvnw.cmd -B -DskipTests test-compile
```

**BUILD SUCCESS**, conclusão **04/11/2026 às 14:00:22 -03:00**, duração 3,298 s.
Log local: `target/aula14-checkpoint7a-compilacao.log`. As classes já estavam atualizadas.
Isso não significa execução da suíte JUnit; nenhum total de aprovação novo é atribuído a esse comando.

Também executados:

```powershell
node --check docs/aula14/EXECUTAR-NEWMAN.cjs
node --check docs/aula14/VALIDAR-POSTMAN.cjs
node docs/aula14/VALIDAR-POSTMAN.cjs
git diff --check
```

Resultado offline: **11 pastas, 155 requisições definidas, 126 históricas preservadas, 163 scripts
com sintaxe válida e 21 assertions selecionadas exercitadas com respostas fictícias**. Dois cenários de
último administrador são pulados por padrão. **Zero chamadas HTTP e zero conexões de banco** nessa
validação. Schema oficial Postman v2.1 validado com ferramentas locais já existentes e ignoradas.
Isso não é execução Newman nem evidência de respostas do servidor.

Justificativa: **378 palavras**, contadas na seção própria, dentro do intervalo solicitado de 300–500.
Quatro diagramas Mermaid foram produzidos e revisados textualmente contra classes/mappings. Não houve
renderização ou validação gráfica dos diagramas neste checkpoint.

Foram conferidos caminhos/links locais, referências às classes de teste, matriz HTTP, annotations,
propriedades de bootstrap, escopo das alterações e código de captura/limpeza do runner.
`git diff --check` sem erros; avisos de conversão LF/CRLF do Git não indicam falha de whitespace.
Nenhuma alteração da coleção histórica, changelogs históricos, pom.xml ou documentação das Aulas 10–13
foi encontrada no diff. Alterações anteriores em testes históricos estão relacionadas à autorização,
mas algumas expectativas ainda precisam ser adaptadas, conforme os achados A1–A3.

## Credenciais, arquivos e Git

A inspeção não encontrou tokens JWT reais, chaves privadas ou valores sensíveis locais nos candidatos.
Senhas e tokens iniciais da coleção estão vazios; credenciais fictícias são criadas somente em memória.
`.env` continua ignorado e foi comparado por hash em memória, sem exibir seu conteúdo. `.env.example`
contém placeholders e as variáveis administrativas vazias/desabilitadas.

Somente README e quatro novos documentos da Aula 14 foram alterados/criados no Checkpoint 7A:
SEGURANCA, ATIVIDADE-ORIENTADA, EVIDENCIAS e VALIDACAO-FINAL. Código, testes, coleção e runners permanecem
idênticos ao início desta etapa, conferidos por hash. Nenhum arquivo foi staged, commitado ou enviado.

## Pendências históricas do Checkpoint 7A

Naquele checkpoint, a suíte integral da Aula 14 não havia sido executada. As expectativas de integração obsoletas identificadas
estaticamente não foram reproduzidas no banco nem corrigidas automaticamente; não há contagem de falhas
da suíte atual. Os 510 testes e 129 chamadas/420 assertions Newman da Aula 13 são históricos e não
comprovam a nova política.

Não houve bootstrap real, cadastro/promoção/bloqueio de contas, Newman HTTP, limpeza SQL ou interação
manual com Swagger/Postman nesta etapa. Nenhuma fixture foi criada, logo não existe limpeza real a
registrar. O experimento de último administrador também permanece não executado.

Os limites de status de Pessoa, contagem concorrente, JWT anterior e identificação do banco de limpeza
estão em [SEGURANCA.md](SEGURANCA.md). O [plano final](VALIDACAO-FINAL.md) estabelece os passos sujeitos
a autorização posterior. A recomendação de não versionar correspondia àquele momento, superado pelas
validações abaixo, preservadas as limitações conhecidas.

## Suíte PostgreSQL — Checkpoints 7H e 7H.1

A primeira execução registrou 671 testes: 670 aprovados, uma falha, zero erros e ignorados.
A propriedade JVM global de desabilitação do bootstrap prevaleceu sobre a configuração do teste
`AdministradorInicialConfigTest.propriedadesDaAplicacaoDevemUsarVariaveisExternasEDefaultFalse`.
O PowerShell também tratou um aviso Mockito em stderr como erro do comando, prejudicando a captura
do resumo Maven. Não se atribuiu esse resultado a defeito de produção.

No 7H.1 mudou somente o procedimento: retirada da propriedade global conflitante,
`INITIAL_ADMIN_ENABLED=false` no ambiente e captura conjunta de stdout/stderr pelo shell nativo.
Conclusão em **05/11/2026 às 09:12:29 -03:00**:

| Executados | Aprovados | Falhas | Erros | Ignorados | Maven | Saída |
|---:|---:|---:|---:|---:|---|---:|
| 671 | 671 | 0 | 0 | 0 | BUILD SUCCESS | 0 |

Conferidos 54 relatórios Surefire e `target/aula14-7H1-maven.log`. Os seis testes de configuração
passaram. Totais anteriores não se somam a esta suíte.

## Isolamento comprovado

PostgreSQL **17.6**, cluster **7704384549022663752**, destino
`127.0.0.1:55414/instrumento2026_test`, schema `public`. Consultas de identificação confirmaram
cluster, PGDATA, endereço, banco e usuário. Na API, DataSource e Liquibase receberam a URL com
`currentSchema=public`. Dez conexões TCP da API foram correlacionadas por porta cliente com sessões
PostgreSQL identificadas por ApplicationName. Hibernate confirmou `instrumento2026_test/public`.

Foram confirmados **41 changeSets registrados, 12 tabelas e lock Liquibase liberado**, sem migração
nova da Aula 14. A instância original na porta 5432 permaneceu ativa e os quatro arquivos de
configuração comparados permaneceram inalterados. Não se afirma auditoria integral de suas páginas de dados.

## Preparação HTTP e recuperação — histórico 7I a 7I.4

O primeiro startup falhou ao aplicar `connection-init-sql` depois de o Hikari estar iniciado e selado.
A execução corrigida usou o parâmetro JDBC `currentSchema=public`, sem mudança de produção.
Outro comando auxiliar falhou por aspas em `node -e`, antes do runner. Essas tentativas não são
execuções Newman e não entram nos totais finais.

O bootstrap real criou uma conta fictícia; outra conta administrativa de recuperação foi preparada
pela API. As senhas ficaram somente em memória e deixaram de estar disponíveis após aquela sessão.
A recuperação foi autorizada separadamente. No 7I.4 atualizou-se somente `senha_hash` da conta 226,
Pessoa 640, em transação com uma linha afetada. Foi usado o `PasswordEncoderConfig` existente,
confirmando o resultado com `matches()`. Demais campos e a conta 225 foram comparados e preservados.

Recuperação, autenticação e Newman ocorreram na mesma sessão, sem persistir a senha. A API iniciou
com bootstrap desligado e health 200. O runner foi chamado diretamente por arquivo; o observador
local registrou somente códigos HTTP e nomes de assertions, sem headers, corpos, credenciais ou tokens.

## Newman real — Checkpoint 7I.4

Execução em **05/11/2026, das 09:36:37 às 09:36:59 -03:00**:

| Chamadas HTTP | Assertions | Falhas de requisição | Falhas de assertions | Falhas totais | Saída |
|---:|---:|---:|---:|---:|---:|
| 156 | 487 | 0 | 0 | 0 | 0 |

As pastas **01–10** forneceram 153 requisições da coleção e três consultas auxiliares de CPF.
A coleção contém 155 definições porque inclui os dois cenários da **pasta 11, não executada**.
A validação estática conferiu **163 scripts e 126 cenários históricos preservados**, exercitando
21 assertions offline. Essas assertions não se somam às 487 da execução HTTP.

| HTTP | Ocorrências |
|---|---:|
| 200 | 76 |
| 201 | 21 |
| 204 | 4 |
| 400 | 21 |
| 401 | 11 |
| 403 | 10 |
| 404 | 5 |
| 409 | 8 |

Relatórios locais: `target/aula14-newman-resultados.json` e `target/aula14-7I4-http-status.json`;
logs em `target/aula14-7I4-api.stdout.log` e `target/aula14-7I4-api.stderr.log`.
São artefatos ignorados, não candidatos ao commit.

## Limpeza e alcance final

O runner capturou seis Pessoas, três contas, um Cliente, um Colaborador, três categorias,
dois fornecedores, três instrumentos e duas vendas. A limpeza por IDs confirmou **zero fixtures
remanescentes**; consulta posterior confirmou zero registros comerciais.

As contas administrativas de preparação **225 e 226**, Pessoas **639 e 640**, permaneceram ativas
com OPERADOR e ADMINISTRADOR. São externas ao inventário de limpeza, não resíduos da coleção.
A API foi encerrada, as credenciais liberadas da sessão e o cluster preservado.

Autoproteção, promoção/restauração de perfis, status, 401/403 e operações comerciais foram observados
por HTTP. **A pasta 11 não foi executada no Newman comum**. Idempotência e solicitação de rollback do
bootstrap foram testadas com componentes simulados: não há prova de reinicialização real idempotente
nem de rollback físico do bootstrap. Diagramas foram revisados textualmente; não se atribui interação
manual com Swagger nem renderização gráfica a esses resultados HTTP.

O 7J conferiu relatórios, quatro diagramas e justificativa de 378 palavras. O 7K apenas atualiza
documentação; não repete JUnit, Newman, API ou operações de banco. Permanecem os limites de
[SEGURANCA.md](SEGURANCA.md).
