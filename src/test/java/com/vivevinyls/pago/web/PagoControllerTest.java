package com.vivevinyls.pago.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
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
import com.vivevinyls.pago.Pago;
import com.vivevinyls.pago.PagoRepository;

/**
 * Pago de pedidos (CU-04) end-to-end contra H2: máquina de estados
 * (PENDIENTE_PAGO → PAGADO solo si captura), reintento tras fallo, conflicto de
 * estado (409), consolidación neutral en el ledger, RN-04 (sin datos de tarjeta)
 * y la constancia (GET con el último pago).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PagoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private SelloRepository sellos;
    @Autowired private ViniloRepository vinilos;
    @Autowired private MovimientoStockRepository movimientos;
    @Autowired private ClienteRepository clientes;
    @Autowired private DireccionRepository direcciones;
    @Autowired private StockService stock;
    @Autowired private PagoRepository pagos;
    @Autowired private ArbitroStockEnMemoria arbitro;

    @BeforeEach
    void setUp() {
        arbitro.vaciar();
    }

    @Test
    void pagoCapturadoTransitaPedidoAPagadoYConsolidaReserva() throws Exception {
        Contexto ctx = nuevoContexto("pago-ok@vivevinyls.com", "30.00", 10);
        UUID pedidoId = crearPedido(ctx, 2);

        mockMvc.perform(post("/pedidos/{id}/pago", pedidoId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resultadoSimulado":"CAPTURA"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CAPTURADO"))
                .andExpect(jsonPath("$.estadoPedido").value("PAGADO"))
                .andExpect(jsonPath("$.monto").value(60.00))
                .andExpect(jsonPath("$.referenciaExterna").isNotEmpty());

        // Consolidación neutral: el disponible no cambia respecto al post-reserva
        // (10 - 2 = 8); el CONFIRMACION del ledger no altera la suma.
        assertThat(stock.disponible(ctx.viniloId())).isEqualTo(8);
        assertThat(movimientos.findByViniloId(ctx.viniloId()))
                .anyMatch(m -> m.getTipo() == TipoMovimiento.CONFIRMACION && m.getCantidad() == 0);
    }

    @Test
    void pagoRechazadoDejaPedidoPendienteYNoLiberaReserva() throws Exception {
        Contexto ctx = nuevoContexto("pago-falla@vivevinyls.com", "30.00", 10);
        UUID pedidoId = crearPedido(ctx, 2);

        mockMvc.perform(post("/pedidos/{id}/pago", pedidoId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resultadoSimulado":"RECHAZO"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("FALLIDO"))
                .andExpect(jsonPath("$.estadoPedido").value("PENDIENTE_PAGO"));

        // La reserva NO se libera con el rechazo: el disponible sigue descontado.
        assertThat(stock.disponible(ctx.viniloId())).isEqualTo(8);
    }

    @Test
    void reintentoTrasFalloPuedeCapturar() throws Exception {
        Contexto ctx = nuevoContexto("reintento@vivevinyls.com", "25.00", 5);
        UUID pedidoId = crearPedido(ctx, 1);

        pagar(ctx.token(), pedidoId, "RECHAZO")
                .andExpect(jsonPath("$.estadoPedido").value("PENDIENTE_PAGO"));
        pagar(ctx.token(), pedidoId, "CAPTURA")
                .andExpect(jsonPath("$.estado").value("CAPTURADO"))
                .andExpect(jsonPath("$.estadoPedido").value("PAGADO"));

        // Quedan dos intentos registrados sobre el mismo pedido.
        assertThat(pagos.findByPedidoId(pedidoId)).hasSize(2);
    }

    @Test
    void pagarPedidoYaPagadoDevuelve409() throws Exception {
        Contexto ctx = nuevoContexto("doble-pago@vivevinyls.com", "25.00", 5);
        UUID pedidoId = crearPedido(ctx, 1);

        pagar(ctx.token(), pedidoId, "CAPTURA").andExpect(status().isOk());

        mockMvc.perform(post("/pedidos/{id}/pago", pedidoId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resultadoSimulado":"CAPTURA"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void pagoSinDatosDeTarjeta_soloGuardaEstadoMontoYReferencia() throws Exception {
        Contexto ctx = nuevoContexto("rn04@vivevinyls.com", "40.00", 3);
        UUID pedidoId = crearPedido(ctx, 1);

        pagar(ctx.token(), pedidoId, "CAPTURA").andExpect(status().isOk());

        // RN-04: el Pago solo conserva estado, monto y la referencia de la pasarela.
        List<Pago> intentos = pagos.findByPedidoId(pedidoId);
        assertThat(intentos).hasSize(1);
        Pago pago = intentos.get(0);
        assertThat(pago.getReferenciaExterna()).startsWith("SIMUL-");
        assertThat(pago.getMonto()).isEqualByComparingTo("40.00");
    }

    @Test
    void getPedidoSirveDeConstanciaConElUltimoPago() throws Exception {
        Contexto ctx = nuevoContexto("constancia@vivevinyls.com", "30.00", 5);
        UUID pedidoId = crearPedido(ctx, 1);
        pagar(ctx.token(), pedidoId, "CAPTURA").andExpect(status().isOk());

        mockMvc.perform(get("/pedidos/{id}", pedidoId)
                        .header("Authorization", "Bearer " + ctx.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PAGADO"))
                .andExpect(jsonPath("$.ultimoPago.estado").value("CAPTURADO"))
                .andExpect(jsonPath("$.ultimoPago.monto").value(30.00))
                .andExpect(jsonPath("$.ultimoPago.referenciaExterna").isNotEmpty());
    }

    @Test
    void pagarPedidoAjenoDevuelve404() throws Exception {
        Contexto ctx = nuevoContexto("duenio-pago@vivevinyls.com", "20.00", 5);
        Contexto intruso = nuevoContexto("intruso-pago@vivevinyls.com", "20.00", 5);
        UUID pedidoId = crearPedido(ctx, 1);

        mockMvc.perform(post("/pedidos/{id}/pago", pedidoId)
                        .header("Authorization", "Bearer " + intruso.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---------------------------------------------------------

    private record Contexto(String token, UUID clienteId, UUID viniloId, UUID direccionId) {
    }

    private Contexto nuevoContexto(String email, String precio, int stockInicial) throws Exception {
        Sesion sesion = registrar(email);
        Vinilo vinilo = crearVinilo(precio, stockInicial);
        UUID direccionId = crearDireccion(sesion.clienteId());
        return new Contexto(sesion.token(), sesion.clienteId(), vinilo.getId(), direccionId);
    }

    private org.springframework.test.web.servlet.ResultActions pagar(String token, UUID pedidoId,
            String resultado) throws Exception {
        return mockMvc.perform(post("/pedidos/{id}/pago", pedidoId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"resultadoSimulado":"%s"}""".formatted(resultado)));
    }

    private UUID crearPedido(Contexto ctx, int cantidad) throws Exception {
        MvcResult res = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"viniloId":"%s","cantidad":%d}],"direccionId":"%s"}"""
                                .formatted(ctx.viniloId(), cantidad, ctx.direccionId())))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                json.readTree(res.getResponse().getContentAsString()).get("pedidoId").asText());
    }

    private record Sesion(String token, UUID clienteId) {
    }

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

    private Vinilo crearVinilo(String precio, int stockInicial) {
        Sello sello = sellos.save(new Sello("Sello " + UUID.randomUUID()));
        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo("Disco " + UUID.randomUUID());
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
        direccion.setPais("Perú");
        return direcciones.saveAndFlush(direccion).getId();
    }
}
