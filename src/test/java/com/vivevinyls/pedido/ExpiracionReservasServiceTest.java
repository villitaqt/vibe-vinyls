package com.vivevinyls.pedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
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
 * Expiración de reservas (CU-03 flujo 4a). El scheduler está desactivado en el
 * perfil de test: se invoca el servicio manualmente con un corte determinista
 * ({@code expirarAnterioresA}). Cubre: cancelación con liberación de stock,
 * idempotencia y que un pedido PAGADO no se toca.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ExpiracionReservasServiceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private SelloRepository sellos;
    @Autowired private ViniloRepository vinilos;
    @Autowired private MovimientoStockRepository movimientos;
    @Autowired private ClienteRepository clientes;
    @Autowired private DireccionRepository direcciones;
    @Autowired private StockService stock;
    @Autowired private ArbitroStockEnMemoria arbitro;
    @Autowired private ExpiracionReservasService expiracion;
    @Autowired private PedidoRepository pedidos;

    @BeforeEach
    void setUp() {
        arbitro.vaciar();
    }

    @Test
    void reservaVencidaSeCancelaYLiberaStock() throws Exception {
        Ctx ctx = nuevoContexto("expira@vivevinyls.com", 10);
        UUID pedidoId = crearPedido(ctx, 3);
        assertThat(stock.disponible(ctx.viniloId())).isEqualTo(7); // tras la reserva

        // Corte en el futuro: el pedido recién creado cuenta como vencido.
        int cancelados = expiracion.expirarAnterioresA(Instant.now().plusSeconds(60));

        assertThat(cancelados).isEqualTo(1);
        assertThat(pedidos.findById(pedidoId).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.CANCELADO);
        // El disponible vuelve al valor previo a la reserva: 10 - 3 + 3 = 10.
        assertThat(stock.disponible(ctx.viniloId())).isEqualTo(10);
        assertThat(movimientos.findByViniloId(ctx.viniloId()))
                .anyMatch(m -> m.getTipo() == TipoMovimiento.CANCELACION && m.getCantidad() == 3);
    }

    @Test
    void invocarDosVecesNoDobleLibera() throws Exception {
        Ctx ctx = nuevoContexto("idempotente@vivevinyls.com", 10);
        crearPedido(ctx, 3);

        int primera = expiracion.expirarAnterioresA(Instant.now().plusSeconds(60));
        int segunda = expiracion.expirarAnterioresA(Instant.now().plusSeconds(60));

        assertThat(primera).isEqualTo(1);
        assertThat(segunda).isZero(); // ya está CANCELADO: no se vuelve a tocar
        assertThat(stock.disponible(ctx.viniloId())).isEqualTo(10); // no doble-liberado
    }

    @Test
    void pedidoPagadoNoLoTocaLaExpiracion() throws Exception {
        Ctx ctx = nuevoContexto("pagado-no-expira@vivevinyls.com", 10);
        UUID pedidoId = crearPedido(ctx, 2);

        mockMvc.perform(post("/pedidos/{id}/pago", pedidoId)
                        .header("Authorization", "Bearer " + ctx.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        int cancelados = expiracion.expirarAnterioresA(Instant.now().plusSeconds(60));

        assertThat(cancelados).isZero();
        assertThat(pedidos.findById(pedidoId).orElseThrow().getEstado())
                .isEqualTo(EstadoPedido.PAGADO);
        assertThat(stock.disponible(ctx.viniloId())).isEqualTo(8); // sigue vendido
    }

    // --- helpers ---------------------------------------------------------

    private record Ctx(String token, UUID viniloId, UUID direccionId) {
    }

    private Ctx nuevoContexto(String email, int stockInicial) throws Exception {
        Sesion sesion = registrar(email);
        Vinilo vinilo = crearVinilo(stockInicial);
        UUID direccionId = crearDireccion(sesion.clienteId());
        return new Ctx(sesion.token(), vinilo.getId(), direccionId);
    }

    private UUID crearPedido(Ctx ctx, int cantidad) throws Exception {
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

    private Vinilo crearVinilo(int stockInicial) {
        Sello sello = sellos.save(new Sello("Sello " + UUID.randomUUID()));
        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo("Disco " + UUID.randomUUID());
        vinilo.setAnio(2020);
        vinilo.setPrecio(new BigDecimal("20.00"));
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
