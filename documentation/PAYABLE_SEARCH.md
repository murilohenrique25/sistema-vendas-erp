# Consulta de contas a pagar — implementação de 11/11/2026

Contas a pagar com busca por fornecedor, referência, documento, descrição ou identidade completa do título; período de vencimento; situação; saldo vencido; e origem com/sem recebimento de compra. Os filtros podem ser combinados. O texto usa o nome do fornecedor registrado no título, não seu nome atual no cadastro.

O detalhe de um título vinculado oferece **Ver todas as contas deste recebimento**. Essa ação limpa os demais filtros e mostra a mesma origem, incluindo parcelas pagas/canceladas. O link de uma parcela na tela de compras leva ao título e já limita a lista ao recebimento. **Retirar filtro do recebimento** permite ampliar a busca; **Limpar filtros** retorna à consulta geral. Ter vínculo com recebimento não significa necessariamente ter sido gerado automaticamente: títulos manuais antigos também podem ter essa ligação.

## API

`GET /api/v1/payables` preserva `status`, `overdue`, `page` e `size` e aceita os opcionais:

| Parâmetro | Semântica |
|---|---|
| `query` | Até 200 caracteres; trecho literal de fornecedor histórico/referência/documento/descrição ou identidade completa do título; sem diferenciar maiúsculas/minúsculas, sem promessa de ignorar acentos |
| `dueFrom`, `dueTo` | Datas ISO inclusivas de vencimento, uma ou ambas opcionais; anos 0001–9999; inicial não pode ultrapassar final |
| `branchId` | Filial acessível ao usuário; opção de API sem novo seletor na interface desta entrega |
| `supplierId` | Identidade exata de fornecedor; opção de API; interface oferece busca pelo nome registrado no título |
| `receiptId` | Identidade do recebimento; origem exata das contas consultadas |
| `withReceipt` | `true` com recebimento vinculado, `false` sem vínculo, omitido para ambos |

Os critérios combinam por AND. Combinações contraditórias retornam zero resultados, por exemplo recibo exato com `withReceipt=false`, ou situação paga com `overdue=true`. Saldo vencido é de títulos OPEN/PARTIAL cujo vencimento é anterior à data financeira atual, sem considerar o próprio dia como atraso.

Escopo de empresa e filiais acessíveis, contagem, totais e paginação usam o mesmo conjunto de critérios no SQL. Filial explícita exige autorização. O texto é vinculado como parâmetro, e `%`, `_` e `!` são escapados como caracteres literais. Ordenação permanece por vencimento/identidade. O endpoint conserva `payable:read` e não concede novos acessos.

## Totais

A resposta mantém a página e acrescenta `summary`, `today` e `zone`. `summary` contém strings monetárias:

- `originalAmount`: soma dos valores originais de todos os títulos encontrados, inclusive cancelados.
- `paidAmount`: soma do saldo pago registrado nos títulos, líquido dos estornos já aplicados.
- `outstandingAmount`: soma do saldo original menos pago, excluindo cancelados.
- `overdueAmount`: parte do saldo a pagar que já está vencida em OPEN/PARTIAL.

Os totais cobrem todos os resultados dos filtros, não apenas a página. São posições atuais dos títulos, não uma demonstração de pagamentos realizados dentro do período de vencimento selecionado. Não confirmam movimentação bancária nem substituem o fluxo de tesouraria; pagamentos sem vínculo de caixa também entram no valor pago registrado.

Contagem, agregação e página são lidas em transação com snapshot consistente; uma única data financeira é usada para classificar atrasos nessa consulta. A interface preserva os critérios aplicados ao paginar e ignora respostas de consultas anteriores que cheguem depois de uma busca mais recente. Alterar campos sem aplicar não altera a consulta em andamento.

A rota Contas a pagar passou a carregar sob demanda, mantendo permissões e proteção contra saída com comando pendente. Os parâmetros `title` e `sourceReceipt` permitem abrir um título e limitar sua lista à origem, respectivamente; o backend continua autorizando toda leitura independentemente do URL.

## Validação adiada

Implementação verificada apenas por compilação/JAR com `-Dmaven.test.skip=true` e build Angular. Nenhum teste escrito/executado, consulta SQL/migração executada, banco acessado, container iniciado ou implantação realizada. V27/V28 de compras seguem não aplicadas. Login real/revisão visual e validação funcional permanecem adiados.

Na etapa final, validar permissões/empresa/filial, filial explícita, texto com curingas e acentos, vencimentos inclusivos/anos limites, atraso na virada da data financeira, filtros contraditórios, referência de parcela, origem manual/gerada, totais além da página e cancelados/estornos, snapshot sob concorrência, SQL H2/PostgreSQL, compatibilidade sem novos parâmetros, consultas fora de ordem, filtros aplicados versus campos editados e navegação por título/origem.

Evidências de compilação: `test-evidence/payable-search-backend-build.txt` e `payable-search-frontend-build.txt`. Compilar não comprova funcionamento do SQL ou da navegação.
