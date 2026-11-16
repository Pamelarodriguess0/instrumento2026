# Aula 15 — Testes, cobertura e portão de qualidade

Material: [Aula 15 do professor Jefferson](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/15aula/15aula.md).
Projeto temático: instrumento2026. Início em 11/11/2026; fechamento em 16/11/2026.
Continuidade: [Aula 14](../aula14/README.md).

## Estado final da Aula 15

**Checkpoints 1–5 e atividades das seções 24–26 concluídos.**
As execuções descritas abaixo já foram realizadas; não houve nova execução de
testes ou acesso ao banco no fechamento documental.

| Evidência final | Resultado |
|---|---|
| Suíte completa após TDD, 16/11/2026 às 11:14:20 -03:00 | 679 aprovados em 55 classes; zero falhas, erros e ignorados |
| Linhas globais | 1.236/1.277 — 96,79% |
| Branches globais | 398/442 — 90,05% |
| Classes de produção analisadas | 96 |
| Portão final corrigido, 11:18:10 -03:00 | `All coverage checks have been met.`, BUILD SUCCESS, código 0 |
| Limites oficiais | BUNDLE, COVEREDRATIO, LINE 0.70 e BRANCH 0.40 |

A validação inicial integrada do TDD encontrou uma divergência entre a coleta de
`report` e a de `check`. Ela foi corrigida sem repetir testes: ambos os goals leram
a mesma coleta nova no caminho padrão, com hashes conferidos e sem incompatibilidade.
Somente essa aprovação corrigida é considerada o portão final. [Evidência completa](TDD.md#correção-autorizada--portão-sobre-a-coleta-nova).

Os 671 casos dos checkpoints, os 674 da atividade orientada e os 679 finais são
fotografias sucessivas, não totais a somar. Os 167 casos de domínio de GREEN e
REFACTOR também se sobrepõem e estão incluídos na suíte final.

## Atendimento dos requisitos obrigatórios

| Referência oficial | Atendimento e documento |
|---|---|
| Seção 21, Checkpoint 1 | Inventário e estratégia com alcance/custo/limites: [INVENTARIO-E-ESTRATEGIA.md](INVENTARIO-E-ESTRATEGIA.md) |
| Seções 8 e 21, Checkpoint 2 | ContaAcesso isolado, 15 aprovados: [EVIDENCIAS.md](EVIDENCIAS.md) |
| Seção 21, Checkpoint 3 | Medição inicial sem portão: [COBERTURA-INICIAL.md](COBERTURA-INICIAL.md) |
| Seções 16 e 21, Checkpoint 4 | Plugin e limites oficiais, clean verify aprovado: [PORTAO-COBERTURA.md](PORTAO-COBERTURA.md) |
| Seção 21, Checkpoint 5 | Falha por limite temporário e aprovação oficial posterior: [FALHA-CONTROLADA.md](FALHA-CONTROLADA.md) |
| Seção 24, itens 1–5 | Domínio, Mockito, PostgreSQL, MockMvc e autorização negativa existentes: [inventário](INVENTARIO-E-ESTRATEGIA.md) |
| Seção 24, itens 6–8 | JaCoCo, baseline e justificativa dos limites: [medição](COBERTURA-INICIAL.md) e [portão](PORTAO-COBERTURA.md) |
| Seção 24, item 9, e seção 25 | ItemVendaTest escolhido por risco, válido/inválidos e antes/depois: [ATIVIDADE-ORIENTADA.md](ATIVIDADE-ORIENTADA.md) |
| Seção 26 | Regra nova de reposição, oráculo e evidências RED–GREEN–REFACTOR sem commits intermediários: [TDD.md](TDD.md) |
| Seções 23 e 29 | Banco de teste explícito nas execuções integradas, evidências sem segredos, relatórios ignorados e README principal atualizado |

Não há pendência técnica obrigatória identificada para a entrega. Mutação,
Testcontainers, regras por pacote e pipeline não foram tratados como entregas
obrigatórias. Commit, tag e push não foram realizados e dependem de autorização.

## Escopo dos checkpoints

**Checkpoint 1 — inventário da suíte**, seção 21 do material oficial, concluído.
O checkpoint pede listar os testes e classificar pelo menos um de cada nível,
registrando unidade observada, infraestrutura carregada, tipo de evidência,
custo esperado, defeito detectável e defeito que o teste não detecta.

**Checkpoint 2 — teste unitário de ContaAcesso**, concluído em 11/11/2026:
classe existente aproveitada, asserções complementadas nos próprios métodos e
execução isolada com 15 testes aprovados, zero falhas, erros e ignorados.

**Checkpoint 3 — medição sem portão**, concluído em 11/11/2026:
JaCoCo 0.8.16, 671 testes aprovados, cobertura inicial de 96,78% das linhas e
90,05% dos branches. Sem limites mínimos e sem goal `check`.

**Checkpoint 4 — plugin e portão**, concluído em 16/11/2026:
`clean verify` aprovado, 671 testes aprovados, `check` global com os mínimos
oficiais de 70% das linhas e 40% dos branches, sem exclusões adicionais.
Cobertura nova: 96,78% das linhas e 90,05% dos branches.

## Documento do checkpoint

- [INVENTARIO-E-ESTRATEGIA.md](INVENTARIO-E-ESTRATEGIA.md): inventário das 54 classes,
  classificação por dependências reais, exemplos de cada nível e estratégia de uso.
- [EVIDENCIAS.md](EVIDENCIAS.md): comparação das regras, alterações mínimas e
  resultado da execução isolada do Checkpoint 2.
- [COBERTURA-INICIAL.md](COBERTURA-INICIAL.md): segurança do banco, comando,
  tentativas, resultados e limitações da medição do Checkpoint 3.
- [PORTAO-COBERTURA.md](PORTAO-COBERTURA.md): limites oficiais, execução segura
  de `clean verify` e aprovação do portão no Checkpoint 4.
- [FALHA-CONTROLADA.md](FALHA-CONTROLADA.md): Checkpoint 5, demonstração com
  mínimo temporário de 91% de branches e aprovação final dos limites oficiais.
- [ATIVIDADE-ORIENTADA.md](ATIVIDADE-ORIENTADA.md): seções 24, item 9, e 25;
  teste de ItemVenda escolhido por risco, comparação e 674 casos aprovados.
- [TDD.md](TDD.md): seção 26, reposição em unidades inteiras, 679 aprovados e
  correção documentada da coleta utilizada pelo portão final.

## Conferência das exigências

| Exigência do Checkpoint 1 | Registro |
|---|---|
| Listar os testes existentes | Inventário completo, com links e contagem de métodos |
| Classificar pelo menos um teste de cada nível | Matriz de níveis: domínio, aplicação unitária, aplicação integrada, persistência, API, contrato, segurança e concorrência |
| Identificar unidade e infraestrutura | Matriz de níveis e classificação das 54 classes |
| Informar o tipo de evidência | Matriz de níveis, coluna de evidência |
| Registrar custo esperado | Escala qualitativa e justificativas, sem estimativas de duração |
| Explicar defeitos detectáveis e limites | Duas colunas específicas na matriz de níveis |

## Evidência histórica do Checkpoint 1

Foi realizada leitura estática dos fontes, annotations, configuração de teste e
documentação anterior. A auditoria anterior foi aproveitada e as contagens de
métodos foram reconferidas. Não houve execução de Maven, JUnit, aplicação ou
migrations, nem conexão ao banco. Os resultados anteriores são identificados
como históricos no inventário.

Foram criados apenas os dois documentos desta pasta. Código Java, testes,
POM, configurações e documentos anteriores foram preservados. Sem commit ou push.

O inventário preserva o registro da etapa 1; seu encerramento refere-se àquele momento.

## Encerramento do Checkpoint 2

Somente o teste de domínio `ContaAcessoTest` foi executado, sem Spring ou banco.
Os nove métodos continuam produzindo 15 casos; nenhuma classe ou cenário duplicado
foi criado. As evidências e limites estão em [EVIDENCIAS.md](EVIDENCIAS.md).
Código de produção, migrations, configurações e POM foram preservados.
Esse registro encerra historicamente o Checkpoint 2; o avanço autorizado ao
Checkpoint 3 está documentado abaixo. Sem commit ou push.

## Encerramento do Checkpoint 3

O POM recebeu somente a versão do JaCoCo, `prepare-agent` e `report`.
A suíte completa passou com instrumentação: 671 casos, zero falhas, erros e ignorados.
O destino PostgreSQL foi confirmado por SQL somente leitura antes da execução e
por configuração explícita e observação durante a suíte: `instrumento2026_test`,
schema `public`, `127.0.0.1:5432`. DataSource e Liquibase utilizaram o mesmo destino.

Relatório local: [HTML JaCoCo](../../target/site/jacoco/index.html), gerado e ignorado
pelo Git. Contadores e detalhes reproduzíveis: [COBERTURA-INICIAL.md](COBERTURA-INICIAL.md).
Código Java, testes e migrations não foram alterados nesta etapa.
Esse é o encerramento histórico do Checkpoint 3; o avanço autorizado consta abaixo.

## Encerramento do Checkpoint 4

DataSource e Liquibase apontaram explicitamente para `instrumento2026_test` em
`127.0.0.1:5432`, schema `public`, após conferência somente leitura. O `clean verify`
executou a suíte completa e o JaCoCo confirmou `All coverage checks have been met.`
O `prepare-agent` e o `report` foram preservados; `check` aplica mínimos de 70%
das linhas e 40% dos branches no bundle. Não houve mudanças em testes ou produção.
Resultados e comando: [PORTAO-COBERTURA.md](PORTAO-COBERTURA.md).

Esse registro encerra o Checkpoint 4; a demonstração posterior consta abaixo.

## Encerramento do Checkpoint 5

Concluído em 16/11/2026. O limite temporário de 90% da referência passou, pois
a cobertura existente é 90,05%. Um override de 91% provocou `BUILD FAILURE`
especificamente no JaCoCo `check`. Sem override, os limites oficiais de 70% de
linhas e 40% de branches foram novamente aprovados em `verify`.

As três verificações usaram `-DskipTests` e a coleta válida do Checkpoint 4:
nenhum teste novo executado ou acesso ao banco. POM e coleta permaneceram idênticos,
com hashes conferidos. Detalhes: [FALHA-CONTROLADA.md](FALHA-CONTROLADA.md).
Esse registro encerra o Checkpoint 5; a atividade posteriormente autorizada consta abaixo.

## Atividade orientada — seções 24 e 25

Concluída em 16/11/2026. `ItemVendaTest`, antes inexistente, acrescenta três casos
sobre vínculos obrigatórios no construtor da entidade. A regra já existia; produção
e configurações foram preservadas. Execução isolada: 3 aprovados; suíte completa:
674 aprovados em 55 classes, zero falhas, erros ou ignorados.

`ItemVenda` permaneceu com 24/24 linhas e 8/8 branches cobertos. A nova evidência
protege rejeições que não eram verificadas diretamente, sem aumento percentual.
Escopo, risco e comandos: [ATIVIDADE-ORIENTADA.md](ATIVIDADE-ORIENTADA.md).
Esse é o encerramento histórico da atividade orientada, anterior ao TDD.

## Atividade TDD — seção 26

Concluída em 16/11/2026: [TDD.md](TDD.md). O teste foi escrito antes do método;
RED falhou pela ausência da operação, GREEN e REFACTOR aprovaram 167 casos de
domínio em cada execução. A suíte final aprovou 679 casos. O portão sobre a coleta
nova foi efetivamente validado após a correção registrada, com mínimos oficiais
de 70% e 40%, sem avisos de incompatibilidade. Não houve commits intermediários.

**Aula 15 pronta para versionamento, aguardando autorização. Sem commit ou push.**

## Revisão documental final

Fechamento realizado sem testes, Maven ou acesso ao PostgreSQL. Os resultados
finais foram conferidos nos logs/XML existentes e nos hashes das coletas.
O README principal aponta para esta aula e mantém links e informações anteriores.
Os registros de etapas anteriores foram identificados como históricos, sem
substituir suas medições pelas atuais.

- Links relativos e âncoras conferidos, sem destino inexistente.
- `git diff --check` e conferência de whitespace dos documentos novos sem erros.
- `git status --short` revisado: cinco arquivos rastreados modificados e nove novos,
  ainda não commitados. As alterações de Java/POM são das etapas anteriores.
- Nenhum `.env`, conteúdo de `target`, log, coleta, JAR ou temporário entre os
  14 candidatos ao commit; `.env` e os artefatos de build continuam ignorados.
- Busca por padrões de chaves/tokens e comparação com valores privados locais
  sem ocorrências nos candidatos. Isso é uma conferência local, não certificação
  de segurança de todo o histórico do repositório.

Nesta revisão somente README/documentos foram alterados. Sem mudanças em Java,
testes, POM, migrations ou configurações. Não há pendência obrigatória documental
identificada; resta a autorização do usuário para o versionamento.
