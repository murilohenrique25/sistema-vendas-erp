# Conferência de pagamentos — 11/11/2026

O PDV agora apresenta uma conferência local com valor aplicado, recebido informado, troco em dinheiro, saldo a aplicar e excesso aplicado. A conferência é calculada em centavos inteiros, sem converter dinheiro para ponto flutuante. Na venda com crediário, considera somente a entrada, descontando o valor financiado do total. O crediário integral continua sem recebimento inicial.

A conclusão fica bloqueada quando as parcelas não cobrem exatamente a entrada, quando dinheiro recebido é insuficiente, quando há troco bancário, mais de uma parcela em dinheiro, ausência de conta/comprovante/confirmação bancária, comprovante repetido na mesma conta e meio ou valores fora do limite. O excesso de valor aplicado não se transforma automaticamente em troco: ajuste a parcela em dinheiro para o líquido devido e informe o valor efetivamente entregue como recebido.

`PaymentAllocation` centraliza o cálculo monetário utilizado por `SalePayments.prepare` no backend. A calculadora não acessa JDBC, não publica eventos e não recebe dinheiro. O serviço mantém a verificação de conta BANK da filial, autorização, comprovante já registrado no banco e lançamentos da tesouraria. A duplicidade na própria lista é conferida pela calculadora; a duplicidade em operações anteriores continua dependendo do banco na conclusão. Não há novo endpoint, migração ou cobrança bancária nesta entrega.

Na interface, `payment-allocation.ts` faz a conferência local sem API. Um resultado válido não garante autorização, disponibilidade de estoque, preço atual nem recebimento bancário: essas verificações seguem na transação da venda. A funcionalidade não registra pagamentos parciais anteriores à conclusão nem implementa callbacks de provedor.

## Validação sem banco

- 20 testes JUnit de cálculo aprovados, sem Spring, H2, JDBC ou Docker. Cobrem troco, divisão de pagamentos, incompletos/excesso, limites, centavos exatos, confirmação bancária, comprovantes, valores inválidos e imutabilidade.
- 15 testes Node da calculadora TypeScript aprovados, com os mesmos casos monetários essenciais.
- TypeScript `--noEmit` aprovado.
- Angular Compiler `ngc --noEmit` aprovado, incluindo os templates estritos.

O Maven convencional foi interrompido por falta de espaço durante a gravação das classes. Como alternativa, `scripts/RunUnitTestsInMemory.java` compilou em memória as duas classes Java alteradas e o teste, usando JDK 21 e as dependências JUnit existentes, e executou os 20 testes pelo JUnit Platform. Não substitui uma compilação completa ou regressão integrada. Nenhum arquivo do usuário foi limpo.

Comandos normais para repetir quando houver espaço:

```powershell
mvn -o -f backend/pom.xml -Dtest=PaymentAllocationTest test
node --test frontend/tests/payment-allocation.test.cjs
node frontend/node_modules/typescript/bin/tsc --noEmit -p frontend/tsconfig.json
node frontend/node_modules/@angular/compiler-cli/bundles/src/bin/ngc.js -p frontend/tsconfig.json --noEmit
```

O runner alternativo recebe o nome da classe de teste e os arquivos-fonte explícitos. O classpath precisa conter as dependências Maven do projeto e `backend/target/classes` das demais classes já compiladas. Não inicia a aplicação ou escreve `.class`/relatórios.

Não houve acesso ao banco, teste H2/PostgreSQL, atualização de containers ou implantação desta entrega. Login real e revisão visual seguem adiados. Integração transacional, reenvios e concorrência devem ser revalidados antes de aplicar a alteração aos serviços.

## Montagem e correção de parcelas — continuação em 11/11/2026

A interface permite preencher o saldo restante da entrada com um clique, editar uma parcela existente e cancelar a edição mantendo a lista anterior. A sugestão exclui o valor da parcela em edição para não contá-lo duas vezes. Para dinheiro, o operador continua informando quanto recebeu; a sugestão não inventa o valor entregue. Para Pix/transferência, o campo recebido é preenchido com o valor aplicado, mas a confirmação bancária permanece desmarcada.

Ao salvar, a lista é substituída apenas se a composição proposta for válida: valores positivos, limite de cem parcelas, soma sem excesso, troco somente em dinheiro, uma parcela em dinheiro e referência bancária sem repetição na mesma conta/meio. Uma composição incompleta pode permanecer no formulário enquanto o operador monta os pagamentos; concluir ainda exige cobrir exatamente a entrada. O rascunho inválido mostra os problemas e preserva a lista anterior.

Editar um pagamento bancário exige nova confirmação. Alterar meio, conta, referência, valor aplicado ou recebido também desmarca essa confirmação no formulário. Trocar o meio limpa conta e referência anteriores. A conta selecionada continua visível ao paginar a lista de contas, mesmo que pertença a outra página; o servidor continua verificando sua autorização e filial na conclusão.

Enquanto há edição de parcela aberta, a conclusão e a criação de preparação de pagamento ficam bloqueadas. Salve ou cancele a edição antes de prosseguir. Nenhuma dessas ações locais registra ou altera um recebimento já persistido; apenas modifica os pagamentos propostos de uma venda ainda não concluída.

Depois da liberação de espaço, o Maven convencional voltou a executar: compilação completa, 20 testes unitários e empacotamento do JAR aprovados. A calculadora/composição da interface passou em 27 testes Node (15 anteriores e 12 novos). O build de produção Angular também foi aprovado. Esta rodada não executou testes integrados, não conectou a nenhum banco, não iniciou Docker e não atualizou os serviços. A limitação de espaço descrita acima registra a tentativa anterior, já superada para estes builds.

Evidências: `test-evidence/payment-allocation-package.txt`, `payment-composer-unit.txt` e `payment-composer-frontend.txt`.

## Contexto do caixa — continuação de 11/11/2026

A seleção do caixa agora é independente da página da listagem. Paginar mantém a seleção e o carrinho; o caixa escolhido aparece como opção selecionada quando não está na página atual. Respostas antigas de paginação não substituem a página de uma solicitação mais recente. O estado exibido é o último estado consultado, sem monitoramento em tempo real; a API continua revalidando a sessão na conclusão.

Ao escolher outro caixa ou limpar a seleção, os valores aplicados propostos são preservados. Pagamentos bancários perdem a confirmação; se houver mudança de filial ou o contexto anterior não estiver disponível, a conta bancária também é retirada da proposta. Em dinheiro, o valor aplicado é mantido, mas o campo recebido é esvaziado para exigir nova conferência no caixa de destino. Aprovação selecionada, prévia de crediário e edição de parcela em andamento são descartadas na troca. A lista mostra quais parcelas precisam ser editadas e reconferidas.

A correção de uma parcela pode ser salva mesmo quando outras ainda precisam de conferência após a troca. O candidato editado precisa ser válido; não pode introduzir comprovante repetido, segundo pagamento em dinheiro ou excesso aplicado. As parcelas ainda não conferidas continuam bloqueando a conclusão. A sugestão de saldo considera os valores aplicados de todas as outras parcelas, inclusive aquelas aguardando conferência.

Uma suspensão aceita caixas da sua filial; uma preparação de pagamento aceita apenas a sessão original. Ao selecionar uma preparação cujo caixa não aparece na página atual, a interface solicita localizar e selecionar o caixa original. Não há transferência entre turnos ou criação de recebimentos por essa seleção.

Validação desta rodada: 42 testes Node aprovados (30 de cálculo/composição e 12 de regras de contexto), build Angular de produção aprovado. Os testes de regras não substituem navegação real no browser. Sem mudanças de schema/backend, acesso ao banco, Docker ou implantação. Login real e revisão visual permanecem adiados. Evidências: `test-evidence/payment-context-unit.txt` e `payment-context-frontend.txt`.
