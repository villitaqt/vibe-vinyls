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

## ⬜ Pendientes para cerrar la Fase 1

1. **Resolver Maven local.** Java fue instalado pero NO aparece en el PATH de la
   terminal (ni `java` ni `JAVA_HOME`). Maven no está instalado.
   - Opción recomendada: añadir el **Maven Wrapper** (`mvnw`) para no depender de
     un Maven del sistema. Bootstrap con Docker (no requiere Maven local):
     ```powershell
     docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -N wrapper:wrapper
     ```
     Luego, con Java en el PATH: `./mvnw -B clean test`.
   - Configurar `JAVA_HOME` y añadir `%JAVA_HOME%\bin` al PATH (abrir terminal
     nueva tras instalar). Confirmar con `java -version`.
   - Alternativa: instalar Maven (`winget install Apache.Maven`) o usar IntelliJ
     (trae Maven y un JDK embebidos → Run/Maven panel).

2. **Confirmar build verde local:** `./mvnw -B clean test` debe terminar en
   `BUILD SUCCESS` (Tests run: 1, Failures: 0).

3. **Verificar que levanta de extremo a extremo** (cualquiera de las dos):
   - `docker compose up --build` y luego:
     - `curl http://localhost:8080/health` → `{"status":"UP"}`
     - `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`
   - o `docker compose up -d postgres redis` + `./mvnw spring-boot:run`.

4. **Inicializar git y commitear** (el repo aún NO está inicializado):
   ```powershell
   git init
   git add -A
   git commit -m "fase 1: esqueleto Spring Boot desplegable"
   ```

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
