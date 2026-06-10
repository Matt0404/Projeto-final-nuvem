output "ec2_ips" {
  description = "Public IPs of all EC2 instances — consumed by Ansible deploy"
  value = {
    api_gateway          = module.mini-ecommerce-dev-api_gateway.public_ip
    catalog_service      = module.mini-ecommerce-dev-catalog_service.public_ip
    order_service        = module.mini-ecommerce-dev-order_service.public_ip
    notification_service = module.mini-ecommerce-dev-notification_service.public_ip
  }
}

output "rds_endpoint" {
  description = "RDS endpoint — passed to services as DB_URL"
  value       = module.mini-ecommerce-dev-rds.database_endpoint
  sensitive   = true
}

output "ecr_repository_urls" {
  description = "ECR repository URLs for each service"
  value       = { for k, v in aws_ecr_repository.services : k => v.repository_url }
}