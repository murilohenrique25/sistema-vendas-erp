# Pagamentos conferidos e cancelamento integral — V20

Escopo implementado: dividir uma venda entre dinheiro, Pix e transferência já recebidos e conferidos pelo operador; manter cada parcela e cancelar integralmente no turno original aberto. Não executa cobrança, consulta bancária, Pix automático, cartão ou estorno externo. Não representa a conclusão de todo o módulo de pagamentos mistos exigido no mestre.

## Contrato de conclusão

`POST /api/v1/pos/sales` mantém requestId, sessionId e items. O formato V19 com tendered continua aceito como uma parcela em dinheiro, com fingerprint original preservado. Alternativamente envie payments (1–100 parcelas) e omita tendered do cabeçalho. Cada parcela contém:

- method: CASH, PIX ou BANK_TRANSFER;
- amount: valor aplicado à venda, positivo, 17 inteiros/2 decimais;
- tendered: recebido nessa parcela; opcional, assume amount;
- accountId: obrigatório para bancários; dinheiro usa a conta da sessão;
- reference: comprovante obrigatório em bancários, até 160 caracteres;
- confirmed: deve ser true em bancários, após conferência externa do recebimento.

A soma aplicada deve ser exatamente o total. Valores ainda pendentes não concluem a venda nem geram estoque/receita. Não há venda parcialmente paga persistida nesta etapa. Há no máximo uma parcela em dinheiro; apenas ela pode ter troco. Pix/transferência exigem conta BANK da filial da sessão, acesso atual e recebido igual ao aplicado. O cabeçalho mantém total recebido agregado e troco. Cada parcela credita somente seu valor aplicado na conta correspondente. Venda somente bancária não altera o saldo físico do caixa, mas incrementa a versão da sessão.

UUID/fingerprint protegem o comando completo; replay retorna estado atual sem reaplicar pagamentos ou estoque, inclusive após cancelamento. Contas informadas são revalidadas no replay. Comprovante normalizado por trim é único por empresa/conta/meio, inclusive depois de reembolsado; não pode financiar outra venda. A referência é informação declarada: o sistema ainda não verifica autenticidade junto a banco ou provedor.

`pos_sale_payment` preserva método, conta, aplicado, recebido, troco, referência, situação CONFIRMED/REFUNDED e IDs das operações de recebimento/reembolso. `pos_sale.treasuryOperationId` é mantido por compatibilidade e aponta para a primeira parcela; payments é a fonte de todas as parcelas. Consultas retornam strings decimais e estados separados commercialStatus (COMPLETED/CANCELLED), financialStatus (PAID/REFUNDED), fiscalStatus (NOT_ISSUED). Não há documento fiscal emitido por este fluxo.

## Cancelamento integral

`POST /api/v1/pos/sales/{id}/cancel`: requestId, version, note (5–500 caracteres após trim) e externalRefundsConfirmed. Exige pos:read, pos:cancel, treasury:read/move, filial atual e operador da sessão ou pos:supervise. Caixa original deve estar aberto. Reenvio idêntico do cancelamento original funciona após fechar, com permissões e acesso às contas ainda válidos. Outra chave/versão/dados não repete retorno ou reembolso.

Quando há parcelas bancárias, o operador deve declarar que realizou/conferiu os reembolsos externamente. A API não chama o banco nem o provedor. Caixa debita apenas a parcela aplicada em dinheiro; cada banco debita a parcela aplicada original. Saldo insuficiente em qualquer conta rejeita tudo e exige regularização. Referências e valores originais nunca são apagados; pagamentos passam a REFUNDED e recebem reversalOperationId; origens financeiras são POS_SALE/POS_SALE_REVERSAL.

Itens retornam integralmente ao depósito original por ADJUSTMENT_IN, namespace POS_SALE_RETURN, usando o custo de saída original e recompondo a média ponderada atual. Produto inativo pode voltar, mas unidade diferente ou depósito inativo impedem o retorno para evitar conversão implícita. Cada item guarda return_operation_id. Venda recebe status, versão, nota, ator e instante de cancelamento. Itens/preços originais e operações de saída permanecem preservados.

Todos os retornos, débitos, estados, versão do caixa e auditoria compartilham transação/lock da empresa. Produtos em ordem de UUID evitam ordens de lock conflitantes. Se cancelamento vence o fechamento, incrementa versão e invalida a contagem anterior; se fechamento vence, o cancelamento novo é rejeitado. Não há correção automática de contagem divergente.

## Migração e interface

V20 acrescenta tabelas/campos/constraints e converte cada venda V19 existente em uma parcela CASH, reutilizando a operação financeira original, sem repostar valores nem conceder permissões. Whitelist/provisionamento novo inclui pos:cancel (42 permissões); usuários existentes precisam concessão explícita por gestor autorizado.

Tela /pos-sales mantém dinheiro simples e oferece divisão em parcelas, contas paginadas, comprovante e confirmação; calcula aplicado e saldo a cobrir com centavos inteiros. Histórico mostra pagamentos e situação, e permite cancelamento autorizado no turno aberto. Resultado incerto mantém UUID/payload em memória e bloqueia edição/navegação/logout até confirmação. Login real e revisão visual permanecem adiados.

## Pendências preservadas

Pagamentos parciais persistidos, autorização/captura assíncrona, recusa/expiração, cartões/TEF/adquirentes/parcelas/taxas/liquidação, Pix por provedor/webhooks e verificação de assinatura, conciliação transacional automática, crédito/crediário/vouchers, devoluções parciais e após fechar o turno, descontos/alçadas/fiscal/offline/hardware. Essas funções exigem evolução de estados e integrações; não devem ser simuladas como pagamentos reais.

## Devoluções — V21 (15/10/2026)

V21: devoluções parciais e em outro turno implementadas, com limites acumulados por pagamento e histórico independente. Campo status mantém recebimento original; refundedAmount/refundStatus mostram reembolsos. Integrações externas, fiscal e fluxos entre filiais continuam pendentes. Ver POS_RETURNS.md.

## Descontos e suspensão — V22 (15/10/2026)

V22: recebimentos e troco usam o total líquido após descontos. Aprovação e suspensão só são consumidas junto ao sucesso de estoque/recebimento; falha mantém autorização/carrinho disponíveis. Pagamentos não são armazenados nem executados na suspensão.
