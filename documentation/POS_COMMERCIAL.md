# Descontos, autorização e suspensão — V22

## Descontos por item

`POST /api/v1/pos/sales` aceita discountAmount opcional por item: valor em reais do desconto sobre o subtotal bruto arredondado, não por unidade. O servidor calcula bruto = quantidade × preço cadastrado, HALF_UP em duas casas, e líquido = bruto − desconto. Desconto deve ser não negativo e não superar o bruto. Venda completa deve ter líquido positivo. Snapshots de preço, bruto, desconto e líquido permanecem imutáveis; cabeçalho expõe grossTotal e discountTotal. Itens antigos recebem bruto igual ao total original e desconto zero na migração.

Desconto positivo exige pos:discount. `GET /api/v1/pos/discount-policies/{branchId}` consulta o limite percentual por item; ausência de configuração significa 0%, sem autorização automática. `PUT` recebe version e operatorPercent (0–100%, até duas casas) e exige pos:discount-policy. Cada alteração incrementa versão e gera auditoria. A política é por filial e segue escopo de acesso; não exige administração global.

Limite é aplicado a cada item: desconto × 100 ≤ bruto × percentual. Não é permitido diluir desconto alto de um produto em outro item caro para escapar da autorização. Não há promoção automática, desconto global, combinação de cupons ou alçada por valor/categoria nesta etapa.

## Solicitação e decisão

`POST /api/v1/pos/discount-requests` recebe requestId, sessionId, note (5–500 após trim) e items com productId/quantity/expectedUnitPrice/discountAmount. Exige pos:discount, caixa aberto e operador responsável ou pos:supervise. Guarda os itens conferidos pelo servidor, identidade do solicitante, bruto/desconto, justificativa e hash do carrinho. Não movimenta estoque ou dinheiro.

`GET /discount-requests?status=&page=0&size=20` e `GET /discount-requests/{id}` mostram solicitações próprias; usuários com pos:discount-approve podem consultar as da filial autorizada. A filtragem ocorre antes da paginação. Sem acesso retorna 404.

`POST /discount-requests/{id}/decision` recebe requestId, version, approve e note. Exige pos:discount-approve e outro usuário: o solicitante não pode aprovar ou rejeitar a própria solicitação. Aprovação confere produtos/identidades/unidades/preços atuais, acesso e permissão do solicitante; mudança exige nova solicitação. Rejeição preserva os itens e a justificativa. Decisão é versionada e idempotente por UUID/fingerprint; não existe alteração de uma decisão concluída.

Estados: PENDING → APPROVED ou REJECTED; APPROVED → CONSUMED somente na conclusão bem-sucedida. Aprovação vale 30 minutos a partir da decisão e fica vinculada ao solicitante, filial, itens/preços/unidades/quantidades/descontos e versão da política vigente na decisão. Solicitação pendente não tem expiração automática nesta etapa.

Para concluir acima do limite, envie discountApprovalId na venda. O servidor confere todos os vínculos, validade, uso único e autorização atual do aprovador (ativo, permissão e filial). Mudança do carrinho, revogação do aprovador ou alteração da política invalida a autorização. Se estiver dentro do limite, basta pos:discount; caso uma aprovação seja explicitamente enviada, ela também é validada e consumida. Aprovação sem desconto é rejeitada. Autorizações não são transferíveis entre operadores, mesmo para um supervisor.

Aprovação é consumida na mesma transação da venda/estoque/recebimentos. Falha mantém APPROVED, sem baixa ou recebimento parcial. Reenvio do comando de venda retorna o registro atual sem consumir novamente. Cancelamento ou devolução posterior não libera a autorização para outra venda. Nova permissão de política é uma competência administrativa separada: quem a possui pode definir limites sem uma solicitação individual, com auditoria.

## Suspensão e retomada

`POST /api/v1/pos/suspensions` recebe requestId, sessionId, note e items; version deve ser omitida. Exige pos:suspend, acesso à filial e operador do caixa aberto ou supervisor; desconto positivo também exige pos:discount. O carrinho fica SUSPENDED e guarda snapshots de itens/valores. Não há reserva, baixa de estoque, pagamento, contas a receber, aprovação automática ou alteração de saldo/versão do caixa.

`GET /suspensions?status=&page=0&size=20` e `GET /suspensions/{id}` mostram os próprios carrinhos ou, para pos:supervise, os das filiais autorizadas. Retomada pode ocorrer em outro turno/terminal aberto da mesma filial. `PUT /suspensions/{id}` salva itens/motivo/caixa atual, recebe requestId/version e incrementa versão. Preços e produtos são conferidos novamente e o snapshot é atualizado. Conteúdo antigo não é convertido silenciosamente quando o cadastro muda: o operador precisa atualizar os itens e salvar.

Para concluir uma suspensão, envie suspensionId e suspensionVersion na venda, além dos itens/pagamentos e eventual aprovação. O hash deve corresponder ao carrinho salvo; após editar, salve antes de concluir. Na mesma transação, o sistema cria a venda e marca a suspensão COMPLETED, com vínculo único à venda. Não existe retomada exclusiva com reserva: dois acessos podem abrir o carrinho, mas só um pode concluir aquela versão; o outro recebe conflito sem efeitos financeiros ou de estoque.

`POST /suspensions/{id}/discard` recebe requestId, version e note. Marca DISCARDED sem movimentar dinheiro/mercadoria; pode ser usado após o turno original fechar. Estado terminal não pode ser editado ou concluído. O journal de comandos preserva UUID/fingerprint de criação, atualização e descarte; reenvio antigo retorna estado atual, inclusive COMPLETED, sem reaplicar ou reativar o carrinho. Carrinho suspenso não armazena parcelas de pagamento: todos os recebimentos devem ser informados/conferidos na conclusão.

## Coerência e interface

Tenant lock serializa política, decisão, suspensão, fechamento e conclusão; produtos são bloqueados em ordem de UUID. Estoque/preços/filial/depósito/operador/contas continuam conferidos na venda. Snapshots líquidos alimentam cancelamento/devolução: reembolso usa o total efetivamente cobrado, incluindo o desconto, sem devolver o bruto.

Formato de venda sem desconto/aprovação/suspensão preserva o fingerprint V19–V21, inclusive desconto zero explícito. Migração não concede privilégios a contas existentes. Quatro permissões entram apenas no provisionamento novo: pos:discount, pos:discount-approve, pos:discount-policy e pos:suspend (47 no conjunto).

Interface /pos-sales inclui campo de desconto por item, solicitação/conferência/decisão/seleção de autorização, política por filial e listas paginadas de suspensões. Retomar substitui o carrinho atual; a tela informa que é necessário suspendê-lo antes se quiser preservá-lo. Pagamentos locais são limpos ao suspender/retomar, sem execução externa. Comando de resultado incerto preserva payload/UUID em memória e bloqueia edição/navegação/logout; operações de venda, devolução e gestão de carrinhos se bloqueiam mutuamente. Não é outbox durável.

Ainda faltam alçadas por papel/valor/categoria, promoções/cupons, rastreio de todas as versões de edição de carrinhos, retirada da solicitação pendente, reservas, suspensão offline, clientes/pedidos/crediário, provedores/cartões/fiscal e os demais itens do ROADMAP. Login real e revisão visual seguem adiados.
