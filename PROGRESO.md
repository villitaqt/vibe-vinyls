# Progreso ViveVinyls

Bitácora por fases. Retomar siempre desde aquí en un chat nuevo de Claude Code.

- **Fase 1 (esqueleto Spring Boot):** completada. Detalle en
  [`PENDIENTES_FASE1.md`](PENDIENTES_FASE1.md). Commit `30eb199`.
- **Fase 2 (modelo de datos):** completada (este documento). Commit
  "fase 2: modelo de datos y migraciones".

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

## Pendiente para Fase 3 (catálogo) y posteriores

- **Fase 3 — Catálogo (MVP):** endpoints `GET /vinilos` (búsqueda por
  título/artista, filtros por género/artista/año/sello, paginación) y
  `GET /vinilos/{id}` (ficha). Capa de servicio + DTOs. El stock disponible
  mostrado se calculará desde el ledger (y, más adelante, el árbitro Redis).
- **Cuentas:** registro/login validando JWT de Cognito (resource server).
- **Compra (CU-03/04):** checkout con reserva de stock **antes** del cobro
  (árbitro atómico Redis), creación de pedido, congelado de precio/dirección,
  pago simulado, confirmación. Aquí entra el TDD de la lógica crítica
  (total, prevención de sobreventa, transición de estados).
- **Inventario:** servicio de cálculo de stock (físico / reservado / disponible)
  sobre el ledger.
- **Entidades `[+]` no construidas aún** (fuera del MVP): `PROMOCION`, `STAFF`,
  `RESENA`, `ENVIO`.
