variable "name" {
  type        = string
  description = "Name of the service (e.g. catalog-service)"
}
 
variable "vpc_id" {
  type = string
}
 
variable "subnet_id" {
  type        = string
  description = "Single private subnet ID where the instance will be launched"
}
 
variable "instance_type" {
  type    = string
  default = "t3.micro"
 
  validation {
    condition     = can(regex("^t[23]\\..+", var.instance_type))
    error_message = "Instance type must be t2 or t3 family."
  }
}

variable "key_name" {
  type = string
}
 
variable "web_sg_id" {
  type        = string
  description = "ID do web security group"
}
 
variable "tags" {
  description = "Tags to apply to resources"
  type        = map(string)
  default     = {}
}

variable "user_data"{
  type        = string
  default = ""
}