# Implantação

Compose é ambiente local de desenvolvimento. A sequência executável está no README. PostgreSQL persiste no volume `postgres_data`; `docker compose down` preserva dados, `down -v` os destrói e não deve ser usado como operação de manutenção. Não foi efetuado deploy em produção.

Antes de homologação: executar `mvn verify -Ppostgres-tests`, frontend build/typecheck e E2E; revisar variáveis e segredos; configurar HTTPS, rede privada para banco/backend, health checks de readiness, backups criptografados, restauração em ambiente separado, alertas, limites de recursos e política de atualização de imagens. Validação humana fiscal/pagamentos/hardware obrigatória para piloto.

Flyway aplica migrations no início; nunca editar uma migration aplicada. Novas versões devem usar alterações compatíveis com aplicação anterior. Rollback de aplicação só é permitido após verificar compatibilidade do schema. Backup antes de migração destrutiva.

Backup local cifrado e ensaio descartável implementados em scripts/backup-database.ps1 e scripts/verify-backup.ps1. pg_dump custom, AES-256-GCM, metadados autenticados e checksum SHA-256; restauração isolada aprovada com V6. Chave incorreta/ciphertext adulterado rejeitados. Procedimento, limites e evidência em BACKUP.md. Armazenamento fora da máquina, agendamento, ensaio com volume real e reconciliação operacional continuam pendentes. RPO/RTO precisam de acordo comercial; não há SLA anunciado.

CI fornecida executa PostgreSQL/Testcontainers em runner Linux e compila Angular; execução remota ainda não verificada. Dockerfiles e configurações de Nginx escritos, build de imagens depende de Docker ativo.

Incidente local resolvido em 14/10/2026 após reinício do Windows: Docker gravável, imagens atuais aplicadas e três serviços saudáveis. API retorna UP, frontend e CSS retornam HTTP 200. Fonte Angular usa inlineCritical=false, compatível com script-src self. Não executar prune global ou apagar volumes como manutenção rotineira.

Tentativas autorizadas de reinício Docker/WSL precederam o reinício do Windows pelo usuário. A recuperação final confirmou motor, gravação e health checks, preservando os volumes existentes.

### Empacotamento com menor uso de recursos

Frontend local já compilado pode ser empacotado por `docker build -t sistema-vendas-erp-frontend -f infrastructure/Dockerfile.frontend-artifact .`, seguido de `docker compose up -d --no-build --wait`. O contexto desse Dockerfile inclui somente o build frontend e configuração Nginx. Procedimento executado e verificado neste ambiente. Use somente artefato atualizado; alterações de fonte exigem nova compilação Angular. Builds convencionais devem ser feitos um serviço por vez; limites de heap de build: Maven 512 MB e Node 1024 MB. Limites não garantem espaço suficiente para o Docker/WSL.

Testes PostgreSQL do núcleo estão aprovados (31 cenários com V1–V4). Compose disponível em localhost:4200, API 18080 e banco 15432; migrações V1–V4 bem-sucedidas. Banco com zero empresas: provisionar dados reais pelo script documentado. Servidores temporários de teste estão encerrados. Espaço livre observado após subida: 7,59 GB.

Backend também permite empacotar o JAR local atualizado com `docker build -t sistema-vendas-erp-backend -f infrastructure/Dockerfile.backend-artifact .`. Execute Maven package/verify antes; esse caminho não recompila nem executa testes. O contexto inclui somente o JAR gerado e Dockerfile, sem .env. Os Dockerfiles convencionais continuam disponíveis para builds a partir da fonte.

Atualização local V11 aplicada com Dockerfiles de artefatos compilados e compose --no-build. Banco/API/frontend saudáveis, UI atual entregue e API de tesouraria exige autenticação. Sem publicação externa. Restore V11 verificado em container isolado.

V12 aplicada localmente em 14/10/2026 com imagens de artefatos já compilados: banco/API/frontend saudáveis, frontend atualizado, sem publicação externa e sem criação de usuário permanente.

Atualização local V13–V15 em 14/10/2026: imagens de artefatos compilados aplicadas e três serviços saudáveis. V14 Java substitui apenas o check de origem nomeado automaticamente por V12; novas origens preservam AP/manual. Sem publicação externa nem usuário permanente provisionado.

V16 aplicada localmente em 14/10/2026 com imagens de artefatos compilados. Banco/API/frontend saudáveis; nova rota /cash-flow e bundle atual entregues, API protegida. Sem publicação externa e sem provisionamento fictício.

V17 aplicada localmente em 14/10/2026 com imagens de artefatos compilados. Banco/API/frontend saudáveis; rota /reconciliation e chunks inicial/compartilhado/sob demanda entregues. Sem publicação externa ou inserção de extratos fictícios permanentes.

V18 aplicada localmente em 14/10/2026 via artefatos compilados. Banco/API/frontend saudáveis, tela /pos-cash e chunks sob demanda entregues. Sem publicação externa nem provisionamento de usuário, terminal ou caixa permanente.

## Atualização local V19 — 14/10/2026

JAR verificado e Angular compilado; imagens artifact aplicadas ao projeto existente com compose up --no-build --wait. API, banco e frontend saudáveis. Migração V19 confirmada no PostgreSQL permanente; página /pos-sales e cinco scripts HTTP 200, API sem autenticação 401. Banco mantém zero empresas/terminais/sessões/vendas/itens. Evidência pos-sale-local.json. Login real/revisão visual adiados. Sem publicação em produção ou alteração de serviços de outros projetos.

## Dependências locais em D: — 15/10/2026

O C: chegou a aproximadamente 180 MB livres durante a implementação. Somente node_modules deste projeto foi movido para D:\CodexBuild\sistema-vendas-erp-20261015\node_modules, com junção no caminho original do frontend. Build completo após a mudança aprovado. Cerca de 209 MB regeneráveis relocados; nenhum código, segredo, banco ou backup foi movido. Duas imagens V18 deste ERP sem containers dependentes foram removidas por ID; serviços de outros projetos preservados. Evidência dependency-storage-v20.json. C: ainda tem pouco espaço (aproximadamente 400 MB), exigindo liberar espaço adicional antes de trabalhos maiores.

## Aplicação V20 — 15/10/2026

JAR verificado, imagens artifact aplicadas; API/banco/frontend saudáveis. Migration 20, UI/cinco scripts/CSS HTTP 200, consultas e cancelamento sem token 401. Contagens empresa/venda/pagamento/cancelamento 0|0|0|0. Restore V20 aprovado. Evidências pos-mixed-local.json, pos-mixed-deploy.txt e backup-restore-v20.json. Login real/revisão visual e produção seguem adiados.

## Armazenamento do JAR V21 — 15/10/2026

Depois de gerar as imagens e concluir testes, o JAR regenerável foi copiado para D:\CodexBuild\sistema-vendas-erp-20261015\backend-artifacts\v21\erp-backend-0.1.0-SNAPSHOT.jar. SHA-256 verificado antes de remover somente o JAR de backend/target, liberando cerca de 65 MB no C:. Código, banco, segredos e backups preservados. O próximo Maven verify/package regenera o caminho padrão; execute-o antes de recriar a imagem artifact, ou copie de volta o JAR verificado. Evidência artifact-storage-v21.json. Espaço do C: continua limitado; não houve limpeza global nem alteração de recursos de outros projetos.

## Aplicação local V21 — 15/10/2026

Imagens artifact aplicadas com compose --no-build --wait; API/banco/frontend saudáveis. Migration 21 e tabelas novas confirmadas. UI/cinco scripts/CSS HTTP 200; GET/POST de devolução sem token HTTP 401. Banco mantém 0|0|0|0|0 empresas/vendas/devoluções/itens/reembolsos. Restore V21 aprovado. Evidências pos-return-local.json, pos-return-deploy.txt e backup-restore-v21.json. Não houve publicação em produção ou login/revisão visual.

## Aplicação V22 — 15/10/2026

Maven verify gerou novamente o JAR no caminho padrão; imagens artifact aplicadas com compose --no-build --wait. API/banco/frontend saudáveis. Migration 22, UI/cinco scripts/CSS 200, APIs novas sem token 401, contagens empresa/venda/política/solicitação/suspensão/comando 0|0|0|0|0|0. Restore V22 aprovado. Evidências pos-commercial-local.json, pos-commercial-deploy.txt e backup-restore-v22.json. Sem login real/revisão visual ou publicação em produção.

## Validação V23 — 16/10/2026

V23 aplicada localmente com três serviços saudáveis. Migration 23 confirmada; página de recebíveis, seis scripts e CSS HTTP 200; criação/prévia/consulta anônimas HTTP 401. Contagens empresa/plano/vínculo/título/recebimento 0|0|0|0|0. Restore criptografado V23 aprovado; adulteração e chave incorreta rejeitadas. Evidências receivable-plan-local.json, receivable-plan-deploy.txt e backup-restore-v23.json. Banco de origem vazio: ensaio não substitui volume de operação real. Login real/revisão visual e publicação em produção continuam adiados.

## Validação V24 — 16/10/2026

V24 aplicada localmente; API, banco e frontend saudáveis. Migration 24 e tendered NOT NULL confirmados. Rotas de crédito/vendas/recebíveis, sete scripts e CSS HTTP 200; GET/PUT de política e POST de venda anônimos HTTP 401. Contagens empresa/política/venda a prazo/plano/título 0|0|0|0|0. Restore V24 aprovado em banco isolado; adulteração e chave incorreta rejeitadas. Evidências pos-credit-local.json, pos-credit-deploy.txt e backup-restore-v24.json. Origem vazia: não substitui ensaio com volume operacional. Login real, revisão visual e publicação em produção continuam adiados.

## Validação V25 — 16/10/2026

V25 aplicada localmente com três serviços saudáveis. Migration 25 e NOT NULL de received_amount/credited_amount confirmados; páginas de vendas/recebíveis, sete scripts e CSS HTTP 200. Consultas de origens/ajustes e POST de devolução anônimos HTTP 401. Contagens empresa/devolução/abatimento/reembolso de parcela/título abatido 0|0|0|0|0. Restore V25 aprovado em banco isolado; chave incorreta e adulteração rejeitadas. Evidências credit-return-local.json, credit-return-deploy.txt e backup-restore-v25.json. Origem vazia: não substitui ensaio com volume operacional. Sem login real/revisão visual ou publicação em produção.


## Aplicação local V26 — 16/10/2026

Preparação de pagamento aplicada com as imagens locais da API e interface. Três containers saudáveis; health UP; página de vendas e oito arquivos JS/CSS HTTP 200 com MIME correto; GET/POST anônimos da preparação retornam 401. Banco em V26, sem empresas ou preparações permanentes (`26|0|0`). Evidências em `test-evidence/pending-checkout-local.json` e `pending-checkout-deploy.txt`.

Backup criptografado restaurado em PostgreSQL isolado na V26, com adulteração e chave incorreta rejeitadas (`test-evidence/backup-restore-v26.json`). A origem vazia não demonstra recuperação de volume operacional real nem gestão externa de chaves. Login real e revisão visual continuam adiados.
