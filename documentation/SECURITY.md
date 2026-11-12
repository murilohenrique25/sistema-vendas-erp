# Segurança

Implementado: BCrypt custo 12; tokens aleatórios 256 bits persistidos somente em hash; expiração/revogação; bloqueio de conta após cinco falhas por quinze minutos; usuário ativo e permissões verificados por requisição; tenant derivado da autenticação; consultas parametrizadas; validação de DTOs; auditoria sem segredos; limite de paginação; provisionamento protegido e negado no proxy público. Token Angular em memória, sem localStorage/cookies. Constraints relacionais impedem associação de ator a empresa diferente.

Proxy local limita login por IP e adiciona headers CSP/frame/nosniff. A API direta deve ficar em rede privada; o rate limiting de IP está no proxy, não no backend. Não publicar porta 8080. Compose liga todas as portas ao localhost e usa HTTP para desenvolvimento; TLS no ingress é obrigatório antes de produção.

Tela Segurança da conta permite consultar datas das sessões ativas, encerrar outros acessos e trocar senha. Revogação é limitada ao próprio usuário/empresa, preserva sessão atual e audita alterações sem segredos. Lock do usuário serializa com login/troca de senha; após o lock a sessão atual é revalidada. Troca de senha exige senha atual e encerra todos os acessos.

Gestão de usuários implementa user:read/write, cadastro com senha BCrypt e permissões explícitas, lista paginada, ativação/desativação com versão e auditoria. Gestor não pode conceder permissões que não possui nem reativar conta com privilégios superiores. Desativação da própria conta é bloqueada; sessões do usuário desativado são excluídas e não retornam na reativação. Tenant deriva da sessão; operações administrativas revalidam conta, permissão e sessão após lock. Administradores antigos não recebem novos privilégios automaticamente. Convites ainda pendentes. Perfis reutilizáveis copiam permissões explicitamente, com versão e proteções administrativas; alteração de modelo não altera acessos de usuários automaticamente.

Edição de permissões usa a mesma versão da ativação, lock de tenant, revalidação de ator/sessão e auditoria transacional. Bloqueia concessões e edição de contas acima dos privilégios do gestor. user:write exige user:read; último gestor ativo não pode perder a administração. Duas remoções concorrentes são serializadas. Permissões das sessões abertas são relidas pelo filtro; frontend atualiza suas próprias permissões via /auth/me após salvar.

Pendente: MFA, recuperação com token de uso único e provedor de e-mail, limite distribuído de tentativas/IP, identificação de dispositivos e revogação individual, rotação/renovação, RLS, política de auditoria/retencão/backup, LGPD, exportação e anonimização, revisão de dependências e pentest. Não liberar este marco para operação comercial.

PROVISIONING_KEY é segredo operacional da plataforma, nunca incluído no frontend. Remover/desabilitar após provisionamento se não necessário. Senhas de banco via env local; secret manager em produção. Usuários e sessões nunca retornam hashes. Nenhuma integração financeira ou fiscal habilitada.

Estoque: inventory:read/write/cost e loss:read/create/approve são permissões distintas. Custos são suprimidos no backend sem inventory:cost. Referências de depósito/produto/filial/solicitante/aprovador são vinculadas por tenant no serviço e no banco. Novos privilégios somente no provisionamento de nova empresa; não inferir administrador antigo por combinações de permissões.

UiTestServer fica em src/test, usa H2 em memória e endereço 127.0.0.1:18081, contém somente credenciais/dados de teste e não entra no JAR de produção. Nunca utilizar esse harness como ambiente de clientes.




Escopo por filial implementado na API: usuário global ou lista explícita; consulta atual no banco, sem confiar em cabeçalhos/seleção do cliente. Filtra filiais/depósitos/perdas e seus totais; exige autorização para saldos/ledger/movimentos e ambos os depósitos de transferência, inclusive revisões de perdas. Retorna 404 para recurso não autorizado. Administração de usuários/perfis/auditoria/criação de filial exige escopo global. Gestor de usuários não pode ter escopo restrito; concessões via perfil/edição direta respeitam a regra. Catálogo é compartilhado por empresa. Migração preserva acesso global existente, que deve ser restringido explicitamente. RLS e cancelamento de operações já em curso não implementados.


Fornecedores: supplier:read/write são separados, com dados compartilhados por tenant e FKs/índices próprios. Sem consultas cadastrais oficiais nem dados bancários estruturados nesta etapa. Alterações usam versão, lock e auditoria; desativação preserva registro. Novos privilégios só entram no provisionamento de novas empresas, sem elevar usuários existentes. Pedidos futuros precisam verificar escopo de filial, pois cadastro compartilhado de fornecedor não autoriza movimentações em filiais restritas.


Compras: purchase:read consulta preços negociados e documentos de sua filial; write e receive exigem read. Recebimento validado pelo pedido permite entrada física sem conceder inventory:write para ajustes manuais. Pedido/depósito/itens são vinculados por tenant e branch no serviço; histórico e totais são filtrados pelo escopo. Namespace separado impede operação manual mascarada de recebimento. Preços/custo médio não são equivalentes: preços de pedido são visíveis via purchase:read, custo médio do estoque continua com inventory:cost. Nenhuma obrigação financeira/fiscal é gerada nesta etapa.


Contas a pagar: payable:read/write/pay/reverse separados; filtros/recursos/histórico obedecem tenant e filial. Pagamento de outro título/filial e ID inexistente têm o mesmo 404. requestIds e referências únicas; lock do título impede saldo pago acima do valor; reversão preserva original e audita ator/instante. Vínculo de compra exige mesma filial/fornecedor. Registros não acionam banco/provedor e não possuem conta de tesouraria nesta etapa. Datas seguem zona financeira do servidor; não há conciliação automática ou confirmação externa.


## Tesouraria

Leitura/cadastro/movimentação têm permissões explícitas, com escopo das duas filiais para transferências e filtragem antes da paginação. Usuários existentes não ganham permissões por migração. Não há credenciais bancárias nem execução de transferências externas. Ledger não possui endpoints de alteração/exclusão.

## Pagamentos com tesouraria (V12)

Pagamento/estorno vinculado exige treasury:read/move consultados após o lock e acesso à conta, além das permissões de contas a pagar. Conta de outro tenant ou filial sem acesso não pode ser utilizada. Reenvio verifica novamente acesso; vínculos de pagamento não podem ser alterados por retry. Movimentos de pagamento não são reutilizáveis como comandos manuais: fingerprints têm namespaces distintos.

## Clientes e recebíveis

Clientes e dados pessoais exigem permissão e tenant; audit não grava conteúdo de contato. Recebíveis/históricos/listas/totais são filtrados por filial. Créditos/estornos vinculados requerem treasury:read/move consultados na transação e acesso à conta. Permissões novas somente no provisionamento; usuários antigos exigem concessão explícita. Novos DTOs aceitam 100 permissões, mas desconhecidas ou acima dos privilégios do gestor continuam rejeitadas.

## Fluxo de caixa

API exige cashflow:read e leituras de tesouraria/AP/AR. Tenant e filial autorizada filtram contas, lançamentos, pendentes, vencidos e registros sem vínculo antes de somar. Filtro explícito de filial sem acesso retorna 404. Sem nomes/IDs da contraparte de uma transferência fora do escopo. Nova permissão apenas em provisionamento futuro; conjunto atual 32, whitelist e limite de gestor preservados.

## Conciliação de extratos

Consulta exige reconciliation:read/treasury:read; importação e revisão têm privilégios separados. Escopo da conta é verificado em lotes, linhas, sugestões, histórico e totais. Match verifica tenant/conta/valor/sinal/vínculo disponível; versão e request UUID protegem reenvios e concorrência. CSV não é executado e não gera fórmulas/HTML; parser rejeita estrutura inválida, até 500 linhas/1 MiB. Sem armazenamento de arquivo bruto, credenciais bancárias ou grants por migration. Conjunto atual tem 35 permissões.

## Caixa do PDV

Leitura exige pos:read/treasury:read e escopo. Abrir/movimentar/fechar são privilégios separados; operar caixa alheio exige pos:supervise junto aos demais. UUID, versão, locks do tenant e chaves únicas protegem exclusividade/races/reenvios. Reserva da conta verificada dentro da transação antes de movimentos de tesouraria/AP/AR. Não pode usar conta reservada de outro turno. Nenhuma migration concede os cinco privilégios novos; conjunto atual 40, limite/whitelist mantidos.

## Vendas com itens — 14/10/2026

V19: pos:sell é independente de ajuste manual de estoque e exige tesouraria, filial e operador/supervisor; whitelist/provisionamento novo com 41 permissões. Nenhuma elevação de usuário existente. Histórico mantém escopo e custos não aparecem nos itens da venda.

## Pagamentos e cancelamento — 15/10/2026

V20: pos:cancel separado de pos:sell/pos:supervise; exige caixa original, operador/supervisor, tesouraria e escopo. Permissões existentes não são elevadas. Conjunto novo com 42 permissões. Replays validam contas; comprovantes não podem ser usados novamente na mesma conta/meio.

## Devoluções — V21 (15/10/2026)

V21: pos:return separado de cancelamento/venda, com tesouraria, filial, operador atual ou supervisor e revalidação de contas também em replay. Nenhuma elevação de usuários existentes; whitelist/provisionamento novo tem 43 permissões.

## Descontos e suspensão — V22 (15/10/2026)

V22: pos:discount/discount-approve/discount-policy/suspend separados. Sem autoaprovação; autorização vinculada ao solicitante, filial, carrinho, política, prazo e aprovador atual. Suspensões próprias ou supervisor, sempre por filial. Whitelist/provisionamento novo com 47 permissões, sem privilégios retroativos.

## Parcelamento — V23

Prévia/criação exigem receivable:read/write; leitura do plano exige receivable:read. Verificação de tenant/filial também no replay. Lock transacional da empresa, UUID/fingerprint, referências únicas e títulos atômicos; cliente ativo apenas para criação nova. Nenhum banco, estoque ou caixa é movimentado ao gerar parcelas. Não representa autorização de crédito no PDV. Ver RECEIVABLE_PLANS.md.

## Crediário — V24

customer:credit é administração global (customer:read e todas as filiais); pos:credit é concessão operacional com permissões da venda e receivable:read/write. Whitelist/provisionamento novo com 49 permissões, sem migração de privilégios existentes. Padrão bloqueado/limite zero; exposição soma dívida de toda a empresa sem expor dados de filiais ocultas. Lock por empresa protege concessões concorrentes e mutações financeiras. Título originado por venda não pode ser cancelado isoladamente. Contrato e limitações em POS_CREDIT.md.

## Abatimento e reembolso do crediário — V25

Nova receivable:credit separa concessão de crédito de abatimento/reembolso; exige receivable:read, pos:return e permissões anteriores da tesouraria/caixa. Whitelist/provisionamento novo com 50 permissões; usuários existentes não recebem elevação. Origem do recebimento deve pertencer ao plano da venda; conta bancária vinculada usa a origem, e sem vínculo exige escolha autorizada. Comprovantes únicos entre os dois ledgers de reembolso, limites acumulados, lock por empresa e confirmação da divisão da dívida. Recibo reembolsado não aceita estorno integral. Ver POS_CREDIT_RETURNS.md.


## Preparação de pagamento — V26

Veja [POS_PENDING_CHECKOUTS](POS_PENDING_CHECKOUTS.md): preparação persistida sem dinheiro ou reserva, conclusão pela transação normal de venda e descarte auditado. Não implementa cobranças, webhooks ou recebimentos parciais antes da conclusão.
