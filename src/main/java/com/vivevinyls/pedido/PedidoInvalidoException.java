package com.vivevinyls.pedido;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Datos del checkout inválidos: sin ítems, cantidad ≤ 0, o {@code direccionId}
 * inexistente / ajeno al cliente. Responde 400.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PedidoInvalidoException extends RuntimeException {

    public PedidoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
