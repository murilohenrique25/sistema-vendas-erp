# Fluxo de caixa por filial

Consulta de até 366 dias, com filtro opcional de filial ou consolidação das filiais autorizadas. Permissão `cashflow:read` exige também `treasury:read`, `payable:read` e `receivable:read`. O backend exige as quatro antes de consultar; a navegação verifica o mesmo conjunto. Apenas novas empresas recebem a permissão no provisionamento.

## Saldos e movimentos registrados

Fonte: treasury_account, treasury_operation e treasury_entry. Saldo atual = soma dos saldos das contas autorizadas no momento da consulta. Saldo antes do período = soma de deltas registrados antes do início. Saldo ao fim = saldo antes + entradas − saídas + transferências recebidas − transferências enviadas no período. Nenhum cálculo monetário usa ponto flutuante; agregados podem superar o limite de valor de um lançamento individual e são retornados como strings decimais.

Período inclui os dias inicial/final. Horários são agrupados no ERP_FINANCE_ZONE (padrão America/Sao_Paulo), com limite final exclusivo no início do dia seguinte. Movimentos seguem a data de registro na tesouraria, não a data financeira informada no pagamento/recebimento. Novos movimentos gravam o instante depois de adquirir o lock, evitando atribuir ao dia anterior operações que aguardaram durante a virada da data. Dados históricos não são reescritos.

Entradas/saídas externas excluem transferências com origem e destino internos. Transferências aparecem em colunas próprias; se ambas as contas estiverem no filtro, seus efeitos se compensam no saldo consolidado. Se apenas uma filial estiver incluída/autorizada, aparece só o efeito na conta dessa filial, sem exposição da contraparte. Estornos são novos movimentos e seus efeitos permanecem no extrato, inclusive em data diferente do original.

O saldo atual pode diferir do saldo ao fim de um período passado por incluir movimentos posteriores. Datas futuras refletem apenas registros existentes, não lançamentos hipotéticos. O saldo histórico é reconstruído pelas datas de registro, não é um fechamento contábil homologado.

## Títulos pendentes

Fonte: payable/receivable no estado atual OPEN/PARTIAL. Projetar somente amount − paid_amount/received_amount, distribuído pela data de vencimento dentro do período. Cancelados e liquidados são excluídos. A variação prevista de cada dia = pendente a receber − pendente a pagar. Projeção é mostrada separada dos movimentos/saldos; não há soma automática entre realizado e previsto nem promessa de cobrança/pagamento.

Títulos vencidos = pendentes com vencimento anterior ao dia financeiro atual, independentemente do intervalo selecionado. Vencimento hoje não é vencido. Indicador de vencidos pode sobrepor os pendentes do período e não deve ser somado a eles. Consultar período passado não reconstrói a posição histórica dos títulos: usa os valores que continuam pendentes hoje.

## Registros sem conta vinculada

Pagamentos/recebimentos POSTED sem conta, com paid_on/received_on no intervalo, são identificados à parte. Não compõem saldo nem movimento de tesouraria. Registros estornados não entram nesse indicador. Ele representa a posição atual dos registros, não histórico de liquidações em uma data passada. Evite registrar um débito/crédito manual para um evento que já possui vínculo de tesouraria.

Filtros de títulos usam sua filial; tesouraria usa a filial da conta. Uma conta central em outra filial pode receber/pagar um título; por isso fluxo realizado e previsão por filial não são necessariamente atribuídos à mesma filial. Consolidar ambas as filiais permite acompanhar a relação.

## Consistência e limites

Consulta read-only em REPEATABLE_READ para manter agregados de contas/extrato/títulos na mesma visão do banco. Tenant vem da sessão e o escopo é aplicado em toda consulta, antes de agrupar/somar. Filial inexistente ou sem acesso retorna 404. Resultados incluem dias sem movimento ou vencimento, com zeros, e hora/fuso da geração. Não há cache, exportação, previsão estatística, conciliação bancária ou DRE nesta entrega.

V16 adiciona índices de data na tesouraria e registros financeiros; não modifica dados nem concede permissões. O conjunto atual tem 40 permissões, respeitando o limite estrutural de 100 e whitelist. Angular exibe valores fornecidos pelo servidor, sem somar dinheiro no navegador. Login real e revisão visual permanecem adiados pelo usuário.

Validação em CashFlowTest/PostgresIT: vazio/calendário, abertura/fechamento, limites de fuso, transferências consolidadas/por filial, liquidações parciais sem duplicação, cancelamentos/estornos, vencidos, registros sem conta, escopo de todas as fontes, isolamento/permissões e intervalos. Rotinas de AP/AR com tesouraria são revalidadas porque o instante de registro passou a ser explícito.

## Devoluções — V21 (15/10/2026)

V21: reembolso de devolução usa ledger financeiro POS_SALE_RETURN e entra nas saídas externas da conta/data de registro; não altera retrospectivamente saldo congelado do caixa original nem o movimento de recebimento original.

## Ajustes de devolução — V25

Pendentes/vencidos de recebíveis usam amount-received_amount-credited_amount. received_amount é líquido de reembolsos; valores originais/recibos brutos continuam preservados. Indicador sem tesouraria subtrai reembolsos dos registros correntes, inclusive em consultas de períodos passados, seguindo a projeção corrente já aplicada aos estornos; não representa posição histórica reconstruída. Débitos de reembolso efetivos continuam no ledger realizado, sem lançar abatimento como dinheiro.
