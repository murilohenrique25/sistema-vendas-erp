# Pedidos e recebimentos — marco inicial

Fornecedores são compartilhados por empresa. Pedidos pertencem a uma filial autorizada e guardam nome do fornecedor, SKU/nome/unidade base dos produtos, quantidades, custos negociados e totais. Esses valores não mudam quando o cadastro é alterado. Pedido criado fica OPEN; não há reserva ou entrada de estoque na criação.

Até 100 produtos distintos por pedido. Quantidade positiva e custo não negativo usam BigDecimal/NUMERIC(19,6). Total de cada linha = quantidade × custo, HALF_UP em duas casas; total do pedido é a soma desses totais. Limite monetário NUMERIC(19,2) validado antes da gravação. Valores trafegam como strings; Angular não calcula saldos ou dinheiro com ponto flutuante. Não há conversão de embalagem/moeda nem cálculo de frete/impostos nesta etapa.

## Recebimento

O cliente informa receiptId, depósito, justificativa/documento informado e quantidades por item. Depósito precisa estar ativo e pertencer à filial do pedido; filial deve estar autorizada. Produtos/unidades do pedido são usados pelo servidor; o cliente não redefine produto ou custo do recebimento. Produto inativo ou unidade base alterada impede nova entrada.

Pedido é bloqueado antes de verificar saldo pendente. Quantidade recebida nunca ultrapassa a encomendada. Itens repetidos e referências a outro pedido são rejeitados. Recebimentos parciais passam a PARTIAL; quando todos os itens foram recebidos, estado RECEIVED. Cada item cria operação e movimento RECEIPT no estoque; vínculo receipt_line/stock_operation permite rastrear a entrada. Custo negociado entra no custo médio ponderado do depósito.

Pedido, recebimento, itens recebidos, operações, ledger, saldo/custo médio e auditoria pertencem à mesma transação. Se qualquer item falhar, tudo é desfeito. Produtos são bloqueados em ordem estável de UUID para evitar inversão entre pedidos com múltiplos produtos. Recebimentos do mesmo pedido são serializados, evitando ultrapassar saldo pendente.

## Idempotência

Criação exige requestId único por tenant, com fingerprint de filial/fornecedor/notas/itens ordenados. Repetir o mesmo conteúdo, inclusive escalas decimais equivalentes e ordem diferente de itens, retorna o pedido existente no estado atual. Chave usada com outro conteúdo retorna 409. Criação concorrente usa lock do tenant; recebimentos não usam esse lock.

Recebimento exige receiptId único por tenant. Fingerprint inclui pedido/depósito/nota/itens ordenados. Retry idêntico retorna o recebimento original, inclusive depois de o pedido ser encerrado, sem nova entrada/auditoria. Chave reutilizada com outro conteúdo retorna 409. Operação de estoque usa identidade determinística por tenant/receiptId/item e fingerprint de namespace PURCHASE_RECEIPT, diferente do movimento manual. Colisão com operação manual causa rollback; não pode ser tratada como recebimento válido. stock_operation.kind continua MOVEMENT; origem de compra é identificada pelo vínculo persistido.

No Angular, comandos de criação/recebimento/cancelamento ficam preservados em memória enquanto a resposta é incerta (rede/5xx). Formulários, navegação e logout são bloqueados até repetir o mesmo comando. beforeunload avisa sobre saída. Não é outbox durável: fechar o processo pode perder o comando local. Erro 4xx definitivo libera a edição.

## Cancelamento

OPEN/PARTIAL podem ser encerrados como CANCELLED com justificativa e versão atual. Cancelamento fecha apenas quantidades ainda pendentes; preserva recebimentos, estoque e histórico. Não é devolução nem estorno. Mesma justificativa de cancelamento já aplicado retorna o pedido sem nova auditoria. RECEIVED não aceita cancelamento. Em disputa entre receber e cancelar, o lock do pedido define a ordem; versão antiga é rejeitada.

## Permissões e limites

purchase:read consulta pedidos/preços negociados e histórico na filial autorizada. purchase:write, junto com read, cria/cancela; purchase:receive, junto com read, recebe. Não exige inventory:write para entrada vinculada a pedido; ajustes/manuais continuam restritos ao inventory:write. A interface usa branch:read, supplier:read, product:read e inventory:read para seus seletores. Custos médios do estoque seguem inventory:cost; preços negociados pertencem à consulta de compras.

Listas/totais são filtrados antes da paginação. Pedidos de outro tenant/filial não autorizada retornam 404, inclusive histórico, cancelamento e replay. Novos privilégios entram somente no provisionamento de novas empresas; nenhuma migration eleva usuários existentes.

Pendente: cotação/comparação, aprovação/alçadas, rascunho/edição de itens, previsão/entrega, frete/impostos/rateio, divergência de custo na nota, conversões, lotes/validade, XML fiscal, devolução/estorno, contas a pagar/conciliação e homologação de operação real. Documento informado na conferência é texto, sem importação fiscal. Esta etapa não gera obrigação financeira ou documento fiscal automaticamente.
