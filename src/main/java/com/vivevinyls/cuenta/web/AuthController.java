package com.vivevinyls.cuenta.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vivevinyls.cuenta.AuthService;

/**
 * API de autenticación local (CU-01, RF-02). Sección 8 del contrato:
 * {@code POST /auth/registro}, {@code /auth/verificar} y {@code /auth/login}.
 * Endpoints públicos (no exigen JWT).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    /** Crea la cuenta no verificada y devuelve el código (temporal). */
    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse registro(@RequestBody RegistroRequest req) {
        return auth.registro(req);
    }

    /** Valida el código y activa la cuenta. */
    @PostMapping("/verificar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verificar(@RequestBody VerificacionRequest req) {
        auth.verificar(req);
    }

    /** Verifica credenciales y emite un JWT. */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        return auth.login(req);
    }
}
