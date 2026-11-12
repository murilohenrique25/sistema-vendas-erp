# Sequência autorizada — 11/11/2026

Autorização: desenvolver os itens 1 a 7 em sequência, sem novas aprovações. Banco desconectado; testes, login real e revisão visual adiados. Código e compilação não equivalem a homologação ou implantação.

| Ordem | Entrega | Estado |
|---|---|---|
| 1 | Inventário físico: abertura, contagem, divergências, aprovação e ajuste rastreável | Implementado em API e interface; compilado sem testes |
| 2 | Lotes/validade, rastreabilidade e alocação nas movimentações | Implementado em API e interface; compilado sem testes |
| 3 | Estoque mínimo e sugestões de compra | Implementado em API e interface; compilado sem testes |
| 4 | Devolução ao fornecedor e ajuste financeiro | Implementado em API e interface; compilado sem testes |
| 5 | Preços por filial, margens e histórico | Implementado em API e interface; compilado sem testes |
| 6 | Pedidos/orçamentos, aprovação e conversão em venda | Implementado em API e interface; compilado sem testes |
| 7 | Promoções, vendedores/comissões e fidelidade | Implementado em API e interface; compilado sem testes |

## 1. Inventário físico

Rota `/inventory-counts`; API `/api/v1/inventory/counts`; migração V29.

Abertura por depósito com até 100 produtos ativos, fotografia do saldo e contador de movimentos. Contagem versionada, envio para aprovação, cancelamento e aprovação por outro usuário com `inventory:approve`. A aprovação gera movimentos por diferença em uma transação; rejeita a contagem se houve qualquer movimento de estoque desde a abertura, inclusive saída e reposição que deixassem o mesmo saldo. Cancele e abra nova contagem nesse caso.

Contagem zero é explícita. Sobra sem saldo anterior exige custo informado por alguém com `inventory:cost`; com saldo anterior, usa o custo médio corrente. Custos não são expostos a quem não tem essa permissão; contagem por outro usuário preserva o custo existente. Não há congelamento de depósito, contagem cega ou múltiplas rodadas independentes de recontagem nesta entrega.

## 2. Lotes e validade

Rota `/stock-lots`; API `/api/v1/inventory/lots`; migração V30.

Saldos por lote e depósito e movimentos vinculados ao ledger de estoque. Classificação manual de quantidades recebidas em código/validade; classificação não muda o saldo global. Código de lote identifica um produto no tenant e sua validade não pode ser alterada depois de criado. Transferências mantêm lote e validade. Vendas e consumo interno usam primeiro os lotes com vencimento mais próximo (FEFO), excluindo vencidos. Ajustes, perdas e devolução ao fornecedor podem retirar vencidos.

Saldos anteriores e entradas ainda não classificadas ficam em `@UNASSIGNED`, sem inventar origem ou validade. Esse saldo continua utilizável. A captura do lote no recebimento não é obrigatória: a classificação é feita depois, na tela de lotes. Não há escolha manual de um lote específico no carrinho; a alocação é automática. Devoluções de venda restauram as alocações originais quando registradas; vendas anteriores à migração retornam ao saldo sem classificação. Custo continua médio por produto/depósito, sem custo individual por lote. Devolução ao fornecedor usa os lotes disponíveis por FEFO, sem afirmar vínculo entre lote físico e linha de recebimento.

## 3. Reposição

Rota `/replenishment`; API `/api/v1/inventory/replenishment`; migração V31.

Políticas versionadas de mínimo/alvo por produto/depósito, consulta paginada e sugestões quando disponível < mínimo. Disponível desconsidera quantidade classificada vencida. Sugestão = alvo − disponível. Pode criar pedido real de compra com fornecedor, quantidades e custos conferidos. Servidor verifica a necessidade atual e impede reuso divergente da identidade da operação.

Pedidos em aberto não são descontados, não há reserva, previsão de demanda ou geração automática sem ação do usuário. Confira compras em andamento. A sugestão não garante disponibilidade futura. Custo médio sugerido exige `inventory:cost`; quem não vê custos precisa informar o custo de compra.

## 4. Devolução ao fornecedor

Rota `/supplier-returns`; acesso direto pelo histórico de recebimentos em Compras. API `/api/v1/supplier-returns`; migrações V32/V33.

Prévia dos itens e valores seguida de confirmação. Quantidade acumulada limitada ao efetivamente recebido. Valores usam o montante histórico da linha de recebimento, com arredondamento cumulativo para que devolução integral feche exatamente os centavos. Retirada do depósito original e ajustes financeiros são atômicos e idempotentes. Produtos inativos também podem retornar se a unidade continua compatível.

Antes da geração de contas a pagar, devoluções diminuem o valor da futura geração. Depois da geração, abatem primeiro parcelas com vencimento mais distante; não alteram valor original nem inventam pagamento. O excedente sobre dívida pendente vira crédito registrado a reclamar do fornecedor, sem entrada de caixa. Contas a pagar e fluxo de caixa descontam os abatimentos; títulos quitados por abatimento mostram `CREDITED`. Pagamentos/estornos preservam o abatimento existente.

Títulos manuais vinculados ou títulos cancelados bloqueiam a devolução para reconciliação. Havendo crédito a reclamar do fornecedor, estorno de pagamento é bloqueado para não invalidar a origem desse crédito. Ainda não há rotina de recebimento/compensação/liquidação desse crédito, nem desfazimento da devolução: são evoluções futuras, não ações simuladas. A prévia não reserva estoque; confirmação revalida quantidades e divisão financeira e pode ser rejeitada por alterações posteriores.

## 5. Preços por filial

Rota `/branch-prices`; API `/api/v1/branch-prices`; migração V34.

Preço específico versionado por filial/produto com justificativa e histórico. Sem preço específico, vale o preço geral. PDV, suspensões, pedidos, preparações e aprovações de desconto conferem o preço da filial no servidor. Mudança de preço exige refazer o item/reaprovar o pedido; não altera vendas históricas. A troca de caixa atualiza a busca de produtos e avisa para conferir os itens já no carrinho.

Margem bruta exibida somente com `inventory:cost`: (preço da filial − custo médio ponderado pelas quantidades dos depósitos da filial) / preço. Sem saldo usa custo zero; preço zero não tem percentual. Não inclui impostos, frete ou despesas e não impõe margem mínima. Histórico desta tela cobre alterações de preço específico por filial; não é histórico completo de alterações do cadastro geral. Tabelas por cliente e reajuste em lote permanecem fora desta entrega.

## 6. Pedidos e orçamentos

Rota `/sales-orders`; API `/api/v1/sales-orders`; migração V35.

Rascunho versionado com filial, cliente opcional, tipo, validade, observação e itens históricos. Edição, aprovação, reabertura e cancelamento. Aprovação exige `salesorder:approve`; alterações não salvas não são apresentadas como a versão aprovada. Conteúdo aprovado fica imutável até reabrir.

Conversão exige versão aprovada válida, caixa da mesma filial, itens/preços conferidos e permissões da venda/tesouraria. Reutiliza o serviço de venda existente: estoque, pagamento, descontos, crediário e auditoria compartilham a transação. Um pedido gera no máximo uma venda; reenvio preserva o mesmo comando. A interface oferece dinheiro recebido e confirmação explícita; API aceita a composição de pagamentos do PDV. Cliente identificado pode receber pontos na conversão, conforme política e permissão de leitura de cliente.

Não há reserva de estoque, entrega parcial, venda parcial ou congelamento de preço até a validade. A aprovação de pedido não substitui a alçada de desconto do PDV. Vencimento ou mudança de cadastro pode exigir reabertura e nova aprovação. Cancelamento/devolução posterior da venda não reabre automaticamente o pedido convertido.

## 7. Regras comerciais

Rota `/commercial-rules`; API `/api/v1/commercial`; migração V36.

Promoção percentual por produto/filial, com início/fim inclusivos em São Paulo, ativação e versão. Uma regra por produto/filial, aplicada ao preço específico ou geral. Catálogo do PDV e precificação de conclusão usam o mesmo cálculo com centavos HALF_UP. Descontos manuais adicionais seguem as alçadas já existentes. Mudanças de promoção podem invalidar carrinhos/pedidos antigos. Não há cupom, regra por categoria, combinação de promoções ou condição de quantidade.

Vendedores por filial, ativação e percentual de comissão. PDV permite atribuir vendedor e cliente de fidelidade. Comissão apurada sobre total líquido da venda concluída, com percentual/valor histórico e estorno cumulativo proporcional nas devoluções/cancelamentos. Extrato comercial registra débitos/créditos e origem. Não registra pagamento de comissão ou folha.

Fidelidade por filial: pontos por real configuráveis, sem adesão fictícia ou cadastro de exemplo. Pontos inteiros, arredondamento para baixo, somente para cliente identificado e programa ativo. Resgate manual registra pontos e benefício já entregue, com justificativa e saldo suficiente; não cria desconto monetário automático. Devoluções revertem os pontos históricos e podem deixar saldo negativo se houve resgate anterior. Não há expiração, transferência entre filiais ou catálogo de recompensas nesta entrega.

Histórico imutável das configurações comerciais e extrato paginados, escopo por tenant/filial. Vendas anteriores à V36 não recebem comissão/pontos retroativamente. Comandos de configuração, resgate e operações usam identidades idempotentes. Prévia financeira e valores decimais das novas respostas são transmitidos sem perda de centavos.

## Permissões e operação

Novas permissões: `inventory:approve`, `salesorder:read`, `salesorder:write`, `salesorder:approve`, `commercial:read`, `commercial:write`. Constam do catálogo e do administrador de **novas empresas**. Não houve concessão silenciosa a contas existentes nem alteração de banco. A habilitação em contas existentes precisa de provisionamento administrativo autorizado na fase de integração; a tela de usuários só concede privilégios que o gestor possui. Demais APIs reutilizam as permissões de estoque, compras, produtos, clientes, financeiro e PDV.

Operações mutáveis usam transação, auditoria, escopo e reenvio da identidade com o mesmo conteúdo. As telas preservam comandos incertos em memória e bloqueiam saída normal até confirmar; fechar o navegador à força não cria recuperação persistente em disco.

## Verificação e pendências de integração

Backend: empacotamento Maven offline com `-Dmaven.test.skip=true`, sem compilar/executar testes. Frontend: build Angular de produção. Evidências em `test-evidence/sequence-backend-build.txt` e `test-evidence/sequence-frontend-build.txt`. Nenhum banco, Docker, migração aplicada, login real, revisão no navegador ou implantação nesta sequência.

V27/V28 anteriores e V29–V36 desta sequência estão **preparadas, não aplicadas**. A última implantação comprovada permanece em V26. Antes de usar: validar migrações do zero e upgrade, fluxos positivos/negativos, permissões por filial/tenant, concorrência, rollback, precisão, FEFO, devoluções e reenvios; executar regressão integrada e revisão de interface. Essa validação fica para a etapa final por orientação do usuário. As sete frentes têm implementação funcional no código dentro dos limites descritos; o ERP integral ainda tem outras entregas em PENDING_WORK e REQUIREMENTS.
