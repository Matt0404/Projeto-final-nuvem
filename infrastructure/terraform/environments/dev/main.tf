terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

locals {
  common_tags = {
    Project     = "mini-ecommerce"
    Environment = terraform.workspace
    Owner       = "team-Matilde Rodrigues - Sofia Martins"
    ManagedBy   = "terraform"
  }
}

module "vpc" {
  source = "../../modules/vpc"

  vpc_name             = "mini-ecommerce-dev-vpc"
  vpc_cidr             = "10.0.0.0/16"
  public_subnet_cidrs  = ["10.0.1.0/24", "10.0.2.0/24"]
  private_subnet_cidrs = ["10.0.10.0/24", "10.0.20.0/24"]
  availability_zones   = ["eu-central-1a", "eu-central-1b"]
  tags                 = local.common_tags
}

module "mini-ecommerce-dev-api_gateway" {
  source = "../../modules/ec2"

  name          = "api-gateway"
  vpc_id        = module.vpc.vpc_id
  subnet_id     = module.vpc.public_subnet_ids[0]
  instance_type = var.instance_type
  key_name      = var.key_name
  web_sg_id     = module.vpc.web_sg_id
  user_data     = file("${path.module}/docker-install.sh")
  tags          = local.common_tags
}

module "mini-ecommerce-dev-catalog_service" {
  source = "../../modules/ec2"

  name          = "catalog-service"
  vpc_id        = module.vpc.vpc_id
  subnet_id     = module.vpc.private_subnet_ids[0]
  instance_type = var.instance_type
  key_name      = var.key_name
  web_sg_id     = module.vpc.web_sg_id
  user_data     = file("${path.module}/docker-install.sh")
  tags          = local.common_tags
}

module "mini-ecommerce-dev-order_service" {
  source = "../../modules/ec2"

  name          = "order-service"
  vpc_id        = module.vpc.vpc_id
  subnet_id     = module.vpc.private_subnet_ids[0]
  instance_type = var.instance_type
  key_name      = var.key_name
  web_sg_id     = module.vpc.web_sg_id
  user_data     = file("${path.module}/docker-install.sh")
  tags          = local.common_tags
}

module "mini-ecommerce-dev-notification_service" {
  source = "../../modules/ec2"

  name          = "notification-service"
  vpc_id        = module.vpc.vpc_id
  subnet_id     = module.vpc.private_subnet_ids[1]
  instance_type = var.instance_type
  key_name      = var.key_name
  web_sg_id     = module.vpc.web_sg_id
  user_data     = file("${path.module}/docker-install.sh")
  tags          = local.common_tags
}

module "mini-ecommerce-dev-rds" {
  source = "../../modules/rds"

  vpc_id      = module.vpc.vpc_id
  subnet_ids  = module.vpc.private_subnet_ids
  db_password = var.db_password
  db_sg_id    = module.vpc.db_sg_id
  tags        = local.common_tags
}

module "mini-ecommerce-dev-sqs" {
  source = "../../modules/sqs"

  queue_name         = "order-created"
  dlq_name           = "order-created-dlq"
  visibility_timeout = 30
  max_receive_count  = 5
}