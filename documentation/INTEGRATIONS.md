# Integrações

PostgreSQL: único serviço externo necessário à fundação. Docker Engine deve estar ativo para Compose e Testcontainers. Nenhum certificado ou credencial de produção foi utilizado.

Fiscal: provedor especializado externo conforme REQUIREMENTS; seleção entre Focus/Nuvem Fiscal/outro exige comparação atual de documentação, custo e cobertura. Necessários contrato, credenciais de homologação/produção, certificado quando aplicável, inscrição e configuração por estabelecimento, revisão contábil dos leiautes vigentes. Nenhum adaptador implementado/homologado neste marco.

Pix/cartões/TEF: contrato e credenciais, assinatura/autenticidade de webhook, idempotência por cobrança, correlação do terminal, reconciliação e estorno parcial. Não tratar aprovação simulada como recebimento real.

Hardware: modelo/driver/protocolo de cada impressora, balança, gaveta e terminal precisa de validação real. Scanner USB em modo teclado será atendido pelo PDV futuro.

E-mail/WhatsApp/push: provedor e autorização dos destinatários. IA: escolha de provedor, privacidade, isolamento, orçamento e avaliações. Integrações devem permanecer desabilitadas até implementação e homologação individual.

## Vendas com itens — 14/10/2026

V19: integração interna vendas/estoque/tesouraria em dinheiro concluída. Não existe execução PIX/cartão/TEF/fiscal; próximos marcos mantidos em ROADMAP.

## Pagamentos e cancelamento — 15/10/2026

V20: Pix/transferência na venda são registros de recebimentos e reembolsos já conferidos manualmente. Não existe conexão bancária/cobrança/estorno remoto. Cartão, provedores, webhooks e homologação continuam pendentes.

## Devoluções — V21 (15/10/2026)

V21: reembolsos de devolução bancários continuam sendo registros de operações externas já conferidas, sem chamada ao banco/provedor. Comprovante único protege repetição declarada; não comprova autenticidade bancária. Cartões e fiscal pendentes.

## Descontos e suspensão — V22 (15/10/2026)

V22: descontos/autorização e suspensão são operações internas; não executam pagamentos, cobranças ou chamadas fiscais. Provedores/cartões/fiscal e offline continuam pendentes.
