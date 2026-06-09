output "db_instance_id" {
  value = aws_db_instance.main.id
}
 
output "database_endpoint" {
  value       = aws_db_instance.main.endpoint
  description = "RDS endpoint — passed to services as DB_URL"
  sensitive   = true
}