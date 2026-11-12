# Decisões arquiteturais — 14/10/2026

1. Monólito modular reduz custo operacional inicial e preserva transações locais. Escopo integral permanece no documento original.
2. Java 21 já instalado. Spring Boot 3.5.16 escolhido pela compatibilidade com Spring Security 6/Jackson 2 e Springdoc 2; planejar atualização para Boot 4 e revisar janela de suporte antes de implantação comercial. Fonte: https://docs.spring.io/spring-boot/3.5/system-requirements.html.
3. Angular 21 com TypeScript 5.9; Node disponível 20.19 compatível. Lockfile fixa resolução. Fonte: https://angular.dev/reference/versions.
4. Tenant derivado exclusivamente da sessão. Headers/IDs fornecidos pelo cliente não selecionam a empresa. Repositories só disponibilizam consultas delimitadas.
5. Sessões opacas permitem revogação imediata; evitar JWT com permissões obsoletas. Senhas BCrypt custo 12. Limite inicial ASCII 12–72 caracteres evita truncamento de 72 bytes; política Unicode deverá ser revisada com troca de encoder.
6. Provisionamento técnico requer segredo de 32+ caracteres e é negado no proxy público. Sem cadastro público de empresas e sem conta administrativa padrão.
7. PostgreSQL é a fonte de verdade. H2 só acelera testes; validação PostgreSQL/Testcontainers é requisito separado.
8. PDV PWA + agente local é a opção inicial, sujeita à validação de requisitos de hardware. Não foi implementado neste marco.
9. GTIN armazenado com código original e chave canônica preenchida à esquerda até 14 dígitos. Algoritmo Mod-10 conforme GS1: https://www.gs1.org/services/how-calculate-check-digit-manually; representação: https://www.gs1.org/edi-xml/technical-user-guide/Item_Numbers. Códigos internos usam namespace separado. Validação de dígito não comprova titularidade/licença GS1.
10. Dinheiro trafega como string decimal entre API e Angular; backend usa BigDecimal e PostgreSQL NUMERIC. Frontend formata agrupamento/casas como texto, sem cálculo binário.
11. Estoque inicial usa lock pessimista do produto (tenant/id), reduzindo risco de criação concorrente de saldo e transferências. Serialização por produto em todos os depósitos é um compromisso inicial de throughput; medir carga antes de otimizar para locks mais granulares.
12. Custo médio ponderado por depósito, seis casas HALF_UP; valor estimado da perda e custo efetivo da aprovação preservados separadamente. Saídas não aceitam custo fornecido pelo cliente.
13. Todas as perdas exigem aprovação nesta etapa. Reservas e alçadas futuras não são simuladas: saldo revalidado na aprovação e insuficiência deixa perda pendente.
14. Comandos de estoque possuem chave UUID/fingerprint e replay transacional. Frontend conserva tentativa incerta, bloqueia edição/saída e repete exatamente o payload. Persistência durável de tentativas ainda pertence à fase offline.

## Tesouraria interna — 14/10/2026

Serializar escritas de tesouraria por tenant para garantir reenvio único e transferências atômicas. Aceitar a redução de paralelismo nesta fase; medir carga antes de substituir por locks de contas/identificadores. Extrato ordenado por sequência por conta. Não permitir saldo negativo. AP e tesouraria permanecem separados até vínculo transacional explícito; operações internas não disparam banco.

## Pagamentos vinculados — V12

Vínculo opcional e explícito à conta para manter pagamentos externos e legado. Sem reprocessamento retroativo. Fingerprint legado preservado quando accountId é nulo; vínculo informado usa namespace próprio. Operação de tesouraria recebe UUID interno e origem exclusiva; clientes não escolhem esse vínculo. Pagamento e estorno obtêm lock do tenant antes do título, compatível com movimentos manuais. Conta central em outra filial exige acesso às duas. Saldo negativo ou overflow de estorno rejeitam integralmente o comando.

## Clientes e recebíveis — 14/10/2026

Cadastro compartilhado no tenant, títulos manuais por filial e snapshot do nome do cliente. Recebimento e estorno podem ser externos ou vinculados à tesouraria. A rotina comum usa direção de crédito/débito e origens distintas para AP/AR, sem trocar regras dos pagamentos existentes. Estorno de AR não cria saldo negativo; rejeita quando fundos foram consumidos. Migração Java V14 trata nome automático de check diferente entre bancos, com schema atual e exatamente uma correspondência. Limite estrutural de permissões aumentado de 30 para 100, mantendo whitelist de 31 e restrições do gestor.

## Fluxo de caixa — 14/10/2026

Separar realizado de pendente e de registros sem vínculo. Saldo atual é snapshot das contas; abertura/fechamento históricos derivam do ledger. Transferências possuem colunas próprias para não inflar entradas/saídas externas. Previsão usa apenas saldo pendente atual por vencimento, sem reconstruir posição histórica de dívida nem somar vencidos novamente. Valores BigDecimal/string. REPEATABLE_READ mantém agregados na mesma visão. Exigir cashflow e três leituras financeiras para evitar bypass de permissões existentes.

## Conciliação — 14/10/2026

CSV estruturado e entrada manual, sem conexão bancária ou criação implícita de movimento. Deduplicar linha por identificador estável na conta e conteúdo normalizado; arquivos sobrepostos reutilizam linhas. Conflito de conteúdo rejeita lote inteiro. Exigir valor e sinal exatos, confirmação humana, justificativa e versão. Sugestões por igualdade e ±7 dias de registro; data não é imposição para confirmação manual fora da janela. Reset libera vínculo sem apagar história. Serialização pelo tenant mantém exclusividade e reenvio único. Componente Angular sob demanda preserva orçamento do bundle inicial.

## Núcleo de caixa — 14/10/2026

Conta CASH dedicada por terminal para que o esperado venha de um único ledger. Abertura é conferência, sem crédito fictício. Suprimento/sangria atualizam sessão e tesouraria atomicamente; outra conta torna o registro transferência interna. Reserva impede alterações externas durante o turno, inclusive AP/AR. Fechamento congela esperado/contado/diferença e libera reserva; divergência não cria ajuste financeiro implícito. Operador é usuário autenticado; supervisão é privilégio separado, sem substituir permissões funcionais. Reenvio revalida escopo da conta contraparte.

## Vendas com itens — 14/10/2026

V19: venda atômica em dinheiro sem rascunho persistido; preço servidor conferido por expectedUnitPrice; HALF_UP por item, snapshot histórico; estoque CONSUMPTION namespace POS_SALE; tesouraria crédito líquido total com troco registrado. Depósito só muda com caixa fechado. Pagamentos mistos/cancelamento/fiscal continuam pendentes.

## Pagamentos e cancelamento — 15/10/2026

V20: pagamentos mistos inicialmente conferidos manualmente (dinheiro/Pix/transferência); soma exata e troco só em dinheiro. Cancelamento integral restrito ao turno original aberto, com refund declarado e retorno ao custo de saída. Provedores e devoluções parciais/posteriores seguem pendentes.

## Devoluções — V21 (15/10/2026)

V21: devolução parcial usa diferença entre arredondamentos acumulados proporcionais ao subtotal histórico. Pode reembolsar em outro turno/terminal da mesma filial; dinheiro usa gaveta atual e banco usa conta original. Cancelamento integral bloqueado após qualquer devolução. Cross-filial, quarentena e fiscal pendentes.

## Descontos e suspensão — V22 (15/10/2026)

V22: desconto em reais por subtotal do item; limite percentual por item, padrão 0%. Acima dele, outro usuário aprova o carrinho exato por 30 minutos. Política/revogação atual invalida autorização. Suspensão sem reserva, com salvamento versionado explícito antes de concluir após edição. Ver POS_COMMERCIAL.md.

## Parcelamento financeiro — 16/10/2026

Plano manual gera títulos com o fluxo financeiro já existente. Valores divididos em centavos inteiros; resto distribuído nas primeiras parcelas. Calendário ancorado no primeiro vencimento evita deriva após fevereiro. Parcelas e plano são atômicos, com replay retornando estados atuais. Sem antecipar autorização de crédito/venda a prazo, juros, multas ou renegociação. Contrato em RECEIVABLE_PLANS.md.

## Crediário — 16/10/2026

Dívida separada dos pagamentos já recebidos. Limite global por cliente inclui títulos manuais e atraso; negação genérica preserva sigilo entre filiais. Gestão de limite exige todas as filiais e permissão separada. Cancelamento exige todos os recebimentos estornados; devolução parcial de crediário bloqueada até haver alocação conjunta de dívida/reembolsos. Sem juros/multas ou consulta bancária automática. Ver POS_CREDIT.md.

## Devoluções do crediário — 16/10/2026

Abater dívida primeiro, por parcelas com vencimento mais distante; excedente exige fontes explícitas de reembolso. Original do título/recibo imutável; reembolso de recibo reduz recebido líquido e obrigação pelo mesmo valor. Não reabre dívida de mercadoria devolvida. Recibo parcialmente reembolsado não pode ser estornado integralmente. Divisão dívida/reembolso conferida pelo operador e revalidada no commit. Anulação/compensação de devolução fica pendente. Contrato POS_CREDIT_RETURNS.md.
