package com.vivevinyls.cuenta;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Correo o contraseña incorrectos en el login (RF-02). Responde 401 con un
 * mensaje genérico para no revelar si el correo existe.
 */
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class CredencialesInvalidasException extends RuntimeException {

    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}
