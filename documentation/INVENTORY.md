# Estoque e perdas — implementação atual

Depósitos pertencem a uma filial da empresa autenticada. Saldos são separados por depósito/produto. Entradas, ajustes, consumo interno, transferências imediatas e perdas aprovadas persistem ledger, saldo, custo e auditoria em uma única transação. Não há endpoint para editar diretamente saldo ou histórico.

## Consistência e custos

Cada comando exige UUID fornecido pelo cliente. `stock_operation` guarda a identidade do comando e fingerprint de campos com prefixo de comprimento; valores decimais equivalentes produzem a mesma fingerprint. Retry idêntico retorna as movimentações originais. Mesma chave com outro payload retorna 409. Unicidade é por tenant. Registro de perda usa requestId independente; aprovar uma perda já aprovada retorna o registro sem nova baixa/auditoria.

O produto da empresa é bloqueado com SELECT FOR UPDATE antes de qualquer escrita de saldo. Essa estratégia serializa operações do mesmo produto em todos os depósitos, inclusive quando ainda não existe linha de saldo. Produtos diferentes continuam independentes. Transferência usa um único produto bloqueado e dois movimentos na mesma transação; falha no destino desfaz também origem, comando e auditoria. Isso é transferência imediata, sem fluxo de mercadoria em trânsito.

Quantidades/custos unitários: NUMERIC(19,6), BigDecimal, até 13 dígitos inteiros e seis casas. Saldo nunca negativo. Entrada exige custo informado; saída usa custo médio do depósito, sem aceitar custo arbitrário. Custo médio de entrada = (quantidade anterior × custo médio anterior + quantidade recebida × custo recebido) / saldo novo, arredondado HALF_UP para seis casas. Saída preserva o custo médio; saldo zero zera o custo médio. Transferência leva o custo da origem e pondera o destino. Valores trafegam como strings, sem cálculo binário no Angular.

Permissão inventory:cost é necessária para consultar custos/montantes. Revogação passa a ocultar averageCost, unitCost e totais de perdas na resposta seguinte, mesmo que a navegação do frontend ainda esteja com permissões antigas.

## Perdas

Registro PENDING guarda estimativa pelo custo médio vigente. Não reserva nem reduz saldo; aprovação revalida disponibilidade e usa custo médio do momento da baixa, preservando a estimativa original e o custo efetivo. Perdas sobrepostas podem existir, mas aprovação posterior sem saldo retorna 409 e permanece pendente. Aprovação/rejeição trava a perda antes de decidir, impedindo baixa dupla. Rejeição exige justificativa e não movimenta estoque.

Toda perda exige aprovação com loss:approve. Não há aprovação automática por valor nem política de separação de solicitante/aprovador nesta versão. Isso ainda deve ser configurado por alçada antes do uso comercial. Consumo interno e ajustes têm classificação própria no ledger.

## Interface e retentativas

Tela Estoque: cadastrar depósito, selecionar depósito/produto, pesquisar catálogo com paginação, registrar movimento/transferência/perda e consultar saldo/histórico. Tela Perdas: consultar estimativa/valor efetivo conforme permissão, aprovar e rejeitar. Formulários são limpos após confirmação para evitar novo lançamento acidental.

Durante comando ou resultado incerto (falha de rede/5xx), o payload e UUID ficam em memória; campos são bloqueados e botão de retry repete o mesmo comando. A navegação é impedida enquanto houver comando pendente e sair da sessão fica desabilitado. O navegador recebe aviso antes de recarregar/fechar. Essa proteção não é persistência offline: encerrar o processo à força perde a tentativa local. Persistência durável e conciliação de comandos ainda devem ser implementadas com o PDV/offline.

## Ainda pendente no módulo completo

Reservas/bloqueios/estoque em trânsito; lotes/validade/série; localização dentro do depósito; conversões de embalagem; inventário e recontagem; estorno de perdas; fotos/evidências; motivos configuráveis; alçadas por valor/quantidade; reposição/giro/cobertura/alertas/relatórios; recebimento vinculado a pedido/XML e integração com venda/financeiro/fiscal. Permissões por filial, gestão de perfis e RLS também permanecem pendentes. Não considerar este recorte homologado para operação comercial.

Novas permissões são concedidas ao administrador no provisionamento de uma nova empresa. Contas antigas não recebem privilégios adicionais implicitamente; a concessão deve ser explícita pela administração de usuários ainda a implementar.

Recebimentos de compras geram entradas vinculadas a purchase_receipt_line e operação de estoque. Custo vem do pedido; o recebimento usa purchase:read/receive e não concede ajustes manuais. Namespace de fingerprint é distinto, impedindo reutilizar movimento manual como recebimento. Múltiplos produtos bloqueiam em ordem estável; rollback abrange todos os itens. Regras e limites em PURCHASES.md.


## Vendas com itens — 14/10/2026

V19: venda concluída baixa estoque via consumeSale, ledger CONSUMPTION e fingerprint POS_SALE; produto ativo bloqueado, custo médio vigente e saldo não negativo. Falha de pagamento desfaz estoque. Ver POS_SALES.md.

## Pagamentos e cancelamento — 15/10/2026

V20: returnSale recompõe o depósito pelo custo histórico de saída e recalcula custo médio; produto inativo aceito, unidade alterada e depósito inativo rejeitados. IDs de retorno por item e ledger original preservado; falha financeira desfaz o retorno.

## Devoluções — V21 (15/10/2026)

V21: devoluções parciais recompõem depósito original pelo custo de saída, recalculando média atual. Quantidade acumulada limitada à vendida; transação desfaz todos os itens se um retorno/reembolso falhar. Ledger diferencia devolução de cancelamento e referencia ambos os registros. Ver POS_RETURNS.md.

## Descontos e suspensão — V22 (15/10/2026)

V22: suspensão e solicitação de desconto não reservam ou baixam mercadorias. Preço/atividade/saldo e depósito são conferidos de novo ao concluir. Retorno continua pelo custo de saída; reembolso usa o líquido vendido.
