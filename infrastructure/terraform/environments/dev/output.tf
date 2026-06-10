output "resources" {
  value = {
    vpc                      = module.vpc.vpc_id
    api_gateway-ec2          = module.mini-ecommerce-dev-api_gateway.instance_id
    catalog_service-ec2      = module.mini-ecommerce-dev-catalog_service.instance_id
    order_service-ec2        = module.mini-ecommerce-dev-order_service.instance_id
    notification_service-ec2 = module.mini-ecommerce-dev-notification_service.instance_id
    rds                      = module.mini-ecommerce-dev-rds.db_instance_id
  }
}

output "rds_endpoint" {
  value     = module.mini-ecommerce-dev-rds.database_endpoint
  sensitive = true
}

output "queue_url" {
  value = module.mini-ecommerce-dev-sqs.queue_url
}

output "api_gateway_public_ip" {
  description = "IP público da EC2 do api-gateway"
  value       = module.mini-ecommerce-dev-api_gateway.public_ip
}

output "ec2_ips" {
  value = {
    api_gateway          = module.api_gateway_ec2.public_ip
    catalog_service      = module.catalog_service_ec2.public_ip
    order_service        = module.order_service_ec2.public_ip
    notification_service = module.notification_service_ec2.public_ip
  }
}