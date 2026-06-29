package com.vivevinyls.cuenta;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Las credenciales son correctas pero la cuenta sigue
 * {@link EstadoCredencial#PENDIENTE_VERIFICACION}; no puede iniciar sesión hasta
 * verificar el correo (CU-01). Responde 403.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class CuentaNoVerificadaException extends RuntimeException {

    public CuentaNoVerificadaException() {
        super("La cuenta no está verificada");
    }
}
