# Progreso ViveVinyls

Bitácora por fases. Retomar siempre desde aquí en un chat nuevo de Claude Code.

- **Fase 1 (esqueleto Spring Boot):** completada. Detalle en
  [`PENDIENTES_FASE1.md`](PENDIENTES_FASE1.md). Commit `30eb199`.
- **Fase 2 (modelo de datos):** completada (este documento). Commit
  "fase 2: modelo de datos y migraciones".
- **Fase 3 (catálogo, solo lectura):** completada (este documento). Commit
  "fase 3: catálogo (listado, búsqueda, filtros, ficha)".
- **Fase 4 (cuentas):** completada (este documento). Registro/verificación,
  login JWT y libreta de direcciones.
- **Fase 5a (compra — checkout):** completada (este documento). Reserva atómica
  en Redis + creación de pedido con precio y dirección congelados.
- **Fase 5b (compra — pago y cierre):** completada (este documento). Pago
  simulado, consolidación de reservas y expiración. **El backend del MVP queda
  funcionalmente completo** (registro → catálogo → checkout → pago → constancia).
- **Ajuste (portadaUrl + rol en JWT):** completado (este documento). Previo al
  frontend.
- **Fixes post-MVP (`GET /pedidos/me` + rename `RolCliente` → `Rol`):**
  completados (este documento).

---

## Fixes post-MVP (hecho)

Dos cambios pequeños e independientes tras cerrar el MVP funcional.

### `GET /pedidos/me` — historial de pedidos del cliente

- El frontend necesitaba el historial de compras del cliente autenticado, que
  no existía como endpoint dedicado.
- **Mapping `GET /me` declarado ANTES de `GET /{id}`** en `PedidoController`:
  si no, Spring intenta resolver `/pedidos/me` contra `/pedidos/{id}` e
  interpreta `"me"` como un `UUID`, lo que revienta con 400 en vez de resolver
  la ruta correcta.
- `PedidoResumenDTO` (record) nuevo: `pedidoId`, `fechaCreacion`, `estado`,
  `total`, `cantidadItems` (suma de cantidades de los ítems) y
  `estadoPagoUltimo` (nullable — nombre del estado del último `Pago`, o `null`
  si el pedido no tiene ningún intento).
- `PedidoService.pedidosDelCliente(clienteId)`: reutiliza
  `PedidoRepository.findByClienteId` (existente desde Fase 2), ordena en
  memoria por `fechaCreacion` descendente y resuelve el último pago por pedido
  con el mismo patrón que ya usaba `obtener(...)` para la constancia (RF-13).
  `@Transactional(readOnly = true)`.
- Sin cambios en `SecurityConfig`: `/pedidos/**` ya exige autenticación por el
  default `authenticated`.

### Refactor `RolCliente` → `Rol`

- Rename puro (sin cambio de comportamiento): el enum vivía como `RolCliente`
  pero la documentación y el diseño siempre lo llamaron `Rol`. El valor en
  base de datos no cambia (`@Enumerated(EnumType.STRING)`: `'CLIENTE'`,
  `'STAFF'`, `'ADMIN'`).
- Archivos tocados: `Rol.java` (antes `RolCliente.java`), `Cliente.java`
  (campo `rol`) y `DevDataSeeder.java`. `TokenService` ya leía el rol vía
  `cliente.getRol().name()`, sin referenciar el tipo por nombre — no
  requirió cambios.

### Verificación

- `mvn -B clean test` (Maven local, JDK 17 Temurin ya disponible en el
  entorno — antes solo se corría en Docker) →
  **`BUILD SUCCESS`, `Tests run: 57, Failures: 0, Errors: 0`** (4 nuevos):
  `GET /pedidos/me` sin token (401), sin pedidos (`[]`), con pedidos propios
  ordenados por fecha descendente, y aislamiento (no devuelve pedidos ajenos).

---

## Ajuste — portadaUrl en Vinilo y rol en el JWT (hecho)

Dos cambios pequeños e independientes, previos al frontend completo. No tocan
lógica de negocio existente.

### portadaUrl en el catálogo

- `Vinilo.portadaUrl` (`String`, **nullable**): URL de portada. Sin imágenes
  reales aún; el frontend usa placeholder cuando es `null`. Columna nueva nullable
  → la añade Hibernate (`ddl-auto: update`/`create-drop`), sin migración manual.
- Añadido a `ViniloResumenDTO` y `ViniloDetalleDTO` y al mapeo en `CatalogoService`.
- **Sin** carrusel ni tabla de múltiples imágenes (fuera de alcance): solo el
  campo singular.

### Rol en el JWT

- **Dónde vive el rol:** campo `Cliente.rol` (enum `RolCliente` STRING: `CLIENTE`,
  `STAFF`, `ADMIN`; default `CLIENTE`). Se eligió el campo directo en `Cliente`
  (camino recomendado): es el sujeto que se autentica y evita una tabla nueva. La
  columna es `nullable = false`; al registrarse, todo cliente nace `CLIENTE`.
- **JWT:** `TokenService` añade el claim `role` (string) junto a `sub` y `email`.
- **`ClienteDTO`** (`id`, `email`, `nombre`, `rol`) se incluye en `LoginResponse`
  (campo `cliente`) para que el frontend tenga el rol sin decodificar el JWT;
  `clienteId` se mantiene por compatibilidad.
- **No** hay endpoints de gestión de roles (eso es back-office). `SecurityConfig`
  sin cambios (ninguna ruta nueva protegida por rol todavía).
- **Crear un usuario STAFF/ADMIN de prueba manualmente** (no hay endpoint): tras
  registrar y verificar el cliente, promover su rol por SQL directo, p. ej.
  ```sql
  UPDATE cliente SET rol = 'ADMIN' WHERE email = 'admin@vivevinyls.com';
  ```
  (o, en un test, setear `cliente.setRol(RolCliente.STAFF)` antes de emitir el token).

### Verificación

- `mvn clean test` (Docker `maven:3.9-eclipse-temurin-17`) →
  **`BUILD SUCCESS`, `Tests run: 53, Failures: 0, Errors: 0`** (2 nuevos):
  `portadaUrl` viaja en el DTO (valor cuando se setea, `null` cuando no) y el login
  expone `cliente.rol` = `CLIENTE` con el claim `role` presente en el JWT emitido.

---

## Fase 5b — Compra: pago simulado, confirmación y expiración (hecho)

Cierra el flujo de venta del MVP (CU-04 + pasos finales de CU-03). La lógica
crítica es la **máquina de estados del pedido** (TDD). Reutiliza el árbitro y la
creación de pedidos de 5a; añade pago, consolidación y liberación de reservas.

### Endpoints

| Método | Endpoint | CU/RF | Qué hace |
|---|---|---|---|
| POST | `/pedidos/{id}/pago` | CU-04 | Intenta el pago (pasarela simulada), registra un `Pago` y, si captura, transita el pedido a `PAGADO` y consolida la reserva. 200 (captura o rechazo); 409 estado inválido; 404 ajeno/inexistente. |
| GET | `/pedidos/{id}` | RF-13 | (enriquecido) ahora incluye `ultimoPago` → sirve de **constancia** del resultado. |

Expiración de reservas: **servicio invocable** `expirarReservasVencidas()` + un
`@Scheduled` que solo lo dispara (planificación activa fuera del perfil test).

### Máquina de estados del pedido (lógica crítica, documentada en `EstadoPedido`)

```
PENDIENTE_PAGO → PAGADO      (pasarela captura)            — MVP
PENDIENTE_PAGO → CANCELADO   (reserva expirada)            — MVP
PAGADO         → CONFIRMADO  (staff confirma despacho)     — back-office [+]
PAGADO         → CANCELADO   (cancelación con reembolso)   — back-office [+]
```

- **El pago capturado lleva a `PAGADO`, NO a `CONFIRMADO`** (corrección
  documentada en el commit `fix(pedido): ...` previo a esta fase). `CONFIRMADO` y
  `PAGADO → CANCELADO` quedan **soportados estructuralmente pero sin implementar**
  (back-office `[+]`).
- Pagar un pedido que no está en `PENDIENTE_PAGO` → `409`, sin efectos.

### Decisiones de diseño

- **Pasarela tras interfaz `PasarelaPago`** (`cobrar(monto, resultadoSimulado) →
  ResultadoCobro`), impl `PasarelaPagoSimulada`. **RN-04:** nunca se reciben ni
  guardan datos de tarjeta; el `Pago` solo lleva estado, monto y
  `referenciaExterna` (un `SIMUL-<uuid>` ficticio). El request acepta
  `resultadoSimulado` (`CAPTURA`/`RECHAZO`, default `CAPTURA`) — **artefacto
  temporal** de la simulación que desaparece con la pasarela real.
- **Cada intento es un `Pago` nuevo** (un pedido admite varios). Captura →
  `CAPTURADO`; rechazo → `FALLIDO`.
- **Consolidación al capturar (sin tocar Redis):** se registra `CONFIRMACION`
  **neutral (cantidad 0)** por ítem — trazabilidad de la venta sin alterar el
  disponible: la unidad ya salió en la `RESERVA` (5a) y se queda fuera porque se
  vendió. El invariante `disponible = Σ ledger` se mantiene.
- **Rechazo (flujo 4a):** el `Pago` queda `FALLIDO`, el pedido **sigue**
  `PENDIENTE_PAGO` y la reserva **NO** se libera (el cliente puede reintentar);
  solo la expiración libera. Tanto captura como rechazo responden **200** (el
  cliente interpreta el desenlace desde el cuerpo, uniforme para el front).
- **Expiración (CU-03 4a):** `ExpiracionReservasService.expirarReservasVencidas()`
  busca pedidos `PENDIENTE_PAGO` con `fechaCreacion < now − ttl`; por cada uno, en
  **su propia transacción** (`CancelacionPedidoService`, bean aparte para que el
  proxy aplique), libera Redis (`ArbitroStock.compensar`, +unidades), registra
  `CANCELACION` (+) por ítem y pasa el pedido a `CANCELADO`. El disponible vuelve
  al valor previo a la reserva. Ventana configurable
  `app.pedido.reserva-ttl-seconds` (`${VAR:900}`).
- **Idempotencia del job:** la guardia es la transición `PENDIENTE_PAGO →
  CANCELADO`; si ya no está pendiente, se omite. Correr dos veces no doble-libera.
  *Límite conocido:* en single-instance basta; multi-instancia requeriría un lock
  distribuido (fuera del MVP).
- **Scheduler desactivado en test** (`SchedulingConfig` con `@EnableScheduling
  @Profile("!test")`): el método se invoca a mano con un corte determinista
  (`expirarAnterioresA(Instant)`) para que las pruebas no dependan del reloj.
- **Constancia (RF-13):** `GET /pedidos/{id}` incluye `ultimoPago` (estado,
  referencia, monto). `PedidoService` lee el último intento vía `PagoRepository`
  (acoplamiento pedido→pago acotado a esta lectura; `Pago` ya referencia `Pedido`).
- DTOs como `record`; excepciones con `@ResponseStatus` (`EstadoPedidoInvalido`
  409, reutiliza `PedidoNoEncontrado` 404). Sin Flyway, sin deps nuevas.

### Verificación

- `mvn clean test` (Docker `maven:3.9-eclipse-temurin-17`) →
  **`BUILD SUCCESS`, `Tests run: 51, Failures: 0, Errors: 0`** (10 nuevos).
- **TDD máquina de estados** (`PagoControllerTest`): captura → `PAGADO` con
  `CONFIRMACION` neutral; rechazo → sigue `PENDIENTE_PAGO`, reserva no liberada;
  reintento tras fallo → `PAGADO`; pagar `PAGADO` → 409; RN-04 (Pago sin tarjeta);
  constancia con `ultimoPago`; pago ajeno → 404.
- **Expiración** (`ExpiracionReservasServiceTest`): vencido → `CANCELADO` con
  Redis y ledger restaurados; idempotencia (segunda corrida no libera); pedido
  `PAGADO` intacto.

### Deuda conocida al cerrar el MVP

- **Auth local → Cognito** (solo cambia el decoder en `SecurityConfig`).
- **Redis ↔ ledger sin reconciliación automática** (límite aceptado de 5a).
- **Back-office `[+]` necesario** para cerrar el ciclo `PAGADO → CONFIRMADO` (y
  `PAGADO → CANCELADO` con reembolso).

---

## Fase 5a — Compra: checkout con reserva atómica (hecho)

Caso central del MVP (CU-03): `POST /pedidos` reserva stock atómicamente y, si
hay, crea el pedido en `PENDIENTE_PAGO` con precio y dirección **congelados**;
`GET /pedidos/{id}` (RF-13) lo devuelve solo a su dueño. **No** incluye pago,
confirmación, consolidación ni expiración (eso es Fase 5b).

### Endpoints (sección 8 del contrato)

| Método | Endpoint | CU/RF | Qué hace |
|---|---|---|---|
| POST | `/pedidos` | CU-03 | Checkout: reserva atómica (Redis) y, si hay stock, crea el pedido `PENDIENTE_PAGO` con `RESERVA` en el ledger. 201 con el pedido; 409 (agotado) sin crear nada; 404 vinilo inexistente; 400 datos/dirección inválidos. |
| GET | `/pedidos/{id}` | RF-13 | Estado y detalle del pedido. 404 si no existe **o es ajeno** (privacidad). |

`/pedidos/**` exige autenticación (default `authenticated` de `SecurityConfig`;
401 sin token, verificado en test). CORS añadido para `/pedidos/**`.

### Decisiones de diseño

- **Árbitro de stock tras una interfaz (`ArbitroStock`)** — `reservar(List<ItemReserva>)
  → ResultadoReserva` y `compensar(...)`. Dos implementaciones con **idéntica
  semántica atómica** (check-and-decrement todo-o-nada):
  - **Producción `ArbitroStockRedis`** (`@Profile("!test")`): contador por vinilo
    en `stock:disp:{viniloId}`; **siembra perezosa** desde el ledger con `SET NX`
    (segura ante carrera); **reserva atómica multi-ítem con script Lua**
    (`DefaultRedisScript`, `redis/reservar-stock.lua`): verifica que **todas** las
    claves alcancen y solo entonces las decrementa todas; si alguna falta, no toca
    ninguna y devuelve los agotados. Evita reservas parciales.
  - **Test `ArbitroStockEnMemoria`** (`@Profile("test")`, en `src/test`): mapa +
    `synchronized` (Redis corre el Lua en un solo hilo; serializar reproduce esa
    atomicidad). **Testcontainers no es viable** (build dentro de
    `maven:...temurin-17` sin Docker-in-Docker), así que el perfil `test` cablea
    este doble; el test de sobreventa corre contra él. `application.yml` de test:
    `spring.profiles.active: test`.
- **Convención de signos del ledger (invariante `disponible = Σ ledger`):**
  `IMPORTACION` (+), `RESERVA` (−, baja el disponible al reservar),
  `CANCELACION` (+, libera reserva expirada — 5b), `CONFIRMACION` **neutral**
  respecto al disponible (la unidad ya salió vía `RESERVA`; en 5b marca la venta).
- **Redis caliente / ledger durable + límite conocido:** Redis es la verdad del
  "disponible en caliente" (decisión de reserva); el ledger en Postgres es la
  verdad durable y auditable, y se siembra Redis desde él. En este MVP **no hay
  reconciliación automática Redis→ledger** más allá de la escritura del checkout.
  Es un límite aceptado.
- **Catálogo sin tocar:** `StockService` sigue leyendo del ledger (no se
  refactorizó); la divergencia temporal (reservas en vuelo aún no consolidadas)
  es aceptable para CU-02.
- **Orden del checkout (diagrama 7.1):** validar entrada + existencia → **reservar
  antes de tocar Postgres** (RN-05) → si hay stock, persistir en **una sola
  transacción**. La existencia del vinilo se valida **antes** de reservar para dar
  404 (y no un 409 de "agotado") cuando no existe.
- **Compensación ante fallo de persistencia:** si la reserva tuvo éxito pero la
  transacción falla, se `compensar(...)` (devuelve unidades al árbitro) y se
  relanza. La creación vive en un bean aparte (`PedidoCreacionService`,
  `@Transactional`) para que el proxy aplique y la compensación envuelva el commit.
- **Congelado (RN-06, RN-07):** `ItemPedido.precioUnitario` se copia del vinilo al
  momento (no se referencia vivo); la dirección se copia con el
  `copiarDireccionEnvio(...)` existente y se guarda `direccionOrigen` (trazabilidad).
  El total = Σ(precio × cantidad) lo calcula `CalculoPedido` (**lógica crítica →
  TDD**). Líneas duplicadas del mismo vinilo se fusionan.
- **DTOs como `record`** (`CrearPedidoRequest`, `PedidoResponse`…), nunca entidades.
  Excepciones de dominio con `@ResponseStatus`: `PedidoNoEncontrado` 404,
  `PedidoInvalido` 400, y reutiliza `ViniloNoEncontradoException` 404. El **409**
  con la lista de agotados se entrega vía un `@RestControllerAdvice` mínimo
  (`PedidoExceptionHandler`), porque `@ResponseStatus` no puede llevar cuerpo
  dinámico.

### Verificación

- `mvn clean test` (Docker `maven:3.9-eclipse-temurin-17`) →
  **`BUILD SUCCESS`, `Tests run: 41, Failures: 0, Errors: 0`** (16 nuevos).
  ```powershell
  docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -B clean test
  ```
- **TDD lógica crítica:** `CalculoPedidoTest` (total con cantidades distintas);
  `ArbitroStockEnMemoriaTest` (**sobreventa, el más importante**): última unidad
  con 2 hilos → exactamente 1 gana; stock N con 50 solicitudes concurrentes →
  éxitos == N (nunca sobreventa, disponible nunca negativo).
- `PedidoControllerTest` (end-to-end, H2 + árbitro in-memory): total/estado y
  reserva (invariante del ledger 10−2=8), precio congelado (RN-06), dirección
  congelada (RN-07), 409 con vinilo agotado, segundo checkout que agota (409),
  vinilo inexistente (404), dirección ajena (400), cantidad inválida (400), 401
  sin token (POST y GET) y pedido ajeno (404).

## Fase 4 — Cuentas (hecho)

Auth local con JWT + libreta de direcciones sobre las entidades de cuenta de la
Fase 2. Cubre CU-01 (registro/verificación), RF-02 (login) y RF-03 (direcciones).
**Solo backend** (el frontend es fase posterior).

### Endpoints (sección 8 del contrato)

| Método | Endpoint | CU/RF | Qué hace |
|---|---|---|---|
| POST | `/auth/registro` | CU-01 | Crea `Cliente` + `CredencialLocal` en `PENDIENTE_VERIFICACION`, genera el código y responde `clienteId` + **código (temporal)**. 409 si el correo ya existe; 400 si faltan datos / password < 8. |
| POST | `/auth/verificar` | CU-01 | Valida el código y pasa la credencial a `ACTIVA` (204). Idempotente si ya estaba activa. 400 si el código no coincide. |
| POST | `/auth/login` | RF-02 | Verifica password (BCrypt) y estado `ACTIVA`; emite JWT. 401 credenciales malas, 403 cuenta no verificada. |
| GET | `/clientes/me/direcciones` | RF-03 | Lista la libreta del cliente autenticado (por `sub` del JWT). |
| POST | `/clientes/me/direcciones` | RF-03 | Da de alta una dirección para el cliente autenticado (201). |

### Decisiones de diseño

- **Auth local aislado en `CredencialLocal`** (no se usa Cognito todavía). Es una
  entidad nueva en el dominio `cuenta`, `@OneToOne` con `Cliente` vía `@MapsId`
  (la PK de `CREDENCIAL_LOCAL` **es** la FK al cliente, clave compartida). Guarda
  `passwordHash` (BCrypt), `estado` (enum `STRING`: `PENDIENTE_VERIFICACION` /
  `ACTIVA`), `codigoVerificacion` y `fechaCreacion` (`@CreationTimestamp`).
  - **`Cliente` no se tocó** (sigue sin contraseña, RNF-03): la relación se mapea
    enteramente desde `CredencialLocal`. Cuando llegue Cognito se elimina la tabla
    `CREDENCIAL_LOCAL` y `Cliente.cognitoSub` toma el relevo.
  - `CredencialLocalRepository.findByCliente_Email(...)` (join al cliente).
- **Verificación funcional pero SIN envío real:** el flujo de estados (no
  verificada → activa) es real, pero el código de 6 dígitos (`SecureRandom`) se
  **devuelve en el response del registro** (mecanismo temporal). Al migrar a
  SES/Cognito cambia el medio de entrega, no el contrato de la API.
- **Backend como resource server (JWT HS256):** `spring-boot-starter-oauth2-resource-server`.
  - **Emisión y validación con Nimbus** (transitivo del starter; **no** se añadió
    `jjwt`). `TokenService` firma con `NimbusJwtEncoder` + `ImmutableSecret`;
    `NimbusJwtDecoder` valida con el mismo `SecretKeySpec` HS256.
  - **Claims:** `sub = clienteId` (donde Cognito pondrá su `sub`), `email`, `iss`,
    `iat`, `exp`. Los controladores leen `jwt.getSubject()` → no cambian al migrar.
  - **`SecurityConfig`** es el único punto que cambia para Cognito (decoder →
    JWK Set URI RS256, se retira el encoder). Cadena *stateless*, CSRF off, CORS
    reutiliza `CorsConfig`. Público: `/auth/**`, `GET /vinilos`/`/vinilos/**`,
    `/health`, `/actuator/**`; el resto exige JWT.
  - Secreto/issuer/expiración por propiedades `app.jwt.*` (variables de entorno;
    el default de dev nunca va a producción). HS256 exige secreto ≥ 32 bytes.
- **DTOs como `record`**, nunca entidades JPA (registro/verificación/login y
  direcciones). Excepciones de dominio con `@ResponseStatus` (mismo patrón que
  `ViniloNoEncontradoException`): `DatosInvalidos` 400, `EmailYaRegistrado` 409,
  `VerificacionInvalida` 400, `CredencialesInvalidas` 401, `CuentaNoVerificada`
  403, `ClienteNoEncontrado` 404.
- **Direcciones del dueño del token:** el `clienteId` sale del `sub` del JWT, no
  del cuerpo, así "me" siempre es el dueño. Servicios de lectura
  `@Transactional(readOnly = true)`; el alta y la verificación, `@Transactional`.
- **Validación mínima manual** (no se añadió `starter-validation`, no había
  precedente): campos obligatorios no vacíos y password ≥ 8 caracteres.

### Verificación

- `mvn clean test` (Docker `maven:3.9-eclipse-temurin-17`) →
  **`BUILD SUCCESS`, `Tests run: 25, Failures: 0, Errors: 0`** (10 nuevos).
  ```powershell
  docker run --rm -v "${PWD}:/app" -w /app maven:3.9-eclipse-temurin-17 mvn -B clean test
  ```
- `CredencialLocalRepositoryTest` (`@DataJpaTest`): clave compartida `@MapsId`
  (PK credencial == id cliente) y `findByCliente_Email`.
- `CuentaControllerTest` (`@SpringBootTest` + `MockMvc`, end-to-end contra H2):
  registro con código temporal, correo duplicado (409), password corta (400),
  login sin verificar (403), código incorrecto (400), login OK con JWT, password
  incorrecta (401), direcciones sin token (401) y alta+listado con token real
  (recorriendo el flujo completo registro→verificar→login).

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

## Pendiente para fases posteriores

- **Fase 4 — Cuentas (MVP): hecha** (ver sección arriba). Nota: el auth se
  resolvió con **JWT local (HS256)** vía `CredencialLocal`, no con Cognito; la
  migración a Cognito queda para la fase de IaC y se reduce a cambiar el decoder
  en `SecurityConfig` (`Cliente.cognitoSub` toma el relevo, `Cliente` intacto).
- **Fase 5a — Compra (checkout): hecha** (ver sección arriba): reserva atómica
  (Redis/Lua) **antes** del cobro, creación del pedido con precio/dirección
  congelados y TDD de total + sobreventa.
- **Fase 5b — Compra (pago y cierre): hecha** (ver sección arriba): pago simulado
  (CU-04), consolidación de reservas (`CONFIRMACION` neutral), expiración
  (`CANCELACION` + liberación de Redis → `CANCELADO`) y TDD de la transición de
  estados. **Con esto el backend del MVP queda funcionalmente completo.**
- **Back-office `[+]` (post-MVP):** cerrar `PAGADO → CONFIRMADO` (despacho) y
  `PAGADO → CANCELADO` (reembolso); alta/edición de catálogo e importación de
  inventario (RF-14..18); entidades `STAFF`, `PROMOCION`, `RESENA`, `ENVIO`.
- **IaC / migración:** auth local → Cognito (cambiar el decoder en
  `SecurityConfig`); reconciliación Redis↔ledger (límite conocido de 5a);
  empaquetado Docker y despliegue Terraform/Ansible.
- **Entidades `[+]` no construidas aún** (fuera del MVP): `PROMOCION`, `STAFF`,
  `RESENA`, `ENVIO`.
