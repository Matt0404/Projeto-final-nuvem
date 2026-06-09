variable "aws_region" {
  type    = string
  default = "eu-central-1"
}

variable "key_name" {
  type        = string
  description = "Nome do key pair SSH criado na AWS para aceder às EC2"
}

variable "db_password" {
  type        = string
  sensitive   = true
  description = "Password do RDS PostgreSQL"
}

variable "instance_type" {
  type        = string
  description = "Tipo de instância EC2 (família t2 ou t3)"

  validation {
    condition     = can(regex("^t[23]\\.", var.instance_type))
    error_message = "Instance type must be t2 or t3 family."
  }
}