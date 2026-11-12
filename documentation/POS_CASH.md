# Núcleo operacional do PDV — terminais e caixa

Terminal lógico por empresa/filial, com código/nome e uma conta de tesouraria CASH exclusiva da mesma filial. Não identifica hardware nem configura impressora, TEF ou equipamento nesta etapa. Cadastro usa UUID/fingerprint; reenvio idêntico retorna o terminal. Código e conta não podem se repetir na empresa. Conta BANK ou de outra filial é rejeitada. Ativação/desativação tem versão; terminal com caixa aberto não pode ser desativado.

## Abertura e reserva

Operador é sempre a pessoa autenticada. Um terminal, uma conta e um operador podem ter apenas uma sessão OPEN no tenant, protegidos por checks/unicidade de chaves de sessão aberta. Abertura exige terminal/filial ativos, justificativa e valor conferido fisicamente igual ao saldo atual da conta. Zero é permitido. Abertura não credita a tesouraria: prepare o saldo por movimentos reais antes de abrir.

UUID de abertura e fingerprint incluem o operador. Reenvio após o fechamento retorna a sessão CLOSED original, sem criar/reabrir um turno. Novo turno exige novo UUID e nova conferência. A conta fica reservada enquanto OPEN; movimentos manuais, AP e AR externos nessa conta são bloqueados. Isso evita alterar o saldo esperado sem registrar movimento na sessão. Leituras/históricos e reenvios sem novos efeitos continuam disponíveis.

## Suprimento e sangria

SUPPLY aumenta o dinheiro do caixa; WITHDRAW reduz. Valor positivo em reais, NUMERIC(19,2), nota e UUID. Outra conta opcional transforma o movimento em transferência interna: fonte do suprimento ou destino da sangria. Sem outra conta, representa entrada/saída externa de dinheiro já realizada. Não executa transferência bancária ou abertura física de gaveta.

Toda movimentação exige operador responsável ou supervisor explícito, permissões de operação/tesouraria e acesso às contas envolvidas. Conta de outro caixa aberto fica bloqueada, inclusive para supervisor. Saldo insuficiente, overflow, mesma conta ou falta de acesso rejeitam integralmente a operação. Movimento de sessão, origem POS_SUPPLY/POS_WITHDRAW no ledger, atualização de saldos/version e auditoria compartilham transação e lock da empresa.

Reenvio por UUID/fingerprint não reposta valores, mesmo depois de fechar. Chave reutilizada com outros dados/session gera conflito. Operação de tesouraria tem ID interno associado ao movimento da sessão; não se confunde com comando manual. Histórico conserva atores, justificativas, contas e IDs. Não existe exclusão/estorno de movimento neste marco; correções exigem movimento real oposto e justificativa enquanto aberto, com trilha preservada.

## Fechamento

Fechar exige versão atual, UUID, valor contado não negativo e justificativa. Valor esperado é saldo da conta na transação. Sistema grava esperado/contado/diferença = contado − esperado, ator/instante e CLOSED, liberando terminal/operador/conta. Divergência não ajusta automaticamente a tesouraria; investigue e regularize por movimento autorizado após fechar. Sem essa regularização, próxima abertura continua exigindo o saldo registrado, não o contado divergente.

Snapshot de esperado/diferença permanece fixo após fechamento, mesmo que a conta seja usada em outro turno. Reenvio idêntico retorna o fechamento original; outra identidade/conteúdo gera conflito. Movimento concorrente incrementa versão e invalida a contagem antiga; se fechamento vence, o novo movimento é rejeitado. Não existe reabertura de turno fechado, aprovação de divergência ou fechamento cego nesta etapa.

## Acesso

Toda consulta exige pos:read + treasury:read e escopo da filial. pos:manage cadastra/ativa terminais; pos:operate abre/movimenta; pos:close fecha. Movimentação também exige treasury:move. pos:supervise permite operar/fechar caixa de outro operador quando as demais permissões estão presentes; não substitui leitura/filial/operação/fechamento. Por exemplo, gestor pode encerrar sessão de operador inativado com esses privilégios e acesso à filial.

Listas/totais de terminais/sessões e históricos usam tenant/filial. Sessões são consultadas com REPEATABLE_READ e saldo esperado lido junto à linha da sessão. Novas empresas recebem as cinco permissões; migration não eleva usuários antigos. Conjunto atual tem 40 permissões, dentro da whitelist e limite de 100 para usuários/perfis.

Interface `/pos-cash` carregada sob demanda: seleção paginada de filiais/contas, cadastro/ativação de terminais, abertura, lista/detalhe de sessões, suprimento/sangria, contagem/fechamento e histórico. Conta/valor consultado servem de referência; valores de contagem não são preenchidos automaticamente. Resultado incerto rede/5xx preserva payload/UUID em memória e bloqueia edição/navegação/logout para retry. Não é outbox persistente. Login real e homologação visual seguem adiados pelo usuário.

V18 cria terminal/sessão/movimento e amplia origens da tesouraria, sem alterar dados prévios. Guards de reserva também protegem APIs de tesouraria/AP/AR. Somente rotinas internas do caixa podem movimentar sua conta reservada após validar dono/estado sob o mesmo lock. Fluxo de caixa inclui esses movimentos via ledger, separando transferências como antes.

Validação em CashSessionTest/PostgresIT: 15 cenários para terminal, abertura, duplicação, saldo, transferências/retry, rollback, snapshot de divergência, novo turno, bloqueio externo/AR, dono/supervisor, terminal inativo/versão, replay depois de fechar, tenant/filial, permissões, concorrência e reenvio após restrição da conta contraparte. AP/AR vinculados e regressão são revalidados por compartilharem tesouraria.

Pendentes: vendas/itens/preços históricos, estoque/depósito do terminal, descontos/alçadas, cancelamento/devolução, N pagamentos/troco/provedores, cobrança/fiscal, aprovação de divergências, fechamento cego, estorno de caixa, operadores/hardware/offline e homologação comercial. Esta entrega prepara o caixa, não conclui o PDV comercial.

## Vendas com itens — 14/10/2026

V19: terminal expõe warehouseId e permite vincular depósito da mesma filial com versão e caixa fechado. Venda em dinheiro incrementa versão e esperado do caixa, guarda itens/preços e baixa estoque atomicamente. Ver POS_SALES.md. Conjunto atual: 41 permissões; nenhuma elevação automática de usuários existentes.

## Pagamentos e cancelamento — 15/10/2026

V20: venda pode ter parcelas bancárias sem alterar saldo físico; toda venda/cancelamento incrementa versão da sessão, inclusive quando somente bancária. Cancelamento novo exige turno original aberto. Reenvio idêntico não repete efeito. Ver POS_PAYMENTS.md.

## Devoluções — V21 (15/10/2026)

V21: caixa atual pode reembolsar venda de outro turno/terminal da mesma filial; esperado congelado do turno original permanece inalterado. Devolução incrementa versão do caixa atual, inclusive quando somente bancária/zero centavo, protegendo contagem concorrente.

## Descontos e suspensão — V22 (15/10/2026)

V22: guardar carrinho não altera saldo ou versão do caixa e não impede fechamento. Retomada usa caixa aberto da mesma filial e conclusão incrementa versão/movimenta estoque e recebimentos normalmente. Sem fechamento/suspensão financeira fictícia.
