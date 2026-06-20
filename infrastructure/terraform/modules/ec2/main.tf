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

# IAM Role
resource "aws_iam_role" "ec2_ecr_role" {
  name = "${var.name}-ecr-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = {
        Service = "ec2.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.ec2_ecr_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_instance_profile" "ec2_ecr_profile" {
  name = "${var.name}-ecr-profile"
  role = aws_iam_role.ec2_ecr_role.name
}

resource "aws_instance" "this" {
  ami                         = data.aws_ami.amazon_linux.id
  instance_type               = var.instance_type
  subnet_id                   = var.subnet_id
  vpc_security_group_ids      = [var.web_sg_id]
  key_name                    = var.key_name
  associate_public_ip_address = true
  user_data                   = var.user_data
  iam_instance_profile        = aws_iam_instance_profile.ec2_ecr_profile.name

  tags = local.common_tags
}