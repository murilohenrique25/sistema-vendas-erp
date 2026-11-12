# Atividades pendentes — 11/11/2026

Consolidado do estado registrado em PROGRESS, dos contratos recentes e do escopo integral em REQUIREMENTS. Esta lista organiza as principais entregas pendentes; não substitui os requisitos detalhados nem elimina funcionalidades. Não há percentual ou contagem exata de tarefas, pois várias frentes ainda precisam ser decompostas.

## Implementado no código, ainda pendente de validação/aplicação

Sequência 1–7 autorizada em 11/11: inventário físico, lotes/FEFO, reposição, devolução ao fornecedor, preços por filial, pedidos/orçamentos e regras comerciais agora têm API e interface implementadas. Detalhes, limites e permissões em [IMPLEMENTATION_SEQUENCE](IMPLEMENTATION_SEQUENCE.md). Compilação aprovada; sem validação funcional, banco ou implantação. V29–V36 preparadas e não aplicadas.


- Validar a integração das melhorias recentes do PDV: composição/reconferência de pagamentos, seleção de caixa, revisão final e reenvio do comando.
- Validar histórico de vendas com busca/filtros/estados e manutenção versionada/ativação de produtos.
- Validar geração de contas a pagar por recebimento e parcelamento de compras: valores históricos, distribuição de centavos, concorrência, duplicidade, rollback e vínculos.
- Validar filtros, totais e navegação por origem em contas a pagar e a receber, incluindo créditos/reembolsos do crediário.
- Executar e validar upgrades V26→V27→V28→V29→V30→V31→V32→V33→V34→V35→V36 em ambientes descartáveis antes de aplicar migrações aos serviços. V27–V36 estão preparadas, não aplicadas.
- Executar regressão integrada H2/PostgreSQL, testes do fluxo de interface e segurança/concorrência. Conforme orientação vigente, testes ficam para a etapa final; não houve banco nesta retomada.
- Fazer login real, revisão visual e de acessibilidade, ainda adiados pelo usuário.
- Gerar/aplicar imagens atualizadas e verificar implantação quando a validação estiver pronta. As alterações recentes não foram implantadas; a última implantação comprovada é histórica, em V26.

Parte das regras recentes do PDV teve testes unitários locais antes da orientação de adiar testes. As funcionalidades posteriores tiveram compilação/JAR/build, sem validação funcional. Compilação não equivale a entrega homologada.

## Desenvolvimento restante

### Produtos, preços e estoque

- Expandir cadastro comercial/fiscal: categorias, marcas, variações, atributos e dados tributários previstos no escopo.
- Tabelas de preço por cliente, reajuste em lote, custo de aquisição por filial e histórico completo de mudanças do preço geral; preço por filial e margem bruta já implementados.
- Unidades alternativas/conversões e embalagens; a manutenção atual mantém a unidade fixa.
- Importação de cadastros e rotinas de saneamento/duplicidade.
- Evoluir inventário com contagem cega, múltiplas recontagens e congelamento controlado; abertura, contagem, aprovação e ajuste já implementados.
- Reservas, captura obrigatória de lote no recebimento, seleção manual de lote e reposição considerando compras em aberto; lotes/validade/FEFO, rastreabilidade e estoque mínimo já implementados.
- Transferências com trânsito e logística; a transferência imediata já existe.
- Ampliar perdas: alçadas, evidências/fotos, estorno e indicadores.
- Produção, composição, kits e consumo dos componentes.

### Compras e fornecedores

- Cotações, comparação de propostas e alçadas de aprovação.
- Frete, impostos, despesas e rateio no custo de aquisição.
- Importação idempotente de XML e conferência do documento.
- Evoluir devolução ao fornecedor com compensação/recebimento de crédito, reconciliação e desfazimento; retirada de estoque, prévia e abatimento financeiro já implementados.
- Geração financeira opcional dentro da própria confirmação do recebimento; o fluxo implementado é acionado explicitamente depois de receber.
- Renegociação, cancelamento coletivo/compensação e regeneração controlada de parcelas geradas.

### PDV e pagamentos

- Cobranças reais e estados de provedor: criação, consulta, callbacks autenticados e reconciliação.
- Valores parciais recebidos antes da conclusão, expiração, recebimentos tardios e compensação/reembolso em falhas.
- Cartões, parcelamento de cartões, taxas, liquidação e integração TEF/adquirentes.
- Promoções por categoria/cupom/quantidade, combinações de regras, descontos por valor/categoria e alçadas avançadas; promoção percentual por produto/filial já implementada.
- Trocas, políticas de devolução, quarentena de mercadoria e fluxos entre filiais.
- Compensação/anulação dos ajustes e devoluções já registrados.
- Evolução do caixa/terminal e recuperação persistente de comandos; a proteção atual de comandos incertos fica em memória.

### Financeiro e comercial

- Renegociação de dívidas, juros/multas/descontos e políticas de cobrança.
- Integração bancária e conciliação automática/agrupada; conciliação manual/CSV já existe.
- DRE gerencial, categorias/centros de custo e relatórios financeiros previstos.
- Alçadas de crédito, histórico completo do relacionamento e CRM.
- Liquidação/pagamento de comissões, promoções por categoria/cupom/quantidade, expiração de pontos e catálogo de benefícios; vendedores, apuração/estorno, promoção por produto e pontos/resgates manuais já implementados.
- Reserva de estoque, entrega/conversão parcial e acompanhamento logístico; pedidos/orçamentos com aprovação e conversão integral já implementados.

### Fiscal

- Integração com provedor externo e gestão segura das credenciais/certificados necessários.
- Emissão NFC-e/NF-e, documentos/eventos, consulta de status e recuperação de falhas.
- XML, DANFE e vínculo fiscal com vendas, cancelamentos e devoluções.
- Cadastro/regras tributárias e homologação dos cenários do produto.

### Offline, equipamentos e interfaces móveis

- Persistência local/outbox, sincronização, conflitos e limites da operação offline.
- Impressoras, balanças, gavetas, leitores e TEF reais.
- Atualização/identificação dos terminais e aplicações móveis complementares.

### BI, automações e IA

- Dashboards/KPIs com fórmulas, fontes e filtros documentados.
- Relatórios operacionais/gerenciais, exportações e agendamento.
- Notificações e automações comerciais/financeiras.
- Consultas de IA autorizadas, previsões, recomendações, avaliações e controle de custos/limites.

### SaaS, segurança e operação

- Planos, assinaturas, cobrança da plataforma, limites e administração SaaS.
- Onboarding por fluxo de produto, suporte, importação/migração de clientes e treinamento.
- MFA, recuperação de acesso e evolução da gestão de dispositivos/sessões.
- RLS, ampliação da auditoria e controles de retenção/origem/diferenças.
- LGPD, portabilidade, encerramento de empresa e descarte seguro.
- Ampliar manutenção/estado de filiais e depósitos conforme o escopo.
- Observabilidade, alertas, CI/CD e operação em produção.
- Backup operacional externo, retenção e gestão de chaves; scripts criptografados e ensaios anteriores não substituem operação contínua ou validação com volume real.
- Definir e comprovar RPO/RTO, capacidade, desempenho e recuperação de desastre.
- Homologar pagamentos, fiscal e equipamentos, executar piloto e preparar lançamento comercial.

## Próxima prioridade sugerida de desenvolvimento

Inventário físico de estoque: abertura da contagem, registro das quantidades, apuração de divergências e aprovação rastreável. É uma lacuna central de operação, e pode ser implementada sem ligar o banco nesta fase. Seus testes e sua implantação permanecem sujeitos à etapa final acordada.
