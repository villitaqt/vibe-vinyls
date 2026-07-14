terraform {
  required_version = ">= 1.5"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # State local (default de Terraform): suficiente para un proyecto académico
  # ejecutado desde una sola máquina. No se usa backend remoto (S3/DynamoDB).
}

provider "aws" {
  region = var.aws_region
}

# ---------------------------------------------------------------------------
# VPC: la red privada donde vive toda la infraestructura del proyecto.
# ---------------------------------------------------------------------------
resource "aws_vpc" "main" {
  cidr_block           = "10.0.0.0/16"
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = {
    Name = "${var.project_name}-vpc"
  }
}

# ---------------------------------------------------------------------------
# Subnets: dividimos la VPC en 3 zonas según qué tan expuesto a internet debe
# estar cada recurso. Se usa una sola AZ (us-east-1a) para mantener el módulo
# simple; en producción real se duplicaría cada subnet en 2+ AZs para alta
# disponibilidad, pero eso no es necesario para demostrar el concepto en este
# proyecto académico.
# ---------------------------------------------------------------------------

# Subnet pública: aquí vive el ALB, el único punto de entrada desde internet.
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block               = "10.0.1.0/24"
  availability_zone        = "us-east-1a"
  map_public_ip_on_launch  = true

  tags = {
    Name = "${var.project_name}-public"
  }
}

# Subnet privada de aplicación: aquí vive la EC2 con el backend. No es
# alcanzable directamente desde internet, pero sí puede salir a internet
# (por ejemplo para hacer pull de imágenes Docker) a través del NAT Gateway.
resource "aws_subnet" "private_app" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.11.0/24"
  availability_zone = "us-east-1a"

  tags = {
    Name = "${var.project_name}-private-app"
  }
}

# Subnet privada de datos: aquí viven RDS y Redis. Es la subnet más
# protegida — no tiene ruta de salida a internet, solo tráfico interno de la
# VPC (por ejemplo, desde la subnet de aplicación).
resource "aws_subnet" "private_data" {
  vpc_id            = aws_vpc.main.id
  cidr_block        = "10.0.21.0/24"
  availability_zone = "us-east-1a"

  tags = {
    Name = "${var.project_name}-private-data"
  }
}

# ---------------------------------------------------------------------------
# Internet Gateway: la puerta de la VPC hacia internet. Sin esto, ningún
# recurso de la VPC es alcanzable desde afuera (ni puede alcanzar afuera).
# ---------------------------------------------------------------------------
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-igw"
  }
}

# ---------------------------------------------------------------------------
# NAT Gateway: permite que la subnet privada de aplicación salga a internet
# (por ejemplo, para hacer pull de imágenes Docker) sin exponerla a conexiones
# entrantes desde internet. Vive en la subnet pública y necesita una IP
# elástica propia.
# ---------------------------------------------------------------------------
resource "aws_eip" "nat" {
  domain = "vpc"

  tags = {
    Name = "${var.project_name}-nat-eip"
  }
}

resource "aws_nat_gateway" "main" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public.id

  tags = {
    Name = "${var.project_name}-nat"
  }

  depends_on = [aws_internet_gateway.main]
}

# ---------------------------------------------------------------------------
# Route Tables: una por subnet, cada una define hacia dónde sale el tráfico
# que no es interno de la VPC.
# ---------------------------------------------------------------------------

# Pública: todo el tráfico externo sale directo por el Internet Gateway.
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = {
    Name = "${var.project_name}-rt-public"
  }
}

resource "aws_route_table_association" "public" {
  subnet_id      = aws_subnet.public.id
  route_table_id = aws_route_table.public.id
}

# Privada-app: el tráfico externo sale a través del NAT Gateway, así el
# backend puede salir a internet sin recibir conexiones entrantes directas.
resource "aws_route_table" "private_app" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main.id
  }

  tags = {
    Name = "${var.project_name}-rt-private-app"
  }
}

resource "aws_route_table_association" "private_app" {
  subnet_id      = aws_subnet.private_app.id
  route_table_id = aws_route_table.private_app.id
}

# Privada-data: sin ruta a internet. Solo tráfico interno de la VPC (la ruta
# local que Terraform/AWS agrega automáticamente). Esta es la subnet más
# protegida, donde vive la base de datos.
resource "aws_route_table" "private_data" {
  vpc_id = aws_vpc.main.id

  tags = {
    Name = "${var.project_name}-rt-private-data"
  }
}

resource "aws_route_table_association" "private_data" {
  subnet_id      = aws_subnet.private_data.id
  route_table_id = aws_route_table.private_data.id
}
