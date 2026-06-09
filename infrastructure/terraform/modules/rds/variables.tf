variable "vpc_id" {
  type = string
}
 
variable "subnet_ids" {
  type = list(string)
}
 
variable "db_password" {
  type      = string
  sensitive = true
}
 
# multiple services (catalog + order) to connect to RDS
variable "db_sg_id" {
  type        = string
  description = "security group IDs allowed to connect to RDS"
}
 
variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}