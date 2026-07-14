variable "aws_region" {
  description = "Región de AWS donde se crea toda la infraestructura"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nombre del proyecto, usado como prefijo en nombres y tags de todos los recursos"
  type        = string
  default     = "vivevinyls"
}
