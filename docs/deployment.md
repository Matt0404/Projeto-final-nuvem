# Guia de Deploy

Este guia acompanha um novo engenheiro no deploy do mini-ecommerce na AWS de raiz. O deploy pode ser feito de duas formas: **automaticamente via pipeline GitHub Actions** (recomendado) ou **manualmente** passo a passo.

---

## Opção A — Deploy Automático via CI/CD (Recomendado)

O pipeline `deploy.yml` executa-se automaticamente a cada push para `main` e realiza três etapas em sequência, cada uma dependendo da anterior.

### Pré-requisitos

Antes de usar o pipeline, confirma que:

- [ ] Os repositórios ECR estão criados para os 4 serviços (ver [`docs/setup.md`](setup.md))
- [ ] Todos os segredos GitHub Actions estão configurados (ver [`docs/setup.md`](setup.md))
- [ ] O environment `production` está configurado com revisores para aprovação manual
- [ ] O par de chaves SSH está criado na AWS e o conteúdo do `.pem` está no segredo `EC2_SSH_PRIVATE_KEY`

### Etapa 1 — Build & Push das Imagens Docker

Ao fazer push para `main`, o job `build-and-push` é acionado automaticamente. Para cada um dos 4 serviços em paralelo:

1. Faz checkout do código.
2. Configura as credenciais AWS e faz login no **Amazon ECR**.
3. Constrói a imagem Docker a partir de `services/<nome-do-serviço>/`.
4. Envia a imagem para o ECR com duas tags: `<SHA-curto>` e `latest`.

A tag usada no deploy corresponde aos primeiros 8 caracteres do SHA do commit (`${GITHUB_SHA::8}`).

### Etapa 2 — Terraform Apply

O job `terraform-apply` executa após o `build-and-push` com sucesso:

1. Executa `terraform init` e `terraform apply` no diretório `./infrastructure/terraform/environments/dev`.
2. Passa `db_password` e `key_name` como variáveis.
3. Exporta os IPs das EC2 com `terraform output -json ec2_ips` e guarda-os como artefacto do workflow.

### Etapa 3 — Ansible Deploy

O job `ansible-deploy` executa após o `terraform-apply` com sucesso:

1. Instala Ansible, boto3 e botocore.
2. Escreve a chave SSH privada (do segredo `EC2_SSH_PRIVATE_KEY`) para `/tmp/deploy_key.pem`.
3. Usa o **inventário dinâmico** `ansible/inventory/aws_ec2.yml` para descobrir automaticamente as instâncias EC2 pelo tag `Project: mini-ecommerce` e agrupá-las pelo tag `Name`.
4. Executa o playbook `ansible/playbooks/deploy.yml` que, para cada serviço:
   - Faz login no ECR na instância EC2.
   - Faz pull da nova imagem com a tag do commit.
   - Reinicia o container com `restart_policy: always`.

As variáveis de ambiente dos serviços (credenciais de base de dados, credenciais SQS) são injetadas diretamente pelo Ansible no momento do arranque do container.

### Verificar o Deploy

Após o pipeline terminar com sucesso, testa o endpoint:

```bash
# Health check
curl http://<api-gateway-ip>:8080/actuator/health

# Catálogo
curl http://<api-gateway-ip>:8080/api/products

# Criar uma encomenda
curl -X POST http://<api-gateway-ip>:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 2}'
```

O IP público do api-gateway encontra-se no output do Terraform ou na consola EC2.

---

## Opção B — Deploy Manual Passo a Passo

Segue os passos abaixo caso queiras fazer o deploy sem o pipeline CI/CD.

### Passo 1 — Clonar o repositório

```bash
git clone https://github.com/Matt0404/Projeto-final-nuvem.git
cd mini-ecommerce
```

### Passo 2 — Criar utilizadores IAM e credenciais

Cria dois utilizadores IAM na consola AWS com políticas de menor privilégio:

**`order-service-user`** — permissão para publicar na SQS:
```json
{
  "Effect": "Allow",
  "Action": ["sqs:SendMessage", "sqs:GetQueueUrl"],
  "Resource": "arn:aws:sqs:eu-central-1:<account-id>:order-created"
}
```

**`notification-service-user`** — permissão para consumir da SQS:
```json
{
  "Effect": "Allow",
  "Action": ["sqs:ReceiveMessage", "sqs:DeleteMessage", "sqs:GetQueueAttributes"],
  "Resource": "arn:aws:sqs:eu-central-1:<account-id>:order-created"
}
```

Gera as access keys para cada utilizador e guarda-as.

### Passo 3 — Criar repositórios ECR

```bash
for service in api-gateway catalog-service order-service notification-service; do
  aws ecr create-repository \
    --repository-name $service \
    --region eu-central-1
done
```

### Passo 4 — Configurar variáveis de ambiente

```bash
cp .env.example .env
```

Edita o `.env`:

```env
DB_USERNAME="dbadmin"
DB_PASSWORD="StrongPass123"
ORDER_AWS_ACCESS_KEY_ID=AKIA...
ORDER_AWS_SECRET_ACCESS_KEY=...
NOTIFICATION_AWS_ACCESS_KEY_ID=AKIA...
NOTIFICATION_AWS_SECRET_ACCESS_KEY=...
```

### Passo 5 — Provisionar infraestrutura com Terraform

```bash
cd infrastructure/terraform/environments/dev
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

O Terraform cria:
- VPC com subnets públicas e privadas em 2 AZs
- 4 instâncias EC2 (api-gateway, catalog-service, order-service, notification-service)
- Instância RDS PostgreSQL em subnet privada
- Fila SQS `order-created` com dead-letter queue `order-created-dlq`

### Passo 6 — Build e Push das Imagens para o ECR

```bash
# Substitui <account-id> pelo teu ID de conta AWS
ECR_REGISTRY="<account-id>.dkr.ecr.eu-central-1.amazonaws.com"
IMAGE_TAG=$(git rev-parse --short HEAD)

# Login no ECR
aws ecr get-login-password --region eu-central-1 | \
  docker login --username AWS --password-stdin $ECR_REGISTRY

# Build e push de cada serviço
for service in api-gateway catalog-service order-service notification-service; do
  docker build -t $ECR_REGISTRY/$service:$IMAGE_TAG services/$service/
  docker push $ECR_REGISTRY/$service:$IMAGE_TAG
  docker tag $ECR_REGISTRY/$service:$IMAGE_TAG $ECR_REGISTRY/$service:latest
  docker push $ECR_REGISTRY/$service:latest
done
```

### Passo 7 — Deploy com Ansible

Com as instâncias EC2 a correr, executa o playbook Ansible para fazer pull das imagens e arrancar os containers:

```bash
ansible-playbook \
  -i ansible/inventory/aws_ec2.yml \
  ansible/playbooks/deploy.yml \
  --private-key ~/.ssh/nome-da-chave.pem \
  --extra-vars "image_tag=${IMAGE_TAG} ecr_registry=${ECR_REGISTRY}" \
  -u ec2-user
```

O inventário dinâmico `aws_ec2.yml` descobre automaticamente as instâncias EC2 na região `eu-central-1` com o tag `Project: mini-ecommerce` e agrupa-as pelo tag `Name`.

### Passo 8 — Verificar o deploy

```bash
# Health check
curl http://<api-gateway-ip>:8080/actuator/health

# Catálogo
curl http://<api-gateway-ip>:8080/api/products

# Criar uma encomenda
curl -X POST http://<api-gateway-ip>:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"productId": 1, "quantity": 2}'
```

Verifica os logs do notification-service:

```bash
docker logs notification-service -f
```

---

## Destruir a Infraestrutura

```bash
cd infrastructure/terraform/environments/dev
terraform destroy
```

Remove todos os recursos AWS. Pode ser necessário desativar a proteção de eliminação do RDS primeiro.

---

## Pipeline de Pull Request (pr.yml)

Antes de qualquer merge para `main`, o pipeline de PR executa automaticamente:

1. **Lint** — Checkstyle via Maven para os 4 serviços em paralelo.
2. **Testes** — Testes unitários Maven para os 4 serviços; os relatórios Surefire são guardados como artefactos.
3. **Terraform Plan** — Valida e planeia as alterações de infraestrutura; o output do plan é publicado automaticamente como comentário no PR.

O merge só deve acontecer após todos os jobs passarem.

---

## Resolução de Problemas

**Serviços não conseguem chegar ao RDS**
Verifica que o security group `db_sg_id` permite tráfego PostgreSQL de entrada (porta 5432) a partir do security group `web_sg_id`.

**order-service não consegue publicar na SQS**
Confirma que `ORDER_AWS_ACCESS_KEY_ID` e `ORDER_AWS_SECRET_ACCESS_KEY` estão corretos e que o utilizador IAM tem `sqs:SendMessage` no ARN da fila `order-created`.

**notification-service não recebe mensagens**
Confirma que `SQS_QUEUE_NAME=order-created` coincide com a fila provisionada pelo Terraform e que o utilizador IAM tem permissão `sqs:ReceiveMessage`.

**Ansible não encontra as instâncias EC2**
Confirma que as instâncias EC2 têm o tag `Project: mini-ecommerce` e o tag `Name` com o nome correto do serviço (ex: `api-gateway`). Verifica também que as credenciais AWS usadas pelo Ansible têm permissão `ec2:DescribeInstances`.

**Pipeline falha no push para o ECR**
Confirma que os repositórios ECR existem para os 4 serviços em `eu-central-1` e que o segredo `AWS_ACCOUNT_ID` está correto no GitHub Actions.