# Aula 13 — Atividade de transferência

[Índice](README.md) · [Segurança](SEGURANCA.md) · [Evidências](EVIDENCIAS.md)

Esta entrega adapta os nove itens da seção 17 do [material do professor](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/13aula/13aula.md#17-atividade-de-transferência)
ao controle de instrumentos musicais. Não contém chave, token ativo ou senha real.

1. **Chave própria:** configuração local externa ao Git, gerada no Checkpoint 2; validação Base64 e mínimo 32 bytes em `JwtConfig`.
   A auditoria confirma o arquivo ignorado e o tamanho sem divulgar o valor. Não é possível provar aleatoriedade apenas inspecionando bytes existentes.
2. **Emissor e duração:** `https://instrumento2026.local` e `PT30M`. O emissor foi adaptado ao projeto; não foi copiado do Suporte OS.
3. **Login:** `POST /api/autenticacao/login`, público, usa `AuthenticationManager`, que reutiliza conta persistida e BCrypt.
4. **Claims:** `sub` é o login autenticado; `iat` e `exp` têm diferença de 1800 segundos; o claim de acesso `perfis`
   representa OPERADOR/ADMINISTRADOR, vindo da conta autenticada. Não confundir com Cliente/Colaborador.
   Também são incluídos `iss` e `jti`, sem CPF, senha ou hash.
5. **Bearer substitui Basic:** Resource Server valida JWT e não há filtro manual. `AutenticacaoJwtTest` prova que Basic recebe 401.
6. **Postman:** login salva `accessToken`, operações protegidas herdam Bearer, públicos usam No Auth. O runner executa duas fases,
   prepara somente a conta fictícia bloqueada e remove os dados criados. Ver [POSTMAN.md](POSTMAN.md).
7. **Tokens válido, alterado e ausente:** `AutenticacaoJwtTest`, `ContaAcessoSecurityTest` e `JwtConfigTest` cobrem o fluxo HTTP e a criptografia.
   Newman cobre válido, ausente e inválido; não se atribui ao Newman um teste de adulteração criptográfica específico que só existe em JUnit.
8. **Bloqueio:** impede novo login. O token anterior continua funcionando até sua janela de expiração terminar, pois não há consulta
   de status em cada validação Bearer. Troca de senha e inativação da Pessoa possuem a mesma limitação de revogação.
9. **Proposta futura para o domínio:** para impedir que um token de conta bloqueada finalize novas vendas ou movimente estoque,
   uma lista de revogação por `jti`, consultada em armazenamento compartilhado, poderia recusar tokens comprometidos até `exp`
   (considerando também tolerância de relógio). Uma versão de credencial por conta permitiria invalidar todos os seus tokens,
   com consulta em cache/banco e regras de consistência. Ambas exigem estado, disponibilidade e testes adicionais.
   São propostas documentais, **não funcionalidades implementadas**, sem novo endpoint, filtro, tabela ou cache nesta aula.

## Respostas às questões de discussão

1. O payload é legível porque Base64URL é codificação, não criptografia.
2. A assinatura protege integridade e autenticidade para quem confia na chave; não oferece confidencialidade.
3. Quem possui a chave HS256 também pode emitir tokens, não apenas validá-los.
4. Nesta API, JWT é validado a cada chamada sem sessão HTTP de autenticação; o PostgreSQL mantém estado de negócio.
5. Perfis devem vir do servidor para impedir que um cliente declare privilégios no próprio JSON.
6. `exp` limita o tempo de abuso de um token roubado, mas não impede seu uso enquanto válido.
7. Bloqueio não revoga token porque a validação local do Resource Server não consulta o estado atual da conta.
8. Um endpoint didático de emissão não implementa os protocolos, clients, grants e metadados de um Authorization Server completo.
9. Assinatura assimétrica seria preferível ao separar emissão e vários validadores, que receberiam apenas chave pública.
10. Decodificar mostra o conteúdo; validar exige conferir assinatura, emissor e janela temporal com configuração confiável.

As evidências são automatizadas. Não foram criadas capturas, nem alegadas execuções manuais pela interface gráfica.
O marco Git sugerido pelo professor é `aula-13-jwt`; sua criação depende de autorização e não faz parte desta auditoria.
