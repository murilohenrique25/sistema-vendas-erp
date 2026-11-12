# sistema-vendas-erp

ERP comercial com gestão de empresas e filiais, produtos, estoque, vendas, caixa e rotinas financeiras.

## Tecnologias

- TypeScript
- Java
- Maven
- Spring Boot
- Angular
- PostgreSQL
- Flyway
- HTML
- CSS
- Docker Compose

## Projetos e componentes

- [backend](backend/README.md)
- [frontend](frontend/README.md)

---

## Documentação anterior

# ERP Comercial SaaS

Desenvolvimento incremental do escopo integral descrito em [REQUIREMENTS](documentation/REQUIREMENTS.md). Implementados: autenticação, empresas, filiais, auditoria, cadastro interno de produtos/códigos e núcleo de estoque/perdas com frontend Angular integrado a API Spring Boot. Estoque inclui depósitos, custo médio, ledger, transferências imediatas, retentativas idempotentes e aprovação/rejeição de perdas. **Ainda não é um produto pronto para comercialização.** Consulte [PROGRESS](documentation/PROGRESS.md) para evidências e pendências.

## Executar localmente

**Validação:** regressão de 272 testes locais aprovada; vendas/pagamentos/cancelamento/devoluções/descontos/suspensão/caixa/estoque e migração com 117 cenários PostgreSQL aprovados; conciliação com 13 cenários PostgreSQL aprovados; fluxo de caixa/AP/AR vinculados com evidência anterior de 32 cenários (clientes/recebíveis com evidência anterior de 41); tesouraria com oito cenários H2 e oito PostgreSQL aprovados; contas a pagar com 14 cenários H2 e PostgreSQL aprovados; fornecedores com seis cenários PostgreSQL aprovados; escopo por filial com cinco cenários PostgreSQL aprovados; perfis com cinco cenários PostgreSQL aprovados; gestão de usuários/permissões com 12 cenários H2 e 12 PostgreSQL aprovados. Fundação e estoque possuem evidências PostgreSQL anteriores (17 e 16 cenários). V1–V22 verificadas em banco real. **Ambiente local recuperado em 14/10/2026:** banco, API e frontend saudáveis em http://localhost:4200; migrações V1–V22 aplicadas. Servidores temporários 4201/4202/18081 encerrados; reprodução em [TESTING](documentation/TESTING.md). Dependências Angular restauradas e build atualizado.

Pré-requisitos: Docker Engine ativo e Docker Compose. Se `.env` não existir, copie `.env.example` e substitua os dois segredos por valores aleatórios. Não use os exemplos em produção. Nesta máquina, `.env` já foi gerado com segredos aleatórios e está ignorado pelo Git.

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
# Edite .env antes de continuar.
docker compose up --build -d
$env:PROVISIONING_KEY = 'mesmo-segredo-configurado-no-arquivo-env'
./scripts/provision-company.ps1 -CompanyName 'Sua empresa' -Slug 'sua-empresa' -AdminName 'Seu nome' -Email 'seu-email@empresa.com'
```

O script solicita a senha sem ecoá-la. Acesse http://localhost:4200, informe o identificador `sua-empresa`, o e-mail e a senha escolhida. Não existem usuários ou produtos fictícios pré-cadastrados. Atualizar a página exige novo login porque o token fica apenas em memória. Em Segurança da conta, consulte sessões, encerre outros acessos ou troque sua senha. Em Usuários, gestores com user:read/write cadastram pessoas, ativam/desativam contas e editam permissões; somente novas empresas recebem esses privilégios no provisionamento.

API no Compose: http://localhost:18080 (BACKEND_PORT); banco local na porta 15432 (DB_PORT). As portas 8080 e 5432 já estão ocupadas nesta máquina. Para usar o script, defina PROVISIONING_KEY com o valor do arquivo `.env`, sem registrá-lo em logs ou commits. O backend fora de Docker usa porta 18080 e PostgreSQL em 15432 por padrão; ajuste SERVER_PORT/DB_URL para outro ambiente.

## Desenvolvimento sem containers da aplicação

Java 21, Maven 3.9+, Node 22.12+ (ou 20.19+) e PostgreSQL 17. Inicie somente o banco com `docker compose up -d db`. Configure `DB_PASSWORD` e `PROVISIONING_KEY` no terminal do backend.

```powershell
mvn -f backend/pom.xml spring-boot:run
# Em outro terminal:
cd frontend
npm ci
npm start
```

## Verificar

```powershell
mvn -f backend/pom.xml test
mvn -f backend/pom.xml verify -Ppostgres-tests
cd frontend
npm ci
npm run typecheck
npm run build
```

A segunda execução Maven exige Docker e roda os mesmos cenários de integração em PostgreSQL real. H2 é utilizado exclusivamente nos testes rápidos, nunca em produção.

Documentação: [arquitetura](documentation/ARCHITECTURE.md), [banco](documentation/DATABASE.md), [API](documentation/API.md), [segurança](documentation/SECURITY.md), [implantação](documentation/DEPLOYMENT.md), [testes](documentation/TESTING.md), [roadmap](documentation/ROADMAP.md).

Regras do núcleo de estoque/perdas, fórmula de custo médio, idempotência e funcionalidades pendentes: [INVENTORY](documentation/INVENTORY.md).





## Perfis e backup

Em Perfis de acesso, crie conjuntos reutilizáveis; em Usuários, abra Permissões e aplique um perfil. Aplicação substitui os acessos; alterações posteriores do modelo exigem reaplicação explícita.

Backup cifrado e ensaio de restauração local: veja [BACKUP](documentation/BACKUP.md). É necessário configurar ERP_BACKUP_KEY e guardar a chave separadamente. Scripts não agendam cópias externas nem restauram sobre o banco do ERP.


## Acesso por filial

Em Usuários, abra Permissões e configure acesso global ou filiais específicas. A restrição vale na API para filiais, depósitos, estoque, transferências e perdas. Acesso global é o padrão até o gestor restringir explicitamente. Gestores de usuários precisam de acesso global; catálogo é compartilhado pela empresa. Administração global/auditoria não ficam disponíveis a contas restritas.


## Fornecedores

Cadastro compartilhado por empresa em Fornecedores: busca, edição e ativação/desativação com histórico preservado. Permissões supplier:read/write; novas empresas recebem esses privilégios, usuários existentes não são elevados automaticamente. Identificação fiscal é informada, sem validação cadastral oficial. Pedidos e recebimentos básicos integrados ao estoque; XML e contas a pagar ainda pendentes.


## Pedidos de compra

Em Compras, monte pedido por filial/fornecedor com produtos e custos negociados. Abra o pedido para registrar recebimentos parciais/completos no depósito da mesma filial. Cancelamento fecha só saldo pendente e preserva estoque recebido. Comandos incertos devem ser repetidos na mesma tela. Permissões purchase:read/write/receive; seletores usam permissões de consultar filiais, fornecedores, produtos e estoque. Regras e limitações em [PURCHASES](documentation/PURCHASES.md).


## Contas a pagar

Em Contas a pagar, lance obrigações por fornecedor/filial, registre pagamentos já realizados, estorne registros e consulte vencimentos/histórico. Recebimentos de compras podem abrir lançamento pré-vinculado; informe valor/vencimento do documento financeiro. Não executa transferências ou refunds bancários. Permissões payable:read/write/pay/reverse. Datas financeiras via ERP_FINANCE_ZONE, padrão America/Sao_Paulo. Regras em [PAYABLES](documentation/PAYABLES.md).


Tesouraria interna: contas de caixa/banco por filial, saldo/extrato e lançamentos/transferências atômicos. Veja [TREASURY](documentation/TREASURY.md). Ainda sem vínculo automático com títulos nem execução bancária.

Pagamentos de títulos podem selecionar conta de origem: baixa/débito e estorno/crédito são transações únicas. O vínculo é opcional e preserva registros antigos. Exige leitura/movimentação da tesouraria além das permissões de contas a pagar; sem integração bancária. Consulte [PAYABLES](documentation/PAYABLES.md).

Clientes e contas a receber manuais: cadastro/busca/edição/ativação de clientes, títulos por filial, recebimentos parciais, estornos e conta de destino opcional. Recebimento vinculado credita a tesouraria; estorno debita a conta original. Veja [RECEIVABLES](documentation/RECEIVABLES.md). Não há cobrança ou confirmação bancária automática.

Fluxo de caixa por filial: saldo atual, abertura/fechamento do período, movimentações diárias, transferências e posição atual de títulos pendentes/vencidos. Registros sem conta são indicados separadamente. Consulte [CASH_FLOW](documentation/CASH_FLOW.md) para fórmulas e limites; requer cashflow:read e as três leituras financeiras.

Conciliação de extratos por CSV/manual: prévia, importações idempotentes e sobrepostas, correspondências sugeridas com confirmação, ignorar/desfazer e histórico versionado. Não altera saldos nem conecta a bancos. Leia [RECONCILIATION](documentation/RECONCILIATION.md) para formato e permissões.

Núcleo do PDV: terminais com conta CASH exclusiva, sessão por operador, abertura por conferência de saldo, suprimento/sangria integrados e fechamento com diferença preservada. A conta fica reservada durante o turno. Venda, estoque e pagamentos mistos ainda pendentes. Consulte [POS_CASH](documentation/POS_CASH.md).

## Vendas com itens

Tela /pos-sales com depósito por terminal, itens, preço histórico, recebimento em dinheiro/troco e baixa de estoque atômica. Ver [POS_SALES](documentation/POS_SALES.md). Preço desatualizado, estoque insuficiente e caixa fechado rejeitam a conclusão sem efeitos. Divisão de pagamentos conferidos e cancelamento integral no turno disponíveis em POS_PAYMENTS. Provedores/cartões, políticas de troca e fiscal permanecem pendentes; devoluções parciais/em outro turno implementadas. Login real/revisão visual adiados por solicitação do usuário.

## Pagamentos conferidos e cancelamento integral

V20 divide recebimentos entre dinheiro, Pix e transferência já conferidos externamente, preserva parcelas/comprovantes e permite cancelamento integral com retorno de estoque e reversão financeira no turno aberto. Não executa cobrança ou reembolso bancário. Veja [POS_PAYMENTS](documentation/POS_PAYMENTS.md). Migração de vendas antigas, regressão e PostgreSQL verificados; serviços locais saudáveis e restore V21 aprovado. O escopo comercial completo ainda está em desenvolvimento.

## Devoluções parciais e em outro turno

V21 retorna quantidades ao depósito original, pelo custo histórico, com reembolso proporcional acumulado e limites por pagamento. Pode usar outro caixa/turno aberto da mesma filial sem alterar o fechamento original. Consulte [POS_RETURNS](documentation/POS_RETURNS.md). Login real/revisão visual adiados; fiscal, provedores, trocas/quarentena e fluxos entre filiais continuam pendentes.

## Descontos e suspensão de vendas

V22 inclui desconto por item com histórico bruto/líquido, limite percentual por filial, aprovação de outro usuário por 30 minutos e uso único. Carrinhos podem ser suspensos, retomados em outro turno da mesma filial, editados com versão e descartados, sem reserva ou pagamentos fictícios. Ver [POS_COMMERCIAL](documentation/POS_COMMERCIAL.md). Imagens locais e restore V22 verificados; login real e revisão visual continuam adiados.

Parcelamento financeiro V23: prévia de valores/vencimentos e geração atômica de títulos em Contas a receber. Base para crediário; integração da venda a prazo no PDV ainda pendente. Contrato: [RECEIVABLE_PLANS.md](documentation/RECEIVABLE_PLANS.md).

Crediário V24: venda integral a prazo ou entrada conferida, cliente/limite/atraso, títulos automáticos e cancelamento integrado. Configuração em Crédito de clientes; consulta de parcelas na venda e abertura do título no financeiro. Limites atuais: [POS_CREDIT.md](documentation/POS_CREDIT.md).

Devoluções do crediário V25: abatimento de dívida, reembolso de entrada/parcelas e estoque integrados, com preservação dos valores originais e histórico financeiro. Contrato: [POS_CREDIT_RETURNS.md](documentation/POS_CREDIT_RETURNS.md). Anulação de devolução, emissão fiscal e pagamentos assíncronos seguem pendentes.

Preparação de pagamento implementada em V26 no PDV: [contrato e limitações](documentation/POS_PENDING_CHECKOUTS.md). Login real e revisão visual seguem adiados.

Retomada em 11/11/2026, sem conexão com banco: [conferência local dos pagamentos](documentation/PAYMENT_ALLOCATION.md) implementada e validada por testes unitários e compilação sem emissão. Alteração ainda não implantada nos serviços.

Continuação em 11/11/2026: PDV com sugestão do saldo restante e edição segura de parcelas propostas, incluindo nova confirmação bancária. JAR e build Angular gerados, 20 testes Java e 27 Node aprovados, sem banco ou implantação. Veja [PAYMENT_ALLOCATION](documentation/PAYMENT_ALLOCATION.md).

O PDV também possui [revisão final da venda](documentation/POS_SALE_REVIEW.md) antes do envio: resumo de caixa, itens, entrada, crediário e troco, com dados congelados para a confirmação/reenvio. Validado por testes locais e build; sem nova implantação.

Histórico de vendas com [busca e filtros combináveis](documentation/POS_SALE_HISTORY.md) implementado em 11/11/2026. Compilação aprovada; testes deixados para a etapa final por orientação do usuário. Sem acesso ao banco ou nova implantação.

Catálogo com [edição e ativação/desativação de produtos](documentation/PRODUCT_MAINTENANCE.md) implementado em 11/11/2026. Compilação aprovada; testes e implantação continuam pendentes.

Implementada [geração de conta a pagar por recebimento de compra](documentation/PURCHASE_RECEIPT_FINANCE.md), com valor calculado e proteção contra duplicidade. V27 preparada, ainda não aplicada; compilação aprovada e testes adiados. Sem conexão ao banco ou implantação nesta etapa.

O financeiro dos recebimentos também possui [parcelamento com prévia de calendário](documentation/PURCHASE_INSTALLMENTS.md), até 120 parcelas, sem juros automáticos e com soma exata. V28 preparada, compilação aprovada, testes/migrações/implantação pendentes.

Contas a pagar com [busca, totais filtrados e navegação por recebimento](documentation/PAYABLE_SEARCH.md) implementados; compilação aprovada. Testes e implantação seguem pendentes.

Contas a receber com [busca, totais ajustados por devoluções e navegação entre parcelas](documentation/RECEIVABLE_SEARCH.md) implementados; compilação aprovada e testes adiados. Sem acesso ao banco ou implantação.

## Publicação no GitHub

Este repositório reúne o código-fonte e a documentação do projeto. Dependências instaladas, builds, backups, bancos locais e chaves privadas não acompanham a publicação. As configurações sensíveis identificadas foram substituídas por marcadores ou referências a variáveis de ambiente; configure seus próprios valores antes de executar os componentes.
