Tema do projeto

Documentação atual: [Aula 15 — Testes, cobertura e TDD](docs/aula15/README.md).
Fechamento em 16/11/2026: 679 testes aprovados, cobertura de 96,79% das linhas e
90,05% dos branches. Portão JaCoCo aprovado sobre a coleta final, com mínimos
oficiais de 70% e 40%. Checkpoints, atividade por risco e RED–GREEN–REFACTOR
estão documentados; os resultados anteriores permanecem identificados como históricos.

Histórico recente:

- [Aula 14 — Autorização por perfis](docs/aula14/README.md).
- [Aula 13 — JWT e Bearer Token](docs/aula13/README.md).
- [Aula 12 — Autenticação e proteção de senhas](docs/aula12/README.md).
- [Aula 11 — Concorrência](docs/aula11/README.md).

O login em `POST /api/autenticacao/login` emite JWT HS256 para as operações protegidas,
substituindo HTTP Basic. Consulte a documentação da Aula 13 para configurar `JWT_SECRET` localmente,
executar a aplicação, testar Bearer e rodar Newman, sem publicar segredos ou tokens.
As descrições do tema original abaixo foram preservadas.

Identificação

- Nome do projeto: instrumento2026
- Tema: controle de instrumentos musicais de uma loja
- Objetivo em uma frase: cadastrar instrumentos musicais e organizá-los por categoria

Entidade de classificação

- Nome no singular: CategoriaInstrumento
- Nome no plural:CategoriaInstrumentos
- Exemplo 1: Cordas
- Exemplo 2: Madeiras

Entidade principal

- Nome no singular: Instrumento
- Nome no plural: Instrumentos
- Código único: código do instrumento
- Descrição: nome do instrumento
- Medida quantitativa: quantidade em estoque
- Valor monetário: preço unitário
- Data relevante: data cadastro
- Status: ativo ou inativo

Relacionamento

-Uma categoria pode possuir vários instrumentos
-Cada instrumento pertence a uma categoria

Três exemplos de registros

1.
Código único:INT001
Descrição: Violoncelo Eagle
Categoria: Cordas
Quantidade em estoque: 05
Preço unitário: 5.000,00
Data relevante: 15/06/2026
Status: ativo

2.
Código único:INT002
Descrição: Sax Soprano Yamaha
Categoria: Metais
Quantidade em estoque: 08
Preço unitário: 1.500,00
Data relevante: 30/04/2026
Status: ativo

3.
Código único:INT003
Descrição: Clarinete Michael
Categoria: Madeiras
Quantidade em estoque: 03
Preço unitário: 2.500,00
Data relevante: 03/02/2026
Status: ativo
