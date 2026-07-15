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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
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
import com.vivevinyls.cuenta.Rol;
import com.vivevinyls.cuenta.TokenService;
import com.vivevinyls.inventario.ArbitroStockEnMemoria;
import com.vivevinyls.inventario.MovimientoStock;
import com.vivevinyls.inventario.MovimientoStockRepository;
import com.vivevinyls.inventario.TipoMovimiento;

/**
 * Back-office de pedidos ({@code /admin/pedidos}): listado por estado,
 * confirmación de despacho, y la seguridad por rol (401/403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPedidoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private SelloRepository sellos;
    @Autowired private ViniloRepository vinilos;
    @Autowired private MovimientoStockRepository movimientos;
    @Autowired private ClienteRepository clientes;
    @Autowired private DireccionRepository direcciones;
    @Autowired private TokenService tokenService;
    @Autowired private ArbitroStockEnMemoria arbitro;

    @BeforeEach
    void setUp() {
        arbitro.vaciar();
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/admin/pedidos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenDeClienteDevuelve403() throws Exception {
        String tokenCliente = registrarCliente("cliente403@vivevinyls.com").token();

        mockMvc.perform(get("/admin/pedidos")
                        .header("Authorization", "Bearer " + tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmarSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/admin/pedidos/{id}/confirmar", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listaFiltradaPorEstado() throws Exception {
        String tokenStaff = staffToken();
        Sesion comprador = registrarCliente("comprador-admin@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Disco admin", "25.00", 10);
        UUID direccionId = crearDireccion(comprador.clienteId());
        UUID pedidoPagado = crearYPagar(comprador.token(), vinilo.getId(), 1, direccionId);
        // Un segundo pedido queda PENDIENTE_PAGO (no se paga) para probar el filtro.
        crearPedido(comprador.token(), vinilo.getId(), 1, direccionId);

        mockMvc.perform(get("/admin/pedidos").param("estado", "PAGADO")
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].pedidoId").value(pedidoPagado.toString()))
                .andExpect(jsonPath("$[0].cliente.email").value("comprador-admin@vivevinyls.com"))
                .andExpect(jsonPath("$[0].items[0].titulo").value("Disco admin"));
    }

    @Test
    void confirmarDespachoDeUnPedidoPagadoLoPasaAConfirmado() throws Exception {
        String tokenStaff = staffToken();
        Sesion comprador = registrarCliente("confirmar@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Disco confirmar", "25.00", 10);
        UUID direccionId = crearDireccion(comprador.clienteId());
        UUID pedidoId = crearYPagar(comprador.token(), vinilo.getId(), 1, direccionId);

        mockMvc.perform(post("/admin/pedidos/{id}/confirmar", pedidoId)
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
    }

    @Test
    void confirmarUnPedidoQueNoEstaPagadoDevuelve409() throws Exception {
        String tokenStaff = staffToken();
        Sesion comprador = registrarCliente("noPagado@vivevinyls.com");
        Vinilo vinilo = crearVinilo("Disco no pagado", "25.00", 10);
        UUID direccionId = crearDireccion(comprador.clienteId());
        UUID pedidoId = crearPedido(comprador.token(), vinilo.getId(), 1, direccionId);

        mockMvc.perform(post("/admin/pedidos/{id}/confirmar", pedidoId)
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isConflict());
    }

    @Test
    void confirmarUnPedidoInexistenteDevuelve404() throws Exception {
        String tokenStaff = staffToken();

        mockMvc.perform(post("/admin/pedidos/{id}/confirmar", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenStaff))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---------------------------------------------------------

    private record Sesion(String token, UUID clienteId) {
    }

    private String staffToken() {
        Cliente staff = new Cliente();
        staff.setNombre("Staff Test");
        staff.setEmail("staff-test-" + UUID.randomUUID() + "@vivevinyls.com");
        staff.setRol(Rol.STAFF);
        staff = clientes.saveAndFlush(staff);
        return tokenService.emitir(staff);
    }

    private Sesion registrarCliente(String email) throws Exception {
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

    private UUID crearPedido(String token, UUID viniloId, int cantidad, UUID direccionId) throws Exception {
        MvcResult res = mockMvc.perform(post("/pedidos")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkout(viniloId, cantidad, direccionId)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(
                json.readTree(res.getResponse().getContentAsString()).get("pedidoId").asText());
    }

    private UUID crearYPagar(String token, UUID viniloId, int cantidad, UUID direccionId) throws Exception {
        UUID pedidoId = crearPedido(token, viniloId, cantidad, direccionId);
        mockMvc.perform(post("/pedidos/{id}/pago", pedidoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resultadoSimulado":"CAPTURA"}"""))
                .andExpect(status().isOk());
        return pedidoId;
    }

    private String checkout(UUID viniloId, int cantidad, UUID direccionId) {
        return """
                {"items":[{"viniloId":"%s","cantidad":%d}],"direccionId":"%s"}"""
                .formatted(viniloId, cantidad, direccionId);
    }
}
