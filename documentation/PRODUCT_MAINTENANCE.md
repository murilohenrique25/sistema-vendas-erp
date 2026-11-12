# Manutenção de produtos — implementação de 11/11/2026

Catálogo com edição de SKU, nome, descrição, preço e códigos de barras, ativação/desativação e filtro por ativos/inativos. Esta entrega não exclui produtos ou movimenta estoque. A unidade é fixa desde o cadastro, para não reinterpretar quantidades de estoque, compras, vendas ou carrinhos guardados. Para outra unidade, use um produto diferente.

A edição carrega a versão atual do cadastro pela API antes de abrir. Permite manter códigos GTIN e internos no mesmo produto, em campos separados, até vinte códigos somando ambos. A lista enviada substitui a anterior. O servidor valida GTIN/canonicalização e exclusividade por empresa, preservando os dados históricos dos itens de venda. Um código removido deixa de localizar esse produto; a unicidade é sobre os códigos atualmente cadastrados, sem reserva histórica de códigos.

## API e concorrência

- `PUT /api/v1/products/{id}`: `{version, product}`. `product` usa o contrato de criação existente: `sku`, `name`, `description`, `unit`, `salePrice`, `barcodes`. A unidade deve ser igual à atual. Retorna o produto atualizado.
- `PUT /api/v1/products/{id}/active`: `{version, active}`. Ativa ou desativa sem apagar saldos ou histórico.
- `GET /api/v1/products`: novo parâmetro opcional `active=true|false`; sem ele mantém a consulta de todos. Busca, escopo da empresa, ordenação e paginação continuam no banco.

Escritas exigem `product:read` e `product:write`. Produtos pertencem ao catálogo da empresa, sem novo escopo por filial nesta etapa. As mudanças usam transação, bloqueio da empresa e do produto, versão esperada e auditorias `PRODUCT_UPDATED`, `PRODUCT_ACTIVATED` e `PRODUCT_DEACTIVATED`. Atualização de dados e substituição de códigos são atômicas; conflito de SKU/código deve desfazer a operação inteira pela transação existente.

Com versão atual, conteúdo já igual ao pedido é uma operação sem alteração: não incrementa versão nem gera auditoria. Para repetição de um PUT após resultado incerto, a operação também aceita a versão imediatamente seguinte quando o estado atual já corresponde exatamente ao solicitado; o preço é comparado numericamente e a lista de códigos sem depender da ordem. Outras versões/conteúdos resultam em 409. Isso é convergência para o estado desejado, sem diário de comandos ou identificação persistida de cada tentativa. Se outras alterações ocorrerem depois, pode ser necessário recarregar e reconciliar manualmente o cadastro.

A interface conserva o corpo pendente para repetir em falhas de comunicação/5xx, bloqueia novas mutações/saída/logout enquanto o resultado estiver incerto e orienta recarregar em caso de 409. O formulário mantém os dados locais ao alterar apenas a situação; recarregar explicitamente substitui as alterações locais. A proteção existe em memória, sem recuperação de comandos após encerramento forçado do browser. A operação de criação anterior não foi convertida em comando versionado por esta entrega.

## Efeito operacional

Desativar torna o produto indisponível para operações que já exigem produto ativo; não apaga quantidades, itens de pedidos ou vendas anteriores. Reativar libera o cadastro para essas operações. Lookup por código continua permitindo consultar cadastro inativo. Não há promessa de bloquear todas as rotinas do sistema, nem conversão de unidade ou migração automática de referências.

Vendas guardam seus próprios snapshots. Alterar o preço não reescreve vendas concluídas; futuros fechamentos de carrinhos continuam sujeitos à conferência de preço do servidor. Suspensões/preparações podem exigir atualização ou descarte se nome, SKU ou preço divergirem das condições originais.

## Validação deixada para a etapa final

Testes não escritos/executados nesta rodada, conforme orientação vigente. Compilação do backend com `-Dmaven.test.skip=true package` e build Angular verificados separadamente; isso não comprova execução de SQL/JPQL, concorrência ou interação de interface. Sem acesso ao banco, Docker, migração ou implantação. Login real e revisão visual seguem adiados.

Validar na etapa final: isolamento por empresa e permissões, versões obsoletas e reenvios, concorrência de preço/estado com venda e estoque, atomicidade em conflito de código/SKU, duplicidade de GTIN canônico, manutenção de listas mistas, unidade fixa, valores limites, preservação de histórico, filtro ativo/inativo com paginação, coexistência JDBC/JPA, atualização de lista após desativação, bloqueio/reenvio na interface e revogação de permissão durante uma tentativa.

Evidências de compilação: `test-evidence/product-maintenance-backend-build.txt` e `product-maintenance-frontend-build.txt`.
