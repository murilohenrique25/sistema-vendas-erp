# Contas a pagar — marco inicial

Títulos por empresa/filial/fornecedor, com referência interna única, documento informado, descrição, emissão, vencimento e valor nominal em reais. Fornecedor é copiado pelo nome no lançamento; mudança posterior no cadastro não altera o título. Fornecedor inativo pode ter obrigações existentes; lançamento financeiro não reativa nem autoriza novas compras.

Criação usa requestId persistido/fingerprint e lock do tenant. Mesmo conteúdo com escala monetária equivalente retorna o título existente no estado atual. Mesmo requestId com outros dados retorna 409; referência interna é normalizada em maiúsculas e única por tenant. Documento do fornecedor não é chave fiscal validada e pode se repetir em títulos/parcelas informados manualmente.

## Vínculo a compras

purchaseReceiptId é opcional. Quando informado, o recebimento deve existir na mesma empresa e corresponder à filial e ao fornecedor do título. Tela Compras oferece lançamento a partir do histórico de recebimento, preenchendo esse vínculo, filial e fornecedor. Valor, emissão, vencimento e referência são informados pelo usuário: custo negociado de compra não é automaticamente uma nota ou obrigação financeira. Múltiplos títulos podem apontar ao mesmo recebimento; geração de parcelas e conciliação com valor fiscal ainda pendentes.

## Valores e estados

Valor positivo, até 17 dígitos inteiros/duas casas, NUMERIC(19,2) e BigDecimal. API envia valores como strings e Angular não calcula dinheiro com ponto flutuante. Emissão não pode ser posterior ao vencimento. Valor/data/fornecedor/filial do título não têm edição nesta etapa; correção exige cancelamento permitido e novo lançamento com referência própria.

OPEN: nenhum valor pago. PARTIAL: valor pago positivo inferior ao título. PAID: soma paga igual ao valor. CANCELLED: registro encerrado sem valor pago. Valor pago nunca negativo ou superior ao título; checks do banco protegem combinações de estado/valor. Pendente = valor − pago para títulos válidos; cancelado mostra pendente zero e preserva valor original. Vencido = OPEN/PARTIAL com dueOn anterior à data financeira de hoje. O dia do vencimento não é marcado vencido.

Datas financeiras usam erp.finance-zone, exposto no Compose por ERP_FINANCE_ZONE, padrão America/Sao_Paulo. /calendar informa data/fuso do servidor. Datas são LocalDate, sem deslocamento por timezone no armazenamento; createdAt/reversedAt guardam instante UTC/offset. Não há calendário de feriados, ajuste bancário ou juros automáticos.

## Pagamento realizado informado

Registro de pagamento exige paymentId, valor positivo, data entre emissão e hoje, meio declarado (CASH/BANK_TRANSFER/PIX/OTHER), referência/comprovante informado e justificativa. É registro de pagamento já realizado pelo usuário, não uma ordem bancária ou confirmação de provedor. Referência/comprovante não é validado externamente nem deduplicado como transação bancária.

Empresa é bloqueada antes do título para verificar saldo e inserir pagamento; atualização de pago/status/version e auditoria são atômicas. Pagamento nunca ultrapassa pendente. paymentId é único por tenant, com fingerprint de título/valor/data/meio/referência/nota. Reenvio idêntico retorna o registro, sem nova baixa; mesma chave para outro título/conteúdo retorna 409. Diferentes chaves para a mesma transação externa não são conciliadas nesta etapa.

## Estorno de registro e cancelamento

Estorno preserva valor/data/ator/nota originais, grava requestId/nota/ator/instante de estorno, marca REVERSED e subtrai o valor do pago do título na mesma transação. Reenvio da mesma reversão retorna o registro; outra chave para pagamento já estornado ou chave de estorno reutilizada em outro pagamento retorna 409. Retry do paymentId original após estorno retorna REVERSED e não reposta dinheiro. Não há desestorno; novo pagamento usa nova identidade.

É correção do registro financeiro; não devolve dinheiro nem executa refund bancário. Original continua no histórico. Somente OPEN com pago zero pode ser cancelado, com versão e justificativa. Se há pagamento vigente, estorno autorizado deve ocorrer antes. Cancelamento conserva pagamentos já estornados e auditoria; não estorna recebimento de compra/estoque nem envia cancelamento fiscal.

## Segurança e interface

payable:read consulta valores/histórico da filial autorizada. write cria/cancela; pay registra pagamento; reverse estorna registro, sempre junto com read. Operações exigem tenant e filial; listas/totais são filtrados antes da paginação. Pagamento de outro título/filial retorna o mesmo 404 de pagamento inexistente. Novos privilégios apenas no provisionamento de novas empresas; nenhuma migration eleva usuários antigos. Seletores da interface usam branch:read e supplier:read.

Comandos incertos em rede/5xx ficam em memória com chave/payload originais, bloqueando edição, navegação e logout para retry seguro. beforeunload alerta; não é outbox durável. 4xx libera a edição. Histórico de pagamentos é paginado; não há endpoint para apagar pagamentos ou alterar valor depois de registrado.

Pendente: integração bancária/Pix real, aprovação/alçadas, lote de pagamentos, comprovantes anexos, conciliação de extrato, parcelas automáticas, juros/multas/descontos, moeda/câmbio, importação fiscal, contas a receber, fluxo de caixa, DRE e homologação operacional. Nenhuma transferência monetária ou alteração de saldo bancário é executada por este módulo.

## V12 — Conta de origem opcional

Pagamento aceita treasuryAccountId. O vínculo gera débito na conta e grava treasuryOperationId no pagamento. O estorno gera crédito na mesma conta e grava treasuryReversalOperationId, preservando ambos. Reenvios retornam os mesmos vínculos; conta não pode ser trocada ou removida no replay. Conta de outra filial é aceita com acesso às duas filiais. Pagamento/estorno vinculado exige treasury:read/move; sem conta, as permissões antigas continuam suficientes. Limites monetários e saldo também valem ao estornar; se o crédito ultrapassar o limite, todo o estorno é rejeitado. Pagamentos anteriores mantêm seu fingerprint/vínculos nulos, continuam reexecutáveis e não movimentam a tesouraria retroativamente. Na tela, escolher uma conta é explícito; o padrão permanece somente registro. Não lançar também um débito manual para o mesmo pagamento vinculado.
