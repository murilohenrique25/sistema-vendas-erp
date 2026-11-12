# Arquitetura

Monólito modular Java 21 / Spring Boot 3.5.16, Angular 21, PostgreSQL 17. Pacotes por domínio: auth, tenant, branch, audit e shared. JPA para entidades comerciais; JDBC parametrizado para sessões, permissões e auditoria. Controllers expõem DTOs, nunca entidades JPA. Flyway controla o schema; Hibernate valida e não cria tabelas.

Isolamento inicial: schema compartilhado, tenant_id obrigatório e consultas explicitamente delimitadas pelo Actor autenticado. Repositories comerciais não expõem findAll nem findById global. FKs compostas impedem associações de usuários de empresas diferentes. Não há RLS nesta versão; implementar defesa adicional e revisar todas as novas consultas antes de produção. Permissões atuais são globais à empresa, destinadas ao administrador; escopos por filial ainda devem ser implementados antes de liberar operadores.

Sessão opaca de 256 bits; somente SHA-256 do token persiste. Expira em oito horas. O backend lê permissões e situação do usuário por requisição. Angular mantém o token em memória. Requisições autenticadas usam Authorization Bearer; não há cookie de autenticação. CSRF está desativado por esse contrato. Se autenticação por cookie for adicionada, reativar proteção CSRF.

Cadastro de empresa, administrador, matriz e auditoria é atômico. Cadastro de filial e auditoria compartilham a transação. Não existem pagamentos, estoque ou documentos fiscais simulados no ambiente de produção.

PDV planejado: PWA para interface e agente local autenticado para hardware homologado; persistência local e outbox com IDs globais. Decisão deve ser confirmada com equipamentos reais e TEF contratado. Autorizações eletrônicas offline dependerão do provedor. IA permanece fora das transações comerciais.

Nenhum Redis ou broker nesta etapa. Outbox transacional será introduzida com pagamentos/fiscal. Não adicionar serviços independentes sem justificar consistência e operação.

Módulos inventory e loss adicionados: StockService gerencia depósitos/saldos/ledger/comandos idempotentes, LossService gerencia ciclo de aprovação. JDBC parametrizado é usado para tornar locks e escrita de ledger explícitos. Transações Spring compartilham saldo, operação, perda e auditoria. Lock do produto serializa concorrência inclusive na primeira movimentação; detalhes em INVENTORY.md. H2/UiTestServer são exclusivos de teste, nunca empacotados no JAR comercial.

## Vendas com itens — 14/10/2026

V19: SaleService conclui venda/itens/estoque/recebimento em uma transação; tenant lock serializa fechamento, produtos em ordem de UUID. Interface de vendas lazy. Ver POS_SALES.md.

## Pagamentos e cancelamento — 15/10/2026

V20: SalePayments coordena parcelas conferidas; SaleCancellationService devolve estoque ao custo original e reverte nas contas originais dentro da mesma transação/tenant lock. Estados comercial/financeiro/fiscal separados na resposta. Não há execução externa de cobrança/reembolso.

## Devoluções — V21 (15/10/2026)

V21: SaleReturnService serializa por empresa e compõe estoque ao custo histórico, reembolsos e histórico numa transação. Projeções acumuladas usam tabelas de retorno; status de recebimento original permanece preservado. Paginação em snapshot e escopo da venda.

## Descontos e suspensão — V22 (15/10/2026)

V22: CartPricing centraliza cálculo/snapshots com locks de produto; DiscountService verifica limite por item e autorização de uso único; SuspensionService gerencia carrinhos versionados sem reserva/pagamento. SaleService valida e consome ambos na transação de estoque/recebimentos.

## Plano de recebíveis — V23

ReceivablePlanService calcula calendário/centavos e coordena ReceivableService na mesma transação, com lock por empresa e journal de requisição no plano. Todos os títulos conservam recebimento parcial/estorno/cancelamento existentes. Prévia usa o mesmo cálculo sem gravar. Frontend carrega Contas a receber sob demanda e invalida a prévia quando o conteúdo muda. Ver RECEIVABLE_PLANS.md.

## Crediário — V24

SaleCreditService coordena política/exposição e ReceivablePlanService na transação de SaleService. Pagamentos cobrem só a entrada; dívida usa títulos financeiros, sem simular recebimento em caixa. SaleCancellationService cancela os títulos e reverte entrada/estoque atomicamente; ReceivableService impede cancelamento manual isolado de título de venda. Estado financeiro da venda é projeção de títulos atuais. Ver POS_CREDIT.md.

## Devoluções financeiras — V25

CreditReturnService integra a transação de SaleReturnService e distribui primeiro dívida, depois reembolsos de pagamentos iniciais e recibos. Ledger imutável de ajustes conserva o original; título armazena líquido recebido e abatimento agregado com status atualizado junto. ReceivableService protege estorno de recibo reembolsado e expõe histórico financeiro paginado. Projeções de crédito/caixa passam a descontar abatimentos. Ver POS_CREDIT_RETURNS.md.


## Preparação de pagamento — V26

Veja [POS_PENDING_CHECKOUTS](POS_PENDING_CHECKOUTS.md): preparação persistida sem dinheiro ou reserva, conclusão pela transação normal de venda e descarte auditado. Não implementa cobranças, webhooks ou recebimentos parciais antes da conclusão.
