# Revisão final da venda — 11/11/2026

A conclusão do PDV agora passa por uma revisão local. Depois de montar um carrinho válido, o operador escolhe **Revisar venda antes de concluir**. A tela apresenta caixa/operador, itens com quantidade, preço, desconto e líquido, total bruto/líquido, entrada, valor a prazo, recebido informado e troco. Nos pagamentos divididos, mostra conta e referência. No crediário, mostra cliente e calendário de parcelas conferido.

**Confirmar venda e baixar estoque** envia os dados revisados pela mesma API de venda ou de conclusão de preparação existente. **Voltar ao carrinho** fecha a revisão sem receber valores, salvar venda ou movimentar estoque.

## Coerência da revisão

`sale-review.ts` cria uma cópia JSON independente e congelada recursivamente. O resumo e o comando são capturados juntos; a identidade da requisição é gerada ao revisar, ou corresponde à identidade da preparação quando esse fluxo estiver selecionado. Não há armazenamento no browser, envio de dados ao revisar ou garantia financeira produzida por essa cópia.

Antes de confirmar, a interface verifica novamente a validade do formulário e compara os dados atuais com a cópia revisada. Caixa/versão, itens, quantidade, preço, desconto, pagamentos, contas, referências, confirmações, aprovação, suspensão, preparação/versão, cliente, condições e calendário do crediário precisam continuar correspondentes. Se os dados forem diferentes ou inválidos, a revisão não pode ser confirmada; a tela pede correção e nova revisão. A comparação é local e não consulta dados atuais no servidor.

Ao confirmar, o comando pendente usa o corpo congelado revisado. Falhas de comunicação/5xx preservam corpo e UUID para a repetição existente, com bloqueio de novas mutações e saída. Erros 4xx retiram a revisão da venda para permitir correção e nova conferência. A conclusão bem-sucedida limpa o carrinho e a revisão. Não há recuperação persistente de comandos após encerramento forçado do browser.

As verificações transacionais da API continuam obrigatórias: autorização, caixa aberto, preços, desconto, crédito, contas, unicidade de comprovantes e estoque. A revisão não consulta ou confirma Pix/transferência em provedor, não emite documento fiscal e não reserva mercadoria. Um caixa pode mudar no servidor depois da revisão e a conclusão pode ser recusada.

## Validação desta etapa

54 testes Node aprovados: 30 de cálculo/composição de pagamentos, 12 de contexto do caixa e 12 novos de cópia imutável e invalidação da revisão. Os novos testes cobrem mudança de caixa/versão, quantidade, desconto, conta, comprovante, confirmação, condições do crediário e versão da preparação, além de identidade do comando e valores exatos preservados.

Build Angular de produção aprovado: bundle inicial 477,62 kB; módulo lazy de vendas 96,38 kB. Evidências em `test-evidence/sale-review-unit.txt`, `sale-review-frontend.txt` e `sale-review-offline.json`.

Os testes de dados e a compilação não substituem testes de interação no browser ou regressão transacional. Não houve alteração de backend/schema, acesso ao banco, inicialização/atualização Docker ou implantação. Login real e revisão visual continuam adiados.
