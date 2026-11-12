# Tesouraria interna

Contas de caixa (`CASH`) e banco (`BANK`) pertencem à empresa e a uma filial. O código é único por empresa, normalizado para maiúsculas. O saldo começa em zero. Crédito externo (inclusive saldo inicial), débito externo e transferência entre contas são registros de movimentos já realizados, sem chamada a banco ou provedor.

Cada comando recebe UUID do cliente. Repetição com o mesmo conteúdo retorna o resultado original; reutilização com conteúdo diferente gera 409. Valor positivo em decimal, até 17 dígitos inteiros e dois decimais, sem conversão para ponto flutuante. Não há cheque especial: saldo negativo é rejeitado, assim como excesso do limite monetário.

Transferência exige acesso às duas filiais. Operação, dois lançamentos, atualização de saldos e auditoria são uma transação. A API não permite alterar/apagar lançamentos; correções podem ser registradas como novos movimentos justificados. Movimentos manuais não têm vínculo automático de estorno; pagamentos vinculados de títulos recebem estorno associado. Edição/desativação de conta permanece pendente.

As escritas são serializadas pelo lock da empresa, inclusive comandos simultâneos em contas diferentes, evitando UUID duplicado e interbloqueio em transferências opostas. Isso limita concorrência por empresa e deve ser medido antes de operação de alto volume. O extrato usa sequência crescente por conta e é consultado do mais recente ao mais antigo, independentemente do instante de início da transação.

Permissões: `treasury:read`, `treasury:write` para cadastro e `treasury:move` para lançamentos. Toda escrita também exige leitura. Novas empresas recebem as três no provisionamento; usuários existentes exigem concessão explícita. Listas e totais são filtrados pelo acesso atual às filiais.

Frontend: rota `/treasury`, seleção paginada de contas e filiais, saldo e extrato paginado. Em falha de rede/5xx, o comando é mantido em memória com seu UUID, os formulários e saída ficam bloqueados e a repetição confirma o mesmo resultado. Recarregar/fechar a página pode perder esse comando; ainda não existe outbox persistente.

Ainda pendentes: conciliação bancária, importação de extratos, datas de competência, categorias financeiras, fechamento de caixa, integração bancária, fluxo de caixa projetado e DRE. Contas a pagar permitem selecionar uma conta: pagamento e débito, ou estorno e crédito, são atômicos. Pagamentos sem conta continuam independentes. Evite registrar manualmente um débito já gerado pelo pagamento vinculado.

Validação automatizada em `TreasuryTest`/`TreasuryPostgresIT`: cadastro/reenvio, transferências atômicas, extrato, limites, tenant/permissões/filiais e concorrência de débito/reenvio. Login real e revisão visual com dados do usuário seguem adiados por solicitação.

## V12 — Pagamentos de títulos

treasuryAccountId opcional vincula o pagamento à conta de origem. A conta pode ser de outra filial autorizada, permitindo tesouraria centralizada. Exige treasury:read/move além das permissões de contas a pagar. Pagamentos antigos mantêm vínculos nulos; não há migração automática de saldos nem associação retroativa. Estorno vinculado exige acesso à conta original e registra crédito com origem PAYABLE_REVERSAL; débito tem origem PAYABLE_PAYMENT. Sem execução bancária. Pagamentos e estornos bloqueiam tenant antes do título, seguindo a ordem da tesouraria e impedindo disputa de saldo entre títulos/movimentos manuais. Vínculos e limite de saldo são verificados na transação; se qualquer passo falha, título/pagamento/extrato/auditorias são desfeitos juntos.

## Vendas com itens — 14/10/2026

V19: recebimento de venda em dinheiro possui origem POS_SALE e vínculo à venda; crédito líquido é total, com dinheiro recebido/troco preservados na venda. Acesso ao caixa reservado ocorre apenas após validar sessão e operador na transação. Venda incrementa versão do caixa.

## Pagamentos e cancelamento — 15/10/2026

V20: cada parcela de venda credita a conta correspondente, somente dinheiro afeta gaveta. Cancelamento debita valores aplicados originais com origem POS_SALE_REVERSAL; saldo insuficiente desfaz todos os retornos. Pix/transferência são registros conferidos externamente, sem chamada bancária.

## Devoluções — V21 (15/10/2026)

V21: POS_SALE_RETURN debita valores aplicados dos pagamentos originais. Dinheiro sai do caixa atual aberto da mesma filial; Pix/transferência usam conta original com comprovante externo confirmado. Saldos insuficientes e excesso de reembolso rejeitam toda a transação. Originais permanecem preservados.
