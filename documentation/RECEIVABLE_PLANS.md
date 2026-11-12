# Parcelamento de contas a receber — V23

Geração manual e atômica de 1 a 120 títulos de um mesmo cliente/filial. É uma base financeira para o crediário; ainda não cria venda a prazo no PDV, não autoriza limite de crédito e não cobra juros, multas ou bancos automaticamente.

## API e acesso

- `POST /api/v1/receivables/plans/preview`: calcula sem gravar.
- `POST /api/v1/receivables/plans`: grava plano, títulos, vínculos e auditoria na mesma transação; HTTP 201.
- `GET /api/v1/receivables/plans/{id}`: consulta títulos e saldos atuais em ordem de parcela.

Prévia/criação exigem `receivable:read` e `receivable:write`; consulta exige leitura. Empresa e escopo de filial são verificados também no replay. Cliente ativo é exigido na criação; títulos preservam o nome histórico. Nenhuma permissão nova ou usuário permanente é criado.

Entrada: `requestId`, `branchId`, `customerId`, `reference` (1–50 caracteres alfanuméricos, ponto, hífen ou sublinhado), `document`, `description`, `issuedOn`, `firstDueOn`, `amount` (decimal de até 17 inteiros e 2 casas), `installmentCount` (1–120), `intervalMonths` (1–12).

## Valores e vencimentos

Total em centavos inteiros dividido pela quantidade. Centavos restantes são atribuídos às primeiras parcelas. Cada parcela deve ter pelo menos R$ 0,01. R$ 100,00 em três parcelas gera R$ 33,34, R$ 33,33 e R$ 33,33. Não há ponto flutuante no cálculo financeiro.

Vencimentos usam sempre a data inicial mais N intervalos mensais, evitando deriva após fevereiro. Primeiro vencimento 31/01/2020 gera 29/02/2020 e 31/03/2020. O primeiro vencimento não pode anteceder a emissão; anos devem permanecer entre 1 e 9999. Não há ajuste automático de feriados ou dias úteis.

Referência normalizada em maiúsculas, única por empresa no plano; títulos recebem `.001` a `.120` e participam da unicidade já existente de contas a receber. Um título manual com a mesma referência provoca conflito e rollback de todo o plano, inclusive títulos anteriores e auditoria. Nome/documento/descrição mantêm as regras dos títulos existentes.

## Integridade e idempotência

V23 acrescenta `receivable_plan` e `receivable_installment`, com FKs compostas por empresa. Não altera valores, histórico, permissões ou registros anteriores. Criação usa o lock da empresa adotado pelo financeiro e fingerprint de todos os campos normalizados. UUID repetido com o mesmo conteúdo retorna o plano e o estado atual dos títulos; conteúdo diferente retorna 409. Parcelas recebidas ou canceladas não são recriadas por repetição. Colisões na última parcela desfazem toda a transação.

Cada parcela usa o fluxo existente de recebimento parcial, estorno e cancelamento individual. Não há recebimento/cancelamento em lote, renegociação ou substituição automática do plano. O valor original do plano permanece histórico, inclusive após cancelamento individual. Criação não movimenta tesouraria, estoque, caixa ou pagamentos.

## Interface e validação

Contas a receber aceita quantidade e intervalo mensal. Acima de uma parcela, a prévia do conteúdo atual é obrigatória antes da criação; qualquer alteração em cliente, dados ou condições invalida a prévia. Comando enviado mantém UUID e corpo em memória para repetição de resultado incerto, com bloqueio de navegação e saída da conta. Reload do navegador não é recuperação persistente de comandos. Tela carregada sob demanda.

`ReceivablePlanTest` acrescenta dez cenários e reutiliza os treze cenários financeiros de `ReceivableTest`; a variante PostgreSQL executa o mesmo contrato. Há cobertura de centavos/calendário, consulta sem escrita, criação/replay após baixa, rollback tardio, colisão de referência, concorrência, escopos, permissões, cliente ativo, entradas inválidas e replay após cancelamento.
