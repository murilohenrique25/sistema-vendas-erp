# API v1

JSON. Autenticação por `Authorization: Bearer <token>`. Erros de negócio em `application/problem+json`, sem stack traces. Empresa sempre obtida da sessão. Paginação `page>=0`, `1<=size<=100`, padrão 0/20; resposta `{items,total,page,size}`.

| Método / URI | Acesso | Entrada | Resposta / regras |
|---|---|---|---|
| POST /api/v1/provisioning | segredo X-Provisioning-Key | companyName, slug, adminName, email, password | 201 tenantId; cria matriz/admin/permissões/auditoria em transação; 400/403/409 |
| POST /api/v1/auth/login | público | company (slug), email, password | 200 token, expiresAt, tenantId, name, permissions; 400/401; bloqueio após cinco falhas |
| GET /api/v1/auth/me | sessão válida | — | tenantId, name, permissions; 401 |
| POST /api/v1/auth/logout | sessão válida | — | 204; revoga sessão atual; auditoria |
| POST /api/v1/auth/password | sessão válida | currentPassword, newPassword | 204; revoga todas as sessões do usuário; 400/401 |
| GET /api/v1/auth/sessions | sessão válida | — | lista das próprias sessões não expiradas: createdAt, expiresAt, current; sem tokens/hashes |
| DELETE /api/v1/auth/sessions/others | sessão válida | — | 204; encerra outros acessos do mesmo usuário/empresa, preserva atual; auditoria quando há revogação |
| GET /api/v1/users | user:read | page, size | usuários da empresa: id, name, email, active, version, permissions; nunca senha/hash |
| POST /api/v1/users | user:write | name, email, password, permissions | 201; e-mail normalizado e único por empresa; permissões conhecidas e já possuídas pelo gestor; auditoria transacional |
| PUT /api/v1/users/{id}/active | user:write | active, version (ambos obrigatórios) | DTO atualizado; desativação revoga sessões; 404 outro tenant, 409 versão antiga/própria conta; reativação não restaura sessões |
| PUT /api/v1/users/{id}/permissions | user:write | permissions, version (obrigatórios) | substitui conjunto, incrementa versão e audita somente mudanças; 403 privilégios superiores, 409 versão antiga/último gestor ativo |
| GET /api/v1/access-profiles | user:read | page, size | perfis da empresa com id, name, version, permissions |
| POST /api/v1/access-profiles | user:write | name, permissions | 201 perfil; nome único por empresa; permissões limitadas às do gestor |
| PUT /api/v1/access-profiles/{id} | user:write | name, permissions, version | atualiza definição; 409 versão antiga/duplicidade; não modifica usuários automaticamente |
| PUT /api/v1/users/{id}/profile | user:write | profileId, profileVersion, userVersion | copia conjunto do perfil para usuário; versões obrigatórias, mesmas proteções da edição direta |
| GET /api/v1/users/{id}/branches | user:read + escopo global | — | allBranches, branchIds, version |
| PUT /api/v1/users/{id}/branches | user:write + escopo global | allBranches, branchIds, version | atualiza escopo/version/auditoria; até 100 IDs da mesma empresa; versão antiga 409 |
| GET /api/v1/suppliers | supplier:read | q, page, size | consulta por nome/código/identificação informada; itens e total paginados |
| GET /api/v1/suppliers/{id} | supplier:read | UUID | fornecedor da empresa; 404 para outro tenant |
| POST /api/v1/suppliers | supplier:write | code, name, taxIdentifier, contactName, email, phone, address, notes | 201 DTO; ativo inicialmente; código único por empresa e normalizado em maiúsculas |
| PUT /api/v1/suppliers/{id} | supplier:write | data (mesmos campos do cadastro), active, version | edição/ativação/desativação; 409 versão antiga/duplicidade; no-op sem nova auditoria |
| GET /api/v1/purchase-orders | purchase:read | page, size | cabeçalhos filtrados por filial autorizada |
| GET /api/v1/purchase-orders/{id} | purchase:read | UUID | {header,lines}; snapshots e recebimentos acumulados |
| POST /api/v1/purchase-orders | purchase:read + write | requestId, branchId, supplierId, notes, items[{productId,quantity,unitCost}] | 201 pedido OPEN; até 100 produtos distintos; idempotência persistida |
| GET /api/v1/purchase-orders/{id}/receipts | purchase:read | page, size | histórico paginado com itens/operações/ator/data |
| POST /api/v1/purchase-orders/{id}/receipts | purchase:read + receive | receiptId, warehouseId, note, items[{orderLineId,quantity}] | 200 recebimento; entrada atômica de estoque ao custo negociado; replay original |
| POST /api/v1/purchase-orders/{id}/cancel | purchase:read + write | version, note | encerra saldo pendente; preserva estoque recebido; 409 versão/estado |
| GET /api/v1/payables/calendar | payable:read | — | data/fuso financeiros do servidor |
| GET /api/v1/payables | payable:read | status (vazio/OPEN/PARTIAL/PAID/CANCELLED), overdue, page, size | títulos/saldos filtrados por filial e vencimento |
| GET /api/v1/payables/{id} | payable:read | UUID | título, valor/pago/pendente/estado/version e vínculo opcional a compra |
| POST /api/v1/payables | payable:read + write | requestId, branchId, supplierId, purchaseReceiptId opcional, reference, document, description, issuedOn, dueOn, amount | 201; referência única; criação idempotente |
| GET /api/v1/payables/{id}/payments | payable:read | page, size | histórico paginado, incluindo registros estornados |
| POST /api/v1/payables/{id}/payments | payable:read + pay | paymentId, amount, paidOn, method, reference, note | 200 registro parcial/completo; não executa transferência bancária |
| POST /api/v1/payables/{id}/payments/{paymentId}/reverse | payable:read + reverse | requestId, note | 200; estorno do registro com histórico; não é refund bancário |
| POST /api/v1/payables/{id}/cancel | payable:read + write | version, note | apenas sem valor pago; conserva histórico; 409 versão/estado |
| GET /api/v1/branches | branch:read | page, size | lista paginada por código; 400/401/403 |
| GET /api/v1/branches/{id} | branch:read | UUID | DTO id/code/name/active/version; 404 inclusive para outro tenant |
| POST /api/v1/branches | branch:write | code (1–30 alfanumérico/-/_), name (1–160) | 201 DTO; auditoria; 400/401/403/409 |
| GET /api/v1/audit | audit:read | page, size | lista paginada mais recente primeiro; 400/401/403 |
| GET /api/v1/products | product:read | q, page, size | busca nome/SKU/código parcial; DTOs e total |
| GET /api/v1/products/{id} | product:read | UUID | produto da empresa; 404 para inexistente/outro tenant |
| GET /api/v1/products/lookup | product:read | code, kind=GTIN/INTERNAL | consulta base interna; 400 para GTIN inválido; 404 ausente |
| POST /api/v1/products | product:write | sku, name, description, unit, salePrice, barcodes | 201 DTO; 400 validação, 409 duplicidade; produto/códigos/auditoria atômicos |

Provisionamento: slug 3–80 letras minúsculas/números/hífens; nomes até 160; e-mail até 254; senha ASCII 12–72. Duplicidade de slug/email/código retorna 409. Retry de cadastro pode retornar conflito; ainda não há chave de idempotência para cadastros. APIs monetárias deverão exigir idempotência persistida antes de sua implementação.

OpenAPI gerado via Springdoc em `/v3/api-docs` quando `API_DOCS_ENABLED=true`; exige sessão válida e acesso direto ao backend. Não habilitar em produção pública sem política apropriada.

Usuários: nome até 160, e-mail até 254, senha inicial ASCII 12–72; permissions até 30 itens, sem privilégios implícitos. Gestor só concede permissões que possui e só reativa/edita contas cujos privilégios possui. user:write exige user:read nos conjuntos novos/editados. Edição preserva ao menos um gestor ativo; revogação vale na próxima requisição das sessões existentes. Repetir conjunto sem alteração com versão atual não incrementa versão/auditoria; versão antiga retorna 409. Login encaminha ao primeiro módulo autorizado ou Segurança da conta. Novas empresas recebem user:read/write no provisionamento; nenhuma migration concede essas permissões a usuários existentes. Escopos por filial ainda pendentes.

Perfis são modelos reutilizáveis, com nome de até 160 caracteres e permissões explícitas. Aplicação é uma cópia: não cria vínculo dinâmico nem soma privilégios; substitui o conjunto atual do usuário. Mudanças futuras exigem reaplicação explícita. Não há exclusão de perfil ou atribuição automática nesta etapa. Criar/editar/aplicar serializa no mesmo lock administrativo por tenant; versões impedem aplicação de definições antigas. Auditoria de aplicação usa USER_PERMISSIONS_CHANGED quando altera o usuário.

Escopo de filial: allBranches=true inclui filiais atuais/futuras e exige branchIds vazio. false com lista vazia não permite nenhuma filial; listas são deduplicadas. Usuários existentes e novos preservam acesso global por padrão; o gestor deve restringir explicitamente. Gestor com user:write precisa de acesso global; concessão administrativa a conta restrita é rejeitada. Administração de usuários/perfis, auditoria e criação de filial exigem escopo global, além da permissão. Catálogo permanece compartilhado pela empresa. Filiais/depósitos/perdas são filtrados antes da paginação, inclusive total; IDs sem acesso retornam 404. Transferências exigem origem e destino autorizados. Saldos, ledger, entradas/saídas, perdas e revisão verificam escopo na API. Login e /auth/me retornam allBranches; sessões existentes usam regras atuais no banco. Requisições já em andamento podem concluir conforme a autorização verificada no início da operação; não há cancelamento distribuído de operações em curso.

Fornecedores são compartilhados pelas filiais da mesma empresa, mediante supplier:read/write. Código 1–40 caracteres alfanuméricos/._-, nome até 200; demais campos aceitam string vazia: taxIdentifier/phone até 40, contactName até 160, email até 254 (formato validado quando informado), address até 500, notes até 2000. Identificação fiscal é declarada, sem consulta oficial/dígito verificador/regras fiscais nesta etapa. Código canônico é único; identificação fiscal não é chave única. Alterações usam lock de fornecedor, versão e auditoria transacional SUPPLIER_CREATED/SUPPLIER_UPDATED. Não há exclusão de fornecedor; contas a pagar ainda pendentes. Novas empresas recebem supplier:read/write; migrations não concedem privilégios a usuários existentes.

Compras: notes até 500; justificativa de recebimento/cancelamento 5–500 após trim. Quantidade positiva/custo não negativo com até 13 dígitos inteiros/seis casas; JSON deve usar strings decimais. Pedido OPEN/PARTIAL/RECEIVED/CANCELLED. Recebimento só em depósito ativo da filial do pedido, sem ultrapassar quantidade pendente. Custo vem do pedido; não é aceito custo arbitrário no recebimento. 400 validação/duplicidade de item/depósito de outra filial/overflow monetário; 404 referência/filial/tenant; 409 estado/quantidade/chave reutilizada/versão. Regras, fórmula, atomicidade e limitações em PURCHASES.md. Pedidos/recebimentos e lançamento explícito de contas a pagar implementados; XML, impostos/frete, devoluções e geração financeira automática ainda pendentes.

Contas a pagar: referência interna 1–60 alfanuméricos/._-, documento/referência de pagamento até 160; descrição e justificativas 5–500 após trim. Valores positivos com até 17 dígitos inteiros/duas casas, strings decimais. Datas YYYY-MM-DD; pagamento entre emissão e data financeira atual. Meios declarados CASH/BANK_TRANSFER/PIX/OTHER. paymentId e chave de estorno únicos por tenant. Retry de pagamento já estornado retorna o registro REVERSED, sem repagamento. Vínculo de recebimento exige fornecedor/filial correspondentes. Regras e limites em PAYABLES.md. Não há integração bancária, tesouraria ou conciliação nesta etapa.

Produtos: SKU até 60 caracteres alfanuméricos/._-, nome até 200, descrição até 2000, unidade UN/CX/PCT/KG/G/L/ML/M/M2. salePrice é decimal não negativo, até 17 dígitos inteiros e duas casas; enviá-lo como string e recebê-lo como string evita conversões monetárias binárias no frontend. Até 20 códigos `{code,kind}`; GTIN suporta 8/12/13/14 dígitos e valida Mod-10. Normalização para 14 dígitos impede duplicidade de representações equivalentes. Código INTERNAL pode ser alfanumérico e não usa dígito verificador. Tipos possuem namespace separado; lookup deve indicar o tipo. Código não cadastrado nunca consulta serviços externos.

## Estoque e perdas

| Método / URI | Permissão | Entrada / saída |
|---|---|---|
| GET /api/v1/inventory/warehouses | inventory:read | page/size; lista de depósitos |
| POST /api/v1/inventory/warehouses | inventory:write | branchId, code, name; 201 Warehouse; 404 filial inválida/outro tenant; 409 código duplicado na filial |
| GET /api/v1/inventory/warehouses/{id}/balances | inventory:read | q/page/size; saldos por produto; averageCost null sem inventory:cost |
| GET /api/v1/inventory/warehouses/{id}/movements | inventory:read | page/size; histórico com ator/operação/unidade/quantidade/saldo após/custo/justificativa |
| POST /api/v1/inventory/movements | inventory:write | operationId, warehouseId, productId, type, quantity, unitCost, reason; 200 operação/movimentos, inclusive retry |
| POST /api/v1/inventory/transfers | inventory:write | operationId, sourceWarehouseId, destinationWarehouseId, productId, quantity, reason; 200 operação com dois movimentos |
| GET /api/v1/losses | loss:read | page/size; perdas; totais null sem inventory:cost |
| GET /api/v1/losses/{id} | loss:read | registro com estado, solicitante/aprovador/datas e justificativa |
| POST /api/v1/losses | loss:create | requestId, warehouseId, productId, quantity, reason, details; 200 registro pendente, inclusive retry |
| POST /api/v1/losses/{id}/approve | loss:approve | operationId; 200 registro aprovado; replay por estado sem nova baixa |
| POST /api/v1/losses/{id}/reject | loss:approve | note; 200 registro rejeitado; retry com mesma nota sem nova alteração |

Quantidade estritamente positiva, até 13 dígitos inteiros/seis casas; unitCost não negativo com mesma precisão. Movimento type: RECEIPT/ADJUSTMENT_IN (custo obrigatório), ADJUSTMENT_OUT/INTERNAL_CONSUMPTION (unitCost null). reason/details/note: 5–500 caracteres após trim; UUIDs e tenant validados no backend. Tipo LOSS não é permitido em movimento manual, sendo produzido apenas por aprovação.

Erros: 400 campos/custo/tipo inválidos, 401 sessão, 403 permissão, 404 referência ausente/de outro tenant, 409 saldo insuficiente, overflow, chave idempotente com outro payload ou transição inválida. Operações têm auditoria transacional. Consulta de outra empresa retorna 404. Regras/fórmula/limitações em INVENTORY.md.



## Tesouraria (V11)

GET /api/v1/treasury/accounts?page=0&size=20; GET /api/v1/treasury/accounts/{id}; POST /api/v1/treasury/accounts com requestId, branchId, code, name e kind CASH/BANK.
POST /api/v1/treasury/movements com requestId, sourceId (nullable), destinationId (nullable), amount e note. Uma conta omitida representa movimento externo; duas contas representam transferência interna. GET /api/v1/treasury/accounts/{id}/entries paginado. Valores monetários retornados como strings. Leia TREASURY.md para permissões, idempotência e limites.

## Pagamentos integrados à tesouraria (V12)

POST /api/v1/payables/{id}/payments aceita treasuryAccountId opcional. Quando presente exige treasury:read/move, debita a conta na transação e retorna treasuryAccountId/treasuryOperationId. POST .../payments/{paymentId}/reverse gera crédito vinculado e retorna treasuryReversalOperationId. Sem conta mantém o contrato anterior e vínculos nulos. Extrato retorna origin MANUAL/PAYABLE_PAYMENT/PAYABLE_REVERSAL. Erros de saldo, escopo e chave desfazem toda a operação; nenhuma ordem bancária é executada.

## Clientes e contas a receber (V13–V15)

GET/POST /api/v1/customers, GET/PUT /api/v1/customers/{id}, cadastro equivalente aos campos de fornecedor, com permissões customer:read/write. GET /api/v1/receivables (status OPEN/PARTIAL/RECEIVED/CANCELLED, overdue, page, size); GET /calendar e /{id}; POST cria título com requestId/branchId/customerId/reference/document/description/issuedOn/dueOn/amount. POST /{id}/receipts registra receiptId/amount/receivedOn/method/reference/note/treasuryAccountId opcional. GET /{id}/receipts lista histórico; POST /{id}/receipts/{receiptId}/reverse e /{id}/cancel usam justificativa/identidade ou versão. Valores como strings; vínculo credita no recebimento e debita no estorno; requer tesouraria e escopos. Veja RECEIVABLES.md.

## Fluxo de caixa (V16)

GET /api/v1/cash-flow?from=YYYY-MM-DD&to=YYYY-MM-DD&branchId=UUID opcional retorna summary/actual/pending, datas/fuso e hora de geração. Intervalo inclusivo de até 366 dias. GET /calendar fornece dia/fuso financeiro. Requer cashflow:read + treasury:read + payable:read + receivable:read. Realizado usa registro do ledger; pendentes usam posição atual e vencimento; sem vínculo é indicador separado. Leia CASH_FLOW.md para fórmulas e atribuição de filial.

## Conciliação (V17)

/api/v1/reconciliation: GET /calendar; POST /imports para lote estruturado; POST /imports/csv/preview para validar sem gravar; POST /imports/csv para importar. Campos requestId/accountId/reference e lines(externalId/postedOn/amount/description) ou content CSV. GET /accounts/{id}/imports, /lines (status/importId/page/size), /summary. GET /lines/{id}, /candidates e /history paginados. POST /lines/{id}/match|ignore|reset com requestId/version/entryId opcional/note. Requer reconciliation:read e treasury:read; import/preview exige reconciliation:import; revisão exige reconciliation:review. Sem alteração de saldo. Formato/estados/idempotência em RECONCILIATION.md.

## Terminais e sessões de caixa (V18)

/api/v1/pos: GET /operator fornece identidade do operador autenticado; GET/POST /terminals, GET /terminals/{id}, PUT /terminals/{id}/active; GET/POST /cash-sessions e GET /cash-sessions/{id}; GET/POST /cash-sessions/{id}/movements; POST /cash-sessions/{id}/close. Listas paginadas/filtradas por escopo. Abertura usa requestId/terminalId/openingAmount/note; movimento requestId/kind SUPPLY|WITHDRAW/amount/otherAccountId opcional/note; fechamento requestId/version/countedAmount/note. Pos:read e treasury:read em tudo; permissões de manage/operate/close/supervise e treasury:move conforme operação. Leia POS_CASH.md para reserva de conta e divergência sem ajuste automático.

## Vendas com itens — 14/10/2026

V19: PUT /api/v1/pos/terminals/{id}/warehouse (warehouseId/version); GET /api/v1/pos/sales e /sales/{id}; POST /sales (requestId/sessionId/tendered/items productId+quantity+expectedUnitPrice). Venda exige pos:sell + pos:read + treasury:read/move e operador/supervisor. Contratos/replay/limites em POS_SALES.md.

## Pagamentos e cancelamento — 15/10/2026

V20: POST /pos/sales aceita payments (CASH/PIX/BANK_TRANSFER) ou tendered legado. POST /pos/sales/{id}/cancel recebe requestId/version/note/externalRefundsConfirmed. Contratos, permissões, estados e limites em POS_PAYMENTS.md.

## Devoluções — V21 (15/10/2026)

V21: GET/POST /api/v1/pos/sales/{saleId}/returns e GET /returns/{id}. Criação com requestId/sessionId/version/note/items/refunds e confirmação externa; requer pos:return, caixa aberto da filial e operador/supervisor. Projeções returnedQuantity/returnedAmount/refundedAmount/refundStatus e estados derivados. Ver POS_RETURNS.md.

## Descontos e suspensão — V22 (15/10/2026)

V22: discountAmount por item e discountApprovalId/suspensionId/suspensionVersion opcionais na conclusão. Endpoints /api/v1/pos/discount-policies/{branch}, /discount-requests e /{id}/decision, /suspensions e /{id}/discard. Regras/contratos/permissões/estados em POS_COMMERCIAL.md.

## Parcelamento financeiro — V23 (16/10/2026)

POST /api/v1/receivables/plans/preview e POST /plans geram prévia e títulos em lote atômico; GET /plans/{id} consulta estado atual. Campos/regras e limites em RECEIVABLE_PLANS.md. Exige leitura/escrita para gerar, leitura para consultar e escopo de filial. Não integra ainda venda a prazo no PDV.

## Crediário no PDV — V24 (16/10/2026)

GET/PUT /api/v1/customers/{id}/credit-policy (customer:read/credit e todas as filiais). PUT version/creditLimit/blocked. POST /api/v1/pos/sales aceita credit(customerId/amount/firstDueOn/installmentCount/intervalMonths); exige pos:credit, customer:read, receivable:read/write e permissões anteriores da venda. Entrada cobre somente total-credit.amount; sem entrada omita pagamentos/tendered. DTO credit e estado financeiro derivado do saldo dos títulos. Cancelamento individual de título de venda bloqueado. Contrato e limites: POS_CREDIT.md.

## Devoluções de crediário — V25 (16/10/2026)

POST /api/v1/pos/sales/{saleId}/returns aceita expectedDebtReduction e receiptRefunds(receiptId/amount/method/accountId/reference). Credito exige conferência do abatimento e receivable:credit/read além das permissões do retorno. GET /returns/credit-receipts pagina origens financeiras da própria venda. GET /api/v1/receivables/{id}/adjustments pagina abatimentos/reembolsos com escopo do título. DTO de título acrescenta creditedAmount/status CREDITED; recibo refundedAmount; venda SETTLED_BY_RETURN em devolução integral. Regras e limites em POS_CREDIT_RETURNS.md.


## Preparação de pagamento — V26

Veja [POS_PENDING_CHECKOUTS](POS_PENDING_CHECKOUTS.md): preparação persistida sem dinheiro ou reserva, conclusão pela transação normal de venda e descarte auditado. Não implementa cobranças, webhooks ou recebimentos parciais antes da conclusão.

## Histórico de vendas — filtros opcionais

`GET /api/v1/pos/sales` aceita `from`, `to`, `query`, `commercialStatus`, `method`, `credit` e `branchId`, mantendo `page`/`size`. Contrato, semântica das datas/estados e limitações em [POS_SALE_HISTORY](POS_SALE_HISTORY.md). Implementação compilada, com testes funcionais/integrados adiados; sem alteração de schema ou implantação.

## Manutenção de produtos

`PUT /api/v1/products/{id}` recebe `{version,product}` e `PUT /api/v1/products/{id}/active` recebe `{version,active}`, com permissões product:read/write. `GET /api/v1/products` aceita `active` opcional. Detalhes, regras de reenvio e validações adiadas em [PRODUCT_MAINTENANCE](PRODUCT_MAINTENANCE.md). Sem alteração de schema.

## Conta a pagar por recebimento

GET/POST `/api/v1/purchase-orders/{orderId}/receipts/{receiptId}/payable`: consulta e geração explícita de um título pelo valor histórico recebido, com proteção de origem/reenvio. V27 preparada e não aplicada; testes adiados. Contrato em [PURCHASE_RECEIPT_FINANCE](PURCHASE_RECEIPT_FINANCE.md).

## Parcelamento das compras — V28 preparada

Financeiro do recebimento aceita `installmentCount` e `intervalMonths` opcionais. POST `/api/v1/purchase-orders/{orderId}/receipts/{receiptId}/payable/preview` calcula calendário. Geração usa o mesmo recurso base e retorna também `generatedTitles`. Contrato e limites em [PURCHASE_INSTALLMENTS](PURCHASE_INSTALLMENTS.md). Testes/migrações ainda não executados.

## Busca e totais de contas a pagar

GET `/api/v1/payables` acrescenta `query`, `dueFrom`, `dueTo`, `branchId`, `supplierId`, `receiptId`, `withReceipt`. Resposta acrescenta `summary` monetário, `today` e `zone`, mantendo a página existente. Contrato e limites em [PAYABLE_SEARCH](PAYABLE_SEARCH.md). Implementação compilada, testes adiados; sem nova migração.

## Busca, totais e origem de contas a receber

GET `/api/v1/receivables` acrescenta `query`, `dueFrom`, `dueTo`, `branchId`, `customerId`, `planId`, `saleId` e `origin`; resposta acrescenta `summary`, `today`, `zone`. GET `/{id}/origin` consulta grupo/posição/venda de origem após escopo do título. Contrato em [RECEIVABLE_SEARCH](RECEIVABLE_SEARCH.md), implementação compilada com testes adiados, sem nova migração.
