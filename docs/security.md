# Segurança

## Visão Geral

Este documento descreve o modelo IAM, a abordagem de gestão de segredos, a segurança de rede e os princípios de menor privilégio aplicados à plataforma mini-ecommerce.

---

## Modelo IAM

A plataforma utiliza dois utilizadores IAM dedicados — um por serviço que necessita de credenciais AWS. Nenhum conjunto de credenciais tem acesso simultâneo às operações de produtor e consumidor da SQS.

### Utilizadores IAM

| Utilizador IAM | Usado por | Permissões |
|---|---|---|
| `order-service-user` | order-service | SQS `SendMessage`, `GetQueueUrl` na fila `order-created` |
| `notification-service-user` | notification-service | SQS `ReceiveMessage`, `DeleteMessage`, `GetQueueAttributes` na fila `order-created`; SES `SendEmail` |

### Políticas de Menor Privilégio

**order-service-user** — apenas publicar, sem leitura nem eliminação:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SQSPublish",
      "Effect": "Allow",
      "Action": [
        "sqs:SendMessage",
        "sqs:GetQueueUrl"
      ],
      "Resource": "arn:aws:sqs:eu-central-1:<account-id>:order-created"
    }
  ]
}
```

**notification-service-user** — consumir e enviar email, sem escrita na fila:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "SQSConsume",
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:eu-central-1:<account-id>:order-created"
    },
    {
      "Sid": "SESSend",
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    }
  ]
}
```

Nenhum dos utilizadores tem acesso a RDS, EC2, VPC ou qualquer outro serviço AWS.

### Acesso ao RDS

O RDS é acedido com credenciais de username/password (`DB_USERNAME` / `DB_PASSWORD`). Não existem políticas de autenticação IAM na base de dados na configuração atual. O catalog-service e o order-service partilham as mesmas credenciais de base de dados — considera separá-las por serviço para um isolamento mais estrito.

---

## Segurança de Rede

### Estrutura da VPC

```
Subnets Públicas  (10.0.1.0/24, 10.0.2.0/24)
  └── EC2: api-gateway    ← único componente exposto à internet

Subnets Privadas (10.0.10.0/24, 10.0.20.0/24)
  ├── EC2: catalog-service
  ├── EC2: order-service
  ├── EC2: notification-service
  └── RDS PostgreSQL
```

### Security Groups

| Security Group | Tráfego de entrada | Tráfego de saída |
|---|---|---|
| `web_sg` (serviços EC2) | Portas 8080–8083 do CIDR da VPC | Tudo (para alcançar RDS, SQS, SES) |
| `db_sg` (RDS) | Porta 5432 apenas do `web_sg` | Nenhum |

O RDS nunca é acessível a partir da internet pública. Apenas as instâncias EC2 no `web_sg` podem ligar na porta 5432.

---

## Gestão de Segredos

### Abordagem Atual

Os segredos são passados aos containers como variáveis de ambiente, a partir de um ficheiro `.env` na instância EC2 em tempo de execução.

| Segredo | Onde está guardado |
|-----|------------------------|
| `DB_USERNAME` / `DB_PASSWORD` | Ficheiro `.env` na instância EC2 (não no repositório) |
| `ORDER_AWS_ACCESS_KEY_ID/SECRET` | Ficheiro `.env` na instância EC2 |
| `NOTIFICATION_AWS_ACCESS_KEY_ID/SECRET` | Ficheiro `.env` na instância EC2 |

### O que nunca deve ser commitado para o git

- Ficheiros `.env`
- `terraform.tfvars` (contém `db_password`)
- Qualquer ficheiro com access keys ou segredos AWS

Adiciona o seguinte ao `.gitignore`:

```
.env
*.tfvars
*.tfvars.json
```

### Melhorias Recomendadas (futuro)

Para um sistema em produção, considera migrar os segredos para o **AWS Secrets Manager** ou **SSM Parameter Store** e usar instance roles de EC2 em vez de access keys de longa duração. Isto elimina completamente a necessidade de credenciais armazenadas em disco.

---

## Dead-Letter Queue da SQS

A fila `order-created` tem uma DLQ (`order-created-dlq`) configurada com `maxReceiveCount = 5`. As mensagens que falham o processamento 5 vezes são movidas automaticamente para a DLQ. Monitoriza a DLQ para notificações falhadas e configura alertas para qualquer mensagem que aí apareça.

---

## SES Sandbox

Na configuração atual, o AWS SES opera em modo sandbox. Isto significa:
- Apenas endereços de envio **verificados** podem enviar email (`AWS_SES_FROM` tem de estar verificado em `eu-central-1`).
- Apenas endereços de destinatário **verificados** recebem email.

Para enviar para utilizadores reais, solicita acesso de produção através da consola AWS SES.

---

## Checklist para um Novo Deploy

- [ ] Utilizadores IAM criados com as políticas de menor privilégio acima
- [ ] Sem permissões de admin ou `*:*` atribuídas aos utilizadores IAM dos serviços
- [ ] Ficheiro `.env` presente nas instâncias EC2, fora do controlo de versão
- [ ] `terraform.tfvars` não commitado para o git
- [ ] RDS não acessível publicamente (confirmado na consola AWS)
- [ ] Endereço de envio SES verificado
- [ ] DLQ monitorizada para mensagens falhadas
