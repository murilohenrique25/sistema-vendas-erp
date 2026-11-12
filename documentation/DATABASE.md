# Banco de dados — schema implementado

Fonte executável: `backend/src/main/resources/db/migration/V1__foundation.sql`.

V5 acrescenta app_user.version bigint NOT NULL DEFAULT 0. A API de ativação exige a versão lida e incrementa-a somente quando altera o estado. Administração bloqueia a linha tenant para serializar alterações e verificações de gestores ativos. Nenhuma permissão é concedida por migration; user:read/write entram apenas no provisionamento de novas empresas.

V6 cria access_profile(id UUID, tenant_id UUID, name varchar(160), version bigint) com únicos tenant/id e tenant/name; access_profile_permission tem PK profile/permission e FK composta tenant/profile. São modelos de permissões; aplicação copia o conjunto para user_permission. Nenhuma tabela liga usuário dinamicamente ao perfil, evitando alterações silenciosas dos acessos quando um modelo muda.

V7 acrescenta app_user.all_branches boolean DEFAULT TRUE NOT NULL e user_branch_access(tenant_id,user_id,branch_id), PK usuário/filial e FKs compostas para usuário/filial da mesma empresa. Preserva acesso anterior até restrição explícita. Escopo compartilha app_user.version com permissões/estado; alterações administrativas usam lock de tenant. Não implementa RLS no PostgreSQL; restrição é aplicada nas consultas/serviços da API.

V8 cria supplier com id/tenant_id UUID, code varchar(40), name varchar(200), tax_identifier varchar(40), contact_name varchar(160), email varchar(254), phone varchar(40), address varchar(500), notes varchar(2000), active boolean e version bigint. Únicos tenant/id e tenant/code; índice tenant/name. Código normalizado em maiúsculas; identificação fiscal declarada não é chave única. Fornecedores são compartilhados por empresa e preservados após desativação. Pedidos referenciam fornecedor por FK composta de tenant desde V9.

V9 cria purchase_order (requestId/fingerprint únicos por tenant, filial/fornecedor/ator com FKs compostas, snapshots/total/status/version), purchase_order_line (produto e SKU/nome/unidade/custo/quantidade/total históricos, recebido limitado à quantidade, produto único no pedido), purchase_receipt (id por tenant, pedido/depósito/ator/fingerprint/nota/data) e purchase_receipt_line (item e operação de estoque por FKs compostas, operação única). Total NUMERIC(19,2); quantidades/custos NUMERIC(19,6). Relação receipt/item inclui order_id nas FKs para impedir vínculo a item de outro pedido. Filial do depósito é validada no serviço. Histórico não tem endpoint de edição/exclusão.

V10 cria payable (filial/fornecedor/recebimento opcional/ator com FKs compostas, requestId/fingerprint, referência única, documento/descrição/datas, amount/paid_amount NUMERIC(19,2), estado/version) e payable_payment (identidade por tenant, valor/data/meio/referência/nota/ator, fingerprint, estado e metadata de estorno). Checks limitam pago ao título e coerência de OPEN/PARTIAL/PAID/CANCELLED; pagamento POSTED não tem campos de estorno e REVERSED exige todos eles. Chave de estorno única por tenant; originais não são excluídos. Datas de obrigação/pagamento são DATE; criação/estorno timestamptz. Vínculo a recebimento é uma referência, sem geração automática de dívida/parcelas.

Catálogo em `V2__product_catalog.sql`: product(id UUID PK, tenant_id UUID FK, sku varchar(60), name varchar(200), description varchar(2000), unit varchar(10), sale_price numeric(19,2) com check não negativo, active bool, version bigint). Únicos (tenant_id,sku) e (tenant_id,id); índice tenant/name. product_barcode(tenant_id UUID, product_id UUID, code/canonical_code varchar(60), kind varchar(10)); PK tenant/canonical_code e FK composta para produto; índice tenant/product. Produto 1:N códigos. Não há exclusão de produtos/códigos via API; códigos e cadastro são transacionais. Custos, preços por filial, variantes e histórico de alteração ainda serão modelados.

| Tabela | Campos / tipos | Integridade e histórico |
|---|---|---|
| tenant | id UUID PK; name varchar(160); slug varchar(80); created_at timestamptz | slug único; raiz da empresa |
| app_user | id UUID PK; tenant_id UUID; name varchar(160); email varchar(254); password_hash varchar(100); active bool; failed_attempts int; locked_until timestamptz | FK tenant; único (tenant_id,email) e (tenant_id,id); senhas nunca retornadas |
| user_permission | tenant_id UUID; user_id UUID; permission varchar(80) | PK (user_id,permission); FK composta usuário/tenant |
| auth_session | token_hash varchar(64) PK; tenant_id UUID; user_id UUID; expires_at/created_at timestamptz | FK composta; índice tenant/usuário; exclusão na revogação |
| branch | id UUID PK; tenant_id UUID; code varchar(30); name varchar(160); active bool; version bigint | FK tenant; únicos (tenant,code)/(tenant,id); versão para concorrência futura |
| audit_event | id UUID PK; tenant_id UUID; actor_id UUID; action varchar(80); entity_id UUID; occurred_at timestamptz | FK tenant/ator; índice tenant/data; sem endpoint de edição/exclusão |

Cardinalidades: empresa 1:N usuários/filiais/eventos; usuário 1:N permissões/sessões. Todas as exclusões de dados organizacionais são restringidas pelas FKs; não há cascata destrutiva. `entity_id` identifica a entidade auditada e não possui FK polimórfica. Auditoria inicial registra ações e identificadores, sem payloads sensíveis. Captura de diferenças antes/depois, origem e retenção ainda pendentes.

V3 amplia canonical_code de varchar(60) para varchar(70), acomodando o namespace dos códigos internos sem reduzir os 60 caracteres permitidos ao código original.

Modelagem comercial completa deve ser expandida pelas seções 33 e 34 de REQUIREMENTS. Estoque usará decimal(19,6), dinheiro decimal(19,2), snapshot de preços nos itens, FKs compostas tenant/filial/produto, saldo bloqueado por transação e ledger imutável. Não há tabelas genéricas de processos nem tabelas vazias antecipadas.

## V4 — estoque e perdas

| Tabela | Chaves e campos | Integridade / índices |
|---|---|---|
| warehouse | id UUID PK; tenant_id/branch_id UUID; code varchar(30); name varchar(160); active bool | FK tenant/filial; únicos tenant/id e tenant/filial/code; índice tenant/filial |
| stock_balance | tenant_id/warehouse_id/product_id UUID; quantity/average_cost numeric(19,6) | PK composta dos três IDs; FKs tenant/depósito e tenant/produto; checks não negativos |
| stock_loss | id UUID PK; tenant_id/request_id/warehouse_id/product_id UUID; fingerprint varchar(64); quantity numeric(19,6); reason varchar(30); details varchar(500); estimated/approved_unit_cost numeric(19,6); status varchar(15); created_by/reviewed_by UUID; review_note varchar(500); created/reviewed_at timestamptz | únicos tenant/id e tenant/request_id; FKs compostas depósito/produto/usuários; quantidade positiva; estados PENDING/APPROVED/REJECTED; índice tenant/data |
| stock_operation | tenant_id/id UUID PK composta; fingerprint varchar(64); kind varchar(20); actor_id UUID; created_at timestamptz | FK tenant/ator; kind MOVEMENT/TRANSFER/LOSS_APPROVAL; identifica comando, não substitui entidades de processo |
| stock_movement | id UUID PK; tenant_id/operation_id/warehouse_id/product_id/loss_id UUID; movement_type varchar(30); quantity/balance_after/unit_cost numeric(19,6); unit varchar(10); reason varchar(500); created_at timestamptz | FKs compostas operação/depósito/produto/perda; único tenant/operação/depósito; quantidade não zero, saldo/custo não negativos; índice tenant/depósito/data |

Filial 1:N depósitos; depósito/produto 1:1 saldo; operação 1:N movimentos (transferência possui dois); perda 0:1 baixa lógica pela transação de aprovação. FKs restringem exclusões; sem cascata destrutiva. Saldos não possuem endpoint de escrita direta, ledger sem edição/exclusão via API. Histórico preserva unidade, custo efetivo e saldo após. Alterações privilegiadas diretamente no banco ainda exigem proteção operacional/RLS/retenção a implementar.


## V11 — Tesouraria

treasury_account mantém saldo por filial; treasury_operation registra comandos com fingerprint e UUID único na empresa; treasury_entry preserva débitos/créditos e sequência única por conta. FKs compostas preservam tenant; constraints proíbem saldo negativo e transferência para mesma conta. Locks/transação mantêm ledger e saldos juntos. Sem integração automática com payable_payment.

## V12 — Vínculo de pagamento/tesouraria

Campos opcionais treasury_account_id, treasury_operation_id, treasury_reversal_operation_id em payable_payment, com FKs compostas por tenant, unicidade de operação e check consistente com POSTED/REVERSED. Dados anteriores permanecem nulos. treasury_operation.origin distingue movimentos manuais, pagamentos e estornos. Atualizações de título/pagamento/conta/ledger/auditoria compartilham transação e ordem tenant→título.

## V13–V15 — Clientes/recebíveis

V13 customer com unicidade tenant/code e versão. V14 Java identifica o check de origin no schema atual e o amplia, preservando dados/constraints anteriores sem alterar V12. V15 receivable e receivable_receipt, estado/valor consistentes, UUID/fingerprint, FKs compostas de cliente/filial/ator/tesouraria, operações únicas e histórico de reversão. Sem vínculo automático a venda ou compra. Nenhuma permissão concedida por migration.

## V16 — Índices para fluxo de caixa

Índices tenant/created_at em treasury_operation e tenant/data financeira em payable_payment/receivable_receipt. Relatório agrega ledger e títulos com filtro de escopo em todas as fontes e transação read-only REPEATABLE_READ. Novos movimentos recebem instante explícito após o lock, sem alterar registros existentes. Nenhuma migration concede cashflow:read.

## V17 — Extratos e conciliação

bank_import com UUID/fingerprint e contagens; bank_statement_line único por tenant/conta/external_id, estado/version/vínculo; bank_import_line mantém arquivos sobrepostos; bank_statement_review guarda comandos/versão/histórico. FKs compostas de tenant/conta protegem vínculo a treasury_entry; unicidade de matched_entry_id impede conciliar o mesmo movimento duas vezes. Revisões ordenadas por line_version. Nenhuma alteração em saldos ou grants de usuários.

## V18 — Terminais e caixa

pos_terminal tem conta CASH exclusiva validada na mesma filial; pos_cash_session guarda abertura/operador e snapshot de fechamento. Chaves nullable de sessão aberta impõem unicidade por terminal/operador/conta e são liberadas em CLOSED. Checks exigem contagem/esperado/diferença consistentes. pos_cash_movement liga suprimento/sangria à operação de tesouraria, com UUID/fingerprint e auditoria. FKs compostas preservam tenant. Origem de tesouraria ampliada com POS_SUPPLY/POS_WITHDRAW; movimentos externos bloqueados para conta reservada.

## Vendas com itens — 14/10/2026

V19__pos_sales: pos_terminal.warehouse_id nullable com FK tenant; pos_sale e pos_sale_item imutáveis com snapshots e vínculos a estoque/tesouraria, uniques de request/operação e índices de filial. ck_treasury_origin acrescenta POS_SALE. Terminais existentes sem depósito são configurados explicitamente.

## Pagamentos e cancelamento — 15/10/2026

V20: pos_sale_payment registra parcelas/contas/comprovantes e operações; venda ganha estado/versão/cancelamento; itens guardam retorno de estoque. Backfill V19 não reposta dinheiro. Comprovante único por tenant/conta/meio, inclusive reembolsado. Origem POS_SALE_REVERSAL adicionada.

## Devoluções — V21 (15/10/2026)

V21: pos_sale_return, pos_sale_return_item e pos_sale_return_refund com vínculos compostos à venda/item/pagamento, uniques de request/operação/comprovante e índices por origem. Sem alteração dos snapshots originais ou concessão de permissões. Treasury origin POS_SALE_RETURN.

## Descontos e suspensão — V22 (15/10/2026)

V22: bruto/desconto nos itens, vínculos únicos de aprovação/suspensão nas vendas; pos_discount_policy, pos_discount_request com snapshots, pos_suspension e journal pos_suspension_command. Backfill preserva total antigo como bruto e zero desconto. Sem elevação de usuários existentes.

## Parcelamento financeiro — V23 (16/10/2026)

receivable_plan guarda contrato manual, UUID/fingerprint, filial/cliente, total e calendário; receivable_installment liga posições a títulos existentes com FKs compostas por empresa. Sem backfill financeiro ou concessão de permissões. Rollback de todo o plano em colisão tardia. Ver RECEIVABLE_PLANS.md.

## Crediário — V24 (16/10/2026)

Migração Java V24 localiza dois checks legados de tendered/troco e adapta para total menos credit_amount; operação de tesouraria nula apenas em venda 100% financiada. pos_sale recebe credit_amount, credit_plan_id único e credit_customer_id com FKs compostas. customer_credit_policy registra limite/bloqueio/versão. Legado mantém crédito zero, sem criar dívida, repostar ledger/estoque ou elevar privilégios. Ver POS_CREDIT.md.

## Devoluções de crediário — V25

credited_amount com zero no legado; received_amount líquido de reembolsos e novo status CREDITED, com soma limitado ao original. Check de liquidação localizado por status/received_amount preserva NOT NULL. Ledgers pos_sale_return_debt_credit e pos_sale_return_receipt_refund mantêm origem/título/valor e operação de tesouraria. Índices para recibos/títulos/exposição do cliente. Upgrade dedicado preserva título/recibo antigos, sem ajustes financeiros ou privilégios. Ver POS_CREDIT_RETURNS.md.


## Preparação de pagamento — V26

Veja [POS_PENDING_CHECKOUTS](POS_PENDING_CHECKOUTS.md): preparação persistida sem dinheiro ou reserva, conclusão pela transação normal de venda e descarte auditado. Não implementa cobranças, webhooks ou recebimentos parciais antes da conclusão.

## V27 preparada — financeiro de recebimentos de compra

`purchase_receipt_line.amount` registra custo alocado por diferença acumulada arredondada em centavos; migração atribui valores ao histórico em ordem estável. `purchase_receipt_finance` relaciona uma geração a um recebimento/título, com chave e fingerprint. FK composta exige que título e origem correspondam na mesma empresa. Índice em payable(tenant_id,purchase_receipt_id,status). **Migração não executada ou validada em banco nesta etapa.** Ver [PURCHASE_RECEIPT_FINANCE](PURCHASE_RECEIPT_FINANCE.md).

## V28 preparada — parcelas do recebimento

`purchase_receipt_installment` relaciona empresa/recebimento/posição/título, com posição 1–120 e título único por empresa. Referências compostas exigem a mesma origem. Upgrade inclui gerações V27 como parcela única sem alterar títulos/chaves/fingerprints. **V27 e V28 ainda não aplicadas.** Detalhes em [PURCHASE_INSTALLMENTS](PURCHASE_INSTALLMENTS.md).
