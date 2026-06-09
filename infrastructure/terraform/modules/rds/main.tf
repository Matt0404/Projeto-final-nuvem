resource "aws_db_subnet_group" "main" {
  name       = "modulos-db-subnet-group"
  subnet_ids = var.subnet_ids
 
  tags = merge(var.tags, {
    Name = "modulos-db-subnet-group"
  })
}
 
resource "aws_db_instance" "main" {
  identifier        = "modulos-database"
  engine            = "postgres"
  engine_version    = "17"
  instance_class    = "db.t3.micro"
  allocated_storage = 20
  storage_type      = "gp3"
 
  db_name  = "modulosdb"
  username = "dbadmin"
  password = var.db_password
 
  vpc_security_group_ids  = [var.db_sg_id]
  db_subnet_group_name    = aws_db_subnet_group.main.name
  backup_retention_period = 1
  storage_encrypted       = true
  publicly_accessible     = false
 
  tags = merge(var.tags, {
    Name = "modulos-database"
  })
}