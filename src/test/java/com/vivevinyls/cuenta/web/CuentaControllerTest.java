package com.vivevinyls.cuenta.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

/**
 * Tests de la capa web de cuentas (CU-01, RF-02, RF-03) end-to-end contra H2:
 * registro (con código temporal en el response), verificación, login (JWT) y la
 * libreta de direcciones protegida por el resource server. El token usado en las
 * pruebas de direcciones es real: se obtiene recorriendo el flujo completo.
 *
 * <p>Todo se revierte por test gracias a {@link Transactional}; no toca datos de
 * dev/producción.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CuentaControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper json;

    @Test
    void registroDevuelveClienteIdYCodigoTemporal() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("nuevo@vivevinyls.com", "Nuevo Cliente", "secreto123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").isNotEmpty())
                .andExpect(jsonPath("$.codigoVerificacion").isNotEmpty());
    }

    @Test
    void registroConCorreoDuplicadoDevuelve409() throws Exception {
        String body = registro("dup@vivevinyls.com", "Cliente", "secreto123");
        mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/registro").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void registroConPasswordCortaDevuelve400() throws Exception {
        mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro("corta@vivevinyls.com", "Cliente", "123")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginSinVerificarDevuelve403() throws Exception {
        registrar("sinverificar@vivevinyls.com", "Cliente", "secreto123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("sinverificar@vivevinyls.com", "secreto123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void verificacionConCodigoIncorrectoDevuelve400() throws Exception {
        registrar("malcodigo@vivevinyls.com", "Cliente", "secreto123");

        mockMvc.perform(post("/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificar("malcodigo@vivevinyls.com", "000000")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginTrasVerificarDevuelveToken() throws Exception {
        String codigo = registrar("activo@vivevinyls.com", "Cliente", "secreto123");
        verificarOk("activo@vivevinyls.com", codigo);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("activo@vivevinyls.com", "secreto123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.clienteId").isNotEmpty());
    }

    @Test
    void loginConPasswordIncorrectaDevuelve401() throws Exception {
        String codigo = registrar("badpass@vivevinyls.com", "Cliente", "secreto123");
        verificarOk("badpass@vivevinyls.com", codigo);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login("badpass@vivevinyls.com", "otra-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void direccionesSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/clientes/me/direcciones"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteAutenticadoDaDeAltaYListaSusDirecciones() throws Exception {
        String token = tokenDe("libreta@vivevinyls.com", "Cliente", "secreto123");

        mockMvc.perform(post("/clientes/me/direcciones")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinatario":"Diego Villa","calle":"Av. Siempre Viva 742",
                                 "ciudad":"Lima","region":"Lima","pais":"Perú",
                                 "codigoPostal":"15001","telefono":"+51999999999"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.calle").value("Av. Siempre Viva 742"));

        mockMvc.perform(get("/clientes/me/direcciones")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ciudad").value("Lima"));
    }

    // --- helpers ---------------------------------------------------------

    /** Registra y devuelve el código de verificación temporal del response. */
    private String registrar(String email, String nombre, String password) throws Exception {
        MvcResult res = mockMvc.perform(post("/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registro(email, nombre, password)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        return body.get("codigoVerificacion").asText();
    }

    private void verificarOk(String email, String codigo) throws Exception {
        mockMvc.perform(post("/auth/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(verificar(email, codigo)))
                .andExpect(status().isNoContent());
    }

    /** Flujo completo registro → verificar → login; devuelve el JWT. */
    private String tokenDe(String email, String nombre, String password) throws Exception {
        String codigo = registrar(email, nombre, password);
        verificarOk(email, codigo);
        MvcResult res = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(login(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    private String registro(String email, String nombre, String password) {
        return """
                {"email":"%s","nombre":"%s","password":"%s"}""".formatted(email, nombre, password);
    }

    private String login(String email, String password) {
        return """
                {"email":"%s","password":"%s"}""".formatted(email, password);
    }

    private String verificar(String email, String codigo) {
        return """
                {"email":"%s","codigo":"%s"}""".formatted(email, codigo);
    }
}
