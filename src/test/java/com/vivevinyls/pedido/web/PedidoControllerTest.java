package com.vivevinyls.pedido.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivevinyls.catalogo.Sello;
import com.vivevinyls.catalogo.SelloRepository;
import com.vivevinyls.catalogo.Vinilo;
import com.vivevinyls.catalogo.ViniloRepository;
import com.vivevinyls.cuenta.Cliente;
import com.vivevinyls.cuenta.ClienteRepository;
import com.vivevinyls.cuenta.Direccion;
import com.vivevinyls.cuenta.DireccionRepository;
import com.vivevinyls.inventario.ArbitroStockEnMemoria;
import com.vivevinyls.inventario.MovimientoStock;
import com.vivevinyls.inventario.MovimientoStockRepository;
import com.vivevinyls.inventario.StockService;
import com.vivevinyls.inventario.TipoMovimiento;

/**
 * Checkout (CU-03) y consulta de pedido (RF-13) end-to-end contra H2, con el
 * árbitro in-memory cableado por el perfil de test. Cubre: total y estado del
 * pedido, precio congelado (RN-06), dirección congelada (RN-07), invariante del
 * ledger, rechazo por falta de stock (RN-02/RN-05), validaciones, y la seguridad
 * (401 sin token, 404 de pedido ajeno).
 *
 * <p>Todo se revierte por test ({@link Transactional}); el estado en caliente del
 * árbitro se limpia en {@link #setUp()}.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PedidoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private SelloRepository sellos;
    @Autowired private ViniloRepository vinilos;
    @Autowired private MovimientoStockRepository movimientos;
    @Autowired private ClienteRepository clientes;
    @Autowired private DireccionRepository direcciones;
    @Autowired private StockService stock;
    @Autowired private ArbitroStockEnMemoria arbitro;

    @BeforeEach
    void setUp() {
        arbitro.vaciar();
    }

    @Test
    void checkoutCreaPedidoCongelaPrecioCalculaTotalYReservaStock() throws Exception {
        Sesion sesion = registrar("comprador1@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Kind of Blue", "30.00", 10);
        UUID direccionId = crearDireccion(sesion.clienteId());

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + sesion.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(vinilo.getId(), 2, direccionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pedidoId").isNotEmpty())
                .andExpect(jsonPath("$.estado").value("PENDIENTE_PAGO"))
                .andExpect(jsonPath("$.total").value(60.00))
                .andExpect(jsonPath("$.items[0].cantidad").value(2))
                .andExpect(jsonPath("$.items[0].precioUnitario").value(30.00))
                .andExpect(jsonPath("$.items[0].subtotal").value(60.00))
                .andExpect(jsonPath("$.direccionEnvio.ciudad").value("Lima"));

        // Invariante del ledger: disponible = 10 (IMPORTACION) - 2 (RESERVA) = 8.
        org.assertj.core.api.Assertions.assertThat(stock.disponible(vinilo.getId())).isEqualTo(8);
    }

    @Test
    void precioCongeladoAunqueCambieElDelVinilo() throws Exception {
        Sesion sesion = registrar("congela@vivevinyls.com");
        Vinilo vinilo = crearVinilo("A Love Supreme", "30.00", 5);
        UUID direccionId = crearDireccion(sesion.clienteId());

        UUID pedidoId = crearPedido(sesion.token(), vinilo.getId(), 1, direccionId);

        // El precio del vinilo sube después de la compra.
        vinilo.setPrecio(new BigDecimal("99.00"));
        vinilos.saveAndFlush(vinilo);

        mockMvc.perform(get("/pedidos/{id}", pedidoId)
                        .header("Authorization", "Bearer " + sesion.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].precioUnitario").value(30.00))
                .andExpect(jsonPath("$.total").value(30.00));
    }

    @Test
    void direccionCongeladaAunqueSeEditeLaLibreta() throws Exception {
        Sesion sesion = registrar("dircongela@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Blue Train", "20.00", 5);
        UUID direccionId = crearDireccion(sesion.clienteId());

        UUID pedidoId = crearPedido(sesion.token(), vinilo.getId(), 1, direccionId);

        // Se edita la dirección de la libreta tras la compra.
        Direccion direccion = direcciones.findById(direccionId).orElseThrow();
        direccion.setCiudad("Cusco");
        direcciones.saveAndFlush(direccion);

        mockMvc.perform(get("/pedidos/{id}", pedidoId)
                        .header("Authorization", "Bearer " + sesion.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.direccionEnvio.ciudad").value("Lima"));
    }

    @Test
    void sinStockSuficienteDevuelve409ConElViniloAgotado() throws Exception {
        Sesion sesion = registrar("agotado@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Edición limitada", "50.00", 1);
        UUID direccionId = crearDireccion(sesion.clienteId());

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + sesion.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(vinilo.getId(), 2, direccionId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.agotados[0]").value(vinilo.getId().toString()));
    }

    @Test
    void segundoCheckoutQueAgotaElStockDevuelve409() throws Exception {
        Sesion sesion = registrar("secuencial@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Última copia", "50.00", 1);
        UUID direccionId = crearDireccion(sesion.clienteId());

        // Primera compra toma la única unidad.
        crearPedido(sesion.token(), vinilo.getId(), 1, direccionId);

        // La segunda no encuentra stock disponible en el árbitro.
        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + sesion.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(vinilo.getId(), 1, direccionId)))
                .andExpect(status().isConflict());
    }

    @Test
    void viniloInexistenteDevuelve404() throws Exception {
        Sesion sesion = registrar("novinilo@vivevinyls.com");
        UUID direccionId = crearDireccion(sesion.clienteId());

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + sesion.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(UUID.randomUUID(), 1, direccionId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void direccionAjenaDevuelve400() throws Exception {
        Sesion sesion = registrar("clienteA@vivevinyls.com");
        Sesion otro = registrar("clienteB@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Disco", "20.00", 5);
        UUID direccionAjena = crearDireccion(otro.clienteId());

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + sesion.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(vinilo.getId(), 1, direccionAjena)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cantidadInvalidaDevuelve400() throws Exception {
        Sesion sesion = registrar("cantidad@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Disco", "20.00", 5);
        UUID direccionId = crearDireccion(sesion.clienteId());

        mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + sesion.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(vinilo.getId(), 0, direccionId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkoutSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/pedidos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(UUID.randomUUID(), 1, UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPedidoSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/pedidos/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getPedidoAjenoDevuelve404() throws Exception {
        Sesion duenio = registrar("duenio@vivevinyls.com");
        Sesion intruso = registrar("intruso@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Disco", "20.00", 5);
        UUID direccionId = crearDireccion(duenio.clienteId());
        UUID pedidoId = crearPedido(duenio.token(), vinilo.getId(), 1, direccionId);

        // El intruso no puede ver el pedido ajeno: 404 (no se revela su existencia).
        mockMvc.perform(get("/pedidos/{id}", pedidoId)
                        .header("Authorization", "Bearer " + intruso.token()))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---------------------------------------------------------

    private record Sesion(String token, UUID clienteId) {
    }

    /** Recorre registro → verificar → login y devuelve token + clienteId. */
    private Sesion registrar(String email) throws Exception {
        MvcResult registro = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","nombre":"Cliente","password":"secreto123"}"""
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode cuerpo = json.readTree(registro.getResponse().getContentAsString());
        String codigo = cuerpo.get("codigoVerificacion").asText();
        UUID clienteId = UUID.fromString(cuerpo.get("clienteId").asText());

        mockMvc.perform(post("/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","codigo":"%s"}""".formatted(email, codigo)))
                .andExpect(status().isNoContent());

        MvcResult login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secreto123"}""".formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        String token = json.readTree(login.getResponse().getContentAsString()).get("token").asText();
        return new Sesion(token, clienteId);
    }

    private Vinilo crearVinilo(String titulo, String precio, int stockInicial) {
        Sello sello = sellos.save(new Sello("Sello " + UUID.randomUUID()));
        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo(titulo);
        vinilo.setAnio(2020);
        vinilo.setPrecio(new BigDecimal(precio));
        vinilo.setSello(sello);
        vinilo = vinilos.saveAndFlush(vinilo);

        MovimientoStock importacion = new MovimientoStock();
        importacion.setVinilo(vinilo);
        importacion.setTipo(TipoMovimiento.IMPORTACION);
        importacion.setCantidad(stockInicial);
        movimientos.saveAndFlush(importacion);
        return vinilo;
    }

    private UUID crearDireccion(UUID clienteId) {
        Cliente cliente = clientes.findById(clienteId).orElseThrow();
        Direccion direccion = new Direccion();
        direccion.setCliente(cliente);
        direccion.setDestinatario("Diego Villa");
        direccion.setCalle("Av. Siempre Viva 742");
        direccion.setCiudad("Lima");
        direccion.setRegion("Lima");
        direccion.setPais("Perú");
        direccion.setCodigoPostal("15001");
        direccion.setTelefono("+51999999999");
        return direcciones.saveAndFlush(direccion).getId();
    }

    private UUID crearPedido(String token, UUID viniloId, int cantidad, UUID direccionId)
            throws Exception {
        MvcResult res = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(viniloId, cantidad, direccionId)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                json.readTree(res.getResponse().getContentAsString()).get("pedidoId").asText());
    }

    private String checkout(UUID viniloId, int cantidad, UUID direccionId) {
        return """
                {"items":[{"viniloId":"%s","cantidad":%d}],"direccionId":"%s"}"""
                .formatted(viniloId, cantidad, direccionId);
    }
}
