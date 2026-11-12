# Devoluções do crediário — V25 (16/10/2026)

Devolução parcial ou integral de mercadoria vendida a prazo, inclusive em outro turno aberto da mesma filial. Integra quantidade/custo históricos, abatimento de dívida, reembolso de pagamentos iniciais e reembolso de recebimentos de parcelas. Não executa transferência bancária nem emissão fiscal.

## Composição financeira

O valor histórico devolvido, calculado por item com arredondamento acumulado de V21, é dividido assim:

1. `debtReduction = min(valor devolvido, dívida atual da venda)`.
2. Abatimento automático nas parcelas com vencimento mais distante primeiro; posição decrescente desempata datas iguais.
3. O excedente precisa ser alocado explicitamente a pagamentos iniciais e/ou recebimentos de parcelas já registrados.

`valor devolvido = abatimento da dívida + reembolso dos pagamentos iniciais + reembolso dos recebimentos de parcelas`. Não há troco ou crédito fictício em tesouraria. Devolução de fração com valor arredondado zero pode retornar estoque sem gerar ajuste monetário.

## Títulos e recibos preservados

O valor original do título e o valor bruto de cada recebimento permanecem imutáveis. `receivable.credited_amount` guarda a redução da obrigação, incluindo os valores reembolsados de parcelas; `received_amount` passa a representar o recebido líquido de reembolsos. Logo:

`outstanding = amount - credited_amount - received_amount`.

Abater dívida aumenta `credited_amount` sem alterar dinheiro recebido. Reembolsar uma parcela aumenta `credited_amount` e reduz `received_amount` pelo mesmo valor, evitando reabrir a dívida de uma mercadoria que retornou. O ledger de reembolsos conserva o recebimento POSTED original e seu valor bruto. DTO de recebimento acrescenta `refundedAmount`. Não se deve somar o abatimento total dos títulos aos reembolsos como se fossem eventos independentes: o reembolso da parcela também reduz sua obrigação.

Status financeiro novo `CREDITED`: título liquidado com abatimento (`received_amount + credited_amount = amount`, com crédito positivo). OPEN/PARTIAL continuam indicando saldo pendente; RECEIVED permanece quitação sem abatimento. Receber ou estornar um registro sem reembolso conserva o abatimento existente e recalcula status/saldo. Um recebimento já reembolsado, mesmo parcialmente, não aceita estorno integral; anulação/compensação da devolução ainda não está implementada.

Exposição de crédito, vencidos e projeção de contas a receber descontam `credited_amount`. Indicador de recebimentos sem tesouraria usa o valor líquido de reembolsos dos registros atuais, sem reconstruir sua posição histórica passada. O realizado continua baseado exclusivamente no ledger de tesouraria.

## API e autorização

O `POST /api/v1/pos/sales/{saleId}/returns` mantém campos de V21 e acrescenta:

- `expectedDebtReduction`: obrigatório para crediário, inclusive zero, com até duas casas decimais. Se a divisão dívida/reembolso mudar antes da confirmação, retorna 409 sem gravar.
- `receiptRefunds`: até 100 entradas com `receiptId`, `amount`, `method` CASH/PIX/BANK_TRANSFER, `accountId` opcional para dinheiro e `reference` de reembolso bancário.

Pagamentos iniciais continuam em `refunds` com `paymentId`. Venda sem crediário não aceita recebimentos de parcelas nem abatimento positivo. UUID/fingerprint preservam o contrato legado quando os novos campos estão ausentes. Replay de devolução já concluída é resolvido antes de recalcular a dívida, sem reaplicar abatimento/estoque/reembolso após um recebimento posterior.

Crediário exige `receivable:read` e nova permissão `receivable:credit`, além de `pos:return`, leitura/movimentação de tesouraria e operador/supervisor do caixa escolhido. `pos:credit` continua sendo concessão de crédito, não autorização para abatê-lo. Novas empresas recebem 50 permissões; migração não eleva contas existentes. Empresa/filial são conferidas antes das consultas de origem e de histórico.

`GET /api/v1/pos/sales/{saleId}/returns/credit-receipts?page&size` lista recebimentos POSTED daquele plano, incluindo valor bruto, reembolsado e restante. Exige também `receivable:read`; paginação ocorre no SQL. Recebimento de outra venda/empresa não é origem válida, mesmo que pertença ao mesmo cliente.

`GET /api/v1/receivables/{id}/adjustments?page&size` consulta histórico financeiro com tipo DEBT_CREDIT/RECEIPT_REFUND, valor, data, justificativa e origem. Exige `receivable:read` e acesso à filial do título, sem exigir acesso ao módulo PDV. Listas/get de devoluções de crediário também exigem leitura financeira.

## De onde sai o dinheiro

Pagamento inicial usa as regras de V21: dinheiro sai da gaveta atual; banco usa a conta original.

Recebimento de parcela com vínculo BANK é reembolsado pela conta original, com método bancário, comprovante e confirmação externa. Vínculo CASH usa a gaveta atual. Recebimento sem vínculo à tesouraria exige escolha explícita do meio de reembolso: dinheiro na gaveta atual ou conta BANK autorizada da filial da venda. Essa escolha registra a saída efetiva; não afirma que o recebimento original já havia entrado nessa conta.

Todo reembolso bancário exige confirmação explícita de execução externa. Comprovante é único por empresa/conta/meio entre os dois ledgers de reembolso, inclusive dentro do mesmo comando. Limite reembolsável da parcela é o bruto do recebimento menos reembolsos anteriores. Não altera o caixa antigo nem cria recebimento fictício. Contas/filiais exigem acesso atual e regras de atividade da tesouraria.

## Atomicidade e histórico

Lock da empresa serializa recebimentos, estornos, novas concessões e devoluções. Retorno ao estoque, reembolsos, abatimentos, versões e auditoria pertencem à mesma transação. Falha de saldo, conta, estoque, comprovante, título ou referência desfaz tudo. Títulos atualizam valor líquido, abatimento e status juntos, preservando os checks de integridade. Histórico imutável separa `pos_sale_return_debt_credit` e `pos_sale_return_receipt_refund`; recebimento original permanece consultável.

Cancelamento integral V24 continua disponível no turno original apenas sem devoluções/recebimentos ativos. Após uma devolução, use o fluxo de itens restantes. Venda de crediário totalmente devolvida apresenta `financialStatus=SETTLED_BY_RETURN`; a quantidade/valor devolvidos e os reembolsos efetivos permanecem separados. Em devolução parcial, o estado de pagamento considera o valor dos itens ainda mantidos pelo cliente.

## Interface, migração e limites

PDV mostra valor dos itens, abatimento e reembolso necessário. Seleções de recibos persistem entre páginas e têm resumo completo antes do envio. Resultado incerto mantém comando e UUID em memória com bloqueio de edição/navegação/logout. Alterações de dívida que mudem a divisão exigem atualização. Financeiro mostra original, recebido líquido, abatimentos, restante, valor reembolsado do recibo e histórico paginado dos ajustes.

V25 acrescenta `credited_amount` com zero no legado, adapta o check de liquidação e cria os ledgers e índices. Migração identifica o check pelo conteúdo de status/received_amount, preservando NOT NULL. Upgrade dedicado conserva título parcialmente recebido, recibo bruto, saldos, versões e permissões, sem criar reembolsos ou lançamentos.

Cobertura nova: 20 cenários CreditReturnTest/PostgresIT e um upgrade CreditReturnMigrationTest/PostgresIT. Evidências em `test-evidence/credit-return-*`; resultados finais em TESTING/PROGRESS. Permanecem pendentes emissão fiscal, trocas/fluxos entre filiais, quarentena, política avançada de devoluções e anulação/compensação de devolução já registrada. Login real e revisão visual continuam adiados.

Identidade das fontes: pagamento inicial e recibo financeiro são registros independentes e podem compartilhar UUID. A validação de duplicatas usa conjuntos separados; a unicidade de comprovante bancário continua compartilhada. Cenário específico cobre reembolso conjunto de entrada e parcela com UUID igual, sem confundir os vínculos ou duplicar dinheiro.
