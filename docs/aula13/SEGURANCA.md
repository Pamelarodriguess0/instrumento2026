# Aula 13 — Segurança e limitações

[Índice](README.md) · [Atividade](ATIVIDADE-ORIENTADA.md) · [Evidências](EVIDENCIAS.md) · [Postman](POSTMAN.md)

## Representação e assinatura

O JWT possui três segmentos separados por ponto: header, payload e assinatura. Header e payload são
codificados em Base64URL, que qualquer pessoa pode decodificar. O header informa o algoritmo; o servidor
aceita explicitamente HS256 e não confia no algoritmo declarado pelo cliente para decidir o que permitir.
O payload contém claims. A assinatura HMAC-SHA256 permite verificar integridade e origem a quem conhece a chave.
**JWT assinado não é criptografado**: a assinatura não oculta o conteúdo.

Quem possui a chave HS256 pode tanto validar quanto emitir tokens. Não a distribua a clientes.
Em sistemas com múltiplos validadores, chaves assimétricas permitem separar emissão (privada) de validação (pública),
mas RSA/JWKS não foram implementados. Esta aplicação é um emissor didático e Resource Server, não um OAuth2 Authorization Server completo.

`JwtConfig` rejeita chave ausente, Base64 inválida ou menos de 32 bytes. Não inclui a entrada inválida nas exceções.
O segredo local está somente no `.env` ignorado; `.env.example` contém placeholder e o profile `test`, chave didática.
`JwtProperties`, `LoginRequest`, `TokenAcesso` e `TokenResponse` mascaram credenciais em `toString()`.
Isso reduz exposição acidental, mas não autoriza registrar bodies, headers Authorization, tokens ou senhas em outros logs.

## Autenticação e claims

O login passa pelo `AuthenticationManager`, `ContaAcessoUserDetailsService` e `PasswordEncoder` delegante da Aula 12.
Conta ATIVA e Pessoa ATIVA são requisitos para novos logins. BCrypt e prefixos `{bcrypt}` são preservados.
O guardião de 72 bytes UTF-8 atua antes de `matches`, inclusive para senhas multibyte. Não há comparação manual de BCrypt.

A resposta de login contém o JWT e sua expiração. Claims são construídos pelo servidor:
`iss=https://instrumento2026.local`, `sub=login`, `iat`, `exp`, UUID `jti` e `perfis`.
Os perfis são extraídos das authorities `ROLE_*` do Authentication e ordenados, sem aceitar elevação de perfil via JSON.
Perfil de acesso não é função comercial de Colaborador. O Resource Server usa o conversor padrão e não transforma
o claim `perfis` em regras administrativas nesta aula. Não há `hasRole` para restringir operações comerciais.

Falha de credenciais retorna 401 com `ApiError` e mensagem genérica. Isso reduz enumeração no login,
mas os conflitos do cadastro público ainda podem indicar existência de login/Pessoa; é limitação do bootstrap didático.
Erros de Bearer nos filtros não são erros comerciais e não possuem o mesmo contrato JSON.

## Tempo, bloqueio e troca de senha

`exp - iat = 1800 segundos`, com `Clock` injetado e instantes alinhados a segundos para coerência entre JWT e resposta.
Uso do token não altera `exp`: validade absoluta não é timeout por inatividade nem expiração deslizante.
O decoder usa os validadores padrão com emissor configurado, verificando assinatura e janela temporal (`exp`/`nbf`).
A tolerância padrão de relógio é de 60 segundos, conforme a [documentação do Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html).
Assim, `expiresAt` descreve o claim de expiração; não se promete rejeição com precisão de milissegundos nessa fronteira.
Os testes de expiração usam tokens vencidos além dessa tolerância. Não houve teste de fronteira exata nem espera arbitrária de 30 minutos.

O Resource Server não consulta o estado atual da conta a cada Bearer. Portanto:

- Bloquear a conta ou inativar a Pessoa impede novos logins, mas não revoga JWT já emitido.
- Alterar a senha exige a senha atual; a anterior não permite novos logins. JWT anterior continua válido até sua janela temporal terminar.
- `/me` consulta a conta pelo `Principal.getName()`, derivado do `sub`. O token anterior pode consultar o estado BLOQUEADA.
- Apagar o token no cliente não invalida cópias. Não existe endpoint de logout ou promessa de revogação.
- O token não é prolongado por chamadas e não existe refresh token.

A proposta futura de revogação está na [atividade de transferência](ATIVIDADE-ORIENTADA.md), sem código ou migração.

## Riscos conhecidos e avaliação

| Risco | Classificação | Consequência e tratamento |
|---|---|---|
| Cadastro público de conta sem prova de titularidade da Pessoa | Alto para produção; conhecido desde a Aula 12 | Quem conhece um pessoaId ativo sem conta pode vincular credenciais próprias. Conta inicial OPERADOR não elimina o risco, pois não há distinção de permissões nesta aula. Necessita comprovação de titularidade/convite ou cadastro controlado antes de uso real. Regra preservada por instrução do usuário. |
| HTTP sem TLS fora do laboratório | Alto se implantado assim | Roubo de senha/token em trânsito. HTTPS é requisito de implantação; a URL de issuer não ativa TLS. Nenhuma implantação HTTPS foi validada. |
| Sem revogação imediata | Médio, limitação explícita da aula | Token copiado pode ser usado até expirar, com a tolerância temporal padrão. Bloqueio/troca de senha não o cancelam. |
| Login público sem rate limiting | Médio para produção, melhoria futura | Força bruta, credential stuffing e custo BCrypt. Rate limiting/monitoramento devem ser planejados; não foram implementados. |
| CSRF desabilitado | Condicional ao cliente | Bearer deve ser enviado explicitamente. Reavaliar se autenticação passar a ser automática por cookies; ausência de sessão não elimina todo risco de CSRF. |
| Armazenamento do token em frontend | Decisão futura | Memória reduz persistência; Web Storage fica exposto a JavaScript/XSS; cookies HttpOnly exigem decisões de CSRF e atributos seguros. Postman não define arquitetura de frontend. |
| Chave HS256 compartilhada ou comprometida | Alto em caso de vazamento | Quem conhece a chave pode assinar. Manter segredo por ambiente e planejar rotação; não há rotação gradual implementada. |
| Chave conhecida do profile test | Uso restrito | Exclusivamente didática. Nunca reutilizar em dev/prod; inspeção local confirmou chave diferente. |
| Atualizações simultâneas do mesmo atributo de ContaAcesso | Limitação anterior | `@DynamicUpdate` preserva alterações em campos distintos; não garante serialização de duas trocas de senha ou dois status concorrentes. Nenhum controle adicional foi introduzido. |

Nenhum desses riscos conhecidos equivale a defeito novo reproduzido neste checkpoint. A ausência de falhas nos testes
não demonstra ausência de vulnerabilidades. Autorização por perfil é assunto da Aula 14, não requisito pendente da Aula 13.

## Validação e fronteiras

Testes automatizados comprovam login real, claims, algoritmo, adulteração, emissor, expiração além da tolerância,
ausência de token, rejeição de Basic, identidade /me, troca de senha, limites UTF-8 e acesso com token emitido antes de bloqueio.
OpenAPI preserva operações/schemas históricos e exceções públicas; Swagger/OpenAPI permanecem desabilitados em `prod`.
A verificação do profile `prod` nos testes lê as propriedades: não equivale a implantação completa de produção.
Newman confirma os cenários comerciais de forma sequencial. Concorrência real é verificada pelos testes JUnit existentes.
Interface gráfica do Postman/Swagger, TLS, navegadores, resistência a XSS e testes de carga não foram validados manualmente.
