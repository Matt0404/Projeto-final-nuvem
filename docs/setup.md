# Configuração Inicial (Setup)

Este documento descreve todos os pré-requisitos necessários antes de fazer deploy do mini-ecommerce, tanto a nível local como na AWS.

---

## Pré-requisitos Locais

### Ferramentas necessárias

| Ferramenta | Versão mínima | Verificação |
|------------|---------------|-------------|
| AWS CLI | qualquer | `aws --version` |
| Terraform | ≥ 1.5 | `terraform -version` |
| Docker | qualquer | `docker --version` |
| Docker Compose | qualquer | `docker compose version` |
| Ansible | qualquer | `ansible --version` |
| Python + pip | qualquer | `python3 --version` |

### Instalar dependências Ansible

O inventário dinâmico da AWS requer as bibliotecas Python `boto3` e `botocore`:

```bash
pip install ansible boto3 botocore
```

A coleção `amazon.aws` é necessária para o inventário dinâmico e para o módulo `community.docker`:

```bash
ansible-galaxy collection install amazon.aws community.docker
```

---

## Pré-requisitos AWS

### Credenciais AWS

Configura o AWS CLI com um utilizador que tenha permissões suficientes para provisionar a infraestrutura:

```bash
aws configure
```

O utilizador precisa de permissões para: EC2, RDS, SQS, VPC, IAM e **ECR**.

### Par de Chaves SSH

Cria um par de chaves SSH na consola AWS (ou via CLI) na região `eu-central-1`. O nome da chave será usado como variável no Terraform (`key_name`).

```bash
aws ec2 create-key-pair \
  --key-name nome-da-chave \
  --region eu-central-1 \
  --query 'KeyMaterial' \
  --output text > ~/.ssh/nome-da-chave.pem

chmod 400 ~/.ssh/nome-da-chave.pem
```

### Repositórios ECR

Cria um repositório ECR para cada serviço na região `eu-central-1`. Estes repositórios recebem as imagens Docker construídas pelo pipeline CI/CD.

```bash
for service in api-gateway catalog-service order-service notification-service; do
  aws ecr create-repository \
    --repository-name $service \
    --region eu-central-1
done
```

---

## Configuração do GitHub Actions

Para o pipeline CI/CD funcionar, adiciona os seguintes segredos no repositório GitHub (Settings → Secrets and variables → Actions):

| Segredo | Descrição |
|---------|-----------|
| `AWS_ACCOUNT_ID` | ID da conta AWS (usado para construir o URL do ECR) |
| `AWS_ACCESS_KEY_ID` | Access key do utilizador com permissões de deploy |
| `AWS_SECRET_ACCESS_KEY` | Secret key do utilizador com permissões de deploy |
| `DB_PASSWORD` | Password do RDS PostgreSQL |
| `DB_USERNAME` | Username do RDS PostgreSQL |
| `EC2_KEY_NAME` | Nome do par de chaves SSH criado na AWS |
| `EC2_SSH_PRIVATE_KEY` | Conteúdo do ficheiro `.pem` da chave privada SSH |
| `ORDER_AWS_ACCESS_KEY_ID` | Access key do utilizador IAM `order-service-user` |
| `ORDER_AWS_SECRET_ACCESS_KEY` | Secret key do utilizador IAM `order-service-user` |
| `NOTIFICATION_AWS_ACCESS_KEY_ID` | Access key do utilizador IAM `notification-service-user` |
| `NOTIFICATION_AWS_SECRET_ACCESS_KEY` | Secret key do utilizador IAM `notification-service-user` |

> O pipeline de deploy está configurado com o environment `production`, que requer **aprovação manual** antes de executar. Configura os revisores em Settings → Environments → production.

---

## Configuração Local (.env)

Para correr o projeto localmente com Docker Compose, cria um ficheiro `.env` na raiz:

```bash
cp .env .env
```

Preenche com as tuas credenciais:

```env
# Base de dados (RDS PostgreSQL)
DB_USERNAME="dbadmin"
DB_PASSWORD="StrongPass123"

# Credenciais AWS para o order-service (publicar na SQS)
ORDER_AWS_ACCESS_KEY_ID=AKIA...
ORDER_AWS_SECRET_ACCESS_KEY=...

# Credenciais AWS para o notification-service (consumir SQS)
NOTIFICATION_AWS_ACCESS_KEY_ID=AKIA...
NOTIFICATION_AWS_SECRET_ACCESS_KEY=...
```

> **Nunca commites o `.env` para controlo de versão.** Confirma que está no `.gitignore`.

Adiciona ao `.gitignore`:

```
.env
*.tfvars
*.tfvars.json
```
