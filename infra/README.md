# Infra — Módulo 1: Networking

Primer módulo de infraestructura como código (IaC) de ViveVinyls. Crea la red
base en AWS sobre la que se va a montar el resto de la infraestructura
(seguridad, base de datos, servidor de la aplicación).

El objetivo de este módulo es **demostrar los conceptos básicos de
networking en AWS de forma simple y defendible**, no construir una
arquitectura lista para producción.

## ¿Qué crea este módulo?

- 1 VPC (`10.0.0.0/16`).
- 3 subnets, todas en la misma zona de disponibilidad (`us-east-1a`):
  - **Pública** (`10.0.1.0/24`): aquí va el Application Load Balancer (ALB),
    que es el único punto de entrada desde internet hacia la aplicación.
  - **Privada de aplicación** (`10.0.11.0/24`): aquí va la instancia EC2 que
    corre el backend. No recibe conexiones directas desde internet, pero sí
    puede salir a internet (por ejemplo, para descargar imágenes Docker)
    gracias al NAT Gateway.
  - **Privada de datos** (`10.0.21.0/24`): aquí van RDS (base de datos) y
    Redis. Es la subnet más protegida: no tiene ninguna ruta hacia internet,
    solo puede recibir tráfico interno de la propia VPC (por ejemplo, desde
    la subnet de aplicación).
- 1 Internet Gateway: la puerta de entrada/salida de la VPC hacia internet.
- 1 NAT Gateway (con su Elastic IP): permite que la subnet privada de
  aplicación salga a internet sin que nadie desde afuera pueda entrar
  directamente a ella.
- 3 Route Tables (una por subnet), que definen hacia dónde va el tráfico que
  sale de cada una.

## ¿Por qué solo 1 zona de disponibilidad (AZ)?

En una arquitectura de producción real se usarían 2 o más AZs por cada capa,
para que si una zona de AWS falla, la aplicación siga funcionando en la otra.
Eso implicaría duplicar cada subnet, y más adelante duplicar también el NAT
Gateway, las instancias EC2, etc.

Para este proyecto académico, usar **1 sola AZ** es una decisión consciente
de simplicidad: el objetivo es demostrar el concepto de segmentación de red
(pública / privada / aislada) y cómo se controla el tráfico entre esas capas,
sin la complejidad adicional de la alta disponibilidad multi-AZ. No es una
omisión ni un descuido — es un recorte de alcance justificado por el
propósito del curso.

## ¿Por qué state local (sin backend remoto)?

Terraform guarda el estado de la infraestructura en un archivo (`.tfstate`).
Por defecto lo guarda localmente, y eso es lo que usamos aquí: no hay backend
en S3 ni tabla de bloqueo en DynamoDB. Para un proyecto académico que se
ejecuta desde una sola máquina, el state local es suficiente — un backend
remoto solo aporta valor cuando varias personas o pipelines aplican cambios
al mismo tiempo.

**Importante:** el archivo `.tfstate` contiene información real de los
recursos creados en la cuenta de AWS, así que nunca se sube al repositorio
(ver `.gitignore`).

## Cómo correrlo

### 1. Configurar credenciales de AWS

Este módulo necesita credenciales de AWS configuradas en la máquina donde se
ejecuta (por ejemplo, vía `aws configure`, variables de entorno
`AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`, o un perfil de AWS CLI).

### 2. Configurar variables

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars
```

Los valores por defecto (`us-east-1`, `vivevinyls`) ya son los correctos para
este proyecto; normalmente no hace falta editar el archivo.

### 3. Inicializar, validar y planear

```bash
terraform init      # descarga el provider de AWS
terraform validate  # revisa que el código sea sintácticamente correcto
terraform plan       # muestra qué recursos se van a crear, SIN crearlos
```

`terraform plan` debe mostrar la creación de: 1 VPC, 3 subnets, 1 Internet
Gateway, 1 NAT Gateway + su Elastic IP, y 3 route tables (con sus
asociaciones).

### 4. Aplicar (crear los recursos reales)

```bash
terraform apply
```

**Ojo con el costo:** el NAT Gateway tiene un costo por hora mientras esté
activo (además de un costo por GB de datos procesados). Es el único recurso
de este módulo que no es gratuito en el tier básico de AWS. No lo dejes
corriendo si no lo estás usando — para destruir todo lo creado por este
módulo:

```bash
terraform destroy
```

## Qué le da este módulo al resto de la infraestructura

Los outputs (`vpc_id`, `public_subnet_id`, `private_app_subnet_id`,
`private_data_subnet_id`) son los IDs que van a necesitar los próximos
módulos (grupos de seguridad, RDS, EC2) para saber en qué VPC y en qué subnet
crear sus recursos.
