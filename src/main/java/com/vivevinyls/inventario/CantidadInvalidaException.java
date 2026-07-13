package com.vivevinyls.inventario;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Cantidad de importación no positiva (back-office). Responde 400. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CantidadInvalidaException extends RuntimeException {

    public CantidadInvalidaException(String mensaje) {
        super(mensaje);
    }
}
