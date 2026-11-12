# Testes

Suite `FoundationTest`: aplicação Spring Boot completa, MockMvc, Flyway, JDBC e JPA reais com H2 em modo PostgreSQL. Verifica autenticação obrigatória, criação/listagem/auditoria/logout, isolamento entre tenants com header forjado, revogação de permissão, validação/duplicidade, proteção de provisionamento, bloqueio de login persistido, troca de senha com revogação global e expiração.

`FoundationPostgresIT` herda os mesmos cenários e substitui o datasource por PostgreSQL 17 em Testcontainers. Executar com `mvn -f backend/pom.xml verify -Ppostgres-tests`; falha se Docker não estiver disponível, sem skip silencioso. Sucesso H2 não comprova PostgreSQL.

Frontend: `npm run typecheck` e `npm run build` verificam TypeScript/templates Angular. Testes de interação/E2E e revisão visual ainda necessários. CI exige ambas as verificações mais PostgreSQL. Testes de concorrência existem para catálogo, estoque e usuários. Carga/fiscal/offline/pagamento/backup ainda não possuem validação.

Resultados efetivamente executados e limitações do ambiente serão registrados em PROGRESS.md.

Resultado final desta execução: 25 testes locais passaram com V1–V3 e JAR foi gerado. Quinze testes PostgreSQL passaram antes de V3. Repetição final PostgreSQL bloqueada na criação de container por filesystem Docker somente leitura; não é sucesso nem teste funcional reprovado, pois os cenários não iniciaram. Evidências da aprovação anterior e do bloqueio estão separadas em documentation/test-evidence.

Catálogo: seis cenários adicionais no FoundationTest verificam persistência/busca/GTIN equivalente, rollback integral de duplicidade, rejeição de GTIN/preços inválidos, isolamento de códigos e produtos, permissões/códigos internos e dois cadastros concorrentes (exatamente um 201 e um 409, uma linha e uma auditoria). BarcodeTest cobre dez vetores válidos/inválidos nos quatro tamanhos. Dados são criados somente em bancos descartáveis de testes.

## Segundo marco — estoque/perdas

41 testes locais aprovados, zero falhas/skips. ApiTestSupport compartilha fixtures sem duplicar testes. InventoryTest adiciona 16 cenários de custo/estoque/idempotência/concorrência/transferência/rollback/perdas/permissões e limites. Após recuperação temporária do Docker, **15 cenários FoundationPostgresIT e 16 InventoryPostgresIT passaram com V1–V4 em PostgreSQL 17.6**, sem falhas/skips. Evidências atuais: foundation-v4-postgres.txt e inventory-v4-postgres.txt. Registros anteriores de bloqueio são histórico e não substituem essas evidências.

Browser integrado foi executado com UiTestServer: login, produto, depósito, entrada 3 kg/R$10, perda 2 kg/aprovação R$20 e saldo final 1 kg/histórico. É verificação de interação executada pelo navegador, ainda sem suite E2E automatizada. Evidência PNG em documentation/test-evidence/loss-approval-ui.png.

Reproduzir em ambiente descartável: executar scripts/start-ui-test-server.ps1; em outro terminal, dentro de frontend, executar `npx ng serve --host 127.0.0.1 --port 4202 --proxy-config proxy.ui-test.conf.json`. Empresa interface-test, e-mail ui@example.invalid, senha Temporary-ui-test-2026. Esses dados existem somente no harness de teste, não em produção; parar a API descarta o banco. Portas do harness são restritas a 127.0.0.1. Os fixtures não estão no JAR comercial.

## Marco atual — usuários e permissões

55 testes locais aprovados; UserTest/UserPostgresIT com 12 cenários cada, V1–V5, sem falhas/skips. Incluem versão, revogação imediata em sessão aberta, no-op sem auditoria adicional, privilégios superiores, isolamento e remoção concorrente de gestores. Evidências user-permissions-h2.txt e user-permissions-postgres.txt. Angular build aprovado; interação visual de usuários/permissões ainda pendente.


## Perfis e backup

60 testes locais e cinco AccessProfilePostgresIT aprovados, V1–V6. Perfis: aplicação por cópia, definição antiga/usuário antigo, tenant, escalada, último gestor, no-op e atualizações concorrentes. Evidências access-profiles-h2.txt/access-profiles-postgres.txt. Backup cifrado restaurado em container descartável; GCM rejeitou ciphertext alterado e chave incorreta. Banco usado no ensaio estava sem empresas; testes com volume operacional permanecem pendentes. Evidência backup-restore.json.


## Escopo por filial

65 testes locais e cinco BranchScopePostgresIT aprovados, V1–V7. BranchScopeTest cobre filtros/totais, recursos por ID, sessão aberta, transferência e rollback, aprovação/rejeição, gestor/global, lista vazia, tenant e versões. Evidências branch-scope-h2.txt/branch-scope-postgres.txt. Angular build aprovado; verificação visual da seleção de filiais pendente.


## Fornecedores

71 testes locais e seis SupplierPostgresIT aprovados, V1–V8. Cadastro/busca/canonicalização, tenant, duplicidade/rollback, estado/no-op/versões, permissões e revogação, validação aninhada e concorrência. Evidências suppliers-h2.txt/suppliers-postgres.txt. Angular compilado; teste visual e login real permanecem adiados.


## Pedidos de compra e recebimentos

Regressão de 82 testes locais aprovada; 14 PurchaseTest e 14 PurchasePostgresIT finais aprovados, V1–V9, sem falhas/skips. Três casos adicionais à regressão inicial: criação simultânea por chave, namespace de estoque manual e recebimento sem permissão de ajuste manual. Também há parcial/completo/custo médio, replay, over-receipt, rollback multi-item, cancelamento concorrente, tenant/filial/depósito e lock multi-produto. Evidências purchases-h2.txt/purchases-postgres.txt. Angular build aprovado. Verificação visual e login real ainda adiados.


## Contas a pagar

Regressão de 97 testes locais aprovada; depois 14 PayableTest e 14 PayablePostgresIT finais aprovados com V1–V10. Dois casos adicionais: chave de pagamento concorrente entre títulos e inexistência indistinguível de pagamento em outra filial. Cobertura de vínculo a recebimento, estados/saldos/overdue, datas/precisão, idempotência, estorno com histórico, cancelamento, rollback e concorrência. Evidências payables-h2.txt/payables-postgres.txt. Angular compilado. Nenhum teste fez transferência bancária ou gravou títulos no banco de clientes.


## Tesouraria (V11)

TreasuryTest e TreasuryPostgresIT repetem oito cenários: cadastro/reenvio, transferência/rollback/reenvio, validação, overflow, isolamento/permissão, escopo das duas filiais, débito concorrente e retry concorrente. Evidências treasury-h2.txt e treasury-postgres.txt. Build Angular valida a tela; homologação visual/login real seguem pendentes.

## V12 — Pagamentos/tesouraria

PayableTreasuryTest/PostgresIT: 13 cenários para débito/estorno/replay, insuficiência/overflow, troca/omissão de conta no retry, legado sem privilégio de tesouraria, permissões, filial/tenant, chave duplicada com rollback, disputa entre títulos e disputa com retirada manual. Executar junto com PayableTest/TreasuryTest para validar a compatibilidade. Evidências payable-treasury-h2.txt (32 testes iniciais, antes dos três cenários finais) e payable-treasury-postgres.txt (regressão e PostgreSQL). Build Angular verifica a seleção paginada de contas e identificação de origem no extrato; login real/revisão visual seguem adiados.

V12: execução final verify -Ppostgres-tests com PayableTreasuryPostgresIT,PayablePostgresIT,TreasuryPostgresIT aprovada. 120 testes H2/unitários e 35 PostgreSQL (13 integração, 14 títulos, oito tesouraria), zero falhas/skips. Angular aprovado, 445,02 kB. Evidência final payable-treasury-postgres.txt inclui a regressão e as três suítes PostgreSQL.

## Clientes e recebíveis

CustomerTest seis cenários e ReceivableTest 13 cenários, ReceivableTreasuryTest nove cenários. As versões PostgresIT usam PostgreSQL descartável. Incluem crédito/estorno, fundos já consumidos, limites, retry, concorrência com retirada, isolamento de tenant/filial e permissões completas. Evidências customers-h2.txt e receivables-h2.txt; execução completa em receivables-postgres.txt. Build Angular cobre telas customers/receivables e origens do extrato. Homologação visual e login real seguem adiados pelo usuário.

V13–V15: verify aprovado com 148 testes H2/unitários e 41 PostgreSQL (Customer seis, Receivable 13, ReceivableTreasury nove, PayableTreasury 13); zero falhas/skips. Build Angular aprovado, 476,25 kB, bundle main-RKI2NIPO.js. Evidência receivables-postgres.txt contém regressão e execuções em PostgreSQL.

## Fluxo de caixa — V16

CashFlowTest e CashFlowPostgresIT com dez cenários: vazio/calendário, abertura/fechamento e bordas de fuso, transferências, parcial sem duplicação, liquidação/cancelamento/estorno, vencidos, sem vínculo, escopo, tenant/permissões e limite de datas. Evidência cash-flow-h2.txt; regressão/PG em cash-flow-postgres.txt. AP/AR com tesouraria revalidados pelo novo timestamp explícito. Interface compilada; login real/revisão visual seguem adiados.

V16: verify aprovado com 158 testes H2/unitários e 32 PostgreSQL (CashFlow dez, PayableTreasury 13, ReceivableTreasury nove); zero falhas/skips. Build Angular aprovado, 485,81 kB, bundle main-RG47FD4H.js. Evidência cash-flow-postgres.txt contém a regressão e as três suítes PG.

## Conciliação — V17

ReconciliationTest/PostgresIT com 13 cenários: import/replay/overlap, conflito com rollback, match/reset/replay/history, unicidade/sinal/conta, ignorar/versionar, reutilização de chave, prévia/CSV BOM/aspas/multilinha, formato/valores, tipo de conta/data futura, escopo, tenant/permissões e concorrência. Evidências reconciliation-h2.txt e reconciliation-postgres.txt. Build Angular valida upload/prévia/estado/histórico e lazy chunk. Login real/revisão visual seguem adiados.

V17: verify aprovado com 171 testes H2/unitários e 13 PostgreSQL de conciliação; zero falhas/skips. Evidência reconciliation-postgres.txt inclui regressão e suíte real. Angular aprovado: initial 488,01 kB e componente de conciliação sob demanda (~21,5 kB).

Build final V17: main-MYHV5VIR.js, shared chunk-M6STXXTF.js e lazy chunk-G5ZS55UX.js (21,74 kB). Initial permanece 488,01 kB. Rótulos de origem de movimentos apresentados em português.

## Caixa do PDV — V18

CashSessionTest/PostgresIT: 14 cenários iniciais para exclusividade, abertura, movimentos integrados, insuficiência, fechamento/replay/diferença, reserva externa/financeira, operador/supervisor, versão/ativação, escopo/permissão e concorrência. Ajuste final acrescenta um 15º cenário de reenvio após restrição da filial contraparte. Evidências pos-cash-h2.txt, pos-cash-postgres.txt e pos-cash-final.txt. Build Angular valida componente sob demanda. Login real e visual seguem adiados.

V18: regressão inicial 185 testes sem falhas/skips e módulos afetados 36 PostgreSQL (CashSession 14, AP 13, AR nove). Ajuste final validado com 15 CashSession H2 e 15 PostgreSQL, incluindo um cenário adicional de escopo da contraparte em replay. Não houve uma nova execução integral de 186 casos. JAR final gerado em verify. Angular initial 489,56 kB, shared chunk-WOJ4NHG2.js, main-7TMBTZ2Q.js, lazy pos-cash chunk-HSGY22JF.js e reconciliation chunk-K6PKFGDB.js.

## Vendas com itens — V19

Suíte H2 completa: 201 testes sem falhas. SaleTest possui 15 cenários: arredondamento/troco/crédito líquido, histórico após alteração, preço desatualizado, retry e colisão, rollback multi-item, validação de recebimento/quantidades, inatividade, configuração versionada de depósito, sessão fechada, tenant/filial, operador/permissões, overflow, retries concorrentes, vendas disputando estoque e fechamento concorrente. Build Angular initial 495,23 kB/lazy vendas 15,15 kB. Evidências pos-sale-regression-postgres.txt e pos-sale-frontend.txt. Login real e revisão visual seguem adiados.

PostgreSQL: 46 cenários aprovados, zero falhas/erros/skips — SalePostgresIT 15, CashSessionPostgresIT 15 e InventoryPostgresIT 16. Comando Maven: -B -o -f backend/pom.xml -Ppostgres-tests '-Dit.test=SalePostgresIT,CashSessionPostgresIT,InventoryPostgresIT' verify (com cache local configurado).

Ambiente aplicado: API UP, UI e todos os cinco scripts HTTP 200, API de vendas anônima 401, migration 19, zero dados permanentes; pos-sale-local.json. Restore V19 aprovado com adulteração e chave incorreta rejeitadas; backup-restore-v19.json. Esta verificação HTTP/build não substitui revisão visual ou login real, que permanecem adiados.

## Pagamentos e cancelamento — V20 (15/10/2026)

Regressão integral H2: 222 testes, zero falhas/erros/skips. PostgreSQL: 67 cenários — caixa 15, estoque 16, vendas 15, pagamentos/cancelamento 20 e migração V19→V20 com venda antiga 1. Migração testada também em H2, sem repostar dinheiro ou conceder permissões. Evidência pos-mixed-regression-postgres.txt. Build Angular atualizado: initial 495,37 kB e lazy vendas 24,39 kB; pos-mixed-frontend.txt. Login real e revisão visual seguem adiados.

Cenários novos cobrem parcelas/contas/troco, total exato, confirmação externa, comprovante repetido, isolamento/contas/replay, retorno pelo custo histórico, rollback financeiro/multi-conta, produtos inativos/unidade alterada, concorrência e fechamento, permissões de cancelamento, versão e UUID. Hikari pode registrar conexão fechada quando o Testcontainers encerra um banco anterior ainda referenciado pelo contexto em cache; todos os testes concluíram sem erros.

Aplicação V20 conferida: três serviços saudáveis, API UP, UI/scripts/CSS 200, APIs sem token 401, migration 20, zero dados permanentes. Restore V20 aprovado. Evidências pos-mixed-local.json e backup-restore-v20.json. Build também executado após realocar dependências para D: via junção Windows. Verificação visual e login real não foram executados.

## Devoluções — V21 (15/10/2026)

Regressão H2: 247 testes, zero falhas/erros/skips. SaleReturnTest possui 25 cenários: devolução parcial, arredondamento acumulado/zero centavo, outro turno/gaveta, preservação do caixa original, limites por quantidade/pagamento, reembolsos bancários/comprovantes, custo histórico, inatividade/unidade, rollback financeiro e entre itens, isolamento/operador/supervisor/filial, UUID/replay e concorrência com fechamento. Os 35 cenários anteriores de vendas/pagamentos continuam aprovados. PostgreSQL em execução na evidência pos-return-regression-postgres.txt; resultado final registrado abaixo quando concluído.

Build Angular: initial 495,70 kB; lazy PDV/vendas 38,09 kB. Interface com cálculo acumulado em inteiros e proteção mútua de comandos pendentes. Evidência pos-return-frontend.txt. Login real e revisão visual adiados.

Resultado final V21: 92 testes PostgreSQL aprovados, sem falhas/erros/skips — devoluções 25, vendas 15, pagamentos/cancelamento 20, estoque 16, caixa 15 e migração 1. Suíte H2 integral 247 aprovada. Nenhuma repetição de teste adicional necessária após geração das imagens. Evidência pos-return-regression-postgres.txt.

Aplicação V21: API UP, serviços saudáveis, UI/scripts/CSS 200, GET/POST de devolução sem token 401, migration 21 e zero registros permanentes nos novos fluxos. Restore V21 aprovado; evidências pos-return-local.json e backup-restore-v21.json. JAR idêntico por SHA-256 arquivado em D: após build das imagens; artifact-storage-v21.json. Login real/revisão visual não executados.

## Descontos e suspensão — V22 (15/10/2026)

Regressão H2 completa com 272 testes, zero falhas/erros/skips. PosCommercialTest inclui 25 cenários: limite/default e política versionada, limite por item, permissões separadas, autorizações por outro usuário, validade/cartHash/política/gestor atual, uso único/concorrência, rejeição, rollback, reembolso líquido, suspensão sem reserva/pagamento, edição/retomada em outro turno, descarte, retry tardio e isolamento. Migração com item/ledger antigos verifica bruto/zero desconto, sem repostagem ou concessão de permissões. Os 60 cenários de vendas/pagamentos/devoluções anteriores continuam cobertos.

Build Angular aprovado: initial 496,23 kB, lazy PDV/vendas 58,82 kB. Evidências pos-commercial-regression-postgres.txt e pos-commercial-frontend.txt. Login real/revisão visual adiados; compilação e testes de API não substituem essa homologação.

Resultado final V22: 117 testes PostgreSQL aprovados — descontos/suspensão 25, devoluções 25, pagamentos 20, vendas 15, caixa 15, estoque 16 e migração de venda/item/ledger antigos 1. Regressão H2 272 aprovada. Nenhuma falha/erro/skip. Build final initial 496,23 kB e lazy vendas 58,99 kB; incluiu histórico bruto/desconto/líquido por item. Evidência pos-commercial-regression-postgres.txt.

Aplicação V22: API UP, três serviços saudáveis, UI/scripts/CSS HTTP 200, APIs novas sem token 401 e migration 22. Banco permanente sem dados fictícios. Restore V22 aprovado com adulteração e chave incorreta rejeitadas; pos-commercial-local.json e backup-restore-v22.json. Login real/revisão visual não executados.

## Parcelamento financeiro — V23 (16/10/2026)

Regressão H2 completa: 295 testes, zero falhas/erros/skips. ReceivablePlanTest executa 23 cenários (dez de parcelamento e treze financeiros reutilizados), com prévia sem escrita, centavos/fim de mês, idempotência após recebimento e cancelamento, atomicidade em colisão tardia, concorrência, permissões, cliente ativo e escopos. Evidência: test-evidence/receivable-plan-regression-postgres.txt. Testes PostgreSQL adicionais registrados no resultado final abaixo.

Angular: build aprovado, initial 476,43 kB; contas a receber lazy 25,57 kB. Evidência receivable-plan-frontend.txt. Login real e revisão visual continuam adiados; build não substitui validação de uso no navegador.
Resultado PostgreSQL V23: 149 testes aprovados, zero falhas/erros/skips: parcelamento 23, recebíveis/tesouraria 9, descontos/suspensão 25, devoluções 25, pagamentos 20, vendas 15, caixa 15, estoque 16 e upgrade 1. O total H2 de 295 inclui 13 cenários financeiros herdados na classe de parcelamento, além dos dez cenários novos. Maven verify concluído em 16/10/2026; JAR e imagens gerados. Avisos de conexões fechadas em pools de contextos de teste anteriores não produziram falha na suíte.

V23 aplicada localmente com três serviços saudáveis. Migration 23 confirmada; página de recebíveis, seis scripts e CSS HTTP 200; criação/prévia/consulta anônimas HTTP 401. Contagens empresa/plano/vínculo/título/recebimento 0|0|0|0|0. Restore criptografado V23 aprovado; adulteração e chave incorreta rejeitadas. Evidências receivable-plan-local.json, receivable-plan-deploy.txt e backup-restore-v23.json. Banco de origem vazio: ensaio não substitui volume de operação real. Login real/revisão visual e publicação em produção continuam adiados.

## Crediário no PDV — V24 (16/10/2026)

SaleCreditTest acrescenta 17 cenários H2/PostgreSQL; upgrade preserva tendered NOT NULL, valores/ledger/snapshots antigos e crédito zero. Primeira regressão H2 completa: 312 cenários aprovados. PostgreSQL detectou exposição de NOT NULL como check virtual na descoberta de restrições; seleção corrigida para exigir tendered e total. Evidência inicial preservada em pos-credit-regression-postgres-first-attempt.txt. Correção validada em 18 H2 e 18 PostgreSQL, zero falhas/erros/skips (pos-credit-migration-fix.txt). Resultado da regressão final registrado abaixo.

Angular: initial 477,15 kB, lazy vendas 68,38 kB e customer-credit 6,66 kB; build aprovado em pos-credit-frontend.txt. A compilação não substitui teste de uso/login real ou revisão visual, que permanecem adiados.
Resultado final após correção V24: Maven verify aprovado com 312 H2 e 166 PostgreSQL, zero falhas/erros/skips; crediário 17 H2/PostgreSQL e upgrade 1 H2/PostgreSQL. Evidência completa pos-credit-regression-postgres.txt. Nenhum usuário/dado de negócio permanente criado; fixtures usam bancos descartáveis.

V24 aplicada localmente; API, banco e frontend saudáveis. Migration 24 e tendered NOT NULL confirmados. Rotas de crédito/vendas/recebíveis, sete scripts e CSS HTTP 200; GET/PUT de política e POST de venda anônimos HTTP 401. Contagens empresa/política/venda a prazo/plano/título 0|0|0|0|0. Restore V24 aprovado em banco isolado; adulteração e chave incorreta rejeitadas. Evidências pos-credit-local.json, pos-credit-deploy.txt e backup-restore-v24.json. Origem vazia: não substitui ensaio com volume operacional. Login real, revisão visual e publicação em produção continuam adiados.

## Devoluções do crediário — V25 (16/10/2026)

Regressão H2 completa: 332 testes aprovados. Novos CreditReturnTest/PostgresIT com 19 cenários: dívida primeiro/parcelas distantes, original preservado, arredondamento/zero centavo, novo turno, entrada, recibos com/sem banco, reembolso parcial acumulado, confirmação da divisão, rollback de saldo/estoque, origem de outra venda, replay após recebimento, permissões/filial, recebimento/estorno após crédito, fluxo/limite, fontes paginadas, concorrência e comprovantes únicos entre ledgers. CreditReturnMigrationTest/PostgresIT preserva título e recibo anteriores, NOT NULL, saldos e permissões.

Evidências iniciais credit-return-specific-h2.txt e credit-return-h2.txt; regressão final em credit-return-regression-postgres.txt. Build Angular em credit-return-frontend.txt. Login real/revisão visual continuam adiados; compilação não substitui teste de uso no navegador. Resultados finais e aplicação local registrados abaixo.
Resultado final V25: regressão completa com 332 H2 e 196 PostgreSQL aprovada, zero falhas/erros/skips (credit-return-regression-postgres.txt). Revisão de identidade acrescentou caso em que pagamento inicial e recibo compartilham UUID, usando espaços de identidade independentes; duplicatas da mesma fonte continuam bloqueadas e comprovantes bancários continuam compartilhados. Correção final validada em 46 H2 e 46 PostgreSQL: CreditReturn 20, SaleReturn 25 e upgrade 1. Evidência credit-return-final-validation.txt. JAR final gerado pelo verify direcionado; não houve nova regressão global após essa correção localizada.

V25 aplicada localmente com três serviços saudáveis. Migration 25 e NOT NULL de received_amount/credited_amount confirmados; páginas de vendas/recebíveis, sete scripts e CSS HTTP 200. Consultas de origens/ajustes e POST de devolução anônimos HTTP 401. Contagens empresa/devolução/abatimento/reembolso de parcela/título abatido 0|0|0|0|0. Restore V25 aprovado em banco isolado; chave incorreta e adulteração rejeitadas. Evidências credit-return-local.json, credit-return-deploy.txt e backup-restore-v25.json. Origem vazia: não substitui ensaio com volume operacional. Sem login real/revisão visual ou publicação em produção.


## Preparação de pagamento — V26

Veja [POS_PENDING_CHECKOUTS](POS_PENDING_CHECKOUTS.md): preparação persistida sem dinheiro ou reserva, conclusão pela transação normal de venda e descarte auditado. Não implementa cobranças, webhooks ou recebimentos parciais antes da conclusão.

V26 validada com `-Dtest=PendingCheckoutTest,SaleTest,SalePaymentTest,CreditReturnTest -Dit.test=PendingCheckoutPostgresIT -Ppostgres-tests verify`: 67 H2 e 12 PostgreSQL, zero falhas/erros/skips. Build Angular aprovado. Sem revisão visual ou login real.

## Conferência de pagamentos sem banco — 11/11/2026

Veja [PAYMENT_ALLOCATION](PAYMENT_ALLOCATION.md) e `test-evidence/payment-allocation-offline.json`. 20 testes Java executados com JUnit e compilação em memória, 15 testes Node aprovados, TypeScript e templates Angular verificados sem emissão. Não houve Spring/H2/PostgreSQL/Docker nem nova implantação. Maven completo bloqueado por falta de espaço durante escrita de classes. Regressão integrada fica pendente antes de implantação.

## Continuação — composição de pagamentos, 11/11/2026

Maven convencional `-Dtest=PaymentAllocationTest package`: compilação completa + 20 testes unitários + JAR aprovados. Node `--test frontend/tests/payment-allocation.test.cjs`: 27 aprovados, incluindo 12 casos de sugestão/edição, preservação da lista inválida, duplicidade de comprovante, limite e centavos. Build Angular aprovado. Não houve execução de testes que iniciem Spring ou banco nem implantação. Evidências em `test-evidence/payment-allocation-package.txt`, `payment-composer-unit.txt` e `payment-composer-frontend.txt`.

## Contexto de caixa — 11/11/2026

`node --test frontend/tests`: 42 testes aprovados, sem falhas/skips. `npm run build --prefix frontend`: aprovado, bundle inicial 477,62 kB, vendas lazy 91,92 kB. Testes de reconferência incluem correção em qualquer ordem com outras parcelas inválidas e valores aplicados preservados no cálculo do saldo. `npm run test:unit --prefix frontend` agora usa descoberta por diretório (`node --test tests`), compatível com Windows/Node 20 sem expansão de glob pelo shell. Sem banco, browser, Docker ou implantação.

## Revisão final — 11/11/2026

`npm run test:unit --prefix frontend`: 54 testes aprovados, zero falhas/skips, incluindo 12 novos de revisão imutável e invalidação. `npm run build --prefix frontend`: aprovado. Evidências em `test-evidence/sale-review-unit.txt`, `sale-review-frontend.txt`, `sale-review-offline.json`. Sem testes de navegador, API ou banco nesta etapa. Detalhes em [POS_SALE_REVIEW](POS_SALE_REVIEW.md).

## Implementação com testes adiados — 11/11/2026

Conforme orientação do usuário, novos testes e execução ficam para a etapa final. Histórico de vendas com filtros implementado, compilado e empacotado com `-Dmaven.test.skip=true package`; Angular build aprovado. **Nenhum teste desta funcionalidade foi executado.** SQL/períodos/isolamento/estados/paginação e comportamento de interface ainda precisam ser validados. Lista de cenários em [POS_SALE_HISTORY](POS_SALE_HISTORY.md). Sem banco ou implantação nesta rodada.

## Catálogo — implementação com testes adiados, 11/11/2026

Manutenção versionada e situação do produto compiladas com `-Dmaven.test.skip=true package` e build Angular. Nenhum teste escrito/executado nesta rodada; cenários de API/banco/JPA-JDBC/concorrência/reenvio/interface aguardam a etapa final, detalhados em [PRODUCT_MAINTENANCE](PRODUCT_MAINTENANCE.md). Sem conexão ao banco ou implantação. Evidências: `test-evidence/product-maintenance-backend-build.txt` e `product-maintenance-frontend-build.txt`.

## Financeiro de compras — V27 ainda não aplicada

Geração explícita de conta a pagar por recebimento e alocação acumulada de centavos implementadas. Compilação com `-Dmaven.test.skip=true package` e build Angular; **nenhum teste escrito/executado, SQL ou migração executados**. Validar upgrade V26→V27, histórico de recebimentos, centavos, reenvios, manual/gerado/cancelado, corrida/rollback, escopo e frontend na etapa final. Detalhes em [PURCHASE_RECEIPT_FINANCE](PURCHASE_RECEIPT_FINANCE.md). Sem conexão ao banco ou implantação.

## Parcelamento por recebimento — V28

Implementação compilada sem testes: prévia e lote de contas a pagar, soma exata e datas mensais. Nenhum teste escrito/executado nesta etapa. Migrações V27/V28 não aplicadas. Upgrade, compatibilidade/idempotência, centavos/calendário, conflito intermediário/rollback/concorrência, permissões e interface ficam para o final. Lista em [PURCHASE_INSTALLMENTS](PURCHASE_INSTALLMENTS.md). Evidências de build em `test-evidence/purchase-installments-*-build.txt`.

## Consulta de contas a pagar — testes adiados

Implementação de filtros, totais e navegação por origem compilada com `-Dmaven.test.skip=true package` e build Angular. Nenhum teste escrito/executado nesta etapa. Validar SQL H2/PostgreSQL, permissões/escopo, soma além da página, cancelamento/estorno, atraso/data financeira, origem/parcela e interface ao final. Detalhes em [PAYABLE_SEARCH](PAYABLE_SEARCH.md). Sem banco, migração ou implantação.

## Consulta de recebíveis — testes adiados

Filtros, totais ajustados por devolução e consulta de origem compilados com `-Dmaven.test.skip=true package` e build Angular. Nenhum teste escrito/executado. SQL, escopo, origens, abatimento/reembolso, CREDITED, totais além da página e links precisam de validação na etapa final. Detalhes em [RECEIVABLE_SEARCH](RECEIVABLE_SEARCH.md). Sem banco ou implantação.
