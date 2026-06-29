package com.vivevinyls.pedido;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Se intentó una transición no válida sobre el pedido (p. ej. pagar uno que no
 * está en {@code PENDIENTE_PAGO}). Responde 409 y no produce efectos.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class EstadoPedidoInvalidoException extends RuntimeException {

    public EstadoPedidoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
