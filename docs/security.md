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
| `notification-service-user` | notification-service | SQS `ReceiveMessage`, `DeleteMessage`, `GetQueueAttributes` na fila `order-created` |

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

**notification-service-user** — consumir da fila, sem escrita:

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
    }
  ]
}
```

Nenhum dos utilizadores tem acesso a RDS, EC2, VPC, ECR ou qualquer outro serviço AWS fora do necessário.

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
| `web_sg` (serviços EC2) | Portas 8080–8083 do CIDR da VPC | Tudo (para alcançar RDS, SQS, ECR) |
| `db_sg` (RDS) | Porta 5432 apenas do `web_sg` | Nenhum |

O RDS nunca é acessível a partir da internet pública. Apenas as instâncias EC2 no `web_sg` podem ligar na porta 5432.

---

## Gestão de Segredos

### Segredos no Pipeline CI/CD (GitHub Actions)

Todas as credenciais sensíveis são armazenadas como **segredos encriptados do GitHub Actions** e nunca aparecem nos logs do pipeline. O pipeline injeta-os como variáveis de ambiente nos jobs relevantes.

| Segredo GitHub | Usado em | Propósito |
|---|---|---|
| `AWS_ACCOUNT_ID` | build-and-push | Construir o URL do registo ECR |
| `AWS_ACCESS_KEY_ID` | todos os jobs | Credenciais AWS para deploy |
| `AWS_SECRET_ACCESS_KEY` | todos os jobs | Credenciais AWS para deploy |
| `DB_PASSWORD` | terraform-apply, ansible-deploy | Password do RDS |
| `DB_USERNAME` | ansible-deploy | Username do RDS |
| `EC2_KEY_NAME` | terraform-apply | Nome do par de chaves SSH na AWS |
| `EC2_SSH_PRIVATE_KEY` | ansible-deploy | Chave privada SSH para acesso às EC2 |
| `ORDER_AWS_ACCESS_KEY_ID` | ansible-deploy | Credenciais do order-service-user |
| `ORDER_AWS_SECRET_ACCESS_KEY` | ansible-deploy | Credenciais do order-service-user |
| `NOTIFICATION_AWS_ACCESS_KEY_ID` | ansible-deploy | Credenciais do notification-service-user |
| `NOTIFICATION_AWS_SECRET_ACCESS_KEY` | ansible-deploy | Credenciais do notification-service-user |

A chave SSH privada é escrita em `/tmp/deploy_key.pem` durante o job Ansible e removida automaticamente no final da execução do runner.

### Aprovação Manual no Pipeline

O pipeline de deploy usa o environment `production` do GitHub Actions, que pode ser configurado para exigir **aprovação manual** antes de executar. Isto impede deploys acidentais ou não autorizados para a produção.

### Segredos em Deploy Manual (ficheiro .env)

Quando o deploy é feito manualmente, os segredos são passados aos containers como variáveis de ambiente a partir de um ficheiro `.env` na instância EC2 em tempo de execução.

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

Para um sistema em produção, considera migrar os segredos para o **AWS Secrets Manager** ou **SSM Parameter Store** e usar instance roles de EC2 em vez de access keys de longa duração. Isto elimina completamente a necessidade de credenciais armazenadas em disco ou em segredos do CI/CD.

---

## Dead-Letter Queue da SQS

A fila `order-created` tem uma DLQ (`order-created-dlq`) configurada com `maxReceiveCount = 5`. As mensagens que falham o processamento 5 vezes são movidas automaticamente para a DLQ. Monitoriza a DLQ para notificações falhadas e configura alertas para qualquer mensagem que aí apareça.

---

## Checklist para um Novo Deploy

- [ ] Utilizadores IAM criados com as políticas de menor privilégio acima
- [ ] Sem permissões de admin ou `*:*` atribuídas aos utilizadores IAM dos serviços
- [ ] Segredos GitHub Actions configurados (para deploy via CI/CD)
- [ ] Environment `production` configurado com revisores (para aprovação manual)
- [ ] Ficheiro `.env` presente nas instâncias EC2, fora do controlo de versão (para deploy manual)
- [ ] `terraform.tfvars` não commitado para o git
- [ ] RDS não acessível publicamente (confirmado na consola AWS)
- [ ] DLQ monitorizada para mensagens falhadas
