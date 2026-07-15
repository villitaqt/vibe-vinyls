package com.vivevinyls.cuenta.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vivevinyls.cuenta.DireccionService;

/**
 * Libreta de direcciones del cliente autenticado (RF-03). Sección 8 del
 * contrato: {@code GET/POST /clientes/me/direcciones}.
 *
 * <p>El cliente se identifica por el {@code sub} del JWT validado por el
 * resource server, nunca por un id del cuerpo: así "me" siempre es el dueño del
 * token. Cuando se migre a Cognito, {@code jwt.getSubject()} seguirá siendo el
 * identificador del cliente y este controlador no cambia.</p>
 */
@RestController
@RequestMapping("/clientes/me/direcciones")
public class DireccionController {

    private final DireccionService direcciones;

    public DireccionController(DireccionService direcciones) {
        this.direcciones = direcciones;
    }

    @GetMapping
    public List<DireccionResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return direcciones.listar(clienteId(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DireccionResponse alta(@AuthenticationPrincipal Jwt jwt,
            @RequestBody DireccionRequest req) {
        return direcciones.alta(clienteId(jwt), req);
    }

    private UUID clienteId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
