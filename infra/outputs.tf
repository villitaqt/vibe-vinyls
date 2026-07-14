output "vpc_id" {
  description = "ID de la VPC, lo necesitan los módulos de seguridad, base de datos y servidor"
  value       = aws_vpc.main.id
}

output "public_subnet_id" {
  description = "ID de la subnet pública, donde va el ALB"
  value       = aws_subnet.public.id
}

output "private_app_subnet_id" {
  description = "ID de la subnet privada de aplicación, donde va la EC2 del backend"
  value       = aws_subnet.private_app.id
}

output "private_data_subnet_id" {
  description = "ID de la subnet privada de datos, donde van RDS y Redis"
  value       = aws_subnet.private_data.id
}
