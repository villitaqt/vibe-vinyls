# Pendientes — Fase 1 (esqueleto Spring Boot)

Estado del proyecto ViveVinyls al cerrar esta sesión. Retomar desde aquí en un
chat nuevo de Claude Code.

## ✅ Ya hecho (archivos creados)

- `pom.xml` — Spring Boot 3.3.5, Java 17. Dependencias: Web, Data JPA, Data
  Redis, Actuator, Lombok, Test (JUnit 5), driver PostgreSQL, y H2 (solo `test`).
- `src/main/java/com/vivevinyls/VivevinylsApplication.java` — clase main.
- `src/main/java/com/vivevinyls/comun/web/HealthController.java` — `GET /health` → 200 `{"status":"UP"}`.
- Paquetes por dominio creados (vacíos, con `package-info.java`):
  `cuenta`, `catalogo`, `pedido`, `pago`, `inventario`, `comun`.
- `src/main/resources/application.yml` — Postgres + Redis parametrizados por
  variables de entorno (sin credenciales hardcodeadas).
- `src/test/resources/application.yml` — perfil de test con H2 en memoria.
- `src/test/java/.../VivevinylsApplicationTests.java` — test de contexto.
- `Dockerfile` (multi-stage: build Maven → runtime JRE).
- `docker-compose.yml` — app + postgres:16 + redis:7 (con healthchecks).
- `.gitignore`, `.dockerignore`, `README.md`.

## ✅ Verificación ya realizada

- `mvn clean test` ejecutado dentro de un contenedor Maven (Docker) → el
  contexto de Spring **arranca correctamente** con H2 (no requiere Postgres).
  Falta solo confirmar la línea final `BUILD SUCCESS` con un Maven local.

## ✅ Pendientes de la Fase 1 — TODOS COMPLETADOS (2026-06-21)

1. **Maven Wrapper añadido.** `mvnw`, `mvnw.cmd` y
   `.mvn/wrapper/maven-wrapper.properties` generados (vía Docker). El repo ya no
   depende de un Maven del sistema.
   - Nota de entorno: en local hay un **JDK 25** instalado, pero NO en el PATH y
     sin `JAVA_HOME`. Spring Boot 3.3.5 + Lombok no soportan oficialmente JDK 25,
     así que el build se hace con **Docker** (`maven:3.9-eclipse-temurin-17`),
     que es reproducible y no requiere instalar nada. Para correr `./mvnw` nativo
     habría que instalar/apuntar a un JDK 17 y ponerlo en el PATH.

2. **Build verde confirmado.** `mvn clean test` (en contenedor Temurin-17) →
   `BUILD SUCCESS`, `Tests run: 1, Failures: 0`. (Java 17.0.19 dentro del build.)
   ```powershell
   docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -B clean test
   ```

3. **Extremo a extremo verificado** con `docker compose up --build -d`:
   - `GET http://localhost:8080/health` → `{"status":"UP"}`
   - `GET http://localhost:8080/actuator/health` → `{"status":"UP","groups":["liveness","readiness"]}`
   - postgres:16 y redis:7 arrancan `Healthy` antes que la app.

4. **Git inicializado y commiteado.** Commit `30eb199`
   "fase 1: esqueleto Spring Boot desplegable" (20 archivos + wrapper).

## ⚠️ Bloqueante para la Fase 2

- **Falta `ViveVinyls_Especificacion.md` en la raíz del proyecto.** El directorio
  estaba vacío; la Fase 1 se construyó solo con lo enumerado en el prompt. Antes
  de empezar la Fase 2 (modelo de datos) hay que **añadir ese archivo**, que es
  la fuente de verdad.

## Notas / decisiones tomadas

- H2 se añadió SOLO en scope `test` para que el test de contexto arranque sin un
  Postgres real (el `application.yml` de test usa H2 en memoria, modo PostgreSQL).
- Redis usa Lettuce (conexión perezosa), así que no bloquea el arranque del test.
- `/health` se implementó como controller simple además de `/actuator/health`,
  para garantizar la ruta exacta pedida en el enunciado.
- `ddl-auto: none` (todavía no hay entidades).
