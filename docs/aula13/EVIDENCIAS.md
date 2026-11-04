# Aula 13 — Evidências e auditoria final

[Índice e matriz de conformidade](README.md) · [Segurança](SEGURANCA.md) · [Atividade](ATIVIDADE-ORIENTADA.md) · [Postman](POSTMAN.md)

## Ponto de partida e método

A Aula 12 foi concluída no commit `95291ae6c8bc981e879cb834ee348b5bca29a327`, tag `aula-12-autenticacao-http-basic`,
com 480 testes. A Aula 13 continua sem commit nesta auditoria, branch `main`.
O professor utiliza outro domínio e outro total de testes; não se reproduzem artificialmente esses totais.

Esta auditoria comparou o [material oficial](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/13aula/13aula.md),
especialmente atividade de transferência e checklist final, com código, testes, propriedades e coleção.
As lacunas de documentação foram corrigidas sem alterar produção, testes, migrações, coleção ou runner neste checkpoint.

Não houve execução manual da interface gráfica do Swagger/Postman, captura de tela, implantação HTTPS ou teste em produção.
O teste do profile `prod` verifica as propriedades de desativação de OpenAPI/Swagger, sem iniciar uma implantação prod.

## Histórico comprovado — execuções anteriores

Todos os horários abaixo são de **03/11/2026**, fuso **-03:00**. Os totais representam execuções distintas e sobrepostas;
não devem ser somados para sugerir quantidade de testes únicos.

| Checkpoint | Conclusão | Resultado |
|---|---|---|
| 2 — infraestrutura | 13:26:59 | 491 JUnit, zero falhas/erros/ignorados, BUILD SUCCESS |
| 3 — login e Bearer | 13:51:59 | 508 JUnit, zero falhas/erros/ignorados, BUILD SUCCESS |
| 4 — OpenAPI | 14:01:16 | 510 JUnit, zero falhas/erros/ignorados, BUILD SUCCESS; 58 casos OpenAPI |
| 5 — Postman | 14:10:11 | Newman: 129 chamadas, 420 assertions, zero falhas/erros; limpeza confirmada |
| 5 — repetição Postman | 14:11:05 | Newman: 129 chamadas, 420 assertions, zero falhas/erros; limpeza confirmada |
| 5 — suíte | 14:12:41 | 510 JUnit, zero falhas/erros/ignorados, BUILD SUCCESS |

As tentativas iniciais do Checkpoint 5 falharam por incompatibilidade de encoding Base64URL no sandbox Newman.
As fixtures dessas tentativas foram removidas. O script de decodificação foi corrigido sem retirar assertions.
O procedimento e os resultados posteriores estão em [POSTMAN.md](POSTMAN.md).

## Checkpoint 6 — validação final executada

### JUnit

```powershell
.\mvnw.cmd -B "-Dspring.profiles.active=test" test
```

- PostgreSQL **17.6**, banco local `instrumento2026_test`, profile **test**.
- **510 executados, 510 aprovados, 0 falhas, 0 erros, 0 ignorados**.
- **BUILD SUCCESS**, conclusão em **03/11/2026 às 14:20:45 -03:00**.
- Inclui 58 testes de OpenAPI e os testes existentes de concorrência e regressão comercial.
- Log local ignorado: `target/aula13-checkpoint6-suite-completa.log`.

### Newman

Aplicação iniciada no profile `test`, com propriedades de teste carregadas explicitamente,
em `http://localhost:18013`, banco PostgreSQL **17.6**. Newman **6.2.3**.
Foi reutilizada a instalação local de ferramentas em `target/aula12-postman-tools/node_modules`,
informada por `NEWMAN_MODULE_DIR`; isso não adiciona dependências ao projeto Java.

| Fase | Chamadas HTTP | Assertions aprovadas | Falhas/erros |
|---|---:|---:|---:|
| Pastas 01–02 | 15 | 56 | 0 |
| Pastas 03–09, após SQL restrito | 114 | 364 | 0 |
| Total da execução final | **129** | **420** | **0** |

Conclusão em **03/11/2026 às 14:24:56 -03:00**, saída do runner **0**.
São 126 itens da coleção e três chamadas auxiliares de unicidade de CPF.
Nenhum token, senha, header ou body foi exportado pelo runner para o relatório.
O aviso de depreciação `fs.F_OK` do Newman em Node.js 24 não foi falha de requisição/assertion.

Foram exercitados login válido, senha incorreta, usuário inexistente, ausência de token,
Bearer inválido, Bearer válido, rejeição de Basic, troca de senha e rejeição da senha antiga no novo login,
Pessoa inativa/reativada e conta bloqueada. Token anterior ao bloqueio continua aceito, como esperado.
Também foram executados os cenários históricos completos de estoque, cadastro e vendas.
Esses cenários são sequenciais; não se atribui ao Newman comprovação de concorrência transacional.

### Limpeza e isolamento

O runner capturou os IDs retornados por POSTs com HTTP 201: seis Pessoas, três contas, um cliente,
um colaborador, três categorias, dois fornecedores, três instrumentos e duas vendas.
Alguns foram excluídos pela própria coleção. A limpeza final removeu os restantes e itens de venda,
respeitando FKs e somente os IDs capturados. Consulta posterior confirmou **zero registros remanescentes**.
O SQL preparatório bloqueou somente a conta fictícia `a13.bloqueada.*` desta execução, após login bem-sucedido.
Não houve truncate, exclusão por prefixo genérico ou uso de conta real.

Logs e relatório locais, ignorados pelo Git:

- `target/aula13-checkpoint6-app.log`
- `target/aula13-checkpoint6-newman.log`
- `target/aula13-newman-resultados.json`

## Evidências por componente

| Componente/teste | O que foi inspecionado ou comprovado |
|---|---|
| `JwtConfig` / `JwtConfigTest` | chave ausente/inválida/curta rejeitada, HS256, assinatura de outra chave, emissor incorreto, token expirado e nbf futuro, rejeição de HS512 |
| `TokenService` / `TokenServiceTest` | Clock fixo, instantes em segundos, 1800 segundos, perfis autenticados ordenados, sem credenciais nos claims |
| `AutenticacaoService` | AuthenticationManager reutilizado; AuthenticationException vira mensagem genérica, sem comparação manual de BCrypt |
| `AutenticacaoJwtTest` | contrato e claims de login, jti único, claims enviados pelo cliente ignorados, Basic recusado, tokens adulterado/malformado/expirado/de outro emissor recusados, entrada validada e ausência de sessão |
| `ContaAcessoSecurityTest` | públicos por método, protegidos sem token, normalização de login, conta bloqueada/Pessoa inativa impedem novos logins, token anterior não revogado |
| `ContaAcessoApiTest` | /me identifica a própria conta, troca de senha com Bearer, senha atual exigida, credenciais anteriores não autenticam novamente, contratos anteriores preservados |
| `ContaAcessoAutenticacaoLimiteDiagnosticoTest` | duas variações HTTP: senha correta de 72 bytes retorna 200, extensão de 73 bytes retorna 401 e senha incorreta retorna 401; inclui multibyte |
| `OpenApiDocumentationTest` | bearerAuth global sem basicAuth, operações públicas explícitas, login/DTOs/códigos, senha somente na entrada, contratos históricos e propriedades prod preservados |
| coleção e runner Newman | captura automática, identidade própria, validações de cadastro, erro genérico versus filtros, operações comerciais, SQL restrito e limpeza |

Os quatro testes de diagnóstico de segurança/concorrência da Aula 12 foram preservados;
o teste HTTP de limite foi adaptado do Basic para login JWT. As expectativas de proteção não foram removidas.
Os testes de concorrência de estoque e finalização de venda permaneceram na suíte.

## Conferências Git e entrega

`git diff --check` sem problemas. A inspeção abrange arquivos rastreados modificados e novos,
não apenas o diff que omite arquivos ainda não rastreados.
A chave local decodifica em **32 bytes**, difere da chave didática e não é divulgada.
O `.env` está ignorado e não rastreado; `.env.*` também é ignorado, com exceção do `.env.example` de placeholders.
Varredura dos candidatos não encontrou o segredo JWT local, senhas locais do banco ou JWT literal ativo.
Não houve alterações em changelogs, entidades, repositories, services comerciais ou coleções/documentos históricos.
As mudanças comerciais anteriores se limitam às anotações documentais já feitas em ContaAcessoController.

As lacunas corrigidas neste checkpoint foram: índice da Aula 13 e referência no README principal,
configuração segura da chave e execução, fundamentação de JWT assinado, atividade de transferência,
proposta documental de revogação e consolidação das evidências reais.

Não foram encontradas pendências funcionais obrigatórias nem defeitos novos reproduzidos nesta auditoria.
Persistem os [riscos didáticos conhecidos](SEGURANCA.md), principalmente cadastro público sem comprovação de titularidade,
necessidade de HTTPS e ausência de revogação imediata. Validação gráfica/manual e preparação de produção permanecem fora da evidência automatizada.
Versões antigas de documentos e resultados são preservadas como histórico, não como descrição do fluxo JWT atual.

Conferência final: **34 arquivos candidatos** acumulados (13 rastreados modificados e 21 novos),
após acrescentar a documentação que faltava; **32 links internos** verificados, JSON da coleção válido,
nenhum espaço final encontrado nos candidatos e `git diff --check` concluído com código 0.
Nada no staging. A aplicação temporária usada pelo Newman foi encerrada após a execução.

Parecer: **pronta para versionamento didático da Aula 13**, sem afirmar prontidão para produção.
Commit, push e tag ainda não foram realizados; dependem da autorização do usuário.
