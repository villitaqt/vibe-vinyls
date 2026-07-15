package com.vivevinyls.cuenta;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El cliente referido por el JWT no existe (token válido pero sin cuenta detrás,
 * p. ej. tras borrar datos). Responde 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ClienteNoEncontradoException extends RuntimeException {

    public ClienteNoEncontradoException(UUID id) {
        super("No existe un cliente con id " + id);
    }
}
