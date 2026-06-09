data "aws_ami" "amazon_linux" {
  most_recent = true
  owners      = ["amazon"]
 
  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-*-x86_64-gp2"]
  }
}
 
locals {
  common_tags = merge(var.tags, {
    Name      = var.name
    Role      = "service"
    ManagedBy = "terraform"
  })
}
 
resource "aws_instance" "this" {
  ami                    = data.aws_ami.amazon_linux.id
  instance_type          = var.instance_type
  subnet_id              = var.subnet_id
  vpc_security_group_ids = [var.web_sg_id]
  key_name               = var.key_name
  associate_public_ip_address = true
  user_data = var.user_data
 
 
  tags = local.common_tags
}
