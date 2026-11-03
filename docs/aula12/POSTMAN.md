# Aula 12 — Laboratório Postman

[Índice da aula](README.md) · [Segurança](SEGURANCA.md) · [Evidências consolidadas](EVIDENCIAS.md)

Importe **Instrumento 2026 - Aula 12.postman_collection.json** (Collection v2.1).
Esta coleção é independente das aulas anteriores e acompanha o laboratório de
[HTTP Basic do professor](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/12aula/12aula.md).
As coleções históricas não foram alteradas. Não existe ambiente Postman separado
no padrão atual: os valores ficam nas variáveis da coleção.

## Preparação local

Use uma aplicação local conectada a `instrumento2026_test` ou `instrumento2026_dev`.
Nunca execute este laboratório em produção. A coleção cria quatro Pessoas e três
contas de acesso de teste; a Pessoa de reserva permanece sem conta.

Os CPFs têm dígitos verificadores gerados, sem vínculo com pessoas reais; isso não
comprova identidade civil. Nomes, e-mails `@example.com` e senhas são fictícios.
HTTP Basic local usa HTTP para fins didáticos; em produção depende de HTTPS.
O cadastro público por `pessoaId` não comprova quem está solicitando o acesso.

## Variáveis da coleção

| Variável | Uso |
|---|---|
| `baseUrl` | URL local, inicialmente `http://localhost:8080` |
| `authLogin`, `authPassword` | Credencial da conta principal; Basic herdado na raiz |
| `nextAuthPassword`, `oldAuthPassword` | Senha fictícia nova e senha anterior após a troca |
| `blockedLogin`, `blockedPassword` | Conta específica para testar bloqueio |
| `inactiveLogin`, `inactivePassword` | Conta cuja Pessoa será temporariamente inativada |
| `pessoaId`, `contaId` | IDs capturados do cadastro principal |
| `blockedPessoaId`, `blockedContaId` | IDs da fixture de bloqueio |
| `inactivePessoaId`, `inactiveContaId` | IDs da fixture de inativação |
| `reservaPessoaId` | Pessoa sem conta, usada nos testes de conflito e senha inválida |
| `runId`, `cpf_principal`, `cpf_bloqueada`, `cpf_inativa`, `cpf_reserva` | Valores gerados na primeira requisição |
| `blockedFixtureReady` | Inicialmente `false`; definir `true` somente após confirmar o bloqueio SQL |

Não defina variáveis homônimas em um ambiente: elas poderiam sobrepor as variáveis
da coleção e impedir o uso da senha atualizada. Não substitua os exemplos por
credenciais reais nem exporte valores de um ambiente de produção.

## Ordem de execução

Execute uma iteração por vez, sem concorrência entre runners.

1. **01 — Infraestrutura:** health público e OpenAPI público. A primeira requisição
   gera um `runId`, logins exclusivos, CPFs e limpa os IDs da execução anterior.
2. **02 — Preparação das contas:** cria quatro Pessoas e três contas públicas;
   consulta `/me` das três contas, confirmando que estão ATIVAS com OPERADOR e que
   suas credenciais funcionam. Não continue se algum teste desta fase falhar.
3. **Pause e prepare o bloqueio**, conforme a seção abaixo.
4. **03 — Acesso comercial:** instrumentos com Basic (200), sem Basic (401), senha
   incorreta (401) e consulta própria anônima (401).
5. **04 — Validações e conflitos:** login duplicado em outra Pessoa (409), segunda
   conta da mesma Pessoa (409), senha curta (400) e senha multibyte acima de 72
   bytes UTF-8 (400). A Pessoa de reserva foi criada antes desses testes.
6. **05 — Alteração da própria senha:** PUT `/me/senha` (204); somente após esse
   sucesso o script copia a senha anterior para `oldAuthPassword` e atualiza
   `authPassword`. Confirma 401 com a antiga e 200 com a nova em `/me` e instrumentos.
7. **06 — Pessoa inativa:** a conta principal inativa outra Pessoa pelo PUT de
   status existente; suas credenciais retornam 401. Em seguida reativa essa Pessoa
   e confirma novamente 200 com as mesmas credenciais.
8. **07 — Conta bloqueada:** confirma 401 com as credenciais que funcionaram na
   pasta 02. Exige `blockedFixtureReady=true`; a ausência dessa preparação é um
   erro de execução, não um teste aprovado de bloqueio.

Para repetir tudo, reinicie pela pasta 01. Para apenas retomar após a preparação
SQL, não execute novamente a pasta 01: ela gera outro conjunto de IDs e logins.
Não repita isoladamente a troca usando a mesma nova senha já aplicada: o serviço
rejeita reutilização da senha atual.

## Preparação da conta bloqueada

Esta aula não expõe endpoint de bloqueio. Não é correto chamar uma rota inventada
ou utilizar login inexistente para obter um 401 que seria atribuído ao bloqueio.

Após as pastas 01 e 02, copie `blockedLogin` das variáveis atuais. No **mesmo banco
local usado pela aplicação**, execute o arquivo auxiliar, usando sua configuração
local de conexão PostgreSQL (sem gravar senha no comando ou no repositório):

```powershell
& 'C:\Program Files\PostgreSQL\17\bin\psql.exe' -X -v ON_ERROR_STOP=1 `
  -h localhost -p 5432 -U instrumento_app -d instrumento2026_test `
  -v login_bloqueado=a12.bloqueada.TOKEN_DESTA_EXECUCAO `
  -f docs/aula12/PREPARAR-CONTA-BLOQUEADA.sql
```

O script usa uma transação, rejeita outros nomes de banco, exige exatamente uma
conta, valida prefixos de login/e-mail de teste, Pessoa ATIVA, conta ATIVA e hash
BCrypt. Altera somente essa conta para BLOQUEADA. Não modifica perfis nem senha.
Um erro interrompe o comando e impede a confirmação da transação.

Confira no banco o estado `BLOQUEADA` e a Pessoa `ATIVO`; só então defina
`blockedFixtureReady` como a string `true` na coleção e execute as pastas 03–07.
No banco de desenvolvimento, ajuste apenas o parâmetro `-d` para o banco local
correspondente. O SQL de preparação não é migração Liquibase.

## Respostas verificadas

Os scripts conferem o status esperado de cada requisição e a ausência de valores
de senha e marcadores de hash nas respostas. Cadastros e consultas verificam os
seis campos do DTO e o perfil OPERADOR. Também verificam o schema Basic no OpenAPI,
mensagens de conflito, erro de validação, limite UTF-8 e corpo vazio do 204.

Erros de negócio 400/409 usam o `ApiError`; os scripts não presumem que o 401 dos
filtros tenha esse JSON. As URLs, bodies e tipos numéricos de `pessoaId` correspondem
aos DTOs implementados. Rotas públicas/anônimas usam `noauth` explícito; as demais
herdam Basic ou usam a credencial específica do cenário.

A coleção exercita **200, 201, 204, 400, 401 e 409** em operações apropriadas.
O 404 existe, por exemplo, no cadastro que referencia Pessoa inexistente, mas não
é esperado nos cenários desta coleção: não são fabricados IDs ausentes.

## Dados e isolamento

Cada execução cria dados com token próprio e só altera as Pessoas/contas criadas
nessa execução. Em uma execução manual, os dados permanecem para inspeção e a conta
de bloqueio permanece BLOQUEADA. Não há exclusão de contas via API nesta etapa.
Para limpeza, utilize somente os IDs/logins capturados desta execução, em banco
local; não remova dados por um prefixo genérico nem trunque tabelas.

## Validação realizada em 03/11/2026

- JSON validado contra o schema oficial Postman Collection v2.1.0 com Ajv 6.
- Sete pastas, 29 requisições e 31 scripts com sintaxe JavaScript válida.
- Execução automatizada real pelo Newman **6.2.3**, com a aplicação no profile
  `test`, PostgreSQL **17.6**, URL local `http://localhost:18012`.
- Fase 01–02: **12 requisições, 32 assertions, zero falhas**.
- Preparação SQL executada e estado **BLOQUEADA/ATIVO** confirmado no banco.
- Fase 03–07: **17 requisições, 43 assertions, zero falhas**.
- Total: **29 requisições e 75 assertions, zero falhas**, em 03/11/2026 às
  **09:39:59 -03:00**. Dados próprios removidos após a execução automatizada.
- A primeira tentativa do runner auxiliar não transportou as variáveis entre fases
  e parou na preparação SQL. O runner foi corrigido; a execução completa acima foi
  realizada depois. Os dados dessa tentativa também foram limpos.

Ferramentas, logs, schema baixado e runner auxiliar ficaram em `target`, sem alterar
o `pom.xml` ou introduzir dependências na aplicação. Não houve execução manual pela
interface gráfica do Postman. A interface gráfica não foi validada.
