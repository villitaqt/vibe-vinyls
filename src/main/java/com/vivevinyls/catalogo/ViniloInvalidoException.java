package com.vivevinyls.catalogo;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Datos de alta/edición de vinilo incompletos o inválidos (back-office). Responde 400. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ViniloInvalidoException extends RuntimeException {

    public ViniloInvalidoException(String mensaje) {
        super(mensaje);
    }
}
