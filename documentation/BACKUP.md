# Backup local e ensaio de restauração

Scripts executáveis em PowerShell 7.2+ e Docker ativo. `backup-database.ps1` usa pg_dump custom no PostgreSQL do Compose, cifra com AES-256-GCM (nonce aleatório de 12 bytes/tag de 16 bytes), autentica formato/data e retorna caminho/checksum SHA-256 do arquivo cifrado. Segredos não aparecem no arquivo nem na saída. Arquivos ficam em backups/, ignorado pelo Git. O destino usa CreateNew e nunca sobrescreve arquivo existente.

Defina ERP_BACKUP_KEY como Base64 de uma chave aleatória de 32 bytes, guardada em cofre separado dos backups. A mesma chave é necessária para restaurar. Não registrar em commits/logs. Não gerar outra chave para restaurar um arquivo existente. Perder a chave torna o backup irrecuperável.

```powershell
# Obtenha a chave do seu cofre; este texto é apenas um marcador.
$env:ERP_BACKUP_KEY = 'chave-base64-de-32-bytes-do-seu-cofre'
$backup = ./scripts/backup-database.ps1
./scripts/verify-backup.ps1 -Path $backup.Path
```

`verify-backup.ps1` autentica e decifra antes de criar um banco. Depois restaura em PostgreSQL 17.6 descartável, sem rede externa/portas publicadas, com pg_restore --exit-on-error, verifica histórico Flyway e retorna contagens de empresas/usuários/produtos/movimentos. A restauração valida as constraints presentes no dump. O container/volume desse ensaio e arquivos temporários são removidos ao finalizar. O script não sobrescreve o banco do ERP e não oferece restauração destrutiva em produção.

Há cópia temporária sem cifra no diretório local durante exportação/restauração, excluída no finally. Proteja permissões e disco do host; encerramento forçado do processo pode impedir essa limpeza. Limite do dump: 256 MB; o algoritmo local usa memória e não substitui ferramenta de streaming para bases grandes. Compose atual é desenvolvimento, não um serviço de backup gerenciado.

Ensaio executado em 14/10/2026: backup do banco local com V1–V6, restauração aprovada, contagens 0/0/0/0 (banco sem empresas). Alteração de ciphertext com Base64 válido e chave incorreta rejeitadas pela autenticação GCM. Chave de teste existiu apenas em memória; arquivos de teste excluídos. Evidência em test-evidence/backup-restore.json. Nenhum backup operacional permanente ou agendamento foi configurado.

Reproduzir esse ensaio: `./scripts/test-backup.ps1`. O script usa chave temporária, testa restauração/adulteração/chave errada, restaura a variável de ambiente anterior e exclui somente seus arquivos de teste. Ele lê o banco atual do Compose, sem alterá-lo; proteja o host que recebe os arquivos temporários.

Antes de produção: testar com volume e dados representativos, reconciliar saldos/ledger após restauração, definir RPO/RTO/retencão, guardar cópias cifradas fora da máquina com controle de acesso, garantir custódia/rotação das chaves, alertas de execução e ensaios periódicos. Backup do banco não inclui certificados fiscais, documentos externos, configuração do host ou segredos da aplicação.

Restauração local repetida após V11: aprovada, migração 11 restaurada; testes de ciphertext alterado e chave incorreta rejeitados. Evidência test-evidence/backup-restore-v11.json. Banco de origem continua vazio; teste não comprova desempenho em volume representativo.

Restore V12 aprovado após vínculo pagamento/tesouraria, em container isolado. Chave incorreta/adulteração rejeitadas. Evidência backup-restore-v12.json. Origem sem dados de negócio; validação em volume representativo permanece pendente.

Restore V15 aprovado em container isolado após clientes/recebíveis. Testes de adulteração e chave incorreta rejeitados. Evidência backup-restore-v15.json. Origem sem dados de negócio; volume representativo e rotina operacional de backup continuam pendentes.

Restore V16 aprovado após índices de fluxo de caixa. Container isolado removido após teste; adulteração/chave incorreta rejeitadas. Evidência backup-restore-v16.json. Origem vazia; volume representativo e plano operacional continuam pendentes.

Restore V17 aprovado após extratos/conciliacão; container isolado e arquivos temporários removidos pelo script. Adulteração e chave incorreta rejeitadas. Evidência backup-restore-v17.json. Origem vazia; teste não substitui verificação com volume representativo nem plano operacional de backup.

Restore V18 aprovado após terminais/caixa, em container isolado com cleanup do próprio teste. Chave incorreta/adulteração rejeitadas. Evidência backup-restore-v18.json. Origem vazia; volume real e política operacional seguem pendentes.

V19: ensaio local de restauração aprovado (backup-restore-v19.json), com histórico de migrations 19, adulteração do ciphertext e chave incorreta rejeitadas. Banco ainda vazio (zero empresas/usuários/produtos/movimentos); não constitui ensaio com volume de dados de operação real.

V20: restore local aprovado, migration 20, sem dados permanentes, ciphertext adulterado e chave incorreta rejeitados. Evidência backup-restore-v20.json. Não substitui ensaio com volume real ou política de cópia remota/guarda de chave.

V21: restore local aprovado, migration 21, contagens permanentes vazias; ciphertext adulterado e chave incorreta rejeitados. Evidência backup-restore-v21.json. Continua pendente ensaio com dados/volume reais, guarda operacional da chave e cópias remotas.

V22: restauração local aprovada, migration 22, contagens vazias; ciphertext adulterado e chave incorreta rejeitados. Evidência backup-restore-v22.json. Ensaio continua sem massa/volume de operação real e não configura cópias remotas/guarda operacional de chave.

## Validação V23 — 16/10/2026

V23 aplicada localmente com três serviços saudáveis. Migration 23 confirmada; página de recebíveis, seis scripts e CSS HTTP 200; criação/prévia/consulta anônimas HTTP 401. Contagens empresa/plano/vínculo/título/recebimento 0|0|0|0|0. Restore criptografado V23 aprovado; adulteração e chave incorreta rejeitadas. Evidências receivable-plan-local.json, receivable-plan-deploy.txt e backup-restore-v23.json. Banco de origem vazio: ensaio não substitui volume de operação real. Login real/revisão visual e publicação em produção continuam adiados.

## Validação V24 — 16/10/2026

V24 aplicada localmente; API, banco e frontend saudáveis. Migration 24 e tendered NOT NULL confirmados. Rotas de crédito/vendas/recebíveis, sete scripts e CSS HTTP 200; GET/PUT de política e POST de venda anônimos HTTP 401. Contagens empresa/política/venda a prazo/plano/título 0|0|0|0|0. Restore V24 aprovado em banco isolado; adulteração e chave incorreta rejeitadas. Evidências pos-credit-local.json, pos-credit-deploy.txt e backup-restore-v24.json. Origem vazia: não substitui ensaio com volume operacional. Login real, revisão visual e publicação em produção continuam adiados.

## Validação V25 — 16/10/2026

V25 aplicada localmente com três serviços saudáveis. Migration 25 e NOT NULL de received_amount/credited_amount confirmados; páginas de vendas/recebíveis, sete scripts e CSS HTTP 200. Consultas de origens/ajustes e POST de devolução anônimos HTTP 401. Contagens empresa/devolução/abatimento/reembolso de parcela/título abatido 0|0|0|0|0. Restore V25 aprovado em banco isolado; chave incorreta e adulteração rejeitadas. Evidências credit-return-local.json, credit-return-deploy.txt e backup-restore-v25.json. Origem vazia: não substitui ensaio com volume operacional. Sem login real/revisão visual ou publicação em produção.


## Aplicação local V26 — 16/10/2026

Preparação de pagamento aplicada com as imagens locais da API e interface. Três containers saudáveis; health UP; página de vendas e oito arquivos JS/CSS HTTP 200 com MIME correto; GET/POST anônimos da preparação retornam 401. Banco em V26, sem empresas ou preparações permanentes (`26|0|0`). Evidências em `test-evidence/pending-checkout-local.json` e `pending-checkout-deploy.txt`.

Backup criptografado restaurado em PostgreSQL isolado na V26, com adulteração e chave incorreta rejeitadas (`test-evidence/backup-restore-v26.json`). A origem vazia não demonstra recuperação de volume operacional real nem gestão externa de chaves. Login real e revisão visual continuam adiados.
