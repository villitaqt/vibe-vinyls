# ViveVinyls — Backend

Backend de un e-commerce de vinilos. Monolito modular (Spring Boot) con paquetes
por dominio. Este repositorio se construye por fases; **esta es la Fase 1: el
esqueleto desplegable** (arranca y responde `/health`, sin lógica de negocio aún).

## Stack

- Java 17, Spring Boot 3.3
- Maven
- Spring Web, Spring Data JPA (Hibernate), Spring Data Redis, Actuator, Lombok
- PostgreSQL 16, Redis 7

## Estructura de paquetes (por dominio)

```
com.vivevinyls
├── cuenta       # usuarios, auth, perfiles
├── catalogo     # vinilos, artistas, categorías
├── pedido       # carrito y órdenes
├── pago         # procesamiento de pagos
├── inventario   # stock
└── comun        # utilidades transversales (incluye /health)
```

Todos vacíos en esta fase salvo `comun`.

## Configuración

Las credenciales **no están hardcodeadas**; se leen de variables de entorno
(con valores por defecto para desarrollo local en `application.yml`):

| Variable            | Por defecto                                       |
|---------------------|---------------------------------------------------|
| `POSTGRES_URL`      | `jdbc:postgresql://localhost:5432/vivevinyls`     |
| `POSTGRES_USER`     | `vivevinyls`                                       |
| `POSTGRES_PASSWORD` | `vivevinyls`                                       |
| `REDIS_HOST`        | `localhost`                                        |
| `REDIS_PORT`        | `6379`                                             |
| `SERVER_PORT`       | `8080`                                             |

## Levantar el entorno local

### Opción A — todo en Docker (recomendado)

Levanta app + Postgres + Redis:

```bash
docker compose up --build
```

Comprobar que responde:

```bash
curl http://localhost:8080/health
# {"status":"UP"}

curl http://localhost:8080/actuator/health
# {"status":"UP", ...}
```

Parar y limpiar:

```bash
docker compose down          # conserva datos
docker compose down -v       # borra el volumen de Postgres
```

### Opción B — app local + dependencias en Docker

Solo Postgres y Redis en contenedores, la app desde Maven:

```bash
docker compose up -d postgres redis
mvn spring-boot:run
```

## Compilar y testear

```bash
mvn compile     # compila
mvn test        # tests (el contexto arranca con H2 en memoria, sin Postgres)
mvn package     # genera el JAR en target/
```

## Fases siguientes

La fuente de verdad del sistema es `ViveVinyls_Especificacion.md` (en la raíz).
La Fase 2 implementará el modelo de datos a partir de esa especificación.
