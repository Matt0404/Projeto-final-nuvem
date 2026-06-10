# mini-ecommerce

Plataforma de e-commerce cloud-native baseada em microsserviços, construída com Spring Boot e deployada na AWS com Terraform e Docker.

## Visão Geral

Este projeto implementa um backend de mini e-commerce composto por quatro serviços independentes que comunicam via HTTP e AWS SQS. A infraestrutura é provisionada com Terraform e os serviços são containerizados com Docker.

## Arquitetura

```
Cliente / curl
     │
     ▼
ALB / api-gateway  (EC2 Público — porta 8080)
     │
     ├──► catalog-service  (EC2 Privado — porta 8082) ──► RDS PostgreSQL
     │
     └──► order-service    (EC2 Privado — porta 8083) ──► RDS PostgreSQL
                                   │
                                   ▼
                           SQS: order-created
                                   │
                                   ▼
                       notification-service (EC2 Privado — porta 8081) ──► AWS SES
```

Consulta [`docs/architecture.md`](docs/architecture.md) para o diagrama completo e descrição dos componentes.

## Serviços

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `api-gateway` | 8080 | Ponto de entrada — encaminha pedidos para os serviços de catálogo e encomendas |
| `catalog-service` | 8082 | Catálogo de produtos — lê/escreve no RDS PostgreSQL |
| `order-service` | 8083 | Gestão de encomendas — escreve no RDS e publica na SQS |
| `notification-service` | 8081 | Consome a fila SQS `order-created` e envia email via SES |

## Pré-requisitos

- Docker & Docker Compose
- Terraform ≥ 1.5
- AWS CLI configurado (`aws configure`)
- Conta AWS com permissões para EC2, RDS, SQS, SES, VPC e IAM
- Endereço de email verificado no AWS SES

## Início Rápido (Local)

```bash
# 1. Clonar o repositório
git clone https://github.com/Matt0404/Projeto-final-nuvem.git
cd mini-ecommerce

# 2. Configurar variáveis de ambiente
cp .env.example .env
# Edita o .env com as tuas credenciais (ver secção Variáveis de Ambiente abaixo)

# 3. Construir e arrancar todos os serviços
docker compose up --build
```

O API gateway estará disponível em `http://localhost:8080`.

## Variáveis de Ambiente

Cria um ficheiro `.env` na raiz do projeto com os seguintes valores:

```env
# Base de dados (RDS PostgreSQL)
DB_USERNAME="dbadmin"
DB_PASSWORD="StrongPass123"

# Credenciais AWS para o order-service (publicar na SQS)
ORDER_AWS_ACCESS_KEY_ID=AKIA...
ORDER_AWS_SECRET_ACCESS_KEY=...

# Credenciais AWS para o notification-service (consumir SQS + enviar SES)
NOTIFICATION_AWS_ACCESS_KEY_ID=AKIA...
NOTIFICATION_AWS_SECRET_ACCESS_KEY=...
```

> **Nunca commites o `.env` para controlo de versão.**

## Deploy na AWS

Consulta [`docs/deployment.md`](docs/deployment.md) para o guia passo a passo completo.

```bash
# Provisionar infraestrutura
cd infra/environments/dev
terraform init
terraform apply

# Fazer deploy dos serviços via Docker Compose em cada instância EC2
```

## Estrutura do Projeto

```
mini-ecommerce/
├── services/
│   ├── api-gateway/
│   ├── catalog-service/
│   ├── order-service/
│   └── notification-service/
├── infra/
│   ├── environments/
│   │   └── dev/
│   │       ├── main.tf
│   │       ├── variables.tf
│   │       └── docker-install.sh
│   └── modules/
│       ├── vpc/
│       ├── ec2/
│       ├── rds/
│       └── sqs/
├── docker-compose.yml
├── .env.example
└── docs/
    ├── architecture.md
    ├── deployment.md
    └── security.md
```

## Segurança

Consulta [`docs/security.md`](docs/security.md) para o modelo IAM, políticas de menor privilégio e gestão de segredos.

## Equipa

Matilde Rodrigues & Sofia Martins
