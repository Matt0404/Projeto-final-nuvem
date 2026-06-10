# Limitações e Melhorias Futuras

Este documento descreve o que está incompleto ou simplificado na implementação atual, e o que seria feito numa versão de produção.

---

## Limitações Atuais

### Infraestrutura

**Sem load balancer**
O api-gateway corre diretamente numa instância EC2 pública sem um Application Load Balancer (ALB) à frente. Isto significa que não há distribuição de carga, sem terminação TLS automática e sem failover para outra instância.

**Uma instância por serviço, sem redundância**
Cada serviço corre numa única instância EC2. Se uma instância falhar, o serviço fica indisponível. Não existe Auto Scaling Group nem instâncias em standby.

**Ambiente único (dev)**
O Terraform apenas define um ambiente (`dev`). Não existe separação entre ambientes de desenvolvimento, staging e produção.

**Docker instalado via `user_data`, sem gestão de configuração**
A instalação do Docker nas instâncias EC2 é feita via script `user_data` do Terraform. Se a instância for substituída, a instalação repete-se, mas não existe gestão formal do estado da máquina.

### Deploy

**Downtime durante o deploy**
O Ansible reinicia o container com `restart: true`, o que causa um breve período de indisponibilidade do serviço durante o deploy. Não existe estratégia de rolling update ou blue/green deployment.

**Sem health check pós-deploy**
O pipeline Ansible não verifica se o container ficou saudável após o arranque. Um container que arranca mas falha imediatamente não é detetado pelo pipeline.

**Dependência de IP privado para comunicação entre serviços**
O api-gateway recebe os IPs privados dos outros serviços como variáveis de ambiente no momento do deploy. Se um IP mudar (ex: instância substituída), é necessário um novo deploy do api-gateway.

### Segurança

**Credenciais de longa duração (access keys IAM)**
Os serviços order-service e notification-service usam access keys IAM de longa duração em vez de instance roles. Isto é um risco de segurança se as chaves forem comprometidas.

**Credenciais de base de dados partilhadas**
O catalog-service e o order-service partilham o mesmo utilizador e password do RDS PostgreSQL. Não existe isolamento de permissões a nível de base de dados entre serviços.

**Envio de email não operacional**
O notification-service tem no código a integração com o AWS SES e a variável `AWS_SES_FROM` configurada, mas o SES não foi provisionado nem configurado na conta AWS (nenhum endereço verificado, sem pedido de saída do modo sandbox). Na prática, o serviço consome mensagens da SQS mas o envio de email falha em runtime.

**Segredos armazenados como variáveis de ambiente**
Os segredos são passados como variáveis de ambiente nos containers, o que os torna visíveis via `docker inspect`. Numa solução de produção, deveriam ser obtidos de um gestor de segredos como o AWS Secrets Manager.

### Monitorização

**Sem métricas nem alertas**
Não existe integração com CloudWatch Metrics, alertas automáticos ou dashboards. A monitorização da DLQ SQS é manual.

**Logs apenas via `docker logs`**
Os logs dos serviços são acessíveis apenas por SSH à instância e via `docker logs`. Não existe agregação de logs centralizada (ex: CloudWatch Logs).

---

## Melhorias Futuras

### Alta Disponibilidade

- Adicionar um **Application Load Balancer** (ALB) à frente do api-gateway com terminação TLS.
- Criar **Auto Scaling Groups** para cada serviço com políticas de scaling baseadas em CPU ou métricas SQS.
- Distribuir instâncias pelas duas AZs disponíveis para tolerância a falhas.

### Deploy sem Downtime

- Implementar uma estratégia de **rolling update** ou **blue/green deployment** no Ansible.
- Adicionar health checks pós-deploy que verifiquem o endpoint `/actuator/health` antes de marcar o deploy como bem-sucedido.
- Usar o **ECS (Elastic Container Service)** em vez de EC2 puro para gestão de containers com rolling updates nativos.

### Segurança

- Migrar os segredos para o **AWS Secrets Manager** ou **SSM Parameter Store**.
- Substituir as access keys IAM por **EC2 Instance Roles** (IAM roles associadas às instâncias), eliminando a necessidade de credenciais em disco.
- Criar utilizadores de base de dados separados por serviço no RDS.
- Verificar um endereço de envio no **AWS SES**, configurar a política IAM do `notification-service-user` com `ses:SendEmail`, e solicitar saída do modo sandbox para enviar emails para utilizadores reais.

### Monitorização e Observabilidade

- Configurar **CloudWatch Logs** para agregar logs de todos os containers.
- Criar alarmes CloudWatch para a DLQ SQS (mensagens na DLQ indicam falhas no processamento de notificações).
- Adicionar métricas de aplicação com **Spring Boot Actuator** e exportá-las para CloudWatch.

### Infraestrutura

- Criar ambientes separados de `staging` e `production` no Terraform.
- Adicionar **proteção de eliminação** no RDS e **snapshots automáticos**.
- Implementar **VPC Endpoints** para SQS e ECR, eliminando o tráfego de saída pela internet para estes serviços.
