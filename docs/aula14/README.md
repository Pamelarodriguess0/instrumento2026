# Aula 14 — Autorização por perfis

Material: [Aula 14 do professor Jefferson](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/14aula/14aula.md).
Projeto temático: instrumento2026, Java 21, Spring Boot 4.0.7 e Spring Security 7.0.6.
Marco anterior: `d7aaf93a353a66aa6309efece2f3c6ef2ec3fe96`, tag `aula-13-autenticacao-jwt`.
A tag `aula-13-jwt` do projeto de referência não se aplica ao nosso marco.

## Histórico dos Checkpoints 7A e 7B

Checkpoints 1–6 implementados; documentação e auditoria estática registradas nesta etapa.
Não houve inicialização da aplicação, provisionamento ou gravação no PostgreSQL.
A suíte completa de 510 testes e o Newman da Aula 13 são históricos, não resultados da Aula 14.
Na auditoria 7A havia testes históricos a adaptar. Essa adaptação foi feita no 7B, com 237 testes
isolados aprovados e integração apenas compilada naquele momento.

## Situação final — atualização 7K

Em 05/11/2026, JUnit concluiu com **671 testes aprovados, zero falhas, erros e ignorados,
BUILD SUCCESS e código 0**. Newman concluiu **156 chamadas HTTP e 487 assertions, zero falhas,
código 0**, nas pastas 01–10. **A pasta 11 não foi executada** no Newman comum.
A validação estática conferiu 155 requisições definidas, 163 scripts e 126 cenários históricos preservados.

PostgreSQL 17.6 em `127.0.0.1:55414/instrumento2026_test`, schema `public`, cluster
`7704384549022663752`: 41 changeSets e lock liberado. A instância original na porta 5432 foi preservada.
A limpeza confirmou zero fixtures Newman e zero registros comerciais remanescentes. As duas contas
administrativas fictícias de preparação (225/226) e Pessoas (639/640) permaneceram separadamente.
A API temporária foi encerrada. Datas e alcance dos resultados: [EVIDENCIAS.md](EVIDENCIAS.md).

## Documentos e entregáveis

- [SEGURANCA.md](SEGURANCA.md): arquitetura, conformidade, invariantes e achados.
- [ATIVIDADE-ORIENTADA.md](ATIVIDADE-ORIENTADA.md): diagramas e justificativa de 300–500 palavras.
- [EVIDENCIAS.md](EVIDENCIAS.md): resultados comprovados, sem somar execuções sobrepostas.
- [POSTMAN.md](POSTMAN.md): preparação, coleção, credenciais privadas e limpeza.
- [VALIDACAO-FINAL.md](VALIDACAO-FINAL.md): procedimento corrigido e registro da validação real.
- [AMBIENTE-POSTGRESQL.md](AMBIENTE-POSTGRESQL.md): diagnóstico histórico 7C e isolamento posteriormente comprovado.
- [INFRAESTRUTURA-DESCARTAVEL.md](INFRAESTRUTURA-DESCARTAVEL.md): plano histórico 7D e infraestrutura efetivamente utilizada.
- [Coleção](Instrumento%202026%20-%20Aula%2014.postman_collection.json), [runner](EXECUTAR-NEWMAN.cjs) e [validador offline](VALIDAR-POSTMAN.cjs).

## Matriz HTTP implementada

Perfil de acesso não substitui Cliente, Colaborador nem a função comercial VENDEDOR.
`perfis` é convertido em `ROLE_OPERADOR`/`ROLE_ADMINISTRADOR`; `sub` continua identificando o usuário.

| Método/caminho real | Anônimo | OPERADOR | ADMINISTRADOR |
|---|---|---|---|
| GET/HEAD `/api/health` | Permitido | Permitido | Permitido |
| GET/HEAD documentação/Swagger habilitados | Permitido | Permitido | Permitido |
| POST `/api/autenticacao/login` | Público, exige credenciais válidas | Público | Público |
| POST `/api/pessoas` | 401 | Permitido | Permitido |
| POST `/api/contas-acesso` | 401 | 403 | Permitido |
| PUT `/api/contas-acesso/{id}/perfis` | 401 | 403 | Permitido, sujeito às invariantes |
| PUT `/api/contas-acesso/{id}/status` | 401 | 403 | Permitido, sujeito às invariantes |
| GET `/api/contas-acesso/me` | 401 | Permitido | Permitido |
| PUT `/api/contas-acesso/me/senha` | 401 | Permitido | Permitido |
| DELETE `/api/categorias/{id}` | 401 | 403 | Permitido; sucesso 204 |
| DELETE `/api/instrumentos/{id}` | 401 | 403 | Permitido; sucesso 204 |
| DELETE `/api/vendas/{vendaId}/itens/{itemId}` | 401 | 403 | Permitido; sucesso 200 |
| Demais operações comerciais mapeadas em `/api/**` | 401 | Permitido | Permitido |
| Outros caminhos | Negado | Negado | Negado |

Consultas, cadastros e atualizações de categorias, fornecedores, instrumentos, Pessoas, Clientes e
Colaboradores seguem a regra comercial, assim como estoque, inclusão de itens, finalização e cancelamento
de vendas. Não há exceção ao DELETE para item de venda. Permissão não garante sucesso: validações e
retornos 400/404/409 continuam aplicáveis conforme cada contrato. Token válido sem role suficiente recebe
403; Basic não autentica rotas protegidas e recebe 401.

Documentação pública: `/v3/api-docs`, `/v3/api-docs/**`, `/v3/api-docs.yaml`, `/swagger-ui.html`,
`/swagger-ui/**`, somente GET/HEAD. Em `prod`, os recursos permanecem desabilitados.
Regras administrativas precedem `/api/**`; ao final há `denyAll()`. A regra geral não cria endpoints:
rota inexistente pode resultar em 404 após autorização.

## Proteção dos serviços e bootstrap

`@EnableMethodSecurity` e `@PreAuthorize("hasRole('ADMINISTRADOR')")` protegem:

- `ContaAcessoService.cadastrar`, `alterarPerfis` e `alterarStatus`;
- `CategoriaInstrumentoService.excluir`;
- `InstrumentoService.excluir`;
- `VendaService.removerItem`.

Os testes chamam proxies Spring e verificam negativas antes de operações nos repositories.
Consulta e troca de senha da própria conta continuam usando a identidade autenticada. O provisionamento
utiliza serviço transacional separado, sem depender de autoinvocação ou usuário previamente autenticado.
`INITIAL_ADMIN_ENABLED` tem padrão false; campos sensíveis vêm do ambiente privado. O mecanismo não
foi ativado no desenvolvimento. `operador.loja` não deve ser presumido administrador.

## OpenAPI e encerramento

`bearerAuth` continua global. Somente health/login são públicos entre as operações da API documentadas.
Administração e DELETE indicam ADMINISTRADOR; 401/403 dos filtros não prometem `ApiError`.
Os testes preservam explicitamente contratos históricos e descrições de concorrência.
As invariantes têm limites de concorrência e interação com status de Pessoa, detalhados em SEGURANCA.

**Documentação preparada para versionamento didático**, com validações concluídas e limitações explícitas.
Commit, tag e push dependem de autorização. Não há prova de rollback físico do bootstrap nem de
idempotência por reinicialização real: esses aspectos foram testados com componentes simulados.
