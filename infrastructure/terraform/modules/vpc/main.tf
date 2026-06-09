resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
 
  tags = merge(var.tags, {
    Name = var.vpc_name
  })
}


resource "aws_eip" "nat" {
  domain = "vpc"

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-nat-eip"
  })
}



# Gateway da publica/privada
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
 
  tags = merge(var.tags, {
    Name = "${var.vpc_name}-igw"
  })
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id  

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-nat"
  })

  depends_on = [aws_internet_gateway.main]
}



# Subnet publica/privada
resource "aws_subnet" "public" {
  count = length(var.public_subnet_cidrs)
 
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true
 
  tags = merge(var.tags, {
    Name = "${var.vpc_name}-public-${count.index + 1}"
  })
}
 
resource "aws_subnet" "private" {
  count = length(var.private_subnet_cidrs)
 
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]
 
  tags = merge(var.tags, {
    Name = "${var.vpc_name}-private-${count.index + 1}"
  })
}
 


#Rou table para associar internet/nat ás suas subnets
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
 
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
 
  tags = merge(var.tags, {
    Name = "${var.vpc_name}-public-rt"
  })
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id
 
  route {
    cidr_block = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }
 
  tags = merge(var.tags, {
    Name = "${var.vpc_name}-private-rt"
  })
}



#Associar rout tables ás subnets publicas/privadas
resource "aws_route_table_association" "public" {
  count = length(aws_subnet.public)
 
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count = length(aws_subnet.private)
 
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}



#Security Groups para o EC2/ALB e RDS
# Web tier — ALB/EC2 publica
resource "aws_security_group" "web" {
  name        = "${var.vpc_name}-web-sg"
  description = "Security group for web/app tier"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "SSH"
    from_port   = 22
    to_port     = 22
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-web-sg"
  })
}



# DB tier — para o RDS, só aceita tráfego vindo do web SG
resource "aws_security_group" "db" {
  name        = "${var.vpc_name}-db-sg"
  description = "Security group for database tier"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL from web tier only"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.web.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = merge(var.tags, {
    Name = "${var.vpc_name}-db-sg"
  })
}
