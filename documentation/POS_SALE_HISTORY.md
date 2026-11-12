# Consulta do histórico de vendas — implementação

Implementação de 11/11/2026. Testes funcionais, de integração e de browser deixados para a etapa final por orientação do usuário. Não houve conexão ao banco, implantação ou migração nesta entrega.

## Funcionalidade

O histórico do PDV possui formulário de busca por identificador completo de venda, SKU/nome histórico dos itens ou nome atual do cliente do crediário. Permite combinar período de venda, situação comercial, meio usado na conclusão e presença/ausência de crediário. Os filtros só mudam os resultados após **Aplicar filtros**. **Limpar filtros** retorna à consulta geral; **Atualizar resultados** conserva a consulta aplicada.

A lista apresenta situação comercial, financeira e fiscal, total original da venda, saldo atual do crediário e valor reembolsado. **Ver itens e pagamentos** abre o detalhamento existente. A situação fiscal continua `NOT_ISSUED`; esta entrega não implementa emissão ou integração fiscal. Valores exibidos pertencem a cada venda: não são totalizadores financeiros do período nem valores recebidos dentro do intervalo selecionado.

O período considera o momento da venda em `America/Sao_Paulo`, incluindo o dia final inteiro. Datas na lista também usam essa zona, inclusive o histórico de horário de verão. Recebimentos ou devoluções realizados em outra data não mudam a data original da venda. A situação financeira apresentada é a situação atual consultada, não uma reconstrução histórica na data final do filtro.

## API

`GET /api/v1/pos/sales` mantém `page`/`size` e aceita parâmetros opcionais:

| Parâmetro | Significado |
|---|---|
| `from`, `to` | Datas ISO `yyyy-MM-dd`, intervalo aberto em qualquer lado; anos 0001–9998; inicial não pode ultrapassar final |
| `query` | Até 160 caracteres; identificador completo da venda ou trecho de SKU/nome de item ou nome atual do cliente do crediário; sem diferenciar maiúsculas/minúsculas; sem promessa de ignorar acentos |
| `commercialStatus` | `COMPLETED`, `CANCELLED`, `PARTIALLY_RETURNED`, `RETURNED`; vazio ou omitido para todas |
| `method` | `CASH`, `PIX`, `BANK_TRANSFER`; meio de pagamento registrado na conclusão, mesmo que reembolsado posteriormente |
| `credit` | `true` com plano de crediário; `false` sem plano; omitido para ambos |
| `branchId` | Filial acessível ao usuário; filtro disponível na API, sem seletor adicional na interface desta etapa |

Os parâmetros combinam por AND; os campos pesquisados por `query` combinam por OR. Percentual, sublinhado e exclamação na busca são tratados literalmente, sem permitir curingas SQL fornecidos pelo usuário. A entrada não é concatenada no SQL: valores ficam nos parâmetros vinculados.

`COMPLETED` significa venda concluída sem devolução. Devoluções parciais/integral são derivadas das quantidades devolvidas por item. Cancelamento integral continua como `CANCELLED`, não como devolução. O filtro de meio não inclui os recebimentos posteriores das parcelas do crediário; uma venda totalmente a prazo, sem entrada, não aparece ao filtrar um desses meios iniciais.

Escopo por empresa/filial e filtros são aplicados no SQL antes da contagem e paginação, usando os mesmos critérios. A filial explícita exige autorização. Ordenação continua por criação decrescente e identidade para desempate. O contrato da página e os parâmetros antigos permanecem disponíveis. A leitura mantém a transação de snapshot consistente entre contagem e seleção.

A interface evita que respostas de consultas antigas substituam uma busca mais recente. Ao aplicar filtros, retira a lista anterior enquanto consulta a nova página. Alterar os campos sem aplicar não altera os critérios usados pela paginação.

## Pendências da etapa final de testes

Validar combinações de filtros, datas limítrofes e horário de verão, isolamento/revogação por filial/empresa, consulta de filial explícita, texto com acentos e curingas literais, SQL em H2 e PostgreSQL, compatibilidade da listagem sem parâmetros, devoluções acumuladas/integral de itens gratuitos, cancelamento, entrada versus recebimento posterior do crediário, paginação/contagem e respostas assíncronas fora de ordem. Medir desempenho com volume representativo antes de decidir novos índices.

Não foram escritos ou executados testes nesta rodada. Compilação e empacotamento são verificações separadas de funcionamento: não comprovam a execução do SQL ou a navegação na interface. Login real e revisão visual seguem adiados.
