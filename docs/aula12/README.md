# Aula 12 — Autenticação e proteção de senhas

Documentação do projeto `instrumento2026`, baseada no [material oficial do professor](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/12aula/12aula.md), no código implementado e nas execuções registradas. Consolidada em **03/11/2026**, sem novas execuções JUnit nesta etapa documental.

## Documentos e artefatos

- [Segurança e decisões técnicas](SEGURANCA.md): componentes, persistência, senhas, filtros, rotas, OpenAPI e riscos.
- [Atividade orientada](ATIVIDADE-ORIENTADA.md): realização da atividade de transferência da seção 13, diagrama, justificativas e questões da aula.
- [Testes e evidências](EVIDENCIAS.md): resultados comprovados, referências aos testes e limites da validação.
- [Laboratório Postman](POSTMAN.md): variáveis, ordem de execução, troca de senha e preparação do bloqueio.
- [Coleção da Aula 12](<Instrumento 2026 - Aula 12.postman_collection.json>): 29 requisições com credenciais fictícias.
- [Preparação SQL da conta bloqueada](PREPARAR-CONTA-BLOQUEADA.sql): fixture local; não é migração nem endpoint administrativo.

## Evolução da Aula 11

A [Aula 11](../aula11/README.md) encerrou com 351 testes aprovados e proteção transacional do estoque. A Aula 12 acrescenta a identificação de quem chama a API: uma credencial ligada a Pessoa, hash de senha, HTTP Basic e exigência de autenticação nas rotas comerciais. Os bloqueios pessimistas, o estoque inteiro e o preço histórico das vendas continuam no domínio comercial.

Pessoa continua representando a identidade civil; Cliente e Colaborador continuam representando papéis comerciais. ContaAcesso representa a credencial. Vender instrumentos ou exercer função profissional não concede automaticamente login ou permissão administrativa.

## Correspondência com o material

| Etapa do professor | Implementação e registro no projeto |
|---|---|
| Dependências | Starters de segurança e de testes gerenciados pelo Spring Boot 4 |
| Banco versionado | Migração 006, duas tabelas e 41 changeSets totais |
| Conta persistida | Entidade, enums, repository, serviço, DTOs e BCrypt |
| API protegida | UserDetailsService, HTTP Basic, STATELESS, endpoints `/me` e bootstrap público |
| OpenAPI/Swagger | `basicAuth` global e três operações públicas explícitas |
| Testes antigos | Cinco classes de controller com `addFilters = false`; testes de segurança com filtros ativos |
| Laboratório | Coleção própria, Newman e preparação SQL local do bloqueio |
| Atividade de transferência e ameaças | ATIVIDADE-ORIENTADA e SEGURANCA |
| Encerramento | Evidências consolidadas; commit e tag ainda não realizados |

As subdivisões locais 3A/3B, 4A–4E e 5A/5B permitiram implementar e verificar cada parte separadamente. A auditoria do Checkpoint 6 levou aos diagnósticos 7A, às correções 7B e à atualização documental 7C. Não representam novos endpoints além do escopo da aula.

## Ambiente e resultados comprovados

Java 21, Spring Boot 4.0.7, Spring Security 7.0.6, Springdoc 3.1.1 e PostgreSQL 17.6. O professor apresenta Suporte OS, PostgreSQL 16, 33 changeSets e 65 testes ao final; nosso projeto preserva seu histórico e registra **41 changeSets e 480 testes completos aprovados**. Esses números não são metas copiadas do outro domínio.

| Execução | Resultado |
|---|---|
| Suíte JUnit completa, 7B | 480 aprovados; zero falhas, erros ou ignorados; BUILD SUCCESS, 03/11/2026 às 10:29:23 -03:00 |
| Diagnósticos após correção, 7B | 4 aprovados, incluídos na suíte completa |
| OpenAPI | 56 aprovados, incluídos na suíte completa; última execução isolada registrada no 4E |
| Regressão relacionada, 7B | 350 aprovados, seleção sobreposta à suíte completa |
| Newman, 5A | 29 requisições e 75 assertions aprovadas, em duas fases, antes das correções 7B |

Os 480 testes correspondem aos 476 anteriores mais quatro novos casos de diagnóstico, com as expectativas preservadas. As seleções de 4, 56 e 350 não devem ser somadas ao total de 480. As execuções anteriores de 476 testes e regressão de 307 permanecem como histórico, com datas, comandos e fontes em [EVIDENCIAS](EVIDENCIAS.md). Não houve reexecução JUnit ou Newman nesta atualização documental 7C.

## Defeitos corrigidos após a auditoria

- **Senha excedente no Basic:** o teste HTTP reproduziu 200 para 73 bytes com o mesmo prefixo de uma senha válida de 72 bytes. `PasswordEncoderConfig` agora retorna `false` acima de 72 bytes UTF-8 antes de delegar a verificação. Os hashes `{bcrypt}`, a senha integral e a normalização de login foram preservados.
- **Sobrescrita entre senha e status:** duas transações com estado antigo podiam perder a senha nova ou reativar uma conta bloqueada, conforme a ordem dos commits. `@DynamicUpdate` em ContaAcesso limita o UPDATE aos atributos alterados. Os testes confirmaram BLOQUEADA com a nova senha persistida nas duas ordens.

Esses defeitos têm reprodução e correção comprovadas; não se confundem com os riscos didáticos ainda presentes. Atualização seletiva não garante proteção entre duas mudanças concorrentes do mesmo atributo. A explicação e os limites estão em [SEGURANCA](SEGURANCA.md).

## Limites desta entrega

O bootstrap público não comprova titularidade de Pessoa. Basic depende de HTTPS em produção; CSRF desabilitado exige reavaliação para navegadores. Não há JWT, autorização administrativa por perfil ou endpoint HTTP de bloqueio. OpenAPI e Swagger permanecem desabilitados em `prod`.

Execução pela interface gráfica do Postman, inspeção de Authorization no console e uso interativo de Authorize no Swagger continuam sem validação manual registrada. A automação executada não é apresentada como captura de tela ou teste manual.

Os documentos e coleções históricos das Aulas 10 e 11 foram preservados. O estado de referência permanece `main`, commit `14efe10`, tag `aula-11-concorrencia-estoque`; o versionamento da Aula 12 depende de autorização posterior.
