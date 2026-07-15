package com.vivevinyls.cuenta;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El código de verificación no coincide (o la cuenta no admite verificación).
 * Responde 400.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class VerificacionInvalidaException extends RuntimeException {

    public VerificacionInvalidaException() {
        super("Código de verificación inválido");
    }
}
