# Aula 13 — Postman e Newman com JWT

Coleção: [Instrumento 2026 - Aula 13.postman_collection.json](Instrumento%202026%20-%20Aula%2013.postman_collection.json).
Segue a [seção 12 do professor](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/13aula/13aula.md#12-postman-automatizado), adaptada ao instrumento2026.
As coleções das Aulas 7, 10 e 12 permanecem intactas.

## Variáveis e autenticação

O padrão do repositório utiliza variáveis da coleção, sem ambiente Postman separado.
`baseUrl` começa em `http://localhost:8080`. O runner usa `http://localhost:18013`.
Não crie variáveis homônimas em ambientes: elas sobrepõem os valores atualizados pelos scripts.

- `authLogin`, `authPassword`: conta principal fictícia, criada na própria execução.
- `nextAuthPassword`, `oldAuthPassword`: senha nova e senha anterior da troca.
- `accessToken`: token principal capturado automaticamente pelo login; as chamadas protegidas herdam Bearer da raiz.
- `blockedLogin`, `blockedPassword`, `blockedAccessToken`: fixture específica de bloqueio.
- `inactiveLogin`, `inactivePassword`, `inactiveAccessToken`: fixture de Pessoa inativa.
- `blockedFixtureReady`: definir `true` somente depois da preparação SQL confirmada.
- `runId`, CPFs gerados e IDs de Pessoas/contas: identificam os registros desta execução.
- Variáveis dos cenários históricos (`a10_*`, `execucao`, IDs de categoria/fornecedor/instrumento): preservam o isolamento original.
- `invalidToken`: texto propositalmente inválido, utilizado somente no teste negativo.

Os tokens iniciais e IDs estão vazios no arquivo. Senhas são exclusivamente fictícias.
Não exporte a coleção com tokens preenchidos; não registre JWTs no console ou em relatórios compartilhados.
Use HTTPS fora do laboratório local. A chave HS256 não é necessária ao Postman e não faz parte destes arquivos.

## Ordem de execução

1. **01 — Infraestrutura:** inicializa logins exclusivos e variáveis, health e OpenAPI públicos.
2. **02 — Preparação das contas:** cria quatro Pessoas e três contas; autentica as três e confirma `/me` com Bearer.
3. Pause. Execute [PREPARAR-CONTA-BLOQUEADA.sql](PREPARAR-CONTA-BLOQUEADA.sql) no mesmo banco local, passando o `blockedLogin` desta execução. Confirme sucesso antes de definir `blockedFixtureReady=true`.
4. **03 — Acesso comercial:** Bearer válido, ausência de autenticação, senha incorreta, usuário inexistente, token inválido e rejeição de Basic.
5. **04 — Validações e conflitos:** login duplicado, segunda conta para a mesma Pessoa, senha curta e senha multibyte acima de 72 bytes UTF-8.
6. **05 — Alteração da própria senha:** altera a senha; confirma que a anterior não permite novo login; obtém novo JWT com a senha nova e consulta `/me` e instrumentos.
7. **06 — Pessoa inativa:** inativa outra Pessoa criada no laboratório, confirma rejeição de novo login, reativa, autentica novamente e consulta `/me`.
8. **07 — Conta bloqueada:** rejeita novo login da conta previamente validada; confirma que o token anterior continua válido até expirar.
9. **08 — Estoque e cadastro:** preserva todas as requisições e assertions comerciais da Aula 7, agora autenticadas com Bearer.
10. **09 — Vendas e histórico:** preserva todas as requisições e assertions comerciais da Aula 10, incluindo preço histórico, baixa única, cancelamento, conflitos e paginação. As consultas auxiliares de CPF também enviam Bearer.

Login, cadastro de Pessoa/conta, health e OpenAPI usam `noauth` explícito.
Basic aparece somente no teste negativo de protocolo; não autentica nenhuma operação funcional.
A coleção é sequencial: não simula duas transações simultâneas. Os testes JUnit de concorrência continuam sendo a evidência desse comportamento.

Execute uma iteração completa por vez. Depois de uma execução manual, os dados permanecem até limpeza restrita aos IDs criados.
O runner automatiza essa limpeza. Para repetir, reinicie pela pasta 01; retomar depois do SQL não deve reinicializar as pastas 01–02.
O JWT vence por prazo absoluto de 30 minutos. Um novo login atualiza a variável; não há refresh token.
Bloqueio, inativação da Pessoa ou troca de senha não revogam JWTs já emitidos.

## Execução automatizada local

Pré-requisitos: Node.js, PostgreSQL/psql local, Java 21, aplicação apontando para `instrumento2026_test`.
O runner recusa URL da API e conexão PostgreSQL remotas e qualquer outro banco.
Não altera a API nem usa senha de banco na linha de comando; lê as variáveis locais do `.env` ignorado ou do processo.

Instale Newman somente em diretório ignorado:

```powershell
npm install --prefix target/aula13-postman-tools --no-save newman@6.2.3
```

Inicie a aplicação em um terminal:

```powershell
.\mvnw.cmd -B "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.arguments=--server.port=18013 --spring.config.additional-location=file:./src/test/resources/application-test.properties" spring-boot:run
```

Depois de a aplicação estar pronta, em outro terminal:

```powershell
node docs/aula13/EXECUTAR-NEWMAN.cjs
```

Opções locais do runner: `NEWMAN_BASE_URL`, `NEWMAN_MODULE_DIR` (pasta contendo `newman`) e `PSQL_PATH`.
O padrão de `PSQL_PATH` é `C:/Program Files/PostgreSQL/17/bin/psql.exe`.

O [runner](EXECUTAR-NEWMAN.cjs) executa duas fases: 01–02, SQL restrito à conta fictícia, depois 03–09.
Transporta as variáveis em memória entre fases e não exporta tokens/coleções preenchidas.
O SQL exige banco local autorizado, login/e-mail `a13.bloqueada.*`, Pessoa ATIVA, conta ATIVA e BCrypt.
Não altera senha ou perfis. Não existe endpoint HTTP de bloqueio nesta aula.

O runner captura somente IDs de recursos efetivamente criados com HTTP 201 nesta execução.
Em `finally`, remove esses registros em ordem de FKs, incluindo itens das vendas, e consulta novamente os IDs para confirmar ausência.
Não trunca tabelas nem limpa por prefixos genéricos. Falha de limpeza é reportada como erro e exige inspeção local.
Os resumos ficam em `target/aula13-newman-resultados.json`, sem tokens, senhas, bodies ou headers.

## Assertions

O login válido confere HTTP 200, os quatro campos da resposta, tipo Bearer, validade de 1800 segundos,
expiração e claims do payload. Decodificar o payload no cliente não prova assinatura; o acesso protegido verifica aceitação pelo servidor.
Login inválido recebe 401 com mensagem genérica em ApiError. Erros dos filtros não são tratados como esse JSON.
Os cenários comerciais preservam os códigos e assertions históricos. Cada resposta é inspecionada quanto a exposição de senha/hash.
O 204 da troca de senha continua sem corpo.

A interface gráfica do Postman não está validada por uma execução Newman. Não há evidência de testes manuais de interface neste checkpoint.

## Validação efetivamente executada — Checkpoint 5

Coleção validada contra o schema oficial Postman Collection v2.1, com 126 itens de requisição,
nove pastas principais e 132 scripts com sintaxe JavaScript válida. Os IDs e tokens iniciais estão vazios.

Duas execuções completas independentes, Newman 6.2.3, API local na porta 18013, profile `test`, PostgreSQL 17.6:

| Fase | Chamadas HTTP | Assertions | Falhas de requisição/assertion |
|---|---:|---:|---:|
| 01–02 | 15 | 56 | 0 |
| 03–09 | 114 | 364 | 0 |
| Total de cada execução | 129 | 420 | 0 |

As 129 chamadas incluem os 126 itens e três consultas auxiliares de unicidade de CPF pelo `pm.sendRequest`.
Não houve erros de execução. Conclusões em **03/11/2026 às 14:10:11 -03:00** e **14:11:05 -03:00**.
Não somar os resultados dessas duas execuções como uma única suíte.

Em cada execução foram capturados seis IDs de Pessoas, três contas, um cliente, um colaborador,
três categorias, dois fornecedores, três instrumentos e duas vendas. A limpeza removeu os registros
próprios ainda existentes e os itens de venda; a consulta posterior confirmou **zero registros remanescentes**
entre os IDs capturados. A Aula 7 já exclui alguns desses registros pelas suas próprias requisições.

As tentativas iniciais encontraram incompatibilidade de `Buffer` com o nome de encoding `base64url`
no sandbox do Newman. O script agora converte o alfabeto URL para Base64 antes de decodificar;
as assertions de claims foram mantidas. Essas tentativas interrompidas também tiveram suas fixtures removidas.
O Node.js 24 emitiu um aviso de depreciação de `fs.F_OK` em dependência do Newman, sem falha das execuções completas.
