# Fechamento do núcleo básico — 11/11/2026

Escopo atual: vendas, caixa/PDV, entradas e saídas de estoque e emissão fiscal. Os demais blocos avançados não são prioridade deste fechamento.

| Frente | Critério para fechar | Estado |
|---|---|---|
| Vendas/caixa/estoque | Regressão integrada, precisão, concorrência, rollback e permissões | Código existente; validação atual pendente |
| Banco/migrações | Instalação nova e upgrade V26→versão atual em ambiente descartável | Autorizado banco descartável; regressão em andamento |
| Fiscal | Configuração por filial, dados fiscais, provedor, emissão/consulta/cancelamento e documentos | Em implementação |
| Fluxo completo | Compra→estoque→venda→pagamento→documento fiscal e reversões | Pendente de integração/homologação |

Decisão do usuário: módulo próprio, integração direta com SEFAZ, Goiás, NF-e e NFC-e. Testes com banco descartável autorizados. Essa decisão substitui a exigência original de API fiscal intermediária. Não se presume autorização para aplicar migrações no banco operacional ou emitir documento de produção. Credenciais/certificados reais devem ser fornecidos por configuração segura; não inserir dados fictícios no produto. Autorização de produção externa e homologação não serão declaradas por compilação.
