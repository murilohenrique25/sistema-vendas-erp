# Devoluções parciais e em outro turno — V21

O fluxo registra devolução física de itens, retorno ao depósito original e reembolso vinculado aos pagamentos da venda. Funciona no turno da venda ou em outro caixa/turno aberto da mesma filial. Não reabre o turno original nem altera sua contagem congelada. É independente do cancelamento integral V20, que continua restrito ao turno original e não pode ser usado depois de uma devolução, mesmo de valor zero.

## Contratos

`POST /api/v1/pos/sales/{saleId}/returns` recebe requestId, sessionId (caixa aberto que registra/reembolsa), version (da venda), note (5–500 caracteres), externalRefundsConfirmed, items (1–100 produtos distintos com productId/quantity positiva, 13 inteiros/6 decimais) e refunds (0–100 parcelas distintas com paymentId/amount positivo, 17 inteiros/2 decimais/reference até 160 caracteres).

`GET /api/v1/pos/sales/{saleId}/returns?page=0&size=20` e `GET /api/v1/pos/sales/{saleId}/returns/{id}` consultam o histórico imutável. Consultas exigem pos:read + treasury:read e escopo da filial da venda. Criação também exige pos:return + treasury:move, acesso atual às contas e operador do caixa escolhido ou pos:supervise. Outra filial é rejeitada. Nova permissão entra somente no provisionamento de novas empresas (43 permissões), sem elevação de usuários existentes.

## Quantidades, valores e custo

A quantidade acumulada devolvida nunca excede a vendida. O valor por item é a diferença entre o valor acumulado proporcional e o que já foi reembolsado por aquele item:

`valor desta devolução = HALF_UP(total histórico do item × quantidade acumulada devolvida / quantidade vendida, 2 casas) − valor já devolvido do item`.

Isso preserva exatamente o subtotal originalmente cobrado na devolução completa e impede criar centavos extras ao fracionar devoluções. Por exemplo, 2 kg a R$ 10,25 totalizam R$ 20,50: devolver 0,5 kg, mais 0,5 kg e depois 1 kg gera R$ 5,13 + R$ 5,12 + R$ 10,25. Fração de quantidade pode corresponder a zero centavo; nesse caso há retorno de estoque, sem lançamento financeiro e sem parcelas de reembolso.

Itens retornam ao depósito original, por ADJUSTMENT_IN e namespace POS_SALE_RETURN, ao custo unitário histórico da saída, recompondo o custo médio atual. Produto inativo pode voltar; unidade alterada e depósito inativo são rejeitados para evitar conversão implícita. O ledger referencia a devolução e a venda; saídas, preços e custos históricos permanecem preservados. Não há retorno para outro depósito/filial nesta etapa.

## Reembolsos

A soma dos reembolsos deve igualar exatamente o valor calculado dos itens. Cada parcela referencia um pagamento da própria venda e não pode superar seu saldo reembolsável, descontadas todas as devoluções anteriores. O operador escolhe a alocação entre os pagamentos originais; não pode converter um pagamento bancário em dinheiro nem usar pagamento de outra venda.

Dinheiro debita a conta do caixa aberto escolhido, inclusive se for outro terminal, respeitando saldo e titular/supervisor; a parcela continua vinculada ao pagamento original. Pix/transferência debitam a conta bancária original e exigem confirmação do reembolso realizado externamente e comprovante novo. Comprovante é único por empresa/conta/meio; não há conexão, verificação de autenticidade ou estorno remoto no provedor. Saldo insuficiente em qualquer conta desfaz toda a devolução. Origem financeira POS_SALE_RETURN aparece na tesouraria/conciliação e compõe saídas no fluxo de caixa.

Pagamento conserva seu status de recebimento original (CONFIRMED/REFUNDED do cancelamento legado). A projeção refundedAmount e refundStatus (NONE/PARTIAL/FULL) informa o saldo de reembolsos de forma independente, com histórico de todas as operações. Venda conserva status original e apresenta commercialStatus COMPLETED/PARTIALLY_RETURNED/RETURNED/CANCELLED, financialStatus PAID/PARTIALLY_REFUNDED/REFUNDED e fiscalStatus NOT_ISSUED. Itens expõem returnedQuantity/returnedAmount; valores decimais são strings JSON.

## Atomicidade e concorrência

Tenant lock serializa devolução, conclusão, cancelamento e fechamento; produtos são bloqueados em ordem de UUID, compartilhando a proteção do estoque. A transação inclui itens, saldo/custo/ledger, débitos, histórico, versão da venda, versão do caixa atual e auditoria. Falha em qualquer item ou reembolso desfaz tudo. Venda e caixa recebem versão nova, invalidando comandos/contagens anteriores.

UUID/fingerprint preservam reenvio idêntico, inclusive depois de fechar o caixa do reembolso. Escopo, operador/supervisor e contas são revalidados antes de replay. Chave com outro conteúdo/venda gera conflito. Devolução da venda cancelada, excesso de quantidade ou de valor, versão desatualizada e comprovante repetido são rejeitados. Venda totalmente devolvida não pode gerar novos retornos. Cancelamento integral depois de qualquer retorno é bloqueado; utilize apenas os itens remanescentes.

## Interface e pendências

Seção de devoluções dentro de /pos-sales oferece seleção paginada de caixa, quantidade por item, cálculo acumulado com inteiros, saldo por pagamento, alocação de reembolsos/comprovantes e histórico paginado. Payload/UUID incerto fica em memória, bloqueando edição/navegação/logout; não é outbox durável. Comandos de venda/cancelamento e de devolução se bloqueiam mutuamente na tela. Login real e revisão visual seguem adiados.

Ainda faltam documento fiscal de devolução, integração de reembolsos com provedores/cartões, devolução entre filiais/depósitos, inspeção/quarentena/avaria, troca/vale/crédito em loja, alçadas e políticas comerciais. Uma devolução deste fluxo pressupõe que a mercadoria voltou fisicamente e está apta a retornar ao estoque; itens avariados precisam tratamento específico, sem confirmação fictícia.

## Descontos e suspensão — V22 (15/10/2026)

V22: returnedAmount e limites de reembolso usam subtotal líquido histórico do item, com desconto já preservado. Cálculo acumulado existente impede devolver valor bruto superior ao cobrado.

## Ampliação V25 — crediário

Devoluções de venda a prazo com abatimento de dívida e reembolso de entrada/parcelas, inclusive em novo turno da mesma filial, estão em POS_CREDIT_RETURNS.md. Permissão adicional receivable:credit/read. O valor total devolvido não deve ser tratado integralmente como saída de dinheiro: parte pode ser abatimento de dívida.
