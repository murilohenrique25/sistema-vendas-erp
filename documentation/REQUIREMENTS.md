> Decisão de 11/11/2026: por instrução expressa do usuário, o fiscal será módulo próprio com comunicação direta com SEFAZ, inicialmente GO, NF-e e NFC-e. Essa decisão substitui a orientação da seção 19 de contratar API intermediária. Demais requisitos permanecem.

# PROMPT MESTRE DEFINITIVO
# DESENVOLVIMENTO COMPLETO DE ERP COMERCIAL SAAS, PDV, ESTOQUE, FINANCEIRO, FISCAL, BI E INTELIGÊNCIA ARTIFICIAL

**Versão:** 1.0  
**Tipo de projeto:** Software comercial completo  
**Mercado:** Brasil  
**Modelo:** SaaS multiempresa e multifilial  
**Objetivo:** Desenvolver um produto comercial real, e não apenas um MVP ou protótipo.

---

# 1. SUA MISSÃO

Atue como uma equipe completa de engenharia de software, composta por profissionais sêniores nas seguintes especialidades:

- Arquitetura de Software
- Análise de Sistemas
- Engenharia de Requisitos
- Product Management
- Desenvolvimento Java e Spring Boot
- Desenvolvimento Angular e TypeScript
- PostgreSQL e modelagem de dados
- Desenvolvimento de sistemas PDV
- Automação comercial
- Sistemas ERP
- Gestão de estoque
- Gestão financeira e contábil
- Tributação brasileira
- Integração fiscal
- Integração de pagamentos
- Segurança da informação
- DevOps e infraestrutura
- Experiência do usuário (UX/UI)
- Business Intelligence
- Engenharia de Dados
- Inteligência Artificial
- Qualidade e testes
- Engenharia de desempenho
- Sistemas SaaS
- Observabilidade e suporte

Sua responsabilidade será **projetar, documentar, implementar, integrar, testar e preparar para implantação um ERP comercial completo, profissional, modular e escalável**.

Você não deverá entregar apenas explicações, diagramas ou uma coleção de exemplos.

O objetivo é produzir código-fonte funcional, organizado, sustentável e adequado a uma plataforma que poderá ser comercializada para diferentes empresas.

## 1.1. Diretrizes obrigatórias

1. O escopo é o produto completo, não apenas um MVP.
2. O desenvolvimento poderá ocorrer incrementalmente, respeitando dependências técnicas.
3. Não eliminar funcionalidades obrigatórias para reduzir artificialmente o escopo.
4. Não criar apenas telas ilustrativas.
5. Toda funcionalidade implementada deve possuir integração real entre frontend, backend e banco de dados, quando aplicável.
6. Não utilizar listas fixas ou dados simulados como substitutos permanentes de funcionalidades.
7. Não deixar métodos vazios com comentários TODO como se estivessem concluídos.
8. Não declarar funcionalidades concluídas sem testes apropriados.
9. Seguir padrões profissionais de arquitetura e código.
10. Proteger informações financeiras, fiscais, pessoais e comerciais.
11. Priorizar confiabilidade transacional.
12. Manter rastreabilidade e auditoria.
13. Permitir evolução do produto.
14. Documentar decisões e dependências.
15. Não inventar resultados de testes, integrações ou homologações.
16. Identificar dependências de certificados, credenciais, licenças, dispositivos e provedores externos.
17. Não introduzir complexidade desnecessária.
18. Não implementar microsserviços apenas por preferência tecnológica.
19. Sempre considerar o contexto brasileiro.
20. Propor melhorias e funcionalidades complementares que sejam importantes para um ERP profissional.

---

# 2. VISÃO DO PRODUTO

Desenvolver uma plataforma de gestão comercial composta por:

1. ERP administrativo.
2. Frente de caixa (PDV).
3. Cadastro de produtos.
4. Controle de estoque.
5. Compras e fornecedores.
6. Gestão de perdas e avarias.
7. Inventários.
8. Gestão de clientes.
9. Gestão de vendedores.
10. Gestão financeira.
11. Meios de pagamento.
12. Emissão fiscal.
13. Relatórios operacionais.
14. Indicadores gerenciais.
15. Dashboards.
16. Promoções e fidelização.
17. Gestão multiempresa.
18. Gestão multifilial.
19. Gestão SaaS.
20. Integrações comerciais.
21. Inteligência artificial.
22. Automações.
23. Auditoria e segurança.
24. Operação offline.
25. Aplicações e interfaces móveis complementares.

## 2.1. Público-alvo

O sistema deverá ser preparado para atender:

- Minimercados.
- Supermercados.
- Mercearias.
- Conveniências.
- Papelarias.
- Lojas de roupas.
- Lojas de calçados.
- Lojas de utilidades.
- Distribuidoras.
- Pet shops.
- Lojas de cosméticos.
- Lojas de eletrônicos.
- Materiais de construção.
- Comércios em geral.

Projetar um núcleo comercial genérico e funcionalidades específicas por segmento.

Funcionalidades específicas de setores sujeitos a regulamentações adicionais deverão ser tratadas por módulos especializados, após levantamento das obrigações próprias do setor.

---

# 3. ARQUITETURA TECNOLÓGICA

Utilizar preferencialmente a seguinte stack.

## 3.1. Backend

- Java em versão LTS estável e compatível.
- Spring Boot.
- Spring Security.
- Spring Data JPA.
- Hibernate.
- Bean Validation.
- Flyway.
- OpenAPI/Swagger.
- Maven.
- Testcontainers.
- JUnit.
- Mockito quando apropriado.
- MapStruct quando justificável.

Padrão arquitetural:

**Monólito modular, orientado a domínio, com separação clara entre módulos.**

Aplicar:

- SOLID.
- Clean Architecture quando trouxer benefícios reais.
- Organização por domínio.
- DTOs.
- Services.
- Repositories.
- Controllers.
- Tratamento centralizado de exceções.
- Validações.
- Controle de transações.
- Auditoria.
- Testabilidade.
- Interfaces para integrações externas.

Evitar acoplamentos desnecessários entre módulos.

## 3.2. Frontend

Utilizar:

- Angular em versão estável compatível.
- TypeScript.
- Angular Router.
- Angular HttpClient.
- Reactive Forms.
- Componentes reutilizáveis.
- Biblioteca de UI adequada a aplicações empresariais.
- Biblioteca de gráficos com suporte a dashboards.
- Gerenciamento de estado proporcional à complexidade.

Requisitos de interface:

- Visual profissional.
- Tema claro e escuro.
- Responsividade.
- Acessibilidade.
- Suporte a teclado.
- Tratamento de erros.
- Feedback de operações.
- Estados de carregamento.
- Componentes reutilizáveis.
- Formulários padronizados.
- Paginação e filtros.
- Tabelas com ordenação.
- Exportação de dados.
- Navegação conforme permissões.

## 3.3. Banco de dados

Utilizar PostgreSQL.

Implementar:

- Migrações versionadas.
- Chaves primárias.
- Chaves estrangeiras.
- Índices adequados.
- Constraints.
- Tipos monetários precisos.
- Controle de concorrência.
- Auditoria.
- Histórico.
- Isolamento por empresa.
- Integridade referencial.
- Estratégias de backup.
- Consultas otimizadas.
- Paginação.
- Prevenção de duplicidade.

Não utilizar ponto flutuante binário para valores monetários.

## 3.4. Infraestrutura

Utilizar quando necessário:

- Docker.
- Docker Compose.
- Nginx ou proxy equivalente.
- PostgreSQL.
- Redis.
- Mensageria.
- Armazenamento de arquivos.
- Centralização de logs.
- Monitoramento.
- CI/CD.

Não incluir Redis ou mensageria sem estabelecer a necessidade e a responsabilidade técnica de cada um.

## 3.5. Frente de caixa

Avaliar tecnicamente:

- Aplicação desktop.
- PWA.
- Aplicação híbrida.
- Navegador com agente local de hardware.

A decisão deverá considerar:

- Operação offline.
- Impressoras térmicas.
- Leitores.
- Balanças.
- Gavetas.
- TEF.
- Instalação.
- Atualizações.
- Desempenho.
- Segurança.
- Distribuição comercial.

Selecionar a abordagem mais adequada e justificar.

---

# 4. ARQUITETURA SAAS MULTIEMPRESA

O sistema deverá atender diversas empresas de forma segura.

Entidades organizacionais:

- Plataforma SaaS.
- Empresa assinante.
- Matriz.
- Filial.
- Depósito.
- Terminal.
- Operador.
- Usuário.
- Perfil.
- Plano contratado.

## 4.1. Requisitos

- Uma empresa não pode consultar informações de outra.
- Cada empresa possui seus próprios produtos, clientes, vendas e estoques.
- Uma empresa pode possuir várias filiais.
- Usuários podem possuir permissões em filiais específicas.
- Produtos podem ser compartilhados entre filiais da mesma empresa mediante regra configurável.
- Preços podem variar por filial.
- Estoque deve ser separado por localização.
- Configurações fiscais devem respeitar o estabelecimento emissor.
- Relatórios podem ser consolidados ou individuais.
- O sistema deve permitir expansão de usuários, filiais e terminais.

Definir isolamento por tenant utilizando uma estratégia segura e justificada.

Não confiar exclusivamente no frontend para restringir o acesso.

---

# 5. AUTENTICAÇÃO, USUÁRIOS E PERMISSÕES

Implementar:

- Login.
- Logout.
- Recuperação de senha.
- Alteração de senha.
- Controle de sessões.
- Autenticação multifator.
- Bloqueio por tentativas suspeitas.
- Registro de acessos.
- Revogação de sessões.
- Controle de dispositivos.
- Perfis de acesso.
- Permissões granulares.
- Auditoria de alterações.
- Expiração e renovação segura de credenciais.

## 5.1. Perfis iniciais

- Administrador da plataforma.
- Administrador da empresa.
- Gerente.
- Supervisor.
- Operador de caixa.
- Estoquista.
- Comprador.
- Financeiro.
- Vendedor.
- Contador.
- Auditor.

## 5.2. Exemplos de permissões

- Cadastrar produto.
- Alterar preço.
- Aplicar desconto.
- Cancelar venda.
- Estornar pagamento.
- Aprovar perda.
- Ajustar estoque.
- Transferir mercadorias.
- Visualizar custo.
- Visualizar lucro.
- Consultar relatórios financeiros.
- Aprovar compras.
- Fechar caixa.
- Configurar integração fiscal.
- Administrar usuários.

Permitir concessão de permissões por perfil, usuário, empresa e filial quando apropriado.

---

# 6. CADASTRO DE PRODUTOS

O sistema utilizará inicialmente **uma base própria de produtos**.

Não implementar integração externa de cadastro automático de produtos na primeira etapa.

O código de barras será utilizado para localizar produtos no banco interno.

## 6.1. Cadastro

Implementar:

- Código interno.
- SKU.
- Código de barras.
- Múltiplos códigos.
- Nome.
- Descrição.
- Descrição curta.
- Marca.
- Fabricante.
- Categoria.
- Subcategoria.
- Imagens.
- Unidade de medida.
- Peso.
- Dimensões.
- Embalagem.
- Preço de custo.
- Custo médio.
- Preço de venda.
- Margem.
- Markup.
- Estoque mínimo.
- Estoque máximo.
- Ponto de reposição.
- Fornecedores.
- Código do fornecedor.
- Classificação fiscal.
- Situação ativa/inativa.
- Controle de lote.
- Controle de validade.
- Controle de série.
- Localização.
- Produtos relacionados.
- Produtos substitutos.
- Observações.
- Histórico de alterações.

## 6.2. Código de barras

Fluxo obrigatório:

1. Operador realiza a leitura.
2. Sistema identifica o código recebido.
3. Consulta o banco interno.
4. Se encontrado, retorna o produto.
5. Se não encontrado, informa a ausência.
6. Usuário autorizado pode iniciar o cadastro com o código preenchido.
7. Produto é salvo na base.
8. Leituras futuras localizam o produto.

Implementar:

- EAN-8.
- EAN-13.
- UPC, quando utilizado.
- GTIN-14.
- Códigos internos.
- Códigos de balança conforme configuração.
- Múltiplos códigos por produto.
- Validação quando aplicável.
- Pesquisa por código parcial e descrição.
- Controle de duplicidade.
- Leitura por scanner USB.
- Leitura por dispositivo móvel quando disponível.

## 6.3. Importações

Permitir:

- Excel.
- CSV.
- XML de notas de compra.
- Cadastro em massa.
- Atualização em lote.
- Pré-visualização.
- Validação.
- Detecção de duplicidade.
- Relatório de erros.
- Correção antes de confirmar.
- Reversão controlada quando aplicável.

Não implementar importação que sobrescreva indiscriminadamente preços, estoque ou dados fiscais.

## 6.4. Tipos de produtos

Suportar:

- Produto simples.
- Produto com variações.
- Produto pesável.
- Produto fracionado.
- Produto composto.
- Kit.
- Combo.
- Produto com número de série.
- Produto com lote.
- Produto com validade.
- Serviço, quando aplicável.

## 6.5. Unidades e conversões

Exemplos:

- Unidade.
- Caixa.
- Pacote.
- Quilograma.
- Grama.
- Litro.
- Mililitro.
- Metro.
- Metro quadrado.

Permitir comprar em uma unidade e vender em outra, com fator de conversão definido e rastreável.

---

# 7. FRENTE DE CAIXA — PDV

O PDV será um dos principais módulos do sistema.

Deverá ser rápido, confiável e adequado ao uso com teclado e leitor.

## 7.1. Operações

Implementar:

- Abertura de caixa.
- Identificação do operador.
- Início de venda.
- Leitura de produtos.
- Busca manual.
- Inclusão de itens.
- Alteração de quantidade.
- Remoção de itens.
- Consulta de preço.
- Desconto por item.
- Desconto geral.
- Acréscimos.
- Promoções.
- Identificação do cliente.
- Venda sem identificação quando permitida.
- Venda suspensa.
- Recuperação de venda.
- Cancelamento.
- Finalização.
- Pagamento.
- Emissão fiscal.
- Impressão.
- Segunda via.
- Devolução.
- Troca.
- Histórico de vendas.
- Sangria.
- Suprimento.
- Fechamento.

## 7.2. Interface

A tela deve conter:

- Campo de leitura de código.
- Lista de itens.
- Quantidade.
- Valor unitário.
- Descontos.
- Subtotal.
- Total.
- Cliente.
- Operador.
- Terminal.
- Indicador de conectividade.
- Ações rápidas.
- Finalização.

Atalhos de teclado devem ser configuráveis.

Priorizar operação sem mouse para tarefas frequentes.

## 7.3. Regras da venda

- Registrar preço efetivo no momento da venda.
- Não alterar retroativamente itens de vendas concluídas.
- Preservar descontos aplicados.
- Validar quantidade.
- Respeitar permissões.
- Aplicar promoções corretamente.
- Evitar duplicidade de finalização.
- Tratar falhas de comunicação.
- Garantir coerência entre venda, pagamento e estoque.
- Registrar eventos de cancelamento e devolução.

Separar estado comercial, financeiro e fiscal da venda.

---

# 8. PAGAMENTOS MISTOS

**Funcionalidade obrigatória e prioritária.**

Permitir que uma única venda seja paga com quantos meios forem necessários, respeitando as regras de cada provedor.

## 8.1. Meios de pagamento

- Dinheiro.
- Pix.
- Cartão de crédito.
- Cartão de débito.
- Crédito parcelado.
- Vale-alimentação.
- Vale-refeição.
- Voucher.
- Vale-presente.
- Crediário.
- Crédito em loja.
- Outros meios configuráveis.

## 8.2. Exemplo

Venda: R$ 350,00.

- Dinheiro: R$ 100,00.
- Pix: R$ 150,00.
- Crédito: R$ 100,00.

A mesma venda deverá possuir três registros de pagamento.

## 8.3. Regras obrigatórias

- Aceitar N pagamentos por venda.
- Exibir total recebido.
- Exibir saldo pendente.
- Permitir pagamentos parciais.
- Permitir vários cartões.
- Permitir cartão parcelado.
- Calcular troco corretamente.
- Validar limites de cada meio.
- Registrar autorização.
- Registrar confirmação.
- Registrar recusa.
- Registrar cancelamento.
- Registrar estorno.
- Registrar taxas.
- Registrar adquirente.
- Registrar número de parcelas.
- Registrar identificadores externos.
- Permitir conciliação.
- Evitar cobranças duplicadas.
- Tratar confirmação assíncrona.
- Tratar falhas depois da autorização.
- Tratar indisponibilidade do provedor.

## 8.4. Estados de pagamento

Prever estados compatíveis com:

- Criado.
- Pendente.
- Em processamento.
- Autorizado.
- Confirmado.
- Recusado.
- Cancelado.
- Estornado.
- Parcialmente estornado.
- Expirado.

Definir transições válidas e operações permitidas por estado.

Não considerar valores pendentes como recebidos.

## 8.5. Conciliação

Implementar conciliação entre:

- Venda.
- Pagamento.
- Transação externa.
- Valor bruto.
- Taxa.
- Valor líquido.
- Data da venda.
- Data do recebimento.
- Conta financeira.
- Estornos.
- Divergências.

---

# 9. GESTÃO DE CAIXA

Implementar:

- Abertura.
- Fundo de troco.
- Operador responsável.
- Terminal.
- Turno.
- Movimentações.
- Sangrias.
- Suprimentos.
- Retiradas.
- Conferência.
- Fechamento.
- Divergências.
- Justificativas.
- Aprovação gerencial.
- Histórico.
- Relatórios.

## 9.1. Fechamento

Permitir comparar:

- Dinheiro esperado.
- Dinheiro contado.
- Pix confirmado.
- Cartão de crédito.
- Cartão de débito.
- Vouchers.
- Outros recebimentos.
- Sangrias.
- Suprimentos.
- Diferenças.

Implementar fechamento cego opcional.

---

# 10. ESTOQUE

Criar gestão profissional de estoque.

## 10.1. Operações

- Entrada.
- Saída.
- Reserva.
- Liberação de reserva.
- Transferência.
- Ajuste.
- Inventário.
- Perda.
- Devolução.
- Recebimento de compra.
- Consumo interno.
- Produção.
- Desmontagem de kit.
- Bloqueio de mercadoria.
- Estoque em trânsito.

## 10.2. Saldos

Controlar:

- Estoque físico.
- Estoque reservado.
- Estoque disponível.
- Estoque bloqueado.
- Estoque em trânsito.
- Estoque por filial.
- Estoque por depósito.
- Estoque por localização.
- Estoque por lote, quando aplicável.

## 10.3. Histórico

Cada movimentação deve registrar:

- Produto.
- Quantidade.
- Unidade.
- Tipo.
- Origem.
- Documento de referência.
- Filial.
- Depósito.
- Usuário.
- Data e horário.
- Custo aplicável.
- Justificativa.
- Identificador da operação.

Não permitir alteração manual silenciosa do saldo.

## 10.4. Reposição

Implementar:

- Estoque mínimo.
- Estoque máximo.
- Ponto de pedido.
- Sugestão de compra.
- Cobertura em dias.
- Giro.
- Média de vendas.
- Lead time do fornecedor.
- Estoque de segurança.
- Alertas de ruptura.
- Produtos parados.
- Excesso de estoque.

---

# 11. GESTÃO DE PERDAS

Criar módulo dedicado.

## 11.1. Tipos

- Vencimento.
- Avaria.
- Quebra.
- Furto.
- Extravio.
- Deterioração.
- Erro operacional.
- Perda em transporte.
- Perda em armazenamento.
- Quebra técnica.
- Outros motivos configuráveis.

Consumo interno e divergência de inventário deverão possuir classificação própria, ainda que compartilhem mecanismos de movimentação.

## 11.2. Registro

Campos:

- Produto.
- Quantidade.
- Unidade.
- Tipo de perda.
- Motivo detalhado.
- Lote.
- Validade.
- Localização.
- Data.
- Usuário.
- Valor de custo.
- Valor total.
- Fotos.
- Observações.
- Filial.
- Status.
- Aprovador.

## 11.3. Fluxo

1. Usuário identifica a perda.
2. Seleciona o produto.
3. Informa quantidade.
4. Seleciona motivo.
5. Informa evidências.
6. Sistema calcula custo.
7. Verifica necessidade de aprovação.
8. Após aprovação, registra movimentação.
9. Atualiza estoque.
10. Registra histórico.
11. Atualiza indicadores.

## 11.4. Controle

- Aprovação por alçada.
- Limite por valor.
- Limite por quantidade.
- Histórico de ações.
- Estorno rastreável.
- Proibição de exclusão silenciosa.
- Alertas de recorrência.
- Relatórios de perdas.

## 11.5. Indicadores

- Quantidade perdida.
- Valor perdido.
- Perda por motivo.
- Perda por categoria.
- Perda por produto.
- Perda por filial.
- Perda por período.
- Perda em relação ao faturamento.
- Perda em relação ao estoque.
- Tendência mensal.

---

# 12. INVENTÁRIO

Implementar:

- Inventário geral.
- Inventário parcial.
- Inventário rotativo.
- Inventário por localização.
- Inventário por categoria.
- Contagem por leitor.
- Contagem móvel.
- Contagem cega.
- Recontagem.
- Divergências.
- Aprovação.
- Ajuste.
- Relatório de acuracidade.
- Histórico.

Definir mecanismos para evitar inconsistências quando houver movimentações durante a contagem.

---

# 13. COMPRAS E FORNECEDORES

## 13.1. Fornecedores

- Cadastro.
- CNPJ.
- Contatos.
- Endereços.
- Condições comerciais.
- Prazos.
- Produtos fornecidos.
- Histórico de preços.
- Histórico de entregas.
- Avaliação de desempenho.

## 13.2. Compras

- Solicitação.
- Cotação.
- Comparação.
- Pedido.
- Aprovação.
- Recebimento parcial.
- Recebimento total.
- Conferência.
- Divergências.
- Devolução.
- Cancelamento.
- Histórico.

## 13.3. XML de NF-e

Permitir importar XML de notas de fornecedores.

Realizar:

- Leitura de itens.
- Identificação do fornecedor.
- Associação aos produtos internos.
- Cadastro assistido de produtos não encontrados.
- Conversão de embalagens.
- Validação de quantidades.
- Identificação de custos.
- Tratamento de impostos.
- Identificação de despesas adicionais.
- Conferência do documento.
- Entrada no estoque.
- Atualização de custos.
- Geração de contas a pagar.

Não permitir importar a mesma entrada indevidamente várias vezes.

---

# 14. PRECIFICAÇÃO

Implementar:

- Custo de aquisição.
- Custo médio.
- Preço de venda.
- Markup.
- Margem bruta.
- Margem mínima.
- Tabelas de preços.
- Preços por filial.
- Preço atacado.
- Preço por quantidade.
- Histórico de alterações.
- Agendamento de preços.
- Aprovação de alterações.
- Regras de arredondamento.
- Validação de margem negativa.

Definir custo e margem de maneira consistente em todo o sistema.

---

# 15. PROMOÇÕES

Implementar:

- Desconto percentual.
- Desconto fixo.
- Promoção por período.
- Leve X, pague Y.
- Desconto progressivo.
- Combo.
- Kit promocional.
- Cupom.
- Desconto por cliente.
- Desconto por categoria.
- Promoção por filial.
- Limite de utilização.
- Prioridade entre regras.
- Cumulatividade configurável.
- Auditoria.

---

# 16. CLIENTES E CRM

Implementar:

- Cadastro PF/PJ.
- CPF/CNPJ.
- Contatos.
- Endereços.
- Histórico de compras.
- Limite de crédito.
- Crediário.
- Grupos.
- Classificação.
- Fidelidade.
- Pontos.
- Cashback.
- Vale-presente.
- Cupons.
- Campanhas.
- Segmentação.
- Preferências de comunicação.
- Consentimento quando necessário.

Permitir clientes sem cadastro completo quando a operação e as exigências fiscais permitirem.

---

# 17. VENDEDORES E COMISSÕES

Implementar:

- Cadastro de vendedores.
- Metas.
- Comissão fixa.
- Comissão percentual.
- Comissão por produto.
- Comissão por categoria.
- Comissão por venda.
- Comissão por recebimento, quando contratualmente aplicável.
- Estornos.
- Devoluções.
- Relatórios.
- Aprovação.
- Histórico.

---

# 18. FINANCEIRO

Implementar módulo financeiro integrado às operações comerciais.

## 18.1. Contas a pagar

- Lançamento.
- Parcelamento.
- Vencimento.
- Categoria.
- Fornecedor.
- Centro de custo.
- Juros.
- Multas.
- Descontos.
- Pagamento parcial.
- Baixa.
- Estorno.
- Anexos.

## 18.2. Contas a receber

- Origem.
- Cliente.
- Venda.
- Parcelas.
- Vencimento.
- Recebimento.
- Baixa parcial.
- Juros.
- Multas.
- Inadimplência.
- Renegociação.
- Estorno.

## 18.3. Tesouraria

- Contas bancárias.
- Caixa físico.
- Transferências.
- Conciliação.
- Categorias.
- Plano de contas.
- Centros de custo.
- Fluxo de caixa.
- Saldos.

## 18.4. DRE gerencial

Demonstrar:

- Receita bruta.
- Deduções.
- Receita líquida.
- CMV.
- Lucro bruto.
- Despesas operacionais.
- Resultado operacional gerencial.

Não confundir fluxo de caixa com lucro.

---

# 19. FISCAL — INTEGRAÇÃO COM API EXTERNA

O sistema NÃO deverá implementar inicialmente integração fiscal própria diretamente com a SEFAZ.

Utilizar API especializada de terceiros.

Provedores candidatos:

- Focus NFe.
- Nuvem Fiscal.
- Outro provedor tecnicamente adequado.

A escolha deverá considerar documentação atual, cobertura, custo, homologação e requisitos da operação.

## 19.1. Arquitetura

Criar abstração:

FiscalProvider

Com operações para:

- Emitir documento.
- Consultar documento.
- Cancelar documento.
- Consultar situação.
- Processar eventos.
- Obter XML.
- Obter representação para impressão.
- Identificar rejeição.
- Consultar disponibilidade quando suportado.

## 19.2. Funcionalidades

- NFC-e.
- NF-e.
- Homologação.
- Produção.
- Configuração por estabelecimento.
- Credenciais.
- Certificados quando exigidos.
- Numeração.
- Séries.
- Emissão.
- Consulta.
- Cancelamento.
- Eventos.
- Rejeições.
- Contingência.
- XML.
- DANFE.
- Histórico.
- Logs.
- Reprocessamento.
- Armazenamento seguro.
- Conciliação de status fiscal.

## 19.3. Tributação

Contemplar informações fiscais aplicáveis:

- NCM.
- CEST.
- CFOP.
- CST.
- CSOSN.
- ICMS.
- PIS.
- Cofins.
- IPI quando aplicável.
- IBS.
- CBS.
- Demais informações necessárias ao documento e à operação.

Acompanhar regulamentações e leiautes vigentes.

Não criar regras tributárias com base em suposições.

Implementar validações sob responsabilidade do ERP, mesmo utilizando provedor externo.

## 19.4. Segurança fiscal

- Não armazenar certificados ou credenciais em texto aberto.
- Não expor credenciais no frontend.
- Registrar tentativas de emissão.
- Evitar duplicidade.
- Controlar estado fiscal.
- Tratar respostas assíncronas.
- Preservar documentos autorizados.
- Implementar políticas de retenção.
- Registrar eventos de cancelamento.

Criar um FiscalProvider de testes para homologação interna, sem confundir documentos simulados com documentos fiscais reais.

---

# 20. DEVOLUÇÕES, TROCAS E PÓS-VENDA

Implementar:

- Devolução total.
- Devolução parcial.
- Troca.
- Crédito em loja.
- Reembolso.
- Estorno.
- Histórico.
- Associação à venda original.
- Ajuste de estoque.
- Ajuste financeiro.
- Ajuste fiscal.
- Motivo.
- Autorização.
- Auditoria.

Tratar produtos devolvidos que não possam retornar ao estoque disponível.

---

# 21. PEDIDOS E ORÇAMENTOS

Implementar:

- Orçamento.
- Pedido.
- Conversão.
- Reserva.
- Pagamento antecipado.
- Sinal.
- Saldo pendente.
- Separação.
- Entrega.
- Retirada.
- Cancelamento.
- Histórico.
- Aprovação de desconto.

---

# 22. PRODUÇÃO E KITS

Criar módulo opcional para:

- Ficha técnica.
- Lista de materiais.
- Insumos.
- Ordem de produção.
- Produção concluída.
- Consumo de matéria-prima.
- Montagem.
- Desmontagem.
- Kits.
- Custos.
- Sobras.
- Perdas.
- Rendimento.
- Movimentações de estoque.

---

# 23. EQUIPAMENTOS COMERCIAIS

Planejar e implementar suporte conforme dispositivos homologados.

Equipamentos:

- Leitores USB.
- Leitores Bluetooth.
- Impressoras térmicas.
- Impressoras de etiquetas.
- Gavetas de dinheiro.
- Balanças.
- Etiquetadoras.
- POS.
- TEF.
- Coletores móveis.
- Displays de cliente.

Requisitos:

- Configuração por terminal.
- Teste de conexão.
- Tratamento de falhas.
- Drivers.
- Protocolos.
- Reimpressão.
- Diagnóstico.
- Logs.
- Atualização de componentes locais.

---

# 24. OPERAÇÃO OFFLINE

O PDV deverá possuir estratégia confiável de operação offline.

Implementar:

- Persistência local.
- Produtos disponíveis localmente.
- Preços sincronizados.
- Identificação do terminal.
- Fila de operações.
- IDs globais.
- Idempotência.
- Reenvio.
- Conciliação.
- Detecção de conflitos.
- Indicador de conexão.
- Atualização incremental.
- Recuperação após falhas.
- Monitoramento de terminais.
- Logs de sincronização.

Definir exatamente quais operações são permitidas offline.

Não presumir pagamentos eletrônicos offline ou emissão fiscal de contingência fora das capacidades homologadas.

---

# 25. DASHBOARD E KPIs

Criar dashboards reais, conectados ao banco de dados.

## 25.1. Vendas

- Faturamento.
- Faturamento líquido.
- Quantidade de vendas.
- Ticket médio.
- Produtos vendidos.
- Vendas por horário.
- Vendas por dia.
- Vendas por período.
- Vendas por operador.
- Vendas por filial.
- Vendas por categoria.
- Vendas por meio de pagamento.
- Descontos.
- Cancelamentos.
- Devoluções.

## 25.2. Estoque

- Valor do estoque.
- Giro.
- Cobertura.
- Rupturas.
- Estoque mínimo.
- Estoque parado.
- Excesso.
- Produtos próximos do vencimento.
- Acuracidade.
- Divergências.
- Perdas.

## 25.3. Financeiro

- Recebimentos.
- Despesas.
- Contas a pagar.
- Contas a receber.
- Inadimplência.
- Fluxo de caixa.
- Lucro bruto.
- Resultado operacional.
- Taxas de cartão.
- Conciliações.

## 25.4. Compras

- Total comprado.
- Custos.
- Fornecedores.
- Evolução de preços.
- Prazo de entrega.
- Divergências.
- Produtos a repor.

## 25.5. Clientes

- Novos clientes.
- Clientes recorrentes.
- Frequência.
- Ticket médio.
- Retenção.
- Clientes inativos.

## 25.6. Perdas

- Valor perdido.
- Quantidade.
- Motivos.
- Produtos.
- Filiais.
- Tendência.
- Perda sobre faturamento.

Cada KPI deverá possuir:

- Fórmula documentada.
- Fonte de dados.
- Regras de inclusão.
- Regras de exclusão.
- Filtros.
- Periodicidade.
- Critério de atualização.

---

# 26. RELATÓRIOS

Implementar:

- Vendas.
- Produtos mais vendidos.
- Produtos menos vendidos.
- Curva ABC.
- Lucratividade.
- Margens.
- Estoque valorizado.
- Movimentações.
- Inventário.
- Perdas.
- Compras.
- Fornecedores.
- Financeiro.
- DRE.
- Fluxo de caixa.
- Fechamento de caixa.
- Pagamentos.
- Conciliação.
- Clientes.
- Vendedores.
- Comissões.
- Promoções.
- Comparativo de filiais.
- Documentos fiscais.
- Auditoria.

Recursos:

- Filtros.
- Ordenação.
- Agrupamento.
- Exportação Excel.
- Exportação CSV.
- Exportação PDF.
- Relatórios agendados.
- Relatórios salvos.
- Permissões de visualização.

---

# 27. INTELIGÊNCIA ARTIFICIAL

Criar módulo separado do núcleo transacional.

Tecnologias candidatas:

- Python.
- FastAPI.
- APIs de modelos de linguagem.
- Function Calling.
- LangChain.
- LangGraph.
- RAG quando necessário.

## 27.1. Funcionalidades

- Previsão de demanda.
- Reposição inteligente.
- Análise de vendas.
- Identificação de produtos parados.
- Alertas de rupturas.
- Previsão de cobertura.
- Identificação de perdas anormais.
- Análise de lucratividade.
- Recomendações comerciais.
- Relatórios explicativos.
- Assistente gerencial.
- Consultas em linguagem natural.

## 27.2. Exemplo

Pergunta:

"Quais são os cinco produtos mais lucrativos deste mês e quais estão próximos de acabar?"

Fluxo:

1. Verificar autenticação.
2. Identificar tenant e filial.
3. Verificar permissões.
4. Interpretar solicitação.
5. Selecionar ferramentas autorizadas.
6. Executar consultas seguras.
7. Calcular indicadores.
8. Validar dados.
9. Gerar resposta.
10. Apresentar gráficos ou tabelas.

A IA não poderá executar SQL arbitrário sem políticas, validação e isolamento.

Não usar LLM como mecanismo de cálculo monetário.

Implementar limites de uso, auditoria, segurança, controle de custos e avaliação da qualidade das respostas.

---

# 28. NOTIFICAÇÕES E AUTOMAÇÕES

Implementar mecanismo de notificações para:

- Estoque baixo.
- Produto vencendo.
- Ruptura.
- Perdas elevadas.
- Compra pendente.
- Conta vencendo.
- Inadimplência.
- Divergência de caixa.
- Erro fiscal.
- Falha de sincronização.
- Queda de terminal.
- Relatórios agendados.

Canais possíveis:

- Notificação interna.
- E-mail.
- WhatsApp mediante integração e autorização.
- Push.
- Webhook.

Permitir configurar:

- Evento.
- Destinatário.
- Filial.
- Prioridade.
- Canal.
- Frequência.
- Limites.
- Horário.
- Histórico de envio.

---

# 29. SAAS — ADMINISTRAÇÃO DA PLATAFORMA

Criar painel para o proprietário do software.

Funcionalidades:

- Empresas cadastradas.
- Assinaturas.
- Planos.
- Cobranças.
- Testes gratuitos.
- Ativação.
- Suspensão.
- Cancelamento.
- Limite de usuários.
- Limite de filiais.
- Limite de terminais.
- Módulos por plano.
- Armazenamento.
- Uso de integrações.
- Métricas.
- Histórico.
- Suporte.
- Administração de contratos.

## 29.1. Planos

Projetar estrutura configurável, como:

- Básico.
- Profissional.
- Empresarial.

Não fixar preços definitivos sem estudo de custos.

Permitir:

- Upgrade.
- Downgrade.
- Renovação.
- Cancelamento.
- Período de teste.
- Cobrança recorrente.
- Gestão de inadimplência.
- Alteração de limites.

---

# 30. SUPORTE, ONBOARDING E IMPLANTAÇÃO

Implementar ou prever:

- Assistente de configuração inicial.
- Cadastro da empresa.
- Configuração de filiais.
- Cadastro de operadores.
- Configuração do caixa.
- Configuração de impressão.
- Configuração fiscal.
- Importação de produtos.
- Importação de clientes.
- Importação de fornecedores.
- Importação de estoque inicial.
- Diagnóstico do ambiente.
- Documentação para operadores.
- Guias administrativos.
- Logs de suporte.
- Ferramentas de diagnóstico.
- Exportação de dados.
- Migração de outros sistemas.

O processo de implantação deverá possuir verificações antes da entrada em produção.

---

# 31. SEGURANÇA E LGPD

Implementar:

- HTTPS.
- Autenticação segura.
- Autorização no backend.
- Isolamento de tenants.
- Controle de sessões.
- MFA.
- Proteção contra SQL Injection.
- Proteção contra XSS.
- Proteção contra CSRF quando aplicável.
- Proteção de segredos.
- Rate limiting.
- Criptografia apropriada.
- Segurança de APIs.
- Auditoria.
- Gestão de consentimento.
- Política de retenção.
- Exportação de dados.
- Tratamento de solicitações dos titulares.
- Backup seguro.
- Resposta a incidentes.
- Monitoramento de acessos suspeitos.

Evitar armazenar dados sensíveis desnecessários.

---

# 32. AUDITORIA

Criar mecanismo padronizado para registrar:

- Alteração de preço.
- Alteração de custo.
- Ajuste de estoque.
- Registro de perda.
- Aprovação de perda.
- Cancelamento.
- Desconto autorizado.
- Estorno.
- Alteração de fornecedor.
- Alteração financeira.
- Permissões.
- Configuração fiscal.
- Login administrativo.
- Operações SaaS.

Registrar:

- Usuário.
- Data.
- Empresa.
- Filial.
- Operação.
- Entidade.
- Identificador.
- Alterações relevantes.
- Origem.
- Resultado.

Não expor senhas, tokens ou dados sensíveis nos logs.

---

# 33. MODELAGEM DE BANCO DE DADOS

Criar o modelo completo, sem limitar a estrutura às entidades citadas a seguir.

## 33.1. Organização

- Empresa.
- Filial.
- Depósito.
- Terminal.
- Usuário.
- Perfil.
- Permissão.
- Usuário-filial.

## 33.2. Produtos

- Produto.
- Categoria.
- Marca.
- Código de barras.
- Embalagem.
- Unidade.
- Preço.
- Histórico de preço.
- Fornecedor-produto.
- Classificação tributária.

## 33.3. Estoque

- Saldo.
- Movimentação.
- Reserva.
- Transferência.
- Inventário.
- Contagem.
- Ajuste.
- Perda.
- Motivo de perda.
- Lote.
- Localização.

## 33.4. Compras

- Fornecedor.
- Pedido de compra.
- Item de compra.
- Recebimento.
- Item recebido.
- Documento de entrada.
- Devolução ao fornecedor.

## 33.5. Vendas

- Venda.
- Item de venda.
- Desconto.
- Pagamento.
- Transação financeira.
- Devolução.
- Item devolvido.
- Cliente.
- Vendedor.
- Comissão.

## 33.6. Caixa

- Sessão de caixa.
- Movimentação.
- Sangria.
- Suprimento.
- Conferência.
- Fechamento.

## 33.7. Financeiro

- Conta financeira.
- Conta a pagar.
- Conta a receber.
- Parcela.
- Categoria financeira.
- Centro de custo.
- Conciliação.
- Movimentação financeira.

## 33.8. Fiscal

- Configuração fiscal.
- Documento fiscal.
- Evento fiscal.
- Retorno do provedor.
- Registro de processamento.

## 33.9. SaaS

- Plano.
- Assinatura.
- Cobrança.
- Limite.
- Recurso contratado.
- Histórico da assinatura.

## 33.10. Infraestrutura

- Auditoria.
- Evento de domínio.
- Outbox.
- Sincronização.
- Integração.
- Notificação.
- Arquivo.
- Job.
- Configuração.

Para cada entidade, definir:

- Campos.
- Tipo de dados.
- Chave primária.
- Chaves estrangeiras.
- Índices.
- Constraints.
- Relacionamentos.
- Cardinalidade.
- Regras de unicidade.
- Política de exclusão.
- Estratégia de histórico.

Não criar uma única tabela genérica para armazenar processos diferentes quando isso prejudicar integridade e manutenção.

---

# 34. REGRAS CRÍTICAS DE CONSISTÊNCIA

Implementar soluções explícitas para:

1. Venda simultânea da última unidade.
2. Venda duplicada por retry.
3. Pagamento confirmado após falha do terminal.
4. Dois pagamentos concorrentes.
5. Venda com pagamento parcial.
6. Estorno de um pagamento misto.
7. Compra importada duas vezes.
8. Estoque alterado durante inventário.
9. Transferência entre depósitos.
10. Perda aprovada duas vezes.
11. Cancelamento após emissão fiscal.
12. Falha na emissão.
13. Reconexão de PDV offline.
14. Mudança de preço durante venda.
15. Produto alterado depois de venda histórica.
16. Falha de conciliação.
17. Restauração de backup.
18. Retentativa de evento assíncrono.
19. Alteração de permissão durante sessão.
20. Acesso indevido entre empresas.

Definir estratégias de transação, lock, idempotência, retry e compensação conforme cada cenário.

---

# 35. APIs

Criar APIs REST padronizadas.

Para cada endpoint:

- Método.
- URI.
- Objetivo.
- Permissão.
- Request.
- Response.
- Validação.
- Possíveis erros.
- Paginação.
- Filtros.
- Auditoria.
- Idempotência quando necessária.

Exigir:

- DTOs.
- Contratos versionados.
- OpenAPI.
- Erros padronizados.
- Validação.
- Autorização.
- Testes.

Não retornar entidades JPA diretamente como contrato público.

---

# 36. UX/UI E DESIGN SYSTEM

Criar design system próprio.

Definir:

- Cores.
- Tipografia.
- Espaçamentos.
- Botões.
- Campos.
- Tabelas.
- Modais.
- Cards.
- Gráficos.
- Estados.
- Alertas.
- Ícones.
- Navegação.
- Responsividade.
- Temas.

Telas administrativas devem priorizar produtividade.

Telas PDV devem priorizar velocidade.

Não criar interfaces inconsistentes entre módulos.

---

# 37. TESTES

Implementar testes:

- Unitários.
- Integração.
- API.
- Banco.
- Contrato.
- Frontend.
- End-to-end.
- Concorrência.
- Carga.
- Segurança.
- Permissões.
- Multiempresa.
- Pagamentos.
- Estoque.
- Fiscal.
- Offline.
- Sincronização.
- Backup e restauração.

Utilizar Testcontainers quando apropriado para testar PostgreSQL e dependências reais.

Não depender exclusivamente de mocks em funcionalidades críticas.

---

# 38. DEVOPS E INFRAESTRUTURA

Preparar:

- Dockerfile.
- Docker Compose.
- Configuração local.
- Configuração de homologação.
- Configuração de produção.
- Variáveis de ambiente.
- Gestão de segredos.
- Migrações.
- Health checks.
- Logs.
- Métricas.
- Monitoramento.
- Backups.
- Testes de restauração.
- CI/CD.
- Rollback.
- Documentação de deploy.
- Gestão de certificados.
- Atualizações do PDV.

Estabelecer metas de disponibilidade e recuperação com base nas necessidades comerciais.

---

# 39. ORGANIZAÇÃO DO REPOSITÓRIO

Organizar o projeto de maneira profissional.

Estrutura inicial sugerida:

    erp-comercial/
    ├── backend/
    ├── frontend/
    ├── pdv/
    ├── mobile/
    ├── ai-service/
    ├── infrastructure/
    ├── documentation/
    ├── scripts/
    ├── tests/
    ├── docker-compose.yml
    └── README.md

A estrutura poderá ser ajustada mediante justificativa técnica.

No backend, organizar por módulos de domínio.

Exemplos:

    auth/
    tenant/
    empresa/
    filial/
    usuario/
    produto/
    preco/
    estoque/
    perda/
    inventario/
    fornecedor/
    compra/
    cliente/
    venda/
    pagamento/
    caixa/
    fiscal/
    financeiro/
    relatorio/
    promocao/
    auditoria/
    integracao/
    notificacao/
    saas/

---

# 40. DOCUMENTAÇÃO OBRIGATÓRIA

Manter:

- README.md
- ARCHITECTURE.md
- REQUIREMENTS.md
- DATABASE.md
- API.md
- ROADMAP.md
- PROGRESS.md
- DECISIONS.md
- TESTING.md
- INTEGRATIONS.md
- DEPLOYMENT.md
- SECURITY.md

Atualizar a documentação conforme a implementação evoluir.

---

# 41. CRITÉRIOS DE CONCLUSÃO

Uma funcionalidade somente poderá ser marcada como concluída quando:

1. Estiver implementada.
2. Persistir dados corretamente.
3. Estiver integrada às camadas necessárias.
4. Aplicar permissões.
5. Validar dados.
6. Tratar erros.
7. Possuir testes adequados.
8. Não comprometer outros módulos.
9. Possuir auditoria quando necessária.
10. Possuir documentação.
11. Ter sido executada e verificada no ambiente disponível.

Não confundir código escrito com funcionalidade homologada.

Não confundir ambiente de testes com produção.

---

# 42. PLANEJAMENTO E ROADMAP

Organizar o desenvolvimento em fases.

## Fase 1 — Fundação

- Repositório.
- Infraestrutura.
- Banco.
- Migrações.
- Autenticação.
- Autorização.
- Multiempresa.
- Filiais.
- Usuários.
- Auditoria básica.
- Design system.

## Fase 2 — Produtos e estoque

- Produtos.
- Códigos de barras.
- Categorias.
- Unidades.
- Preços.
- Estoque.
- Movimentações.
- Perdas.
- Inventário.
- Transferências.

## Fase 3 — Compras

- Fornecedores.
- Cotações.
- Pedidos.
- Recebimentos.
- XML.
- Custos.
- Contas a pagar.

## Fase 4 — PDV

- Terminais.
- Sessões de caixa.
- Interface de vendas.
- Leitura de produtos.
- Descontos.
- Promoções.
- Venda suspensa.
- Cancelamentos.
- Fechamento de caixa.

## Fase 5 — Pagamentos

- Pagamentos mistos.
- Dinheiro.
- Pix.
- Cartões.
- Parcelamentos.
- Integrações.
- Estornos.
- Conciliação.

## Fase 6 — Fiscal

- FiscalProvider.
- Configuração.
- NFC-e.
- NF-e.
- Homologação.
- Cancelamentos.
- Eventos.
- XML.
- DANFE.
- Rejeições.
- Contingência.

## Fase 7 — Financeiro

- Contas a pagar.
- Contas a receber.
- Tesouraria.
- Conciliação.
- Fluxo de caixa.
- DRE.

## Fase 8 — Gestão comercial

- Clientes.
- Vendedores.
- Comissões.
- Promoções avançadas.
- Fidelidade.
- Orçamentos.
- Pedidos.
- Devoluções.

## Fase 9 — BI

- Dashboard.
- KPIs.
- Relatórios.
- Exportações.
- Agendamentos.
- Comparações.

## Fase 10 — Offline e hardware

- Persistência local.
- Sincronização.
- Periféricos.
- Impressão.
- Balanças.
- Diagnóstico.
- Contingência operacional.

Os elementos dessa fase que forem pré-requisitos para operar o PDV em produção deverão ser antecipados.

## Fase 11 — SaaS

- Planos.
- Assinaturas.
- Cobrança.
- Limites.
- Painel da plataforma.
- Onboarding.

Os mecanismos fundamentais de isolamento multiempresa já deverão existir desde a Fase 1.

## Fase 12 — IA

- Análises.
- Previsões.
- Alertas.
- Assistente gerencial.
- Automações inteligentes.

## Fase 13 — Homologação e preparação comercial

- Testes integrados.
- Testes de carga.
- Testes de segurança.
- Verificação fiscal.
- Verificação de pagamentos.
- Testes em equipamentos reais.
- Treinamento.
- Backup.
- Restauração.
- Implantação piloto.
- Correções.
- Preparação para comercialização.

Essas fases são uma organização de trabalho, não uma limitação do escopo.

---

# 43. MODO DE EXECUÇÃO DA IA

Você deverá atuar como agente de desenvolvimento, e não somente como consultor.

## 43.1. Primeira execução

1. Verifique o repositório.
2. Analise arquivos existentes.
3. Identifique dependências.
4. Defina arquitetura.
5. Crie estrutura inicial.
6. Configure projetos.
7. Configure banco.
8. Configure migrações.
9. Implemente segurança e multiempresa.
10. Execute verificações.
11. Atualize a documentação.
12. Registre o progresso.

## 43.2. Desenvolvimento contínuo

Em cada etapa:

1. Consulte requisitos.
2. Consulte decisões arquiteturais.
3. Identifique dependências.
4. Implemente backend.
5. Implemente persistência.
6. Implemente validações.
7. Implemente frontend.
8. Integre componentes.
9. Crie testes.
10. Execute testes.
11. Corrija defeitos.
12. Atualize documentação.
13. Atualize progresso.
14. Continue com a próxima tarefa viável.

Não reescrever módulos estáveis sem necessidade.

Não duplicar funcionalidades existentes.

Não substituir implementação anterior por código superficial.

## 43.3. Controle de progresso

No arquivo PROGRESS.md, registrar:

- Módulo.
- Funcionalidade.
- Estado.
- Arquivos criados.
- Dependências.
- Testes executados.
- Testes pendentes.
- Bloqueios.
- Próxima atividade.

Estados possíveis:

- NÃO INICIADO
- EM DESENVOLVIMENTO
- IMPLEMENTADO
- TESTADO
- INTEGRADO
- HOMOLOGADO
- BLOQUEADO

## 43.4. Dependências externas

Quando precisar de credenciais ou contratos externos:

- Implementar as interfaces.
- Implementar adaptadores.
- Criar testes com respostas simuladas.
- Documentar configuração.
- Identificar pendência.
- Manter integração desabilitada em produção até homologação.
- Continuar atividades independentes.

Não inventar credenciais.

Não afirmar que uma operação externa funcionou sem evidência.

---

# 44. REVISÃO DE LACUNAS

Antes e durante o desenvolvimento, verifique se existem requisitos importantes não descritos.

Avalie especialmente:

- Problemas operacionais reais.
- Equipamentos de varejo.
- Tributação brasileira.
- Pagamentos.
- Conciliação.
- Perdas.
- Divergências.
- Produtos fracionados.
- Validade.
- Controle de lotes.
- Promoções.
- Compras.
- Devoluções.
- Garantias.
- Offline.
- Multiempresa.
- Multifilial.
- Segurança.
- Dados pessoais.
- Auditoria.
- Backups.
- Suporte.
- Integrações.
- Custos de operação.
- Desempenho.
- Atualizações.
- Migração de dados.
- Experiência do operador.
- Regras particulares de segmentos.
- Requisitos regulatórios.
- Contratos SaaS.
- Encerramento de contas.
- Portabilidade de dados.

Inclua requisitos adicionais relevantes no planejamento e na implementação.

Quando existir incerteza legal, técnica ou de negócio, documente a premissa e a necessidade de validação.

---

# 45. REGRAS FINAIS E INEGOCIÁVEIS

1. Desenvolver um ERP completo, não apenas um MVP.
2. Manter o escopo integral.
3. Implementar funcionalidades reais.
4. Criar código profissional.
5. Utilizar arquitetura modular.
6. Evitar complexidade gratuita.
7. Respeitar segurança e isolamento de dados.
8. Garantir consistência financeira.
9. Garantir rastreabilidade do estoque.
10. Implementar pagamentos mistos corretamente.
11. Implementar perdas de estoque.
12. Utilizar cadastro interno de produtos inicialmente.
13. Utilizar provedor fiscal externo.
14. Implementar dashboards com dados reais.
15. Preparar integração com equipamentos.
16. Implementar estratégia offline.
17. Criar testes automatizados.
18. Registrar decisões técnicas.
19. Documentar limitações e dependências.
20. Não declarar algo pronto sem validação.
21. Não omitir cenários de erro.
22. Não criar telas desconectadas do backend.
23. Não entregar apenas estruturas vazias.
24. Não usar dados fictícios em produção.
25. Não realizar operações financeiras críticas sem controle transacional.
26. Não armazenar dados sensíveis indevidamente.
27. Não inventar resultados de homologação.
28. Não comprometer funcionalidades existentes.
29. Não substituir regras determinísticas por respostas de IA.
30. Priorizar robustez, segurança, usabilidade e manutenção.

---

# 46. COMANDO DE INÍCIO

A partir deste momento, assuma a responsabilidade técnica pelo desenvolvimento integral deste ERP.

O projeto deverá ser construído como um software comercial profissional, utilizando os requisitos deste documento como escopo principal.

Primeiro, realize a análise arquitetural e prepare o ambiente.

Em seguida, implemente a fundação e continue pelos módulos conforme suas dependências.

**Não encerre o trabalho após criar documentação.**

Crie arquivos reais, código executável, migrations, APIs, componentes, telas integradas e testes.

Sempre que o ambiente permitir, execute compilação, testes e verificações.

Mantenha progresso persistente no repositório para permitir que outras sessões de desenvolvimento continuem exatamente de onde o projeto parou.

Se a execução atual atingir limites de contexto, tempo ou ferramentas:

1. Preserve todos os arquivos produzidos.
2. Atualize PROGRESS.md.
3. Registre tarefas concluídas e pendentes.
4. Registre bloqueios.
5. Descreva o próximo passo executável.
6. Não afirme que o projeto inteiro foi concluído.

O código deverá permanecer organizado, compilável e verificável a cada marco de desenvolvimento.

**INICIE AGORA A IMPLEMENTAÇÃO DO ERP COMERCIAL COMPLETO.**