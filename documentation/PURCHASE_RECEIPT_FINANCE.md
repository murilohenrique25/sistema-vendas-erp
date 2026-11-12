# Financeiro de recebimentos de compra — V27 em implementação

Implementação de 11/11/2026. Fluxo de geração de **um título a pagar por recebimento**, acionado explicitamente pelo operador após a entrada de mercadoria. Não é geração automática ao registrar estoque, parcelamento de fornecedor ou pagamento bancário. Testes deixados para a etapa final, conforme orientação do usuário. Migração preparada, ainda não aplicada ao banco.

## Valor das mercadorias

Cada linha de recebimento passa a ter `purchase_receipt_line.amount`, calculado com o custo histórico do item do pedido. O valor do recebimento é a soma dessas linhas, não o total do pedido nem um valor digitado pelo operador.

Para recebimentos parciais, o valor da linha é a diferença entre o custo acumulado arredondado após a nova quantidade e o custo acumulado arredondado antes dela, com HALF_UP em centavos. Assim, receber todo o item em várias partes fecha no mesmo total monetário do item pedido. O valor atribuído a um recebimento não muda quando chegar outra parte. Uma parte pode valer zero centavos; recebimento com total zero não gera título. Não há frete, imposto ou despesa adicional ausente do custo registrado.

A V27 acrescenta o valor da linha e atribui valores aos recebimentos anteriores por ordem estável de data/identidade, acumulando quantidades por empresa/item do pedido. Empates de horário antigos são resolvidos pela identidade; a distribuição dos centavos pode diferir de uma ordem operacional que não foi registrada. A migração não altera títulos antigos, custos, quantidades ou pagamentos. Depois da migração, novos recebimentos usam a quantidade já recebida do item, sob o bloqueio existente do pedido. Não há recálculo de valores históricos no endpoint de geração.

## API

Base: `/api/v1/purchase-orders/{orderId}/receipts/{receiptId}/payable`.

- `GET`: exige `purchase:read` e `payable:read`; consulta recebimento do pedido e filial acessível. Retorna identidade, filial, fornecedor, `amount` como string monetária, quantidade de títulos não cancelados vinculados e título gerado, quando existir.
- `POST`: também exige `payable:write`. Corpo `{requestId,reference,document,description,issuedOn,dueOn}`. Filial, fornecedor e valor vêm da origem, sem aceitar substituição pelo cliente. Usa o contrato de datas/descrição da conta a pagar existente. Retorna 201 com o resumo atualizado.

Títulos e vínculo são criados na mesma transação. A tabela `purchase_receipt_finance` guarda uma geração por recebimento, chave/fingerprint e vínculo único com o título. O bloqueio da empresa serializa geração e cadastro manual de títulos. Reenvio da mesma chave/conteúdo retorna o estado atual; chave reutilizada com outros dados resulta em 409. O título pode já estar pago ou cancelado no reenvio; não é recriado.

Se houver título manual não cancelado associado ao recebimento, a geração é bloqueada para reconciliação. Se já houve geração por esse fluxo, uma nova geração é bloqueada mesmo se o título tiver sido cancelado. Cadastro manual de outro título vinculado à mesma origem também é bloqueado após a geração; o reenvio legítimo de um título anterior continua no caminho de idempotência existente.

Essas regras não detectam títulos manuais sem vínculo com a origem, notas duplicadas ou obrigações externas ao sistema. Títulos cancelados anteriores ao primeiro uso do fluxo não entram na contagem de títulos ativos. Antes de gerar após um cancelamento manual, o operador precisa reconciliar a obrigação real. Não há exclusão ou alteração automática de registros legados.

Pagamento, estorno e cancelamento do título continuam nas rotinas existentes de contas a pagar. Cancelar o título não desfaz mercadorias recebidas nem libera uma segunda geração. Cancelar o saldo pendente de um pedido não apaga seus recebimentos anteriores. Compensação, regeneração controlada, parcelamento e financeiro de devolução de compra são próximos desenvolvimentos.

## Interface

No histórico de recebimentos do pedido, **Consultar / gerar financeiro deste recebimento** abre o resumo financeiro. A tela sugere uma referência baseada na identidade do recebimento, consulta a data financeira da API e solicita documento, descrição, emissão e vencimento. O valor é somente leitura. Usuário com permissão apenas de leitura pode consultar, sem gerar.

Depois de gerar, mostra título, vencimento e saldo a pagar, com link para abri-lo em Contas a pagar. Valores das linhas recebidas também aparecem no histórico de compras. Falhas de comunicação/5xx preservam o corpo/UUID para repetir o mesmo comando e bloqueiam novas mutações, saída e logout enquanto o resultado estiver incerto. A recuperação é em memória; não persiste após encerrar o navegador.

## Validação adiada e implantação

Somente compilação/JAR com `-Dmaven.test.skip=true` e build Angular nesta etapa. Nenhum teste escrito ou executado, banco acessado, container iniciado/atualizado, backup ensaiado ou implantação realizada. V27 ainda precisa de execução e validação em banco antes de aplicar aos serviços. Login real e revisão visual continuam adiados.

Na etapa final, validar: upgrade V26→V27 em H2/PostgreSQL, valores acumulados/empates/itens gratuitos/custos fracionários e grandes volumes; recebimento parcial concorrente; isolamento por empresa/filial/permissão; geração única/reenvio simultâneo/conflito de chave; título manual ativo/pago/cancelado e sem vínculo; falha de criação/auditoria/vínculo com rollback integral; cancelamento posterior e proteção contra regeneração; não movimentação de tesouraria/estoque pela geração; filtros/fluxo de caixa e navegação para título; incerteza de rede e revogação durante reenvio.

Evidências de compilação em `test-evidence/purchase-finance-backend-build.txt` e `purchase-finance-frontend-build.txt`. Compilar não demonstra funcionamento do SQL, migração ou transações.
