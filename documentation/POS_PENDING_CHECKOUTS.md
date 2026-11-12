# Preparação de pagamento — V26

Primeira etapa do fluxo de pagamentos pendentes: carrinho persistido, ainda sem recebimento. Não é uma cobrança Pix, autorização de cartão ou confirmação de provedor. Não há reserva de estoque, lançamento na tesouraria, título financeiro ou alteração da versão do caixa ao preparar ou descartar.

## API

Base `/api/v1/pos/pending-checkouts`. Todas as rotas exigem `pos:read`, `pos:sell` e `treasury:read`; a conclusão também exige `treasury:move`, além das verificações da venda normal.

- `POST /`: `{requestId, sessionId, items}` usando o contrato de itens da venda. Exige sessão aberta do operador ou supervisor, terminal ativo com depósito acessível e preços atuais. Retorna 201 com identidade gerada pelo servidor, itens históricos, total, estado `PENDING` e versão zero. Reenvio da mesma chave e conteúdo retorna o registro atual; conteúdo diferente resulta em 409.
- `GET /?page=0&size=20`: paginação no banco, com escopo da filial e criador/supervisor aplicado antes de contar ou paginar. Inclui histórico concluído/descartado.
- `GET /{id}`: consulta individual com o mesmo isolamento.
- `POST /{id}/discard`: `{requestId, version, note}`. Justificativa de 5 a 500 caracteres. Descarte versionado e idempotente, possível após fechar o caixa. Não executa reembolso, pois a preparação não recebeu dinheiro.
- `POST /{id}/complete`: `{version, sale}`; `sale` segue o contrato de conclusão existente, com `requestId` igual à identidade da preparação e `sessionId` igual ao original. Itens, quantidade, preço, desconto e descrição/unidade atuais devem corresponder ao snapshot. Pagamentos devem estar integralmente conferidos pelo operador. Não aceita crediário ou suspensão vinculada nesta etapa. Aprovação de desconto continua sujeita às regras e à validade originais.

A conclusão utiliza a transação existente de venda: revalida sessão, filial, depósito, preços, alçadas, saldo de estoque, contas e comprovantes; movimenta estoque e tesouraria; vincula a venda e incrementa a versão da preparação. Falha em qualquer parte mantém a preparação pendente e desfaz os efeitos da venda. O bloqueio por empresa serializa conclusão, descarte e demais operações financeiras. Conclusão ou descarte vence uma disputa concorrente, nunca ambos. Reenvio de uma conclusão já aplicada usa a impressão digital original da venda, inclusive após fechamento do caixa ou mudança no cadastro.

O endpoint normal de venda permanece disponível. A preparação é um fluxo operacional opcional, sem dinheiro em custódia. Não interprete uma preparação pendente como recebível, obrigação de reembolso, garantia de preço ou de disponibilidade.

## Interface

A página de vendas permite preparar o carrinho, paginar o histórico, selecionar uma preparação para conferir os recebimentos e descartar com justificativa. A seleção usa o caixa original, que precisa estar aberto e disponível na lista de sessões para concluir. Se os itens/preços mudarem, descarte e prepare novamente. Preparação de pagamento e suspensão de carrinho possuem identidades e finalidades independentes.

Comando com resultado incerto conserva corpo e UUID em memória para repetição, bloqueando novas mutações, navegação e logout conforme a proteção existente do PDV. Isso não substitui recuperação de comandos após encerramento forçado do navegador. Revisão visual e login real continuam adiados.

## Ainda pendente

Integração real com provedores, criação de cobrança, callbacks autenticados, consulta de status, pagamentos parciais antes da conclusão, expiração automática, reservas, recebimentos tardios, compensação/reembolso e transferência de preparação entre turnos. Não há webhook público ou confirmação automática nesta versão. Essas etapas exigem um ledger próprio antes de receber valores para uma venda ainda não concluída.

## Validação

`PendingCheckoutTest` e sua repetição `PendingCheckoutPostgresIT` verificam ausência de efeitos ao preparar, reenvio, conflito de conteúdo/versão/carrinho, conclusão única, falha de pagamento/estoque, mudança de preço, descarte, disputa conclusão/descarte, isolamento por empresa/filial, revogação de permissão, validação e replay após fechar o caixa. Evidências em `test-evidence/pending-checkout-*.txt`.

Rodada aprovada: 67 H2 e 12 PostgreSQL, sem falhas/erros/skips; build Angular aprovado (477,36 kB iniciais). V26 aplicada localmente, serviços saudáveis e backup/restauração verificados, sem dados de teste permanentes. Não houve nova regressão global ou validação visual.
