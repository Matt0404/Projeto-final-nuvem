# Guia de Deploy

Este guia acompanha um novo engenheiro no deploy do mini-ecommerce na AWS de raiz. Segue os passos por ordem.

---

## Pré-requisitos

Antes de começar, confirma que tens:

- [ ] AWS CLI instalado e configurado (`aws configure`) com um utilizador com permissões suficientes (ver [Segurança](security.md))
- [ ] Terraform ≥ 1.5 instalado (`terraform -version`)
- [ ] Docker e Docker Compose instalados
- [ ] Um par de chaves SSH criado na AWS 
- [ ] Endereço de email verificado no AWS SES (obrigatório para os emails do notification-service)
- [ ] Acesso ao repositório

---

## Passo 1 — Clonar o repositório

```bash
git clone https://github.com/Matt0404/Projeto-final-nuvem.git
cd mini-ecommerce
```

---

## Passo 2 — Criar utilizadores IAM e credenciais

Cria dois utilizadores IAM na consola AWS (ou via CLI) com políticas de menor privilégio:

**`order-service-user`** — precisa de permissão para publicar na SQS:
```json
{
  "Effect": "Allow",
  "Action": ["sqs:SendMessage", "sqs:GetQueueUrl"],
  "Resource": "arn:aws:sqs:eu-central-1:<account-id>:order-created"
}
```

**`notification-service-user`** — precisa de permissão para consumir da SQS e enviar via SES:
```json
{
  "Effect": "Allow",
  "Action": [
    "sqs:ReceiveMessage",
    "sqs:DeleteMessage",
    "sqs:GetQueueAttributes"
  ],
  "Resource": "arn:aws:sqs:eu-central-1:<account-id>:order-created"
},
{
  "Effect": "Allow",
  "Action": ["ses:SendEmail", "ses:SendRawEmail"],
  "Resource": "*"
}
```

Gera as access keys para cada utilizador e guarda-as — vais precisar no Passo 3.

---

## Passo 3 — Configurar variáveis de ambiente

Copia o ficheiro de exemplo e preenche os valores:

```bash
cp .env.example .env
```

Edita o `.env`:

```env
# Credenciais RDS
DB_USERNAME="dbadmin"
DB_PASSWORD="StrongPass123"

# Credenciais AWS para o order-service
ORDER_AWS_ACCESS_KEY_ID=AKIA...
ORDER_AWS_SECRET_ACCESS_KEY=...

# Credenciais AWS para o notification-service
NOTIFICATION_AWS_ACCESS_KEY_ID=AKIA...
NOTIFICATION_AWS_SECRET_ACCESS_KEY=...
```

---

## Passo 4 — Provisionar infraestrutura com Terraform

```bash
cd infra/environments/dev
```

Cria um ficheiro `terraform.tfvars`:

```hcl
aws_region    = "eu-central-1"
instance_type = "t3.micro"       
key_name      = "nome-da-chave"  
db_password   = "password"
```

Inicializa e aplica:

```bash
terraform init
terraform plan   
terraform apply 
```

O Terraform irá criar:
- VPC com subnets públicas e privadas em 2 AZs
- 4 instâncias EC2 (api-gateway, catalog-service, order-service, notification-service)
- Instância RDS PostgreSQL em subnet privada
- Fila SQS `order-created` com dead-letter queue `order-created-dlq`

Guarda o IP público da instância EC2 do api-gateway a partir do output do Terraform.

---

## Passo 5 — Fazer deploy dos serviços nas EC2

Cada serviço corre como um container Docker na sua instância EC2. O Docker é instalado automaticamente via `user_data` quando o Terraform provisiona as instâncias.

### Copiar ficheiros para cada EC2

Para cada serviço, faz SSH para a instância EC2 correspondente e copia o diretório do serviço e o ficheiro `.env`.

**Exemplo para o api-gateway (subnet pública — acessível diretamente):**

```bash
# Substitui <api-gateway-ip> pelo IP público do output do Terraform
scp -i ~/.ssh/chave.pem -r services/api-gateway ec2-user@<api-gateway-ip>:~/
scp -i ~/.ssh/chave.pem .env ec2-user@<api-gateway-ip>:~/
scp -i ~/.ssh/chave.pem docker-compose.yml ec2-user@<api-gateway-ip>:~/
```

Para serviços em **subnets privadas**, faz SSH usando o api-gateway como bastion:

```bash
ssh -J ec2-user@<api-gateway-ip> ec2-user@<ip-ec2-privado>
```

### Arrancar todos os serviços

A partir da instância EC2 do api-gateway:

```bash
ssh -i ~/.ssh/chave.pem ec2-user@<api-gateway-ip>
cd ~
docker compose --env-file .env up --build -d
```

Ou arranca cada serviço individualmente na sua própria instância EC2 usando o nome do serviço no `docker compose`.

---

## Passo 6 — Verificar o deploy

A partir da tua máquina local, testa o endpoint do API gateway:

```bash
# Health check (ajusta o path ao teu actuator Spring Boot)
curl http://<api-gateway-ip>:8080/actuator/health

# Catálogo
curl http://<api-gateway-ip>:8080/api/products

# Criar uma encomenda
curl -X POST http://<api-gateway-ip>:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 2}'
```

Verifica os logs do notification-service para confirmar o consumo da SQS:

```bash
docker logs notification-service -f
```

---

## Passo 7 — Destruir a infraestrutura (quando terminares)

```bash
cd infra/environments/dev
terraform destroy
```

Remove todos os recursos AWS criados no Passo 4. Pode ser necessário desativar a proteção de eliminação do RDS primeiro.

---

## Resolução de Problemas

**Serviços não conseguem chegar ao RDS**
Verifica que o security group `db_sg_id` permite tráfego PostgreSQL de entrada (porta 5432) a partir do security group `web_sg_id`.

**order-service não consegue publicar na SQS**
Confirma que `ORDER_AWS_ACCESS_KEY_ID` e `ORDER_AWS_SECRET_ACCESS_KEY` estão corretos e que o utilizador IAM tem `sqs:SendMessage` no ARN da fila `order-created`.

**notification-service não recebe mensagens**
Confirma que `SQS_QUEUE_NAME=order-created` coincide com a fila provisionada pelo Terraform e que o utilizador IAM tem permissão `sqs:ReceiveMessage`.

**Email não é enviado**
Verifica que o endereço de envio SES (`AWS_SES_FROM`) está verificado em `eu-central-1`. No modo sandbox do SES, o endereço do destinatário também precisa de estar verificado.
