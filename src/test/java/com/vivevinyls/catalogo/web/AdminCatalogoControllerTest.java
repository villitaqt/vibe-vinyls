package com.vivevinyls.catalogo.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vivevinyls.cuenta.Cliente;
import com.vivevinyls.cuenta.ClienteRepository;
import com.vivevinyls.cuenta.Rol;
import com.vivevinyls.cuenta.TokenService;

/**
 * Back-office de catálogo ({@code /admin/vinilos}): alta, edición y la
 * seguridad por rol (401/403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCatalogoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;
    @Autowired private ClienteRepository clientes;
    @Autowired private TokenService tokenService;

    @Test
    void crearSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/admin/vinilos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVinilo("Disco", "20.00")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void crearConTokenDeClienteDevuelve403() throws Exception {
        String tokenCliente = registrarCliente("catalogo403@vivevinyls.com");

        mockMvc.perform(post("/admin/vinilos")
                        .header("Authorization", "Bearer " + tokenCliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVinilo("Disco", "20.00")))
                .andExpect(status().isForbidden());
    }

    @Test
    void crearVinilioPersisteArtistasYGeneros() throws Exception {
        String tokenStaff = staffToken();

        MvcResult creado = mockMvc.perform(post("/admin/vinilos")
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVinilo("Nuevo Disco", "45.50")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.titulo").value("Nuevo Disco"))
                .andExpect(jsonPath("$.artistas[0]").value("Artista Uno"))
                .andExpect(jsonPath("$.generos[0]").value("Jazz"))
                .andReturn();

        UUID id = UUID.fromString(
                json.readTree(creado.getResponse().getContentAsString()).get("id").asText());

        // El listado público de la ficha ve el mismo vinilo recién creado.
        mockMvc.perform(get("/vinilos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Nuevo Disco"));
    }

    @Test
    void editarActualizaLosCampos() throws Exception {
        String tokenStaff = staffToken();

        MvcResult creado = mockMvc.perform(post("/admin/vinilos")
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVinilo("Disco Original", "20.00")))
                .andExpect(status().isCreated())
                .andReturn();
        UUID id = UUID.fromString(
                json.readTree(creado.getResponse().getContentAsString()).get("id").asText());

        mockMvc.perform(put("/admin/vinilos/{id}", id)
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVinilo("Disco Editado", "30.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Disco Editado"))
                .andExpect(jsonPath("$.precio").value(30.00));
    }

    @Test
    void editarUnViniloInexistenteDevuelve404() throws Exception {
        String tokenStaff = staffToken();

        mockMvc.perform(put("/admin/vinilos/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenStaff)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpoVinilo("Disco", "20.00")))
                .andExpect(status().isNotFound());
    }

    // --- helpers ---------------------------------------------------------

    private String staffToken() {
        Cliente staff = new Cliente();
        staff.setNombre("Staff Test");
        staff.setEmail("staff-catalogo-" + UUID.randomUUID() + "@vivevinyls.com");
        staff.setRol(Rol.STAFF);
        staff = clientes.saveAndFlush(staff);
        return tokenService.emitir(staff);
    }

    private String registrarCliente(String email) throws Exception {
        MvcResult registro = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","nombre":"Cliente","password":"secreto123"}"""
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        String codigo = json.readTree(registro.getResponse().getContentAsString())
                .get("codigoVerificacion").asText();

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
        return json.readTree(login.getResponse().getContentAsString()).get("token").asText();
    }

    private String cuerpoVinilo(String titulo, String precio) {
        return """
                {"titulo":"%s","anio":2020,"precio":%s,"sello":"Sello Admin",
                 "artistas":["Artista Uno"],"generos":["Jazz"],"portadaUrl":null}"""
                .formatted(titulo, precio);
    }
}
