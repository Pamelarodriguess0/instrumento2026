# Aula 16 — Ciclo de vida dos tokens

Material: [Aula 16 do professor Jefferson](https://github.com/jeffersonarpasserini/suporteos2026/blob/main/docs/16aula/16aula.md).
Projeto: instrumento2026. Ponto inicial: commit
`ebe61ba2b57c7fcec8a8a0dad67a7447422786c2`, tag `aula-15-testes-cobertura`.
Início em 16/11/2026; atualização em 18/11/2026.

## Escopo autorizado

**Atualização: validações concluídas.** Os três checkpoints foram validados;
o ciclo HTTP integrado passou com 16 casos e os testes isolados OpenAPI/contrato
com 63 casos. Em 17/11, a execução real Newman terminou com 40 requisições,
108 asserções e zero falhas. Em 18/11, o clean verify terminou com BUILD SUCCESS:
771 testes aprovados, zero falhas, erros ou ignorados. O JaCoCo registrou
96,85% LINE e 90,83% BRANCH e aprovou os limites de 70% e 40%, respectivamente.
Esses resultados são de execuções anteriores; não foram repetidos nesta revisão.
Os parágrafos seguintes preservam o histórico das etapas iniciais.

**Ponto A e Checkpoint 1 validados.** A preparação de 16/11 não acessou o banco.
Na execução posteriormente autorizada de 17/11, às 15:15:52 -03:00, os nove testes
de persistência passaram. Liquibase aplicou três changeSets no banco de teste,
preservando os 41 anteriores, totalizando 44. Host `127.0.0.1:5432`, banco
`instrumento2026_test`, schema `public`; Hibernate validou o schema.

**Ponto B concluído.** Propriedades tipadas e repository implementados;
11 testes de configuração, um de consultas sem JDBC e quatro de repository
aprovados. Sem nova migration ou suíte completa. Login e JWT preservados.

**Checkpoint 2 validado.** As regras já existiam no modelo. Foi complementada
somente a evidência unitária: última atividade distinta da criação e preservação
do uso após revogação. Dez casos de SessaoRefreshTokenTest aprovados, zero falhas,
erros ou ignorados, em 17/11/2026 às 15:31:33 -03:00. Sem Spring ou PostgreSQL;
nenhuma alteração na entidade.

- [modelo.md](modelo.md): mapeamentos,
  regras, constraints, diferenças da referência e evidências.
- [repositorio.md](repositorio.md):
  propriedades, queries, transações, testes e limites da evidência.
- [dominio.md](dominio.md): comparação das regras,
  complementos dos testes e execução unitária isolada.
- [Marco anterior — Aula 15](../aula15/README.md): evidências históricas preservadas.
- [sessoes.md](sessoes.md): emissão, rotação,
  rollback/commit e concorrência com PostgreSQL.
- [api.md](api.md): contratos HTTP,
  histórico das correções e 16 casos integrados aprovados.
- [postman.md](postman.md): OpenAPI, coleção, runner, validação offline e
  resultados da execução real Newman.
- [validacao.md](validacao.md): histórico das tentativas, correções e aprovação
  da suíte completa, cobertura e preservação das fixtures.

Emissão, rotação, services de sessão, endpoints, logout e sid foram implementados
nas etapas posteriores às validações iniciais. A documentação acima distingue
resultados históricos de resultados finais. Esta revisão alterou somente a
documentação, sem executar testes, Newman ou acessar o banco.
**O encerramento e o versionamento aguardam revisão e autorização do usuário.**

## Decisão arquitetural — seção 25

O projeto não inclui um cliente web. Para esse cliente, a decisão é manter o
access token e o refresh token apenas em memória durante a sessão, sem gravá-los
em localStorage, URLs ou arquivos. Ao fechar ou recarregar o cliente, será
necessário autenticar novamente. Isso reduz a persistência de credenciais no
dispositivo, mas não impede roubo por código malicioso executado no cliente.
É uma decisão para o cliente, não uma proteção implementada pelo backend.
Na demonstração existente, o Postman usa variáveis da coleção em execução;
tokens e senhas não devem ser exportados ou sincronizados.

Foram mantidos os padrões da aula: access token de 30 minutos, inatividade do
refresh de 30 minutos e duração absoluta da família de oito horas. O primeiro
prazo limita o tempo de uso de um JWT roubado sem exigir consulta por requisição;
o segundo encerra a renovação de sessões ociosas; o terceiro exige nova
autenticação mesmo com renovações frequentes. São escolhas didáticas para este
projeto, não limites universais de segurança. Cada renovação reinicia a janela
de inatividade da nova geração, sem deslocar o limite absoluto original.

O logout individual revoga uma família; o global revoga as famílias da própria
conta autenticada. O access token já emitido continua válido até expirar, pois
sua validação é local e stateless. A rotação reduz a possibilidade de reutilizar
um refresh roubado: cada geração só pode renovar uma vez; reapresentá-la revoga
a família inteira, inclusive o sucessor, porque não é possível distinguir qual
cliente está com a credencial comprometida.

Duas renovações do mesmo refresh são serializadas por lock pessimista na linha.
A primeira pode criar o sucessor; a segunda encontra a geração utilizada e
revoga a família por reutilização. O cliente deve coordenar uma única renovação
por vez. O teste transacional confirmou esse caso no PostgreSQL, com um sucessor
e posterior revogação da família; não comprova todas as corridas possíveis.
