variable "queue_name" {
  type = string
}

variable "dlq_name" {
  type = string
}

variable "visibility_timeout" {
  type    = number
  default = 30
}

variable "max_receive_count" {
  type    = number
  default = 5
}