package com.vivevinyls.pedido;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El pedido no existe <b>o no pertenece</b> al cliente del token. Se responde
 * 404 en ambos casos por privacidad: no se revela la existencia de pedidos
 * ajenos.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class PedidoNoEncontradoException extends RuntimeException {

    public PedidoNoEncontradoException(UUID id) {
        super("No existe un pedido con id " + id);
    }
}
