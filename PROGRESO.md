# Progreso ViveVinyls

Bitácora por fases. Retomar siempre desde aquí en un chat nuevo de Claude Code.

- **Fase 1 (esqueleto Spring Boot):** completada. Detalle en
  [`PENDIENTES_FASE1.md`](PENDIENTES_FASE1.md). Commit `30eb199`.
- **Fase 2 (modelo de datos):** completada (este documento). Commit
  "fase 2: modelo de datos y migraciones".
- **Fase 3 (catálogo, solo lectura):** completada (este documento). Commit
  "fase 3: catálogo (listado, búsqueda, filtros, ficha)".

---

## Análisis de código (SonarQube) — rama `sonarqube`

Rama `sonarqube` (desde `main`) para la actividad de análisis estático del curso.

- **JaCoCo agregado** (`jacoco-maven-plugin` 0.8.12 en el `pom.xml`): `prepare-agent`
  instrumenta la JVM antes de los tests y el goal `report` (fase `test`) genera
  `target/site/jacoco/jacoco.xml`, que es el formato que SonarQube lee para el % de
  cobertura. Sin esto SonarQube reporta 0% aunque haya tests.
- **Fix de configuración de test (no de código):** el perfil de test no definía
  `app.cors.allowed-origins`, propiedad que `CorsConfig` exige sin default desde la
  fase 3.5; el contexto de Spring fallaba al cargar. Se agregó la propiedad a
  `src/test/resources/application.yml` (espejando el default del perfil dev). No se
  tocaron tests ni código de aplicación.
- **Verificación:** `mvn -B clean verify` (Docker `maven:3.9-eclipse-temurin-17`) →
  **`BUILD SUCCESS`, `Tests run: 15, Failures: 0, Errors: 0`** y se genera
  `target/site/jacoco/jacoco.xml`.
- Pendiente (pasos posteriores): configurar SonarQube y el pipeline.

---

## Fase 3 — Catálogo, solo lectura (hecho)

Capa de servicio + web del módulo catálogo sobre las entidades JPA de la Fase 2.
Cubre CU-02 y RF-05 a RF-08. **No** toca cuentas, compra ni inventario
transaccional (eso es Fase 4+).

### Endpoints (sección 8 del contrato)

| Método | Endpoint | Qué hace |
|---|---|---|
| GET | `/vinilos` | Listado con búsqueda por texto (`q`), filtros opcionales (`genero`, `artista`, `anio`, `sello`) y paginación (`page`, `size`, `sort` vía `Pageable`). Cada vinilo trae su **stock disponible**. |
| GET | `/vinilos/{id}` | Ficha completa: datos, artistas, géneros, sello y stock disponible. **404** si no existe. |

### Decisiones de diseño

- **DTOs de respuesta, no entidades JPA:** `ViniloResumenDTO` (listado) y
  `ViniloDetalleDTO` (ficha), ambos como `record`. Las entidades nunca se
  serializan directamente.
- **Búsqueda y filtros con JPA Specifications** (`ViniloSpecifications` +
  `JpaSpecificationExecutor`): se eligió Specifications porque los cinco
  criterios son **opcionales y combinables**; con query methods habría que
  enumerar cada combinación. Cada criterio nulo/vacío no restringe. Texto (`q`)
  busca en título **o** nombre de artista (RF-07); los filtros que cruzan los
  puentes N:M marcan la query `distinct` para no duplicar vinilos. Comparaciones
  de nombres insensibles a may/min.
- **Stock disponible en el dominio inventario, reutilizable:** `StockService`
  (`disponible(viniloId)` y `disponiblePorVinilos(ids)` en lote). El cálculo es
  la **suma con signo del ledger** `MovimientoStock` (no un atributo), vía
  `@Query` con `coalesce(sum(...),0)`. El listado usa la variante en lote (una
  sola consulta con `group by`) para evitar N+1.
  - > El árbitro atómico de Redis (descuento de reservas en caliente durante la
    > compra) **no** se implementa aquí; llega en la fase de compra. Por ahora
    > disponible = suma del ledger, como pide el alcance de la fase.
- **404:** `ViniloNoEncontradoException` anotada con
  `@ResponseStatus(NOT_FOUND)`; el servicio la lanza desde `ficha(id)`.
- **Solo lectura:** `CatalogoService` es `@Transactional(readOnly = true)`.

### Verificación

- `mvn clean test` (Docker `maven:3.9-eclipse-temurin-17`) →
  **`BUILD SUCCESS`, `Tests run: 15, Failures: 0, Errors: 0`** (6 nuevos).
  ```powershell
  docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -B clean test
  ```
- `CatalogoControllerTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`,
  end-to-end contra H2) cubre lo pedido: listado con stock desde el ledger,
  filtro por género, búsqueda por texto (en título y en artista), paginación,
  ficha existente y ficha 404.
- **Seed solo en test:** el dataset (3 vinilos con sus dimensiones y movimientos
  de stock) se crea en `@BeforeEach` y se revierte por test gracias a
  `@Transactional`. No hay seed en dev/producción.

---

## Fase 2 — Modelo de datos (hecho)

Capa de persistencia del núcleo MVP: entidades JPA + repositorios Spring Data,
organizados por dominio. **Sin endpoints ni lógica de servicios** (eso es Fase 3+).

### Decisiones de diseño transversales

- **IDs:** `UUID` en todas las entidades, generados por Hibernate
  (`@GeneratedValue(strategy = GenerationType.UUID)`). Consistente en todo el modelo.
- **Dinero:** `BigDecimal` con `precision = 12, scale = 2` (`precio`, `total`,
  `precio_unitario`, `monto`).
- **Enums:** persistidos como texto (`@Enumerated(EnumType.STRING)`), no ordinal,
  para que el esquema sea legible y estable ante reordenamientos.
- **Timestamps:** `Instant` con `@CreationTimestamp` (Hibernate).
- **Esquema gestionado por Hibernate, SIN Flyway** (decisión de la spec/enunciado):
  - perfil dev (`src/main/resources/application.yml`): `ddl-auto: update`.
  - perfil test (`src/test/resources/application.yml`): `ddl-auto: create-drop`
    sobre la H2 en memoria que ya existía (modo PostgreSQL).
  - > Nota: el enunciado menciona "las migraciones Flyway corren limpio" en la
    > lista de salida, pero el cuerpo del mismo pide explícitamente NO usar Flyway
    > y dejar el esquema a Hibernate. Se siguió la instrucción detallada: no hay
    > Flyway. No hay migraciones que correr; el esquema lo deriva Hibernate.

### Entidades y repositorios por dominio

| Dominio | Entidades | Repositorios |
|---|---|---|
| `catalogo` | `Vinilo`, `Artista`, `Genero`, `Sello`, puentes `ViniloArtista` / `ViniloGenero` (con `@EmbeddedId` compuesto) | uno por entidad |
| `cuenta` | `Cliente`, `Direccion` (libreta 1→N) | `ClienteRepository` (`findByEmail`), `DireccionRepository` (`findByClienteId`) |
| `pedido` | `Pedido` (+ enum `EstadoPedido`), `ItemPedido` | `PedidoRepository` (`findByClienteId`), `ItemPedidoRepository` (`findByPedidoId`) |
| `pago` | `Pago` (+ enum `EstadoPago`) | `PagoRepository` (`findByPedidoId`) |
| `inventario` | `MovimientoStock` (+ enum `TipoMovimiento`) | `MovimientoStockRepository` (`findByViniloId`) |

### Cómo se respetaron las reglas y notas de la spec

- **N:M con entidades puente:** `VINILO_ARTISTA` y `VINILO_GENERO` se modelan
  como entidades explícitas con clave compuesta (`@EmbeddedId` + `@MapsId`),
  fieles al diccionario (sección 5.1). El `SELLO` es `@ManyToOne` (uno por vinilo).
- **Stock como cálculo, no atributo (sección 5, nota):** `Vinilo` NO tiene campo
  de stock. `MovimientoStock` es un ledger **append-only** (campos `updatable = false`)
  con `cantidad` con signo; el stock físico es la suma del ledger (verificado en test).
- **RN-06 (precio congelado):** `ItemPedido` guarda su propio `precioUnitario` y
  `cantidad`; no se leen del vinilo.
- **RN-07 (dirección congelada):** `Pedido` guarda una copia propia de la
  dirección de envío (campos `envio*`) vía `copiarDireccionEnvio(...)`, más una FK
  opcional `direccionOrigen` solo para trazabilidad (coincide con el diagrama ER).
- **RN-04 (sin datos de tarjeta):** `Pago` solo guarda estado, monto y
  `referenciaExterna` de la pasarela. Un pedido admite varios intentos de pago.
- **Estados:** `EstadoPedido` = PENDIENTE_PAGO, PAGADO, CONFIRMADO, CANCELADO.
  `EstadoPago` = PENDIENTE, AUTORIZADO, CAPTURADO, FALLIDO, REEMBOLSADO.
  `TipoMovimiento` = IMPORTACION, RESERVA, CONFIRMACION, CANCELACION.
- **Cuenta sin credenciales (RNF-03):** `Cliente` no guarda contraseña; la
  autenticación es de Cognito (campo `cognitoSub` para enlazar el JWT).

### Verificación

- `mvn clean test` en contenedor `maven:3.9-eclipse-temurin-17` →
  **`BUILD SUCCESS`, `Tests run: 9, Failures: 0, Errors: 0`**.
  ```powershell
  docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -B clean test
  ```
- Un test de repositorio por entidad núcleo (persistir + leer), sobre el perfil
  de test con H2:
  - `CatalogoRepositoryTest` — sello/artista/género, vinilo+sello, puentes N:M.
  - `CuentaRepositoryTest` — cliente por email, libreta de direcciones.
  - `PedidoRepositoryTest` — pedido + ítem, copia congelada (RN-07), precio
    congelado (RN-06).
  - `PagoRepositoryTest` — varios intentos de pago sobre un pedido (RN-04).
  - `MovimientoStockRepositoryTest` — ledger con/sin pedido, stock = suma con signo.
- El test de contexto `VivevinylsApplicationTests` (`@SpringBootTest`) sigue verde:
  Hibernate genera el esquema completo desde las entidades sobre H2.

### Entorno de build (igual que Fase 1)

Local hay JDK 25 (no soportado por Spring Boot 3.3.5 + Lombok) y no está en el
PATH. El build se hace con Docker (Temurin 17), reproducible. Para `./mvnw` nativo
habría que instalar/apuntar a un JDK 17.

---

## Pendiente para Fase 4 (cuentas) y posteriores

- **Fase 4 — Cuentas (MVP):** CU-01 (registro/verificación) y RF-02/RF-03.
  Endpoints del contrato: `POST /auth/registro`, `POST /auth/verificar`,
  `POST /auth/login`, `GET/POST /clientes/me/direcciones`. Autenticación con
  JWT de Cognito (el backend como resource server; `Cliente.cognitoSub` enlaza
  el JWT). El `Cliente` no guarda contraseña (RNF-03).
- **Compra (CU-03/04):** checkout con reserva de stock **antes** del cobro
  (árbitro atómico Redis), creación de pedido, congelado de precio/dirección,
  pago simulado, confirmación. Aquí entra el TDD de la lógica crítica
  (total, prevención de sobreventa, transición de estados).
- **Inventario:** servicio de cálculo de stock (físico / reservado / disponible)
  sobre el ledger.
- **Entidades `[+]` no construidas aún** (fuera del MVP): `PROMOCION`, `STAFF`,
  `RESENA`, `ENVIO`.
