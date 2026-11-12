# Crediário integrado ao PDV — V24 (16/10/2026)

Venda a prazo com cliente ativo, limite previamente configurado e geração de recebíveis na mesma transação do estoque. Permite entrada em dinheiro/Pix/transferência conferidos e financiamento do restante, ou venda integral a prazo. Não executa cobrança bancária, consulta a birôs, juros, multas, renegociação ou pagamento assíncrono em provedor.

## Configuração e acesso

`GET/PUT /api/v1/customers/{id}/credit-policy` exigem `customer:read`, `customer:credit` e acesso atual a todas as filiais. PUT recebe `version`, `creditLimit` e `blocked`. Padrão sem registro: limite zero, bloqueado, versão zero. Repetir o PUT com a versão original e o mesmo resultado imediatamente seguinte é seguro; conteúdo divergente com versão antiga retorna 409. Configuração é auditada; não altera a dívida existente.

Venda a prazo exige `pos:credit`, `customer:read`, `receivable:read/write`, além das permissões e escopos atuais da venda/tesouraria e operador do caixa ou supervisor. Novas empresas recebem 49 permissões no provisionamento; V24 não concede privilégios a usuários existentes. Configuração e concessão operacional são permissões separadas.

O limite usa a soma de `amount-received_amount` dos títulos OPEN/PARTIAL do cliente em toda a empresa, inclusive títulos manuais e de filiais ocultas ao operador. Qualquer título vencido antes da data financeira atual bloqueia novas concessões. Negação usa mensagem genérica e não revela exposição de outras filiais. Todas as mutações financeiras envolvidas serializam pelo lock da empresa. Reduzir limite ou bloquear cliente não altera títulos antigos nem impede seu recebimento ou cancelamento autorizado da venda.

## Conclusão da venda

`POST /api/v1/pos/sales` recebe o campo opcional:

```json
{
  "credit": {
    "customerId": "UUID do cliente",
    "amount": "6.25",
    "firstDueOn": "2026-11-16",
    "installmentCount": 2,
    "intervalMonths": 1
  }
}
```

Os demais campos da venda continuam obrigatórios. O valor a prazo deve ser positivo e não exceder o líquido dos itens. Pagamentos confirmados cobrem exatamente `total-credit.amount`; troco continua exclusivo do dinheiro. Venda 100% a prazo omite `tendered` e `payments`, sem criar pagamento ou operação de tesouraria. No caso com entrada, `tendered` e `changeAmount` descrevem apenas o valor recebido na conclusão e seu troco, sem incluir a promessa futura.

Plano automático com referência `POS-<UUID da venda>`, títulos `.001` em diante e emissão na data financeira do servidor. Calendário e divisão de centavos reutilizam V23. Plano, títulos, entrada, estoque, desconto/autorização, suspensão e auditoria pertencem à mesma transação; falha em qualquer etapa desfaz tudo. Não existe reserva separada de limite: somente a dívida efetivamente confirmada consome limite. UUID/fingerprint incluem condições do crédito; repetição devolve a venda atual sem recriar dívida ou movimentação, inclusive após recebimentos/cancelamento. Alterar condições com o mesmo UUID retorna 409.

## Histórico e recebimentos

DTO de venda inclui `credit` (cliente, nome histórico, plano, valor original e saldo pendente atual). `financialStatus` para crediário: UNPAID, PARTIALLY_PAID, PAID ou CANCELLED; V25 acrescenta SETTLED_BY_RETURN. Recebimentos/estornos dos títulos alteram a projeção financeira da venda; não são duplicados como pagamentos originais do PDV nem reabrem o caixa antigo. Os fluxos existentes de recebimento com vínculo opcional à tesouraria permanecem; sem vínculo é apenas registro financeiro, e não movimento de saldo. PAID indica registros internos, sem verificação automática em banco/provedor.

Os títulos de venda não aceitam cancelamento manual isolado, para evitar apagar dívida mantendo venda/estoque ativos. Recebimento parcial, estorno e consulta individual continuam disponíveis. A tela de vendas mostra os títulos e abre o título correspondente em Contas a receber. Nome do cliente, produtos, unidades, preços, descontos e custos históricos permanecem preservados.

## Cancelamento e limites atuais

Cancelamento integral exige a sessão original aberta e as permissões atuais, inclusive as de crediário. Todos os títulos devem estar OPEN e sem recebimentos ativos; estorne os registros antes de cancelar. A transação cancela as parcelas, devolve estoque ao custo original e reverte apenas as entradas realmente recebidas. Se uma parcela posterior ou a reversão da entrada falhar, parcelas anteriores, estoque e auditoria são desfeitos. Repetição não cancela ou devolve duas vezes.

V25 ampliou o fluxo para devoluções parciais ou integrais com abatimento de dívida e reembolso de entrada/parcelas, inclusive em novo turno da mesma filial; consulte POS_CREDIT_RETURNS.md. Também permanecem pendentes renegociação, cancelamento após turno, crédito entre empresas, alçadas por cliente/valor e consulta externa de risco.

## Interface, migração e testes

Rota lazy `/customer-credit` configura política por cliente. PDV permite escolher cliente, valor e calendário; exige prévia atual de parcelas antes de concluir. Alteração de carrinho/cliente/condições invalida a prévia. A prévia não reserva limite nem garante aprovação. Condições de crédito não são guardadas na suspensão do carrinho e são informadas novamente ao retomar. Comandos com resultado incerto preservam corpo/identificador em memória; navegação e logout ficam bloqueados até confirmação. Reload não é recuperação persistente.

V24 em Java localiza os dois checks antigos de recebido/troco no PostgreSQL/H2 e os substitui por regras que descontam o valor financiado; permite operação de tesouraria nula somente na venda integral a prazo. Acrescenta vínculos únicos/compostos ao plano/cliente, valor financiado e política por cliente. Valores legados usam crédito zero; upgrade testa preservação de dinheiro, estoque, snapshots e ausência de concessões de permissão ou dívidas artificiais.

SaleCreditTest/PostgresIT: 17 cenários de política/padrão/versionamento/escopo, crédito integral/entrada, parcelas exatas, recebimentos/estornos, idempotência, limite/dívida anterior/atraso, concorrência, rollback de estoque e financeiro, cliente ativo, permissões, cancelamento e exigência de conferência no fluxo de devolução ampliado em V25. Evidências em `test-evidence/pos-credit-*`; validação final registrada em TESTING/PROGRESS. Login real e revisão visual seguem adiados. Não é homologação comercial.

Correção de compatibilidade: PostgreSQL expõe NOT NULL como check em information_schema. A seleção de V24 exige referência simultânea a tendered e total para localizar apenas as duas regras monetárias. Upgrade verifica que tendered permanece NOT NULL. Primeira tentativa foi revertida em banco temporário; evidências do diagnóstico preservadas em pos-credit-regression-postgres-first-attempt.txt. Ensaio direcionado após correção: 18 testes H2 e 18 PostgreSQL aprovados em pos-credit-migration-fix.txt.
