# Parcelamento de compras recebidas — V28 preparada

Implementação de 11/11/2026, com testes deixados para o final por orientação do usuário. Amplia a geração financeira da V27 para uma ou mais contas a pagar por recebimento. Não altera estoque, custo recebido ou tesouraria; não acrescenta juros, multas, taxas ou desconto financeiro.

## Calendário e valores

O operador informa de 1 a 120 parcelas, primeiro vencimento e intervalo de 1 a 12 meses. O total vem exclusivamente das linhas monetárias imutáveis do recebimento. A divisão usa centavos inteiros: todas as parcelas recebem a parte inteira da divisão, e os centavos restantes são distribuídos pelas primeiras parcelas. A soma permanece igual ao total recebido; nenhuma parcela pode ser inferior a um centavo.

As datas são calculadas a partir do primeiro vencimento, acrescentando `posição × intervalo` meses, sem encadear a data ajustada da parcela anterior. Se o dia não existir no mês, usa o último dia desse mês. Não há ajuste de dias úteis/feriados. Por exemplo, uma base no dia 31 pode cair em fevereiro no dia 28/29 e voltar ao dia 31 no mês seguinte. Anos de emissão/vencimento devem ficar entre 0001 e 9999, e o primeiro vencimento não pode anteceder a emissão.

Para parcela única, mantém a referência original. Para múltiplas, a referência base aceita até 56 caracteres e recebe sufixos `.001` até `.120`, respeitando o limite de 60 do título. Conta única aceita a referência original de até 60 caracteres. Conflito com referência já usada desfaz o lote inteiro.

## API

Base `/api/v1/purchase-orders/{orderId}/receipts/{receiptId}/payable`.

- `POST /preview`: mesma entrada da geração e permissões de leitura (`purchase:read`, `payable:read`). Consulta origem/escopo e calcula `{amount,installments:[{position,reference,dueOn,amount}]}` sem escrever título ou vínculo. `requestId` continua no contrato, mas não é reservado pela prévia.
- `POST` na base: aceita os campos V27 e os opcionais `installmentCount`/`intervalMonths`. Ausência significa uma parcela/um mês. Para uma parcela, o intervalo é irrelevante e normalizado como um mês. Exige também `payable:write`.
- `GET` na base: mantém `generatedTitle` como primeiro título, por compatibilidade, e acrescenta `generatedTitles` com todos os títulos atuais em ordem de posição. Cancelados e pagos continuam visíveis. `linkedTitleCount` continua contando títulos não cancelados ligados à origem.

O campo `dueOn` existente passa a representar o primeiro vencimento. O total, fornecedor e filial continuam derivados do recebimento, sem aceitar valores alternativos do cliente. A geração recalcula o calendário no servidor; uma prévia não reserva referências nem autoriza o lote.

## Atomicidade e reenvios

As parcelas são criadas antes do vínculo de geração, dentro da mesma transação e com bloqueio da empresa, para reutilizar o serviço de títulos sem liberar cadastros manuais extras após a geração. Depois, o vínculo da V27 aponta para a primeira parcela e `purchase_receipt_installment` relaciona todas as posições/títulos à mesma origem. FKs compostas exigem empresa e recebimento correspondentes. Qualquer falha em uma parcela, referência, vínculo ou auditoria desfaz o lote todo.

A primeira parcela usa a chave da geração; as seguintes têm chaves derivadas deterministicamente da empresa, chave e posição. Colisões com títulos existentes são rejeitadas, sem reaproveitar silenciosamente outro lançamento. A impressão digital inclui quantidade/intervalo para múltiplas parcelas. Para parcela única conserva o formato V27, permitindo repetir comandos antigos com os mesmos dados após o upgrade.

Vínculo único por recebimento e proteção contra títulos manuais ativos continuam válidos. Reenvio da mesma geração retorna o estado atual de todas as parcelas. Outra chave não gera um segundo lote, inclusive quando uma ou todas as parcelas forem canceladas. Pagamentos/estornos/cancelamentos continuam individuais nas rotinas existentes. Não há cancelamento coletivo, renegociação ou regeneração de saldo nesta entrega.

## Interface

O financeiro do recebimento mostra quantidade, intervalo e primeiro vencimento. **Conferir parcelas** busca o calendário calculado pela API. A geração só fica disponível com uma prévia correspondente aos campos atuais e ao total da origem; alterar condições invalida essa prévia. Isso é uma comparação local, não uma autorização persistida no servidor.

Após gerar, mostra todas as contas com valor original, vencimento, saldo e situação, além do link de cada parcela em Contas a pagar. Reenvios incertos conservam corpo/identificador e os bloqueios existentes. Não há persistência dessa proteção após encerrar o navegador.

## Migração e validação adiada

V28 cria `purchase_receipt_installment` e inclui cada geração V27 existente como parcela única, preservando título, chave, impressão digital, recebimento e pagamentos. **V27 e V28 permanecem não aplicadas ao ambiente local.** Nenhuma conexão ao banco, execução de SQL/migração, inicialização/atualização Docker, teste ou implantação nesta etapa.

Backend compilado/empacotado com `-Dmaven.test.skip=true`; build Angular de produção aprovado. Evidências: `test-evidence/purchase-installments-backend-build.txt` e `purchase-installments-frontend-build.txt`. Isso não valida o comportamento de SQL, transações ou telas. Login real e revisão visual seguem adiados.

Na etapa final, validar upgrade V26→V27→V28 e V27 com gerações existentes; reenvio antigo sem novos campos; soma/resto/limite de parcelas/um centavo; calendário de fim de mês e anos bissextos; referências longas e duplicadas em parcela intermediária; rollback integral; chaves concorrentes e colisões; isolamento/revogação/filiais; pagamento e cancelamento de parcelas; bloqueio de nova geração; atualização do fluxo de caixa; prévia obsoleta, incerteza de rede e links individuais.
