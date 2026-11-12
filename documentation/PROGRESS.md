# Progresso — 16/10/2026

Fundação, catálogo, estoque/perdas, sessões e gestão inicial de usuários verificados. Regressão completa com 332 testes H2 e 196 PostgreSQL aprovada; correção final de identidade de fontes validada em 46 testes H2 e 46 PostgreSQL; vendas com itens, preço histórico e baixa integrada ao estoque implementadas (15 cenários H2 e PostgreSQL); caixa final com 15 cenários H2/PostgreSQL aprovados; tesouraria com oito cenários H2 e oito PostgreSQL aprovados. gestão de usuários/permissões com 12 cenários H2 e 12 PostgreSQL aprovados. Execução local Docker recuperada após reinício do Windows: banco, API e frontend saudáveis. Escopo integral preservado em REQUIREMENTS. **Produto ainda em desenvolvimento; não homologado para operação comercial.**

| Módulo / funcionalidade | Estado | Arquivos principais | Validação / pendências |
|---|---|---|---|
| Repositório e documentação | IMPLEMENTADO | README, documentation/*.md, .gitignore | Git em main; sem commit/remote |
| Spring Boot / migrações | TESTADO | backend/pom.xml, migrations V1–V26 | JAR gerado; contas a pagar transacionais verificadas |
| Autenticação / sessões / senha / bloqueio | TESTADO | backend/.../auth | API H2/PostgreSQL; MFA/recuperação/renovação/dispositivos pendentes |
| Empresa / provisionamento | TESTADO | backend/.../tenant, scripts/provision-company.ps1 | transação e segredo testados; nenhum usuário permanente criado |
| Usuários / ativação | IMPLEMENTADO | backend/.../user, frontend/users.ts, V5 | cadastro/lista/estado/edição de permissões; 12 cenários H2/PostgreSQL; perfis e escopos de filial implementados; recuperação/MFA pendentes |
| Isolamento / permissões | TESTADO | SessionFilter, repositories, schema | isolamento/revogação imediata testados; escopo por filial e perfis implementados; RLS pendente |
| Filiais / auditoria | TESTADO | backend/.../branch, audit, shared/Audit | cadastro/lista/consulta/eventos; atualização/desativação/diferenças/origem/retencão pendentes |
| Catálogo simples / códigos | TESTADO | backend/.../product, V2/V3, frontend/products | persistência/busca/GTIN/códigos internos/concorrência; cadastro completo/custos/preços por filial/importações pendentes |
| Depósitos / estoque / custo médio | TESTADO | backend/.../inventory, V4, frontend/inventory.* | entradas/ajustes/consumo, saldos/ledger, concorrência/idempotência aprovados também em PostgreSQL |
| Transferência imediata | TESTADO | StockService, InventoryTest | origem/destino/custos/auditoria atômicos; trânsito/logística pendentes |
| Perdas / aprovação / rejeição | TESTADO | backend/.../loss, V4, frontend/losses.ts | perda pendente sem baixa, aprovação única/rejeição/custo estimado e efetivo; alçadas/fotos/lotes/estorno/relatórios pendentes |
| Angular integrado / design system | INTEGRADO | frontend/src | build/typecheck e fluxo real no browser com H2 temporário; E2E automatizado/PostgreSQL pendentes |
| Infraestrutura / CI | EM DESENVOLVIMENTO | compose, Dockerfiles, nginx, workflows | imagens atuais aplicadas; três containers saudáveis; HTTP/CSS e V1–V25 verificados; CI remota não executada |
| Fornecedores / base de compras | TESTADO | backend/.../purchase, frontend/suppliers.ts, V8 | cadastro/busca/edição/estado; pedidos/recebimentos e contas a pagar manuais implementados; XML pendente |
| Pedidos / recebimentos de compra | TESTADO | backend/.../purchase, frontend/purchases.*, V9 | parcial/completo/estoque/idempotência/cancelamento; XML/alçadas/frete/impostos/financeiro pendentes |
| Contas a pagar / registros de pagamento | TESTADO | backend/.../finance, frontend/payables.*, V10 | títulos/baixa parcial/estorno de registro/filial; tesouraria interna implementada; vínculo opcional com títulos implementado; banco/conciliação pendentes; AR manual implementado |
| Tesouraria interna | TESTADO | backend/.../finance/Treasury*, frontend/treasury.*, V11 | contas/saldo/extrato/transferências/reenvios/concorrência; vínculo opcional com títulos implementado; bancos/conciliação pendentes |
| Pagamentos vinculados à tesouraria | TESTADO | PayableService, TreasuryService, frontend/payables.*, V12 | débito/estorno/reenvio e concorrência; 13 cenários H2/PostgreSQL aprovados; regressão 120 e módulos financeiros 35 PostgreSQL aprovados |
| Clientes / contas a receber | TESTADO | CustomerService, ReceivableService, frontend/customers.ts/receivables.*, V13–V15 | seis cenários clientes + 22 recebíveis/tesouraria H2/PostgreSQL aprovados; regressão 148 e PostgreSQL 41 aprovados; crediário integrado em V24; cobrança automática/CRM/conciliação bancária automática pendentes |
| Fluxo de caixa por filial | TESTADO | CashFlowService, frontend/cash-flow.*, V16 | saldos/movimentos/transferências/pendentes/vencidos/sem vínculo; dez cenários H2/PostgreSQL aprovados; regressão 158 aprovada |
| Conciliação de extratos CSV/manual | TESTADO | ReconciliationService, StatementCsv, frontend/reconciliation.*, V17 | prévia/deduplicação/vínculo/versão/reset/histórico; 13 cenários H2/PostgreSQL aprovados; regressão 171 aprovada |
| Terminais / sessões de caixa | TESTADO | CashService, frontend/pos-cash.*, V18 | abertura/reserva/suprimento/sangria/fechamento/diferença; 14 cenários H2/PostgreSQL iniciais aprovados; versão final 15 H2/PostgreSQL aprovada; vendas implementadas nas fases seguintes; fiscal/hardware pendentes |
| Vendas em dinheiro / itens históricos | TESTADO | SaleService, StockService.consumeSale, frontend/pos-sales.*, V19 | depósitos por terminal, snapshot de preços e baixa/recebimento atômicos; pagamentos mistos/cancelamento implementados em V20; fiscal pendente |
| Parcelas conferidas / cancelamento integral | TESTADO | SalePayments, SaleCancellationService, frontend/pos-sales.*, V20 | dinheiro/Pix/transferência declarados; provas únicas, reversão de caixa/estoque; provedores/cartões/parcelas pendentes |
| Devoluções parciais / outro turno | TESTADO | SaleReturnService, frontend/pos-sale-returns.*, V21 | 25 cenários H2/PostgreSQL; custo histórico, valor acumulado e limites por item/pagamento; fiscal/cross-filial/quarentena pendentes |
| Descontos / aprovação / suspensão | TESTADO | CartPricing, DiscountService, SuspensionService, frontend/pos-commercial.*, V22 | 25 cenários H2/PostgreSQL, limite por item e aprovação por outro usuário, retomada versionada; promoções/alçadas por valor pendentes |
| Parcelamento financeiro | TESTADO | ReceivablePlanService, frontend/receivables.*, V23 | 23 cenários H2/PostgreSQL (dez novos e treze reutilizados), centavos/calendário/atomicidade/replay; renegociação pendente |
| Crediário integrado ao PDV | TESTADO | SaleCreditService, CustomerCreditController, frontend/customer-credit.ts e pos-sales.*, V24 | 17 cenários H2/PostgreSQL; entrada/limite/títulos/estoque/cancelamento; devoluções financeiras implementadas em V25; alçadas avançadas pendentes |
| Devoluções do crediário / ajustes de títulos | TESTADO | CreditReturnService, ReceivableService, frontend/pos-sale-returns.* e receivables.*, V25 | 20 cenários financeiros + upgrade H2/PostgreSQL; dívida/reembolso/estoque atômicos e histórico; anulação/compensação e fiscal pendentes |
| Preparação de pagamento | IMPLEMENTADO | PendingCheckoutController, pos-sales.*, V26 | 67 testes H2 e 12 PostgreSQL aprovados nesta rodada, incluindo 12 cenários novos; carrinho sem fundos/reserva, conclusão/descarte idempotentes; provedores e pagamentos parciais anteriores à conclusão pendentes |
| Restante do produto | NÃO INICIADO / EM DESENVOLVIMENTO conforme ROADMAP | ROADMAP e REQUIREMENTS | fundação/estoque/compras/PDV/pagamentos/financeiro/comercial parcialmente implementados; fiscal/BI/offline/SaaS/IA/preparação comercial não iniciados; detalhes em ROADMAP |

## Ambiente e serviços

Java 21.0.12.1, Maven 3.9.6, Node 20.19, Angular 21 resolvido no lockfile. Docker Desktop 29.3.1 iniciado. Outros projetos ocupam 8080 e PostgreSQL nativo ocupa 5432; nenhum foi parado/alterado. ERP usa banco 15432, API 18080, frontend Docker 4200, todos no localhost.

Frontend atual disponível em **http://127.0.0.1:4200/login** pelo Nginx/Docker, integrado à API. CSS responde HTTP 200 e HTML não contém onload inline. Servidores temporários estão encerrados.

**Verificação do segundo marco:** frontend em http://127.0.0.1:4202 conectado ao UiTestServer em 18081, H2 em memória. Empresa interface-test é exclusivamente descartável de teste e fica fora do ambiente de clientes. Nenhuma funcionalidade nova foi aplicada aos containers antigos. Harness/script em src/test e scripts/start-ui-test-server.ps1; não entra no JAR de produção.

`.env` gerado com segredos aleatórios e ignorado pelo Git. Não há empresas/produtos fictícios permanentes. Dados de testes foram criados em H2/Testcontainers descartáveis. Provisionamento real deve usar dados do usuário via script, após recuperação do ambiente.

## Evidências executadas

- `mvn package`: aprovado, 25 execuções (10 vetores GTIN + 15 cenários API/H2), zero falhas/skips.
- `mvn verify -Ppostgres-tests`: aprovado antes de V3; os mesmos 15 cenários passaram em PostgreSQL 17.6 real. Evidências em `documentation/test-evidence/`.
- Concorrência: um 201 e um 409 para cadastros simultâneos do mesmo produto; uma linha e uma auditoria. Primeiro teste encontrou exceção não traduzida no flush, corrigida com saveAndFlush no repository e retestada.
- `npm run typecheck`: aprovado após catálogo.
- `npm run build`: aprovado, bundle aproximadamente 326 kB; repetido após correção CSP/inlineCritical.
- `docker compose build`: primeiras imagens construídas. Compose iniciou banco/API/frontend. Health HTTP da API retornou UP.
- Navegador: login verificado visualmente em viewport estreito; estilos corrigidos em 4201. Login/cadastro de filial/produto no navegador ainda não homologados.
- Ajustes finais: V3 amplia chave canônica para namespace + código interno de 60 caracteres; login rejeita senha fora da política ASCII. Compilação, geração de JAR e 25 testes H2/unitários passaram novamente. A repetição PostgreSQL falhou antes dos cenários porque Docker não criou Ryuk (`mkdir ... read-only file system`). V3 ainda precisa de verificação PostgreSQL; sucesso anterior não cobre essa migration.

## Segundo marco — evidências

- `mvn -o package` com cache Maven existente: **41 testes aprovados**, zero falhas/skips (10 GTIN, 15 fundação/catálogo, 16 estoque/perdas). Migrações V1–V4 aplicadas em H2. JAR gerado.
- InventoryTest cobre custo médio/ledger/ator, retry com escala equivalente, chave reutilizada com outros dados, saídas concorrentes da última unidade, retry concorrente, transferência atômica/custo no destino/rollback, aprovação/rejeição/replay/concorrência, perdas sobrepostas sem saldo, estimativa versus custo efetivo, isolamento, custos restritos, permissões e limites numéricos.
- InventoryPostgresIT criado para repetir os mesmos 16 cenários com PostgreSQL real. Ainda não executado: criação de container descartável falhou com filesystem Docker somente leitura. Usuário autorizou reiniciar Docker; tentativa executada, mas inicialização ficou bloqueada no WSL (ver diagnóstico abaixo).
- Angular build/typecheck aprovados. Falha inicial de união de tipos Observable corrigida e compilação repetida. Tentativas incertas preservam comando, bloqueiam formulários/saída/logout e usam mesmo UUID no retry; após sucesso formulário é limpo.
- Browser com banco temporário: login; produto UI-001/KG/R$20; depósito UI-D1/Matriz; entrada 3 kg a custo R$10; perda de 2 kg; aprovação efetiva R$20; saldo final 1 kg e ledger com entrada + perda. Cadastrar produto/depósito, entrada, perda e aprovação integrados sem listas fictícias como fonte de dados. Revisão visual em viewport estreito e desktop 1280×900, override restaurado.
- Evidências: inventory-h2.txt, foundation-v4-h2.txt e loss-approval-ui.png em documentation/test-evidence. Homologação comercial não realizada.

## Bloqueio de infraestrutura

C: atingiu zero bytes livres durante construção Docker. Docker passou a retornar `read-only file system` e `input/output error` na reconstrução e cópia de arquivos. Removido somente o cache Maven de 124,2 MB criado neste repositório; código, node_modules, JAR e evidências preservados. Verificação final usa cache Maven preexistente em `C:/Users/user/.m2/repository`. Espaço livre aumentou posteriormente, mas recuperação do filesystem Docker precisa ser confirmada. Não reiniciar Docker/WSL sem considerar outros projetos em execução. Nenhuma limpeza geral/prune realizada.

## Próximos passos executáveis

1. Manter espaço livre e acompanhar o ambiente Docker recuperado. Testes PostgreSQL já concluídos; não repetir sem nova alteração/falha.
2. Evoluir pagamentos com estados parciais/assíncronos e provedores; políticas de troca/quarentena e fluxos entre filiais. Divisão manual conferida, cancelamento integral no turno e devoluções parciais/em outro turno já implementados. Login real e revisão visual seguem adiados pelo usuário; E2E/homologação permanecem pendentes.
3. Completar MFA/recuperação/dispositivos. Escopos por filial implementados para os módulos atuais. Perfis reutilizáveis por cópia explícita implementados. Cadastro/lista/ativação de usuários e consulta/revogação de outros acessos já implementados.
4. Expandir catálogo e estoque com reservas/lotes/validade/inventário/recontagem e perdas com alçadas/estorno; depois compras, PDV e pagamentos mistos prioritários. Ledger/saldos/custo médio/transferência imediata e aprovação básica já implementados; não reescrever esses módulos.

TESTADO descreve um recorte verificado, não homologação do módulo completo ou prontidão comercial.

## Recuperação autorizada do Docker — 14/10/2026

Usuário autorizou reiniciar Docker Desktop. Executados docker desktop restart, verificação de status/motor, parada completa com docker desktop stop --force e nova inicialização com timeout de 45 segundos. Docker permaneceu em starting; docker info falhou/retornou HTTP 500. Teste de início da distribuição com wsl -d docker-desktop --exec /bin/true terminou com Wsl/Service/CreateInstance/HCS_E_CONNECTION_TIMEOUT (máquina virtual/container não respondeu). WslService está Running; Ubuntu foi observado Running e docker-desktop Stopped antes da tentativa de inicialização. Aproximadamente 0,92 GB livres no C: na verificação inicial deste diagnóstico.

Nenhum volume, distro ou imagem foi excluído/resetado. Testes PostgreSQL e atualização dos containers não foram executados, pois o motor não está operacional. Interrupção dos containers foi autorizada; sua retomada ainda não está confirmada. Próxima tentativa: wsl --shutdown e iniciar Docker novamente. Autorização adicional solicitada porque shutdown afeta também todos os processos do Ubuntu; não executar sem resposta explícita. Se não recuperar, preservar discos/volumes e investigar o erro HCS antes de qualquer reparo de filesystem ou reset.

## Resultado da recuperação e validação PostgreSQL

Usuário também autorizou wsl --shutdown. Essa ação recuperou temporariamente o Docker: container descartável iniciou e os containers Omnisales retomaram automaticamente. FoundationPostgresIT passou nos 15 casos com V1–V4; InventoryPostgresIT passou nos 16 casos em execução direcionada posterior. Evidências preservadas em foundation-v4-postgres.txt e inventory-v4-postgres.txt. Total: 41 testes locais + 31 execuções PostgreSQL, sem falhas funcionais.

Reconstruções voltaram a esgotar C:. Build backend concluído com MAVEN_OPTS=-Xmx512m e health check HTTP; frontend Node interrompido quando voltou a ocorrer read-only filesystem. Preparado infrastructure/Dockerfile.frontend-artifact para empacotar frontend/dist/erp/browser já compilado, sem executar Node no container. Esse Dockerfile alternativo ainda não foi executado. Health checks adicionados aos Dockerfiles e frontend aguarda backend saudável no Compose.

Servidores temporários Angular (4201/4202) e UiTestServer (18081) encerrados. Removidos somente node_modules e .angular deste projeto (dependências regeneráveis, aproximadamente 200 MB); código, package-lock, dist, JAR, banco/volumes e evidências preservados. Reinstalar npm ci antes de voltar a desenvolver frontend.

Estado final observado: C: novamente sem espaço; Docker Desktop parado; WslService em StopPending; processo vmmem ainda presente. Tentativa normal de Restart-Service foi negada por privilégio; solicitada elevação Windows para o reinício WSL autorizado, mas serviço permaneceu StopPending. Não houve reset/exclusão de distros, volumes ou imagens de usuários. Retomada dos containers depois dessa última parada não está confirmada. É necessária liberação de espaço e recuperação do WSL pelo usuário, possivelmente com reinicialização do Windows, preservando o trabalho aberto.

Depois da recuperação, verificar Docker gravável e espaço disponível; executar docker build -t sistema-vendas-erp-frontend -f infrastructure/Dockerfile.frontend-artifact . e docker compose up -d --no-build --wait. Confirmar health HTTP da API em 18080 e tela/CSS em 4200. Esses passos ainda não foram concluídos. Código transacional já validado em PostgreSQL; não reescrever módulos nem repetir testes aprovados sem motivo.

## Recuperação concluída após reinício do Windows — 14/10/2026

Docker 29.3.1 iniciou e container descartável confirmou escrita. Imagem frontend empacotada pelo artefato Angular existente; Compose iniciado sem recompilação. Banco, backend e frontend saudáveis. API /actuator/health UP; /login e CSS HTTP 200, sem onload inline. Flyway V1–V4 bem-sucedidas; banco permanece com zero empresas. C: com 7,59 GB livres após subida. Sem exclusão de volumes ou dados de outros projetos. Os bloqueios descritos nas seções históricas acima foram resolvidos nesta recuperação.


## Segurança da conta — 14/10/2026

Implementada tela Angular /account para consultar sessões ativas, encerrar outros acessos e trocar senha com confirmação. API lista somente datas/current, sem tokens/hashes. Revogação preserva sessão atual, respeita usuário/tenant, serializa com login e revalida sessão após lock. Auditoria somente quando há sessões removidas. Senha alterada encerra todos os acessos.

Validação: suíte local de 43 testes aprovada; fundação final repetida com 17 testes H2 e 17 PostgreSQL aprovados. Estoque PostgreSQL permanece com 16 testes aprovados no marco anterior (não repetidos nesta alteração). Build Angular aprovado: 357,57 kB. Evidências account-h2.txt e account-postgres.txt. Verificação visual desta nova tela ainda pendente; nenhum usuário de produção criado. Identificação de dispositivos, revogação individual, MFA, recuperação e gestão de usuários/perfis permanecem pendentes.


Atualização aplicada ao Compose: três containers saudáveis; API UP; rota /account e novo bundle main-Q76AAKJY.js HTTP 200; consulta anônima de sessões retorna 401. Espaço livre: 7,32 GB.



## Gestão inicial de usuários — 14/10/2026

Cadastro/lista paginada/ativação/desativação integrados em /users. Senha inicial BCrypt; email normalizado e único por tenant; permissões explícitas conhecidas, sem concessão superior aos privilégios do gestor. Desativação revoga sessões; reativação exige privilégios suficientes e não restaura sessões. Própria conta não pode ser desativada. V5 adiciona version, obrigatória junto com active no comando; lock tenant serializa alterações e revalida ator/sessão. Auditoria transacional sem senha/hash. Novas empresas recebem user:read/write; usuários existentes não recebem privilégios por migration.

Validação: regressão local 49 testes aprovada; oito cenários finais UserTest e oito UserPostgresIT aprovados, zero falhas/skips; V1–V5 executadas em H2/PostgreSQL. Concorrência de desativação: um 200 e um 409, uma auditoria. Build Angular aprovado (365,16 kB). Evidências users-h2.txt/users-postgres.txt. Perfis reutilizáveis, edição de permissões, convite, redefinição de senha por administrador e escopos de filial ainda pendentes. Verificação visual da nova tela pendente.


Atualização aplicada: V1–V5 bem-sucedidas no PostgreSQL persistente, API UP, rota /users e bundle main-CZCJUHVE.js HTTP 200; API de usuários anônima retorna 401. Banco sem empresas/dados fictícios; C: 7,34 GB livres.


## Edição de permissões — 14/10/2026

API PUT /users/{id}/permissions integrada à tela de usuários, com seleção explícita e versão compartilhada com ativação. Gestor só edita contas/concede permissões dentro dos próprios privilégios. user:write exige user:read. Último gestor ativo não perde administração; lock de tenant serializa alterações concorrentes. Atualização sem mudança não incrementa versão/auditoria; versão antiga retorna 409. Permissões são efetivas na próxima requisição das sessões abertas. Frontend atualiza a própria sessão via /auth/me após salvar e encaminha à conta se perder user:read.

Validação: 55 testes locais e 12 UserPostgresIT aprovados, zero falhas/skips. Teste concorrente de remoção dos dois gestores resultou em 200/409 e preservou um gestor. Cobertura de revogação imediata, no-op, versão antiga, tenant, privilégios superiores, permissão inválida e último gestor. Angular compilado (367,44 kB). Evidências user-permissions-h2.txt/user-permissions-postgres.txt. Não exige nova migration. Verificação visual pendente; perfis reutilizáveis e escopos por filial ainda não implementados.



Atualização de permissões aplicada ao Compose: API UP; /users e bundle main-4U2AJWLL.js HTTP 200; edição anônima retorna 401. Sem nova migration ou dados fictícios permanentes.


## Perfis reutilizáveis — 14/10/2026

Cadastro/lista/edição paginada de modelos de permissões em /profiles e aplicação explícita pela edição de usuários. V6 acrescenta access_profile/access_profile_permission com FKs compostas tenant. Aplicar perfil copia e substitui permissões, não cria vínculo dinâmico. Versões de perfil/usuário obrigatórias; mesmas proteções contra escalada/último gestor e auditoria da edição direta. Atualizar modelo não altera usuários sem reaplicação.

Validação: 60 testes locais e cinco AccessProfilePostgresIT aprovados com V1–V6, zero falhas/skips. Cenários de snapshot, versão antiga, no-op, duplicidade/rollback, tenant, escalada, último gestor e atualização concorrente (200/409). Angular build aprovado (375,65 kB). Evidências access-profiles-h2.txt/access-profiles-postgres.txt. Verificação visual pendente. Escopos por filial continuam pendentes.


## Backup cifrado e restauração descartável — 14/10/2026

Implementados backup-database.ps1/verify-backup.ps1, PowerShell 7.2+, pg_dump custom, AES-256-GCM com nonce aleatório/metadados autenticados, checksum SHA-256 e chave externa ERP_BACKUP_KEY. Restore nunca sobrescreve ERP: usa PostgreSQL descartável sem portas/rede externa, verifica migrations/contagens e limpa somente recursos próprios. Limite local 256 MB, arquivos temporários sem cifra exigem proteção do host.

Ensaio passou com V6 e contagens zero (banco local sem empresas); adulteração de ciphertext válido e chave incorreta rejeitadas. Incompatibilidade inicial de coerção de data pelo PowerShell corrigida com leitura literal System.Text.Json. Evidência backup-restore.json. Chave/arquivos cifrados de teste descartados; nenhum backup operacional agendado ou cópia externa configurado. Volume real, retenção/RPO/RTO/reconciliação e custódia de chaves ainda pendentes. Escopo restante integral continua autorizado e preservado no ROADMAP/REQUIREMENTS; não considerar ERP completo.


Perfis aplicados ao Compose: API UP, rota /profiles e bundle main-N6N2N3ZW.js HTTP 200, consulta anônima retorna 401. Ensaio reproduzível scripts/test-backup.ps1 aprovado; scripts/containers temporários limpos. Espaço livre 7,33 GB. Esta entrega não representa conclusão do escopo integral.



## Acesso por filial — 14/10/2026

V7 acrescenta all_branches e user_branch_access com FKs compostas tenant. Gestão do escopo em Usuários/Permissões: global ou lista até 100 filiais; lista restrita vazia não acessa nenhuma. Versionamento compartilhado com estado/permissões; lock administrativo, no-op sem auditoria extra e USER_BRANCH_SCOPE_CHANGED. Usuários existentes/novos mantêm global por padrão até restrição explícita.

Filiais/depósitos/perdas e totais filtrados no SQL; IDs sem acesso retornam 404. Movimentos/saldos/ledger/criação de depósitos verificam filial; transferência exige origem e destino; aprovação e rejeição de perda não escapam do escopo. Administração de usuários/perfis/auditoria/criação de filiais exige global, e user:write não pode ser concedido a conta restrita. Catálogo segue compartilhado por empresa. allBranches retornado em login/me para navegação; API consulta regras atuais no banco para sessões abertas.

Validação: 65 testes locais e cinco BranchScopePostgresIT aprovados, V1–V7, zero falhas/skips. Cobertura de filtros, IDs, transferência/rollback, revisão de perdas, alterações na sessão aberta, versão antiga, gestor, administração global, lista vazia e tenant. Build Angular aprovado (378,82 kB). Evidências branch-scope-h2.txt/branch-scope-postgres.txt. Verificação visual pendente; RLS e cancelamento de operações já em andamento não implementados. Módulos futuros devem reutilizar/verificar o mesmo escopo.


Entrega aplicada: três containers saudáveis; V1–V7 bem-sucedidas no banco local, API UP, /users e bundle main-72RZP4HC.js HTTP 200, consulta de escopo anônima 401. Banco permanece sem empresas fictícias. Backup/restore repetido para novo schema V7 e aprovado, incluindo adulteração/chave incorreta; evidência backup-restore-v7.json.



## Fornecedores / início de compras — 14/10/2026

V8 cria supplier compartilhado por empresa, com código canônico único por tenant, dados de contato/identificação informada/endereço/notas, active/version. API e Angular /suppliers com cadastro, consulta/busca paginada, edição/ativação/desativação, lock e versão, no-op sem nova auditoria e rollback de duplicidade. Auditoria SUPPLIER_CREATED/SUPPLIER_UPDATED. supplier:read/write separados e disponíveis em usuários/perfis; somente novas empresas recebem os privilégios no provisionamento, sem concessão por migration.

Validação: 71 testes locais e seis SupplierPostgresIT aprovados com V1–V8, zero falhas/skips. Cenários de normalização/busca, tenant/duplicidade/rollback, estado/versão/no-op, permissões/revogação, validação aninhada e concorrência (200/409, uma auditoria). Angular build aprovado (386,82 kB). Evidências suppliers-h2.txt/suppliers-postgres.txt. Verificação visual pendente; login/cadastro real adiado conforme usuário. Identificação fiscal é declarada, sem consulta oficial. Pedidos, cotação, recebimento parcial, XML e contas a pagar ainda pendentes.



Fornecedores aplicados ao Compose: três serviços saudáveis, API UP, rota /suppliers e bundle main-IJL3NHMF.js HTTP 200, API anônima 401. V1–V8 bem-sucedidas; empresas/fornecedores permanentes continuam zerados. Restore do novo schema V8 e rejeição GCM aprovados; evidência backup-restore-v8.json.


## Pedidos e recebimento de compras — 14/10/2026

V9 cria pedido/itens e recebimento/itens vinculados a operações de estoque com FKs compostas. Pedido por filial autorizada, fornecedor/produtos ativos, snapshots de nomes/unidade/custo/quantidade e totais BigDecimal. Estados OPEN/PARTIAL/RECEIVED/CANCELLED. Recebimento parcial não ultrapassa pendente, exige depósito da filial e usa custo negociado; operações, saldos/custo médio/ledger, recebido, estado e auditoria são atômicos. Produtos bloqueados em ordem estável; colisão com namespace manual de estoque é rejeitada.

requestId de criação e receiptId de recebimento persistidos com fingerprint normalizado e replay sem duplicação. Histórico paginado. Cancelamento encerra só saldo pendente, preservando mercadorias já recebidas e estoque. purchase:read/write/receive integrados a usuários/perfis; recebimento não exige direito a ajuste manual inventory:write. UI /purchases multi-item, seletores paginados, detalhes/histórico, recebimento e cancelamento. Comandos incertos em memória conservam UUID/payload e bloqueiam edição/saída/logout; não é outbox durável.

Validação: regressão local com 82 testes aprovada, depois 14 PurchaseTest e 14 PurchasePostgresIT finais aprovados (três cenários adicionais); zero falhas/skips. Cobertura de snapshots/total/idempotência, criação/reenvios concorrentes, recebimento parcial/completo/custo médio, excesso, falha no segundo produto com rollback, tenant/filial/depósito, cancelamento/replay/disputa, lock multi-produto, operação manual não disfarçada como compra e permissões delegadas. Erro de declaração var no teste corrigido antes da execução; warning de tipo opcional Angular corrigido e build final aprovado (407,40 kB). Evidências purchases-h2.txt/purchases-postgres.txt. Verificação visual/login real adiados conforme usuário; cotação/alçadas/XML/frete/impostos/devolução/contas a pagar permanecem pendentes. Regras completas em PURCHASES.md.



Compras aplicadas ao Compose: três serviços saudáveis; API UP, rota /purchases e bundle main-PJP4KQHB.js HTTP 200; consulta anônima 401. V1–V9 bem-sucedidas, empresas/pedidos permanentes zerados. Backup/restore do schema V9 e rejeição criptográfica aprovados; evidência backup-restore-v9.json. C: 7,41 GB livres.


## Contas a pagar — 14/10/2026

V10 cria payable/payable_payment com FKs de tenant/filial/fornecedor/recebimento opcional/atores, checks de valor/estado e estorno, requestIds/fingerprints, referência interna única e índices de vencimento/histórico. Títulos com datas/snapshots/valor nominal; filtros por estado/vencidos e escopo antes de paginação. Registra pagamento realizado informado, parcial/completo, sem ultrapassar saldo; estorno de registro preserva original e reabre saldo. Cancelamento só com pago zero. Locks de título, versões e transações/auditoria impedem baixa dupla e perda de histórico.

UI /payables com seletores paginados, calendário financeiro (ERP_FINANCE_ZONE padrão America/Sao_Paulo), criação, detalhes, registro/estorno/cancelamento/histórico. Recebimento de compra pode abrir título pré-vinculado; valor/vencimento são declarados, sem geração automática de dívida. Comandos incertos em memória bloqueiam saída/logout e usam mesma chave. payable:read/write/pay/reverse distintos; privilégios somente no provisionamento de novas empresas. Interface registra pagamentos já realizados, sem enviar dinheiro ou fazer refund bancário.

Validação: regressão local 97 testes aprovada; 14 PayableTest e 14 PayablePostgresIT finais aprovados com V1–V10, incluindo dois casos adicionais de reuso de chave entre títulos e ocultação de pagamento de outra filial. Zero falhas/skips. Cobertura de parcial/completo/replay/estorno/cancelamento, datas/precisão, overdue, fornecedor inativo, link de compra, rollback por chave de estorno, tenant/filial/permissões e disputas entre pagamento/cancelamento/estorno. Angular build aprovado (429,24 kB), import não utilizado removido. Evidências payables-h2.txt/payables-postgres.txt. Verificação visual/login real pendentes. Regras e limitações em PAYABLES.md; banco/tesouraria/AR/parcelas automáticas/juros/DRE/conciliação continuam pendentes.


Contas a pagar aplicadas ao Compose: três serviços saudáveis; API UP, rota /payables e bundle main-SCPKUDJW.js HTTP 200; API anônima 401. V1–V10 bem-sucedidas; títulos/pagamentos/empresas permanentes continuam zerados. ERP_FINANCE_ZONE=America/Sao_Paulo confirmado no container. Backup/restore V10 e rejeição criptográfica aprovados; evidência backup-restore-v10.json. C: 7,41 GB livres.


## Tesouraria interna — V11

107 testes de regressão sem falhas/skips, mais oito cenários da tesouraria em PostgreSQL 17.6. Build Angular aprovado (441,64 kB). Imagens locais atualizadas; três serviços saudáveis; V1–V11 aplicadas. Página /treasury e bundle HTTP 200; API sem autenticação 401. Banco permanente mantém zero tenants/contas/operações. Backup/restore V11 aprovado, alteração de ciphertext e chave errada rejeitadas. Evidências treasury-h2.txt, treasury-postgres.txt, treasury-local.json e backup-restore-v11.json. Login real e revisão visual seguem adiados. Próximo passo: associar registros de pagamento à conta de tesouraria com débito/estorno atômicos e preservar compatibilidade dos registros existentes.

## Pagamentos vinculados à tesouraria — V12

Conta de origem opcional na tela de pagamentos: baixa e débito, ou estorno e crédito na conta original, atômicos/idempotentes. Registros sem conta mantêm o comportamento anterior. Permissões e acesso à filial da conta são revalidados; conta pode centralizar pagamentos de outra filial autorizada. Histórico guarda IDs de operação e estorno; extrato identifica origem. Regressão 120 testes e PostgreSQL 35 cenários aprovados, incluindo 13 da integração. Build Angular aprovado (445,02 kB). Imagens aplicadas, três serviços saudáveis, V1–V12 aplicadas. UI/bundle 200 e API anônima 401. Restore V12 e rejeição de adulteração/chave incorreta aprovados; banco permanente continua vazio. Evidências payable-treasury-postgres.txt, payable-treasury-local.json, backup-restore-v12.json. Login real/revisão visual adiados. Próximo módulo: clientes e contas a receber; conciliação bancária/fluxo de caixa ainda pendentes.

## Clientes / recebíveis — V13–V15

Cadastro/busca/edição/estado de clientes e títulos manuais por filial, recebimentos parciais/totais, estorno/cancelamento, filtros e histórico implementados. Conta de destino opcional credita na baixa e debita no estorno; insuficiência/overflow/conflitos desfazem todo o comando. Preservados pagamentos AP. 148 testes de regressão e 41 PostgreSQL aprovados; Angular 476,25 kB. DTOs de usuários/perfis aceitam as 31 permissões atuais; nenhuma migration eleva usuários existentes. Imagens atualizadas, três serviços saudáveis, V1–V15 aplicadas. Clientes/recebíveis/bundle HTTP 200, API anônima 401. Banco permanente: zero empresas/clientes/títulos/recebimentos. Restore V15 aprovado com rejeição de adulteração/chave incorreta. Evidências receivables-postgres.txt, receivables-local.json e backup-restore-v15.json. Login real/revisão visual continuam adiados. Próximo passo: fluxo de caixa por filial, distinguindo saldos de tesouraria, lançamentos efetivos e títulos pendentes; depois conciliação e PDV conforme ROADMAP.

## Fluxo de caixa — V16

Implementados saldo atual, abertura/fechamento do intervalo, entradas/saídas externas e transferências próprias por dia. Projeção usa somente saldo pendente atual por vencimento; vencidos e registros sem vínculo têm indicações separadas. Toda fonte filtra tenant/filial; relatório exige cashflow e leituras AP/AR/tesouraria e usa snapshot REPEATABLE_READ. Novos movimentos gravam instante explícito depois do lock. 158 testes de regressão e 32 PostgreSQL aprovados; Angular 485,81 kB. Imagens locais aplicadas e três serviços saudáveis, V1–V16 aplicadas, UI/bundle 200 e API anônima 401. Restore V16 aprovado com adulteração/chave incorreta rejeitadas. Banco permanente continua vazio. Evidências cash-flow-postgres.txt, cash-flow-local.json e backup-restore-v16.json. Login real e revisão visual seguem adiados. Próximo passo: conciliação de extratos informados/importados, sem conexão bancária automática; depois núcleo do PDV conforme ROADMAP.

## Conciliação de extratos — V17

Entrada CSV/manual e prévia sem gravação, deduplicação por identificador na conta, arquivos sobrepostos, correspondências com valor/sinal exatos, revisão versionada e reset com histórico implementados. Importação/revisão não altera tesouraria. Permissões de leitura/importação/revisão separadas, escopo da conta em todas as consultas. Histórico ordenado por versão e componentes sob demanda. 171 testes de regressão e 13 PostgreSQL aprovados, zero falhas/skips. Build Angular initial 488,01 kB e lazy 21,74 kB. Imagens aplicadas; três serviços saudáveis, V1–V17 aplicadas, UI/scripts 200 e API anônima 401. Restore V17 aprovado com adulteração/chave incorreta rejeitadas. Banco permanente mantém zero empresas/importações/linhas/revisões. Evidências reconciliation-postgres.txt, reconciliation-local.json e backup-restore-v17.json. Login real/revisão visual adiados. Próximo passo: núcleo do PDV com terminais e abertura/fechamento de caixa, depois venda integrada ao estoque e pagamentos mistos conforme ROADMAP.

## Terminais / sessões de caixa — V18

Terminais por filial com CASH exclusivo, abertura conferida, sessão única por terminal/conta/operador, suprimento/sangria integrados, reserva e fechamento com snapshot de divergência implementados. Dono/supervisor e permissões separados; acesso à conta contraparte revalidado também em replay. Esperado em sessão aberta segue conta; fechado permanece congelado. Divergência não ajusta dinheiro automaticamente. Regressão inicial 185 e PostgreSQL 36 aprovados; caixa final 15 H2 + 15 PostgreSQL aprovados, com um cenário adicional. Não foi executada uma nova suíte integral de 186. Angular initial 489,56 kB e lazy PDV 21,75 kB. Imagens locais atualizadas, três serviços saudáveis, V1–V18 aplicadas; UI/scripts 200, API anônima 401. Restore V18 aprovado com adulteração/chave incorreta rejeitadas. Banco permanente sem empresas/terminais/sessões/movimentos de PDV fictícios. Evidências pos-cash-postgres.txt, pos-cash-final.txt, pos-cash-local.json e backup-restore-v18.json. Login real e revisão visual adiados. Próximo passo: configuração de depósito do terminal e venda com itens/preço histórico/baixa de estoque, depois pagamentos mistos e fiscal conforme ROADMAP.

## Vendas com itens — V19

Implementados depósito do terminal configurado com versão/caixa fechado, venda em dinheiro com 1–100 produtos distintos, conferência de preço servidor, snapshots de SKU/nome/unidade/preço, HALF_UP por item, troco, baixa CONSUMPTION por item e recebimento líquido POS_SALE. Tenant lock e produtos em ordem de UUID protegem caixa/estoque; retry não repete efeitos, inclusive após fechar. Venda incrementa versão e esperado da sessão. Permissões/operador/supervisor/filial conferidos; 41 permissões no provisionamento novo, sem elevação de contas existentes. Histórico paginado preserva dados, custos não aparecem na venda. Frontend lazy /pos-sales com carrinho e payload pendente em memória.

Regressão 201 e PostgreSQL 46 aprovados (vendas 15, caixa 15, estoque 16), zero falhas/erros/skips. Angular initial 495,23 kB/lazy vendas 15,15 kB. Imagens locais aplicadas, três serviços saudáveis, V1–V19 aplicadas; página/scripts HTTP 200, API anônima 401. Restore V19 aprovado com adulteração/chave incorreta rejeitadas. Banco permanente mantém zero empresas/terminais/sessões/vendas/itens. Evidências pos-sale-regression-postgres.txt, pos-sale-frontend.txt, pos-sale-local.json e backup-restore-v19.json. Login real e revisão visual seguem adiados. Próximos passos: pagamentos mistos e cancelamento/devoluções com reversão rastreável; fiscal/hardware/offline conforme ROADMAP.

## Pagamentos conferidos / cancelamento integral — V20 (15/10/2026)

Venda suporta 1–100 parcelas CASH/PIX/BANK_TRANSFER já recebidas, com conta, comprovante e confirmação explícita em bancários, soma exata e troco somente em dinheiro. Comprovante único por empresa/conta/meio impede reutilização mesmo após refund. Formato V19/fingerprint preservados e backfill sem repostagem. Histórico detalha parcelas, operações, comercial/financeiro/fiscal e cancelamento. pos:cancel separado e somente provisionamento novo ganha permissão (42 no conjunto).

Cancelamento integral exige turno original aberto, operador/supervisor, versão, nota e confirmação de reembolso externo quando aplicável. Reverte valor aplicado nas contas originais, retorna itens ao custo de saída com média atual e mantém a trilha original. Saldo insuficiente, unidade alterada, depósito inativo, acesso/versão incorretos desfazem tudo. Retry, concorrência e fechamento protegidos por tenant lock/versão e produto em ordem. Reenvio da venda cancelada nunca reativa pagamentos/estoque.

222 testes H2 e 67 PostgreSQL aprovados, sem falhas/erros/skips (vendas 15, caixa 15, estoque 16, novos pagamentos/cancelamento 20, upgrade de venda antiga 1). Angular initial 495,37 kB/lazy vendas 24,39 kB. Imagens aplicadas; três serviços saudáveis; V1–V20 confirmadas, UI/scripts/CSS 200 e APIs anônimas 401. Restore V20 aprovado, adulteração/chave incorreta rejeitadas. Banco permanente com zero empresas/vendas/pagamentos/cancelamentos. Evidências pos-mixed-regression-postgres.txt, pos-mixed-frontend.txt, pos-mixed-local.json e backup-restore-v20.json.

C: chegou a ~180 MB livres; node_modules regenerável movido ao D: com junção no caminho original, build verificado. Cerca de 400 MB livres permanecem; nenhum código, segredo, banco, backup ou recurso de outros projetos alterado. Duas imagens V18 próprias e sem uso removidas. Dependências em dependency-storage-v20.json. Login real/revisão visual adiados. Ainda faltam estados parciais/assíncronos e provedores/cartões/parcelamento/taxas, devoluções parciais/após turno, descontos/alçadas, fiscal e o restante do ROADMAP. Não foi concluído o escopo integral do ERP.

## Devoluções parciais e após fechamento — V21 (15/10/2026)

Devoluções com itens/quantidades, valor histórico proporcional acumulado, alocação por pagamentos originais e reembolso em turno aberto da mesma filial implementadas. Dinheiro debita gaveta atual; Pix/transferência debitam conta original com confirmação/comprovante externo. Limites acumulados por item/pagamento, UUID/fingerprint/versão, permissões/operador/supervisor/filial e lock da empresa protegem duplicação/concorrência. Retorno ao custo original, inclusive produto inativo; unidade alterada/depósito inativo e falha financeira desfazem todos os itens. Cancelamento integral após qualquer devolução bloqueado. Histórico preserva originais, valores e quantidades já devolvidos/reembolsados; estados comercial/financeiro derivados distinguem parcial/total. pos:return apenas em provisionamento novo (43 permissões), sem elevação de contas existentes.

247 testes de regressão e 92 PostgreSQL aprovados, zero falhas/erros/skips; devoluções possuem 25 cenários H2/PostgreSQL. Angular initial 495,70 kB, lazy vendas 38,09 kB. Imagens aplicadas, três serviços saudáveis, V1–V21 confirmadas; UI/scripts/CSS HTTP 200 e consultas/criação de devolução sem token 401. Restore V21 aprovado com adulteração/chave incorreta rejeitadas. Banco permanente sem empresas/vendas/devoluções/itens/reembolsos fictícios. Evidências pos-return-regression-postgres.txt, pos-return-frontend.txt, pos-return-local.json e backup-restore-v21.json.

C: continuou com espaço reduzido; JAR gerado/validado arquivado no D: após conferir hash e gerar imagens. Caminho/checksum em artifact-storage-v21.json; Maven verify/package regenera o artifact padrão antes de novo build Docker. Aproximadamente 270 MiB livres no C:; não houve limpeza global, alteração de banco/segredos/backups nem de outros projetos. Login real e revisão visual continuam adiados. Próximos itens: descontos/alçadas e suspensão do PDV; estados parciais/assíncronos/provedores/cartões/taxas; fiscal; políticas de troca/quarentena/fluxos entre filiais, conforme ROADMAP. ERP integral permanece em desenvolvimento.

## Descontos / autorização / suspensão — V22 (15/10/2026)

CartPricing centraliza preço bruto, desconto do item e líquido. Política percentual por filial/versão, padrão 0%, e permissões separadas para aplicar/configurar/aprovar. Acima do limite por item exige outro usuário, carrinho exato/solicitante/filial/política, prazo de 30 minutos e autorização atual do aprovador; uso único consumido na transação. Snapshots preservam bruto/desconto/líquido, e devolução reembolsa o líquido. Novas permissões somente no provisionamento novo (47 no conjunto). Sem autoaprovação ou diluição de desconto entre itens.

Suspensão guarda carrinho sem estoque/pagamentos/reserva/alteração da contagem do caixa. Retomada em outro turno/terminal da filial, atualização por versão, descarte e journal de UUID/fingerprint implementados. Carrinho salvo deve corresponder à conclusão; versão e vínculo único impedem duas vendas da mesma suspensão. Falha financeira/estoque mantém suspensão e aprovação disponíveis. Interface inclui políticas, solicitações/conferência/decisão, suspensão/retomada/salvamento/descarte e trava mútua entre comandos pendentes.

272 testes H2 e 117 PostgreSQL aprovados, zero falhas/erros/skips; novos cenários 25 e upgrade com item/ledger antigos preserva bruto/zero desconto e não reposta dinheiro/estoque ou permissões. Angular initial 496,23 kB/lazy vendas 58,99 kB. Imagens aplicadas; três serviços saudáveis, V1–V22 confirmadas, página/scripts/CSS HTTP 200 e APIs de política/solicitações/suspensões anônimas 401. Restore V22 aprovado com adulteração/chave incorreta rejeitadas. Banco permanente mantém zero empresas/vendas/políticas/solicitações/suspensões/comandos. Evidências pos-commercial-regression-postgres.txt, pos-commercial-frontend.txt, pos-commercial-local.json e backup-restore-v22.json.

Login real e revisão visual continuam adiados. Próximos itens: clientes/crediário e estados parciais de venda/pagamento; provedores/cartões/fiscal, promoções/cupons e alçadas por valor/categoria seguem no ROADMAP integral. Políticas percentuais e autorização básica não significam homologação comercial do módulo completo.

## Parcelamento financeiro — V23 (16/10/2026)

Geração manual de parcelas em Contas a receber: prévia obrigatória para múltiplas parcelas, calendário mensal ancorado, total dividido em centavos exatos e geração atômica de plano/títulos/vínculos/auditoria. Replay retorna títulos atuais sem recriar recebimentos ou cancelamentos. Cada parcela reutiliza baixas parciais, estornos e cancelamento individual. Escopos/permissões existentes; sem movimentar estoque/caixa/tesouraria na geração. Migração aditiva V23 sem conceder privilégios ou criar dados permanentes. Contrato e limites em RECEIVABLE_PLANS.md.

Regressão H2 completa aprovada com 295 testes, zero falhas/erros/skips. Angular aprovado, initial 476,43 kB e tela financeira lazy 25,57 kB. PostgreSQL aprovado com 149 testes, zero falhas/erros/skips. V23 aplicada localmente com três serviços saudáveis. Migration 23 confirmada; página de recebíveis, seis scripts e CSS HTTP 200; criação/prévia/consulta anônimas HTTP 401. Contagens empresa/plano/vínculo/título/recebimento 0|0|0|0|0. Restore criptografado V23 aprovado; adulteração e chave incorreta rejeitadas. Evidências receivable-plan-local.json, receivable-plan-deploy.txt e backup-restore-v23.json. Banco de origem vazio: ensaio não substitui volume de operação real. Login real/revisão visual e publicação em produção continuam adiados.

Venda a prazo no PDV, autorização/limite de crédito, juros/multas, renegociação, pagamentos parciais/assíncronos e provedores permanecem pendentes. A geração manual de parcelas não conclui o crediário nem o ERP integral. Login real e revisão visual continuam adiados.
## Crediário no PDV — V24 (16/10/2026)

Venda integral a prazo ou com entrada conferida implementada, com cliente ativo, política global de limite/bloqueio e geração atômica de títulos/estoque/entrada. Exposição inclui dívidas manuais e de outras filiais, com bloqueio por atraso e negação genérica. Gestão exige todas as filiais e customer:credit; concessão exige pos:credit e permissões financeiras/venda. Whitelist nova com 49 permissões, sem elevar usuários existentes. Detalhe da venda projeta saldo e situação dos títulos e permite abrir a parcela no financeiro.

Cancelamento integral no turno original cancela os títulos e reverte estoque/entrada na mesma transação; recebimentos ativos devem ser estornados antes. Cancelamento manual isolado de título originado por venda bloqueado. Devolução parcial de crediário explicitamente indisponível até tratar abatimento de dívida/reembolsos. Prévia atual das parcelas obrigatória no PDV; crédito não integra o carrinho suspenso. Login real e revisão visual seguem adiados. Contrato completo em POS_CREDIT.md.

Primeira regressão H2 com 312 cenários aprovada; PostgreSQL identificou check virtual de NOT NULL na descoberta de restrições. Migração corrigida para selecionar apenas checks monetários e preservar NOT NULL. Ensaio direcionado de correção: 18 H2 e 18 PostgreSQL aprovados. Regressão final aprovada: 312 testes H2 e 166 PostgreSQL, zero falhas/erros/skips. V24 aplicada localmente; API, banco e frontend saudáveis. Migration 24 e tendered NOT NULL confirmados. Rotas de crédito/vendas/recebíveis, sete scripts e CSS HTTP 200; GET/PUT de política e POST de venda anônimos HTTP 401. Contagens empresa/política/venda a prazo/plano/título 0|0|0|0|0. Restore V24 aprovado em banco isolado; adulteração e chave incorreta rejeitadas. Evidências pos-credit-local.json, pos-credit-deploy.txt e backup-restore-v24.json. Origem vazia: não substitui ensaio com volume operacional. Login real, revisão visual e publicação em produção continuam adiados. Angular aprovado: initial 477,15 kB, lazy vendas 68,38 kB e política de crédito 6,66 kB.
## Devoluções do crediário — V25 (16/10/2026)

Devolução parcial/integral em novo turno da mesma filial integrada a estoque, abatimento de dívida e reembolso de entrada/recebimentos de parcelas. Abate primeiro parcelas mais distantes; somente excedente gera reembolso de fontes explícitas. Original de título/recibo preservado; recebido líquido e credited_amount refletem o ajuste. Ledgers distintos e histórico paginado no financeiro; status CREDITED para título liquidado com abatimento e SETTLED_BY_RETURN para venda totalmente devolvida.

Nova receivable:credit (50 no provisionamento novo, sem elevar existentes), receivable:read e permissões anteriores do retorno/caixa. Comprovantes bancários conferidos e únicos entre ledgers, origem da própria venda, caps acumulados, dívida/reembolso revalidado, UUID/fingerprint e transação única. Recibo já reembolsado não aceita estorno integral. Sem transferência bancária/fiscal automática. Contrato em POS_CREDIT_RETURNS.md.

Regressão completa aprovada com 332 H2 e 196 PostgreSQL. Revisão final acrescentou caso de UUID igual entre pagamento inicial/recibo independente; validação direcionada aprovada com 46 H2 e 46 PostgreSQL, sem falhas/erros/skips. V25 aplicada localmente com três serviços saudáveis. Migration 25 e NOT NULL de received_amount/credited_amount confirmados; páginas de vendas/recebíveis, sete scripts e CSS HTTP 200. Consultas de origens/ajustes e POST de devolução anônimos HTTP 401. Contagens empresa/devolução/abatimento/reembolso de parcela/título abatido 0|0|0|0|0. Restore V25 aprovado em banco isolado; chave incorreta e adulteração rejeitadas. Evidências credit-return-local.json, credit-return-deploy.txt e backup-restore-v25.json. Origem vazia: não substitui ensaio com volume operacional. Sem login real/revisão visual ou publicação em produção. Login real e revisão visual continuam adiados. Ainda faltam anulação/compensação de devolução, fluxos entre filiais/quarentena/políticas avançadas, pagamentos pendentes/assíncronos e demais itens do ROADMAP.

## Preparação de pagamento — V26

Veja [POS_PENDING_CHECKOUTS](POS_PENDING_CHECKOUTS.md): preparação persistida sem dinheiro ou reserva, conclusão pela transação normal de venda e descarte auditado. Não implementa cobranças, webhooks ou recebimentos parciais antes da conclusão.

V26: 67 testes H2 (PendingCheckoutTest 12, SaleTest 15, SalePaymentTest 20, CreditReturnTest 20) e 12 PostgreSQL (PendingCheckoutPostgresIT), sem falhas/erros/skips. Rodada focada, sem nova regressão global. Evidência: `test-evidence/pending-checkout-validation.txt`.


## Aplicação local V26 — 16/10/2026

Preparação de pagamento aplicada com as imagens locais da API e interface. Três containers saudáveis; health UP; página de vendas e oito arquivos JS/CSS HTTP 200 com MIME correto; GET/POST anônimos da preparação retornam 401. Banco em V26, sem empresas ou preparações permanentes (`26|0|0`). Evidências em `test-evidence/pending-checkout-local.json` e `pending-checkout-deploy.txt`.

Backup criptografado restaurado em PostgreSQL isolado na V26, com adulteração e chave incorreta rejeitadas (`test-evidence/backup-restore-v26.json`). A origem vazia não demonstra recuperação de volume operacional real nem gestão externa de chaves. Login real e revisão visual continuam adiados.

## Retomada sem banco — 11/11/2026

Conferência de pagamentos no PDV implementada no código: aplicado, recebido informado, troco, saldo restante e excesso. A conclusão agora bloqueia inconsistências monetárias localmente; backend usa calculadora isolada sem JDBC, mantendo validações de contas e comprovantes no serviço. Contrato em [PAYMENT_ALLOCATION](PAYMENT_ALLOCATION.md).

Validação nesta retomada: 20 testes JUnit em memória e 15 testes Node aprovados; TypeScript e templates Angular com `--noEmit` aprovados. Nenhum banco/H2/PostgreSQL ou Docker foi acessado ou iniciado. Build Maven completo bloqueado por falta de espaço ao gravar classes; compilação das classes Java alteradas e execução JUnit feitas em memória. Sem nova implantação ou regressão integrada. Login real e revisão visual continuam adiados. Evidência: `test-evidence/payment-allocation-offline.json`. As evidências de saúde e implantação V26 acima são históricas de outubro, não verificações atuais.

## Montagem de pagamentos mistos — continuação de 11/11/2026

Implementados preenchimento do saldo restante e edição/cancelamento de edição de parcelas no PDV. A lista anterior é preservada até salvar uma composição válida; excesso, comprovante repetido e troco bancário são bloqueados. Edição bancária exige nova confirmação. Conclusão/preparação ficam bloqueadas enquanto houver parcela em edição.

Após a limpeza de espaço autorizada, compilação completa do backend, empacotamento JAR e 20 testes unitários de cálculo aprovados com Maven convencional. 27 testes Node da calculadora/composição aprovados; build Angular de produção aprovado. Evidências: `test-evidence/payment-allocation-package.txt`, `payment-composer-unit.txt`, `payment-composer-frontend.txt`. Sem banco, H2/PostgreSQL, atualização de Docker ou implantação. Login real, revisão visual e regressão integrada seguem pendentes. Detalhes em [PAYMENT_ALLOCATION](PAYMENT_ALLOCATION.md).

## Caixa selecionado e reconferência — 11/11/2026

Paginação dos caixas preserva a seleção/carrinho. Trocar o caixa mantém valores propostos, mas retira confirmações bancárias e o dinheiro recebido informado; mudar filial também limpa a conta bancária das propostas. Suspensão respeita a filial e preparação fica restrita à sessão original. Parcelas podem ser reconferidas uma a uma; todas precisam estar válidas antes de concluir.

42 testes Node aprovados (30 de cálculo/composição, 12 de contexto), build Angular aprovado, sem acesso ao banco/Docker ou implantação. Testes de regras e compilação não substituem a revisão de browser, ainda adiada com o login real. Detalhes: [PAYMENT_ALLOCATION](PAYMENT_ALLOCATION.md). Evidências em `test-evidence/payment-context-*`.

## Revisão final da venda — 11/11/2026

PDV com revisão local antes da conclusão: caixa/operador, itens/preços/descontos, entrada, crediário/parcelas e troco. Resumo e comando são capturados em uma cópia imutável. A confirmação exige dados atuais correspondentes e formulário válido; alterações pedem nova revisão. Reenvios incertos continuam usando o comando/UUID capturado. Contrato e limites em [POS_SALE_REVIEW](POS_SALE_REVIEW.md).

54 testes Node aprovados (12 novos de revisão), build Angular aprovado; inicial 477,62 kB, vendas lazy 96,38 kB. Sem banco, backend/schema, Docker ou implantação. Login real, revisão visual e testes integrados seguem adiados. Evidências: `test-evidence/sale-review-*`.

## Histórico de vendas com filtros — 11/11/2026

Implementação de busca por venda/SKU/nome histórico do item/cliente do crediário, período em horário de Brasília, situação comercial (concluída, cancelada, devolução parcial/integral), meio inicial e presença de crediário. SQL aplica escopo e critérios antes de contar/paginar; interface exibe situações comercial/financeira/fiscal, saldo atual e reembolso. Detalhes e cenários para a etapa final em [POS_SALE_HISTORY](POS_SALE_HISTORY.md).

Orientação vigente: priorizar implementação e deixar testes para o final. Nesta rodada não foram escritos/executados testes ou acessado banco/Docker. Compilação/JAR sem testes e build Angular aprovados; sem validação funcional do SQL e sem implantação. Login real e revisão visual continuam adiados. Evidências de compilação: `test-evidence/sale-history-backend-build.txt`, `sale-history-frontend-build.txt`.

## Manutenção do catálogo — 11/11/2026

Implementados edição versionada de SKU/nome/descrição/preço/códigos, listas mistas de GTIN e códigos internos, ativação/desativação auditada e filtro ativo/inativo. Unidade fixa para preservar quantidades; snapshots de vendas não são reescritos. Interface carrega cadastro atual, conserva operação incerta para repetição e bloqueia novas mutações/saída durante a pendência. Contrato e limites em [PRODUCT_MAINTENANCE](PRODUCT_MAINTENANCE.md).

Compilação/JAR sem testes e build Angular aprovados. Nenhum teste escrito/executado nesta etapa; validação funcional/SQL/concorrência adiada por orientação do usuário. Sem banco, Docker, migração ou implantação. Login real e revisão visual seguem adiados. Evidências: `test-evidence/product-maintenance-*-build.txt`.

## Financeiro por recebimento de compra — V27 preparada, 11/11/2026

Implementado fluxo explícito de geração de um título a pagar com valor calculado pelas mercadorias recebidas. V27 preparada para valores monetários imutáveis das linhas de recebimento e vínculo único com título; recebimentos parciais distribuem centavos por custo acumulado. Geração bloqueia títulos manuais ativos e não permite duplicar a origem, inclusive após cancelamento do título gerado. Interface permite consultar, gerar, repetir comando incerto e abrir o título em contas a pagar. Não realiza pagamento ou nova movimentação de estoque.

Compilação/JAR sem testes e build Angular aprovados. **V27 não aplicada**, testes/migração/upgrade/transações e interface ainda não validados funcionalmente, conforme orientação de deixar testes para o final. Sem banco, Docker, backup ou implantação nesta rodada. Login real/revisão visual continuam adiados. Contrato, limites e cenários pendentes: [PURCHASE_RECEIPT_FINANCE](PURCHASE_RECEIPT_FINANCE.md). Evidências: `test-evidence/purchase-finance-*-build.txt`.

## Parcelamento dos recebimentos de compra — V28 preparada, 11/11/2026

Implementadas prévia de calendário e geração atômica de 1 a 120 contas a pagar, intervalos de 1 a 12 meses, divisão em centavos exatos e vencimentos ancorados na primeira data. Interface exige prévia atual e mostra todas as parcelas com links individuais. Mantida geração única por recebimento e compatibilidade de reenvios de parcela única da V27.

V28 preparada para tabela de parcelas e conversão das gerações V27 em parcela única. **V27/V28 não aplicadas.** Backend/JAR sem testes e build Angular aprovados. Nenhum teste escrito/executado, SQL/banco ou Docker acessado, nem implantação. Validação funcional/integrada e login/revisão visual seguem adiados. Detalhes em [PURCHASE_INSTALLMENTS](PURCHASE_INSTALLMENTS.md); evidências `test-evidence/purchase-installments-*-build.txt`.

## Consulta e totais de contas a pagar — 11/11/2026

Implementados busca por fornecedor/referência/documento/descrição, período de vencimento, origem com/sem recebimento e consulta de todas as parcelas da mesma origem. API oferece também fornecedor/filial exatos, com escopo aplicado antes de contagem, totais e paginação. Totais atuais de original/pago líquido/a pagar/vencido cobrem toda a consulta. Navegação por título/origem e carregamento sob demanda de contas a pagar implementados.

Backend/JAR sem testes e build Angular aprovados; inicial 477,02 kB, contas a pagar lazy 26,84 kB. Nenhum teste escrito/executado, SQL ou banco acessado, Docker iniciado ou implantação. V27/V28 continuam preparadas e não aplicadas. Login/revisão visual e validação funcional seguem adiados. Contrato em [PAYABLE_SEARCH](PAYABLE_SEARCH.md), evidências `test-evidence/payable-search-*-build.txt`.

## Consulta e totais de contas a receber — 11/11/2026

Implementados busca por cliente/referência/documento/descrição, vencimentos, situação e origem avulsa/manual/crediário; API também aceita cliente/filial/plano/venda exatos. Totais atuais consideram recebido líquido, abatimentos e saldo devedor sem contar devolução como entrada. Detalhe mostra origem/posição e consulta das demais parcelas; links do PDV já filtram o grupo.

Backend/JAR sem testes e build Angular aprovados (inicial 476,96 kB, recebíveis lazy 32,24 kB). Nenhum teste escrito/executado, SQL/banco acessado, schema alterado ou implantação. V27/V28 continuam não aplicadas; login/revisão visual e validação funcional seguem adiados. Detalhes em [RECEIVABLE_SEARCH](RECEIVABLE_SEARCH.md), evidências `test-evidence/receivable-search-*-build.txt`.


## Sequência autorizada de desenvolvimento 1–7 — 11/11/2026

Implementados inventário físico com aprovação separada, classificação de lotes e FEFO, mínimo/alvo e criação de compra por sugestões, devolução ao fornecedor com prévia/abatimento/crédito, preços por filial e margem/histórico, pedidos/orçamentos com aprovação e conversão, promoções por produto/filial, vendedores/comissões e pontos/resgates de fidelidade. Inclui APIs, telas lazy, navegação, permissões, auditoria, versões, reenvio idempotente e integração ao estoque/venda/financeiro. Devoluções/cancelamentos de vendas revertem comissão e pontos proporcionalmente. Contas a pagar e projeção consideram créditos de devolução.

Backend/JAR sem testes e build Angular compilados. Nenhum teste escrito/executado, banco acessado, migração aplicada, Docker iniciado ou implantação. V27–V36 permanecem preparadas e não aplicadas. Login real/revisão visual continuam adiados. As sete frentes estão implementadas dentro dos limites documentados; isso não conclui todos os requisitos do ERP nem comprova funcionamento integrado. Contrato detalhado, restrições operacionais e habilitação de permissões existentes em [IMPLEMENTATION_SEQUENCE](IMPLEMENTATION_SEQUENCE.md). Evidências `test-evidence/sequence-*-build.txt`.
