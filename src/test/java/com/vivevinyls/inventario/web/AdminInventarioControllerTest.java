package com.vivevinyls.inventario.web;

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
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.catalogo.Sello;
import com.vivevinyls.catalogo.SelloRepository;
import com.vivevinyls.catalogo.Vinilo;
import com.vivevinyls.catalogo.ViniloRepository;
import com.vivevinyls.cuenta.Cliente;
import com.vivevinyls.cuenta.ClienteRepository;
import com.vivevinyls.cuenta.Rol;
import com.vivevinyls.cuenta.TokenService;
import com.vivevinyls.inventario.ArbitroStockEnMemoria;
import com.vivevinyls.inventario.MovimientoStock;
import com.vivevinyls.inventario.MovimientoStockRepository;
import com.vivevinyls.inventario.TipoMovimiento;

/**
 * Back-office de inventario ({@code /admin/inventario/importacion}):
 * importación de stock y la seguridad por rol (401/403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminInventarioControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private SelloRepository sellos;
    @Autowired private ViniloRepository vinilos;
    @Autowired private MovimientoStockRepository movimientos;
    @Autowired private ClienteRepository clientes;
    @Autowired private TokenService tokenService;
    @Autowired private ArbitroStockEnMemoria arbitro;

    @BeforeEach
    void setUp() {
        arbitro.vaciar();
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        Vinilo vinilo = crearVinilo(5);
        mockMvc.perform(post("/admin/inventario/importacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(vinilo.getId(), 10)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenDeClienteDevuelve403() throws Exception {
        Vinilo vinilo = crearVinilo(5);
        String tokenCliente = clienteToken();

        mockMvc.perform(post("/admin/inventario/importacion")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(vinilo.getId(), 10)))
                .andExpect(status().isForbidden());
    }

    @Test
    void importacionExitosaIncrementaElStockDisponible() throws Exception {
        Vinilo vinilo = crearVinilo(5);
        String tokenStaff = staffToken();

        mockMvc.perform(post("/admin/inventario/importacion")
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(vinilo.getId(), 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockDisponible").value(15));
    }

    @Test
    void cantidadNoPositivaDevuelve400() throws Exception {
        Vinilo vinilo = crearVinilo(5);
        String tokenStaff = staffToken();

        mockMvc.perform(post("/admin/inventario/importacion")
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(vinilo.getId(), 0)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void viniloInexistenteDevuelve404() throws Exception {
        String tokenStaff = staffToken();

        mockMvc.perform(post("/admin/inventario/importacion")
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo(UUID.randomUUID(), 10)))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---------------------------------------------------------

    private Vinilo crearVinilo(int stockInicial) {
        Sello sello = sellos.save(new Sello("Sello " + UUID.randomUUID()));
        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo("Disco inventario");
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

    private String staffToken() {
        Cliente staff = new Cliente();
        staff.setNombre("Staff Test");
        staff.setEmail("staff-inventario-" + UUID.randomUUID() + "@vivevinyls.com");
        staff.setRol(Rol.STAFF);
        staff = clientes.saveAndFlush(staff);
        return tokenService.emitir(staff);
    }

    private String clienteToken() {
        Cliente cliente = new Cliente();
        cliente.setNombre("Cliente Test");
        cliente.setEmail("cliente-inventario-" + UUID.randomUUID() + "@vivevinyls.com");
        cliente.setRol(Rol.CLIENTE);
        cliente = clientes.saveAndFlush(cliente);
        return tokenService.emitir(cliente);
    }

    private String cuerpo(UUID viniloId, int cantidad) {
        return """
                {"viniloId":"%s","cantidad":%d}""".formatted(viniloId, cantidad);
    }
}
