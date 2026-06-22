package com.vivevinyls.catalogo;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se lanza cuando se pide la ficha de un vinilo inexistente. La anotación
 * {@link ResponseStatus} hace que Spring responda 404 automáticamente.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ViniloNoEncontradoException extends RuntimeException {

    public ViniloNoEncontradoException(UUID id) {
        super("No existe un vinilo con id " + id);
    }
}
