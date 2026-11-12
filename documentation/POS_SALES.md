# Vendas com itens — V19

Venda concluída em dinheiro, vinculada a sessão aberta e depósito do terminal. Não existe rascunho persistido/reserva nesta etapa: o carrinho fica em memória e a baixa ocorre somente ao concluir. Login real e revisão visual permanecem adiados por solicitação do usuário.

## Depósito

`PUT /api/v1/pos/terminals/{id}/warehouse` recebe warehouseId e version. Exige pos:read, treasury:read, pos:manage e inventory:read; depósito ativo e da mesma filial; terminal sem caixa aberto. Versão impede alteração concorrente. Terminais anteriores continuam com warehouseId nulo até configuração explícita, sem atribuição automática de depósitos.

## Conclusão

`POST /api/v1/pos/sales` recebe requestId, sessionId, tendered e items (1–100 produtos distintos, productId, quantity, expectedUnitPrice). Quantidade positiva até 13 inteiros/6 decimais; dinheiro/preço até 17 inteiros/2 decimais. Agrupe produtos repetidos. Exige pos:read, pos:sell, treasury:read/move e acesso à filial; operador responsável ou pos:supervise. A venda não exige inventory:write, que autoriza ajustes manuais independentes. Produto/filial/depósito ativos e caixa aberto são conferidos no servidor.

Preço vem do cadastro, nunca do valor informado como autoridade. expectedUnitPrice é apenas conferência: divergência retorna 409 sem efeitos, exigindo atualização do carrinho. Subtotal = quantidade × preço, HALF_UP para duas casas por item; total é soma dos subtotais, positivo e dentro do limite monetário. Dinheiro recebido deve cobrir o total. Troco = recebido − total; crédito líquido no caixa é total, evitando contabilizar o troco como receita adicional.

Snapshots imutáveis de SKU, nome, unidade, quantidade, preço e subtotal preservam a venda mesmo após alterações no cadastro. IDs de operações de estoque por item e operação de tesouraria permitem rastreio. Saída usa custo médio interno vigente, preserva ledger e impede estoque negativo; custo não aparece na resposta de venda. O movimento de estoque é CONSUMPTION, com namespace POS_SALE e referência à venda. Entrada financeira recebe origem POS_SALE, visível no extrato e fluxo de caixa. Sale, itens, estoque, financeiro, versão de sessão e auditoria compartilham transação.

Lock da empresa serializa venda, movimentos de caixa e fechamento; produtos são bloqueados em ordem de UUID, usando o mesmo mecanismo de estoque. Venda incrementa a versão da sessão, invalidando contagem anterior. Se fechamento vence, a nova venda é rejeitada. Falha em qualquer item, saldo/limite financeiro ou permissão desfaz tudo.

UUID/fingerprint impedem repetição de baixa e receita. Reenvio idêntico retorna o registro original mesmo depois de fechar/inativar produto; conteúdo diferente gera 409. Escopo atual da sessão e permissão financeira são revalidados antes de replay. Fingerprint normaliza escalas decimais, mas preserva ordem dos itens e dinheiro informado.

## Consulta e interface

`GET /api/v1/pos/sales?page=0&size=20` e `GET /api/v1/pos/sales/{id}` exigem pos:read + treasury:read e escopo de filial; filtros antes da paginação e leitura REPEATABLE_READ. Valores monetários e quantidades são strings JSON.

Tela `/pos-sales` carregada sob demanda: vinculação paginada de depósito, seleção de caixa do operador/supervisor, busca paginada, carrinho, recebimento/troco e histórico com itens. Subtotal exibido usa aritmética inteira para reproduzir HALF_UP. Busca atualiza catálogo; para atualizar um item do carrinho é necessário remover e adicionar novamente. Resultado incerto de conclusão conserva UUID/payload em memória e bloqueia edição/navegação/logout até confirmação. Não é outbox durável.

## Limites e próximos passos

Somente dinheiro nesta etapa. Pagamentos mistos, PIX/cartão/TEF, prazo/geração de contas a receber, clientes na venda, descontos/alçadas, cancelamentos/devoluções com reversão, fiscal, impressão, offline e homologação de equipamento permanecem pendentes. Registro exibido é interno, sem validade de documento fiscal. Nova permissão pos:sell entra na whitelist/provisionamento de empresas novas (41 permissões); nenhuma migração eleva usuários existentes.

## Pagamentos e cancelamento — 15/10/2026

V20 amplia a conclusão com parcelas conferidas em dinheiro/Pix/transferência e cancelamento integral no mesmo turno. Dinheiro simples V19 e fingerprint preservados. POS_PAYMENTS.md contém regras atuais e limitações de provedores/devoluções.

## Devoluções — V21 (15/10/2026)

V21: devoluções parciais e após fechar implementadas com caixa atual da mesma filial; preços históricos, quantidades e limites de reembolso preservados. Cancelamento integral não pode seguir uma devolução. Contratos e limites em POS_RETURNS.md.

## Descontos e suspensão — V22 (15/10/2026)

V22: preços brutos, desconto por item e líquido preservados. Limite por filial e aprovação de outro usuário acima da política. Carrinhos podem ser suspensos/retomados/salvos/descartados sem reserva e concluídos uma vez. POS_COMMERCIAL.md contém contratos atuais.
