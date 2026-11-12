# Consulta de contas a receber — implementação de 11/11/2026

Busca por cliente registrado no título, referência, documento, descrição ou identidade completa; período de vencimento; situação; saldo vencido e tipo de origem. A consulta distingue título avulso, parcelamento manual e crediário de venda, sem recalcular ou alterar obrigações financeiras.

## Origem e navegação

No detalhe de uma parcela, a interface mostra referência do grupo, posição e quantidade de parcelas, com **Ver todas as parcelas desta origem**. Essa ação limpa os demais filtros e consulta o grupo inteiro, incluindo títulos recebidos, cancelados e liquidados com abatimento. O link de uma parcela no PDV abre Contas a receber com esse título e o parcelamento já filtrado. **Retirar filtro do parcelamento** amplia a consulta mantendo os outros campos aplicados por essa ação; **Limpar filtros** volta à consulta geral.

Não há autorização nova para ler detalhes da venda: a consulta da origem exige `receivable:read`, verifica empresa e filial do título e devolve apenas identificadores/referência e posição do grupo. Uma venda cancelada continua sendo a origem histórica do seu crediário. Um parcelamento manual de uma única parcela continua classificado como parcelamento, não como título avulso.

## API

`GET /api/v1/receivables` mantém `status`, `overdue`, `page`, `size` e aceita:

| Parâmetro | Significado |
|---|---|
| `query` | Até 200 caracteres; trecho literal de cliente histórico/referência/documento/descrição ou identidade completa do título, sem diferenciar maiúsculas/minúsculas; sem promessa de ignorar acentos |
| `dueFrom`, `dueTo` | Datas ISO inclusivas de vencimento, uma ou ambas opcionais; anos 0001–9999; inicial não pode ultrapassar final |
| `branchId`, `customerId` | Identidades exatas; filial explícita exige autorização; disponíveis na API, sem novo seletor na interface desta entrega |
| `planId` | Grupo exato de parcelas; usado pela navegação de origem |
| `saleId` | Recebíveis pertencentes ao plano de crediário da venda indicada |
| `origin` | `SINGLE` sem grupo; `PLAN` grupo sem venda associada; `POS_CREDIT` grupo vinculado a venda; vazio/omitido para todos |

`GET /api/v1/receivables/{id}/origin` devolve `{planId,position,installmentCount,saleId,reference}`; campos nulos para título avulso. Essa leitura não devolve valores da venda ou autorização fiscal.

Os filtros combinam por AND; combinações contraditórias retornam zero resultados. O escopo de empresa/filiais e os mesmos critérios são aplicados antes de contar, somar e paginar. Consultas de origem usam EXISTS para não multiplicar títulos nem totais. Texto é parametrizado, e `%`, `_`, `!` são tratados literalmente. `overdue` inclui apenas OPEN/PARTIAL com vencimento anterior à data financeira atual. Ordenação permanece por vencimento/identidade.

## Totais e devoluções

A página acrescenta `summary` com strings monetárias, `today` e `zone`:

- `originalAmount`: valores originais de todos os resultados, inclusive cancelados.
- `receivedAmount`: recebido registrado líquido dos estornos/reembolsos já aplicados ao título.
- `creditedAmount`: abatimentos registrados nos títulos pelas devoluções, sem tratá-los como dinheiro recebido.
- `outstandingAmount`: original menos recebido líquido e abatido, excluindo cancelados.
- `overdueAmount`: parte do saldo a receber já vencida em OPEN/PARTIAL.

Um exemplo: título de R$ 100 recebido integralmente e depois ajustado por devolução de R$ 20 pode apresentar recebido líquido R$ 80, abatido R$ 20 e pendente zero. Não se soma o abatimento ao dinheiro recebido. Os detalhes dos ajustes continuam no histórico existente.

Os totais cobrem todos os resultados, não apenas a página. São posições atuais dos títulos; filtrar vencimento não transforma o recebido em fluxo de entradas dentro daquele período. No crediário, o recebível inclui apenas o valor financiado, sem a entrada paga na conclusão da venda. Registros sem vínculo de tesouraria também entram no recebido registrado: a consulta não comprova movimentação bancária.

Contagem, totais e página usam uma transação de leitura consistente e uma única data financeira para classificar atrasos. A interface preserva os filtros aplicados ao paginar e evita que respostas antigas substituam uma consulta mais recente. Alterar campos sem aplicar não altera os critérios da página.

## Validação adiada

Backend/JAR compilado com `-Dmaven.test.skip=true`; build Angular aprovado. Nenhum teste escrito/executado, SQL executado, banco acessado, migração, container ou implantação nesta etapa. V27/V28 de compras seguem não aplicadas; esta entrega não acrescenta schema. Login real e revisão visual continuam adiados.

Na etapa final, validar escopo/permissões, origem avulsa/manual/crediário, grupos unitários, cancelamento da venda, abatimento de dívida e reembolso de recebimento, totais acima de uma página e sem dupla contagem, saldo CREDITED, datas inclusivas/virada financeira, filtros contraditórios, curingas/acentos, links por origem/título, SQL H2/PostgreSQL e comportamento de rede fora de ordem.

Evidências de compilação: `test-evidence/receivable-search-backend-build.txt`, `receivable-search-frontend-build.txt` e `receivable-search-build-only.json`. Compilar não valida o SQL ou a navegação funcional.
