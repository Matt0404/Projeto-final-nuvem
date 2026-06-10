# Arquitetura

## Visão Geral

A plataforma mini-ecommerce é um sistema de microsserviços deployado na AWS (região `eu-central-1`). Todos os serviços correm dentro de containers Docker em instâncias EC2. A infraestrutura é provisionada com Terraform e organizada em módulos reutilizáveis.

---

## Diagrama de Componentes

```
┌──────────────────────────────────────────────────────────────────────┐
│                          AWS eu-central-1                            │
│                                                                      │
│  ┌─────────────────────────────────────────────────────────────┐     │
│  │  VPC  10.0.0.0/16                                           │     │
│  │                                                             │     │
│  │  ┌──────────────────────────┐                               │     │
│  │  │  Subnets Públicas        │                               │     │
│  │  │  10.0.1.0/24 (eu-c-1a)   │                               │     │
│  │  │  10.0.2.0/24 (eu-c-1b)   │                               │     │
│  │  │                          │                               │     │
│  │  │  ┌────────────────────┐  │                               │     │
│  │  │  │  EC2: api-gateway  │  │◄─── Internet (Cliente/curl)   │     │
│  │  │  │  porta 8080        │  │                               │     │
│  │  │  └────────┬───────────┘  │                               │     │
│  │  └───────────┼──────────────┘                               │     │
│  │              │                                              │     │
│  │  ┌───────────┼──────────────────────────────────────────┐   │     │
│  │  │  Subnets Privadas                                    │   │     │
│  │  │  10.0.10.0/24 (eu-c-1a)                              │   │     │
│  │  │  10.0.20.0/24 (eu-c-1b)                              │   │     │
│  │  │              │                                       │   │     │
│  │  │    ┌─────────┴──────────┐   ┌────────────────────┐   │   │     │
│  │  │    │  EC2:              │   │  EC2:              │   │   │     │
│  │  │    │  catalog-service   │   │  order-service     │   │   │     │
│  │  │    │  porta 8082        │   │  porta 8083        │   │   │     │
│  │  │    └────────┬───────────┘   └────────┬───────────┘   │   │     │
│  │  │             │                        │               │   │     │
│  │  │    ┌────────▼───────────────────┐    │               │   │     │
│  │  │    │  RDS PostgreSQL            │◄───┘               │   │     │
│  │  │    │  modulos-database          │    │               │   │     │
│  │  │    │  subnet privada            │    │               │   │     │
│  │  │    └────────────────────────────┘    │               │   │     │
│  │  │                                      │               │   │     │
│  │  │    ┌─────────────────────────────────▼──────────┐    │   │     │
│  │  │    │  Fila SQS: order-created                   │    │   │     │
│  │  │    │  DLQ: order-created-dlq (máx. 5 tentativas)│    │   │     │
│  │  │    └────────────────────────────────────────────┘    │   │     │
│  │  │                           │                          │   │     │
│  │  │    ┌──────────────────────▼─────────────────────┐    │   │     │
│  │  │    │  EC2: notification-service                 │    │   │     │
│  │  │    │  porta 8081                                │───►│AWS SES  │
│  │  │    └────────────────────────────────────────────┘    │   │     │
│  │  └──────────────────────────────────────────────────────┘   │     │
│  └─────────────────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Fluxo de Dados

### Criar uma Encomenda

1. O cliente envia `POST /orders` ao **api-gateway** (EC2 público, porta 8080).
2. O api-gateway encaminha o pedido para o **order-service** (`http://order-service:8083`).
3. O order-service persiste a encomenda no **RDS PostgreSQL** e publica um evento `order-created` na **fila SQS**.
4. O **notification-service** consome o evento da SQS e envia um email de confirmação via **AWS SES**.

### Consultar o Catálogo

1. O cliente envia `GET /products` (ou similar) ao **api-gateway**.
2. O api-gateway encaminha para o **catalog-service** (`http://catalog-service:8082`).
3. O catalog-service consulta o **RDS PostgreSQL** e devolve o resultado.

---

## Módulos de Infraestrutura

| Módulo | Recurso                          | Notas |
|-------|-------------------|----------------------------------------------------------------------------------------|
| `vpc` | VPC, subnets públicas/privadas,   |2 AZs (eu-central-1a, eu-central-1b)                                    |
|_______|_tabelas de rotas, security groups |________________________________________________________________________|
| `ec2` |Instância EC2                      |Reutilizado pelos 4 serviços; Docker instalado via `user_data`          |
| `rds` |RDS PostgreSQL                     |Deployado em subnet privada; `db_sg_id` restringe o acesso              |
| `sqs` |Fila SQS + DLQ                     |Fila `order-created` com DLQ de 5 tentativas; visibility timeout de 30s |

---

## Stack Tecnológica

| Camada          | Tecnologia |
|-----------------|------------|
| Serviços        | Java / Spring Boot |
| Containerização | Docker, Docker Compose |
| Infraestrutura  | Terraform ≥ 1.5, AWS |
| Base de Dados   | PostgreSQL no RDS |
| Mensageria      | AWS SQS (fila standard) |
| Email           | AWS SES |
| Rede            | AWS VPC (subnets públicas + privadas, 2 AZs) |

---

## Segurança de Rede

- O **api-gateway** encontra-se numa **subnet pública** e é o único componente exposto à internet.
- Todos os outros serviços (catalog, order, notification) encontram-se em **subnets privadas** sem acesso direto à internet.
- O RDS está numa subnet privada e protegido por um security group dedicado (`db_sg_id`) que apenas permite tráfego do security group da aplicação (`web_sg_id`).
- A comunicação entre serviços acontece pela rede Docker privada (`microservices-network`) ou pelo routing interno da VPC.
