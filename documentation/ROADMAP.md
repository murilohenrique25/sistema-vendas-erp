# Roadmap integral

Atualização de 11/11/2026: a sequência 1–7 tem código e interface compilados, ainda sem banco/testes/implantação. Consulte [IMPLEMENTATION_SEQUENCE](IMPLEMENTATION_SEQUENCE.md) para escopo e limites. Migrações V27–V36 preparadas, não aplicadas. A matriz histórica abaixo deve ser lida junto dessa atualização.

Todos os itens de REQUIREMENTS permanecem obrigatórios. A matriz abaixo é organização de entrega, não redução do produto.

| Fase | Estado atual | Próximo resultado |
|---|---|---|
| 1 Fundação | EM DESENVOLVIMENTO | cadastro/lista/ativação e edição de permissões de usuários implementados; perfis reutilizáveis por aplicação explícita implementados; escopos de filial para módulos atuais implementados; faltam MFA, recuperação, identificação de dispositivos/revogação individual (consulta e encerramento de outros acessos implementados), RLS e homologação PostgreSQL |
| 2 Produtos e estoque | EM DESENVOLVIMENTO | catálogo/códigos, depósitos, saldo/ledger/custo médio, transferência imediata e perda com aprovação implementados; expandir cadastro/preços/conversões, inventário, reservas/lotes/validade/alçadas/estornos |
| 3 Compras | EM DESENVOLVIMENTO | fornecedores/pedidos/recebimento parcial integrado ao estoque implementados; faltam cotação/alçadas/frete/impostos/XML idempotente/devolução/geração financeira automática |
| 4 PDV | EM DESENVOLVIMENTO | terminais/caixas e vendas em dinheiro com itens, preços históricos e estoque implementados; divisão em dinheiro/Pix/transferência conferidos e cancelamento integral e devoluções parciais/em outro turno implementados; desconto por item, política percentual/aprovação de outro usuário e suspensão/retomada implementados; crediário com entrada opcional, limite e recebíveis automáticos implementado; devoluções do crediário com abatimento/reembolso implementadas; faltam provedores/cartões, promoções/alçadas por valor/categoria, políticas/trocas e fluxos entre filiais, fiscal e hardware |
| 5 Pagamentos | EM DESENVOLVIMENTO | parcelas conferidas em dinheiro/Pix/transferência, troco, cancelamento e reembolsos parciais implementados; saldo de crediário derivado dos títulos implementado; preparação pendente sem recebimento implementada em V26; faltam estados de provedor e pagamentos parciais antes da conclusão, provedores/cartões/parcelamento/taxas e conciliação automática |
| 6 Fiscal | NÃO INICIADO | FiscalProvider externo, NFC-e/NF-e, credenciais seguras, XML/DANFE/eventos e homologação |
| 7 Financeiro | EM DESENVOLVIMENTO | contas a pagar, registro de pagamentos parciais e estorno de registro e tesouraria interna implementados; vínculo opcional título/tesouraria implementado; contas a receber manuais implementadas; fluxo registrado e projeção de pendentes implementados; conciliação manual/CSV implementada; geração manual de parcelas em lote implementada; geração automática por venda a prazo e histórico de abatimentos/reembolsos implementados; faltam geração automática por compra, renegociação, integração bancária/conciliação agrupada/DRE |
| 8 Comercial | EM DESENVOLVIMENTO | cadastro de clientes e política de limite/bloqueio de crédito implementados; faltam histórico detalhado/alçadas de crédito, CRM, vendedores/comissões, promoções, fidelidade, pedidos, trocas e políticas de devolução |
| 9 BI | NÃO INICIADO | KPIs com fórmula/fonte/filtros, relatórios/exportações/agendamento |
| 10 Offline/hardware | NÃO INICIADO | PWA/agente, outbox local, conflitos, impressão/balanças/TEF reais |
| 11 SaaS | NÃO INICIADO | planos/assinaturas/cobrança/limites/painel/onboarding |
| 12 IA | NÃO INICIADO | consultas autorizadas, previsão, avaliações, limites e custos |
| 13 Preparação comercial | NÃO INICIADO | E2E/carga/segurança, fiscal/pagamentos/equipamentos, backup/restore, piloto e treinamento |

Produção/kits, aplicações móveis, notificações/automações, LGPD/portabilidade, suporte e migração acompanham as fases relacionadas. Antecipar requisitos offline/hardware que forem necessários ao piloto. Prioridade funcional após fundação: estoque rastreável, perdas e pagamentos mistos.

Revisão de lacunas: política de encerramento de tenant, portabilidade, descarte seguro, acessibilidade em teclado, RPO/RTO com cliente, política de atualização de terminal, limites de operação offline e preservação fiscal devem ter critérios de aceite explícitos antes do piloto.










## Preparação de pagamento — V26

Veja [POS_PENDING_CHECKOUTS](POS_PENDING_CHECKOUTS.md): preparação persistida sem dinheiro ou reserva, conclusão pela transação normal de venda e descarte auditado. Não implementa cobranças, webhooks ou recebimentos parciais antes da conclusão.

## Consolidação de pendências — 11/11/2026

Lista consolidada de validações/aplicações pendentes e desenvolvimento restante em [PENDING_WORK](PENDING_WORK.md). Considera edição de catálogo, consultas financeiras e geração/parcelamento por recebimento implementados no código, sem tratar esses itens como já homologados ou implantados. O fluxo financeiro de compras atual é explícito após receber; geração dentro da confirmação do recebimento ainda falta. V27/V28 permanecem não aplicadas. Escopo integral de REQUIREMENTS preservado.
