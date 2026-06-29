package com.vivevinyls.cuenta;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Datos de entrada inválidos (campos obligatorios vacíos, contraseña corta…).
 * {@link ResponseStatus} hace que Spring responda 400 automáticamente.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DatosInvalidosException extends RuntimeException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
