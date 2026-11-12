# Conciliação de extratos informados

Importar linhas de extrato para uma conta de tesouraria do tipo BANK, conferir sugestões e vincular cada linha a um lançamento existente. Não há conexão com banco, confirmação externa de transação, criação automática de movimento financeiro, ajuste de saldo, cobrança nem devolução bancária. Contas CASH não aceitam extrato neste módulo.

## Entrada e prévia

Entrada manual estruturada ou CSV UTF-8 de até 1 MiB/500 lançamentos. Cabeçalho exato:

```text
externalId;date;amount;description
```

Separador ponto e vírgula, data AAAA-MM-DD, decimal com ponto e até duas casas; crédito positivo e débito negativo. Zero é rejeitado; até 17 dígitos inteiros. Identificador externo obrigatório, estável e único na conta; não use número da linha como identidade. Descrição pode ser vazia. Data deve ser válida, a partir do ano 1 e até o dia financeiro atual. Suporte a BOM, CRLF/LF, campos entre aspas, aspas duplicadas e descrições com quebras de linha. Cabeçalho/campos malformados, duplicatas no arquivo e conteúdo inválido rejeitam o lote inteiro. OFX, XLSX, outros formatos de CSV e dados de saldo bancário ainda não suportados.

Prévia valida os mesmos campos e escopo do importador, normaliza texto e apresenta valores como strings decimais. Não grava linhas, lotes nem auditoria. A interface exige prévia do conteúdo CSV antes de importar. Referência identifica o arquivo/lote; conteúdo bruto não é armazenado, apenas linhas normalizadas e referência.

## Reimportação e identidade

UUID do cliente e fingerprint persistidos por lote. Mesmo UUID/conteúdo retorna a importação existente, sem duplicar registros/auditoria; outro conteúdo com mesma chave gera 409. Ordem das linhas e escala decimal equivalente não mudam a identidade. Identificador externo é preservado com sua caixa e trim, por empresa/conta.

Arquivos sobrepostos com a mesma linha/conteúdo reutilizam a linha existente. Lote informa quantos registros são novos ou já existentes e mantém vínculo às linhas compartilhadas. Mesmo identificador externo com outra data/valor/descrição é conflito; todo o lote é desfeito, inclusive linhas inseridas antes do conflito. Novos UUIDs podem gerar novos lotes de reimportação, mas não duplicam linhas do extrato. Não há edição/substituição/exclusão de dados bancários importados nesta etapa.

## Conferência e revisão

Estados: UNMATCHED, MATCHED e IGNORED. Sugestões são movimentos livres da mesma conta com valor e sinal exatamente iguais, registrados até sete dias antes/depois da data do extrato. Nenhuma sugestão é confirmada automaticamente, mesmo quando existe uma única candidata. Confira descrição e comprovante: coincidência de valor/data não comprova identidade.

Conciliação exige versão atual, UUID da revisão, ID do lançamento e justificativa. Conta, tenant, valor e sinal são revalidados na transação. Um lançamento não pode ser conciliado a duas linhas. Fora da janela das sugestões, o ID do extrato da tesouraria permite vinculação manual após conferência; não há tolerância monetária, soma de parcelas ou divisão de um movimento entre linhas.

Ignorar requer justificativa e deixa a linha no histórico. Antes de conciliar novamente uma linha MATCHED/IGNORED, desfaça sua revisão: o estado volta a UNMATCHED e o lançamento pode ser conciliado novamente. Desfazer não apaga lançamentos, revisões nem auditoria. Histórico registra ação, ator, nota, ID do movimento e versão, ordenado pela sequência de revisões. Estorno financeiro em AP/AR continua sendo uma operação própria; não desfaz automaticamente a conciliação do débito/crédito original.

Reenvios da mesma revisão não repetem a mutação, mesmo após uma revisão posterior: devolvem a posição atual da linha. UUID reutilizado com conteúdo/linha diferente ou versão vencida gera conflito. Escritas são serializadas pelo lock da empresa, compatível com tesouraria, AP e AR. Alteração de estado/histórico/auditoria é atômica; saldos não são modificados.

## Acesso e interface

`reconciliation:read` e `treasury:read` exigidos em toda consulta. Importação/prévia também exigem `reconciliation:import`; conferência, ignorar e desfazer exigem `reconciliation:review`. Filial é a da conta e o acesso é verificado em lotes, linhas, sugestões, histórico e totais. Tenant sempre vem da sessão. Novas empresas recebem permissões; migração não eleva usuários existentes. Conjunto atual: 40 permissões, dentro da whitelist e limite de 100 nos DTOs de usuários/perfis.

Interface `/reconciliation`, carregada sob demanda: seleção paginada de banco, importação/prévia/manual, filtro por situação/lote, histórico, sugestões e revisão. Extrato da tesouraria mostra ID de lançamento para conferência manual. Em rede/5xx, payload/UUID permanecem em memória para retry, bloqueando edição/navegação/logout. Não existe outbox durável; fechar/recarregar pode perder o comando. Login real e homologação visual seguem adiados pelo usuário.

V17 cria lotes, linhas, associação de importação e histórico versionado, com constraints de estado e vínculos tenant/conta/lançamento, além da unicidade do movimento conciliado. Bank_statement_line não certifica saldo de extrato ou cobertura de um período: resumos contam apenas linhas importadas e sua revisão atual.

Validação: ReconciliationTest/PostgresIT cobre identidade/reimportação, rollback total, CSV/preview, match/reset/history, unicidade e sinal/conta, versões, escopo/tenant/permissões e concorrência. Pendentes: OFX/provedores, conciliação agrupada/parcial, tolerâncias/alçadas, anexos, períodos/saldos de extrato/fechamento, exportações e homologação comercial.
