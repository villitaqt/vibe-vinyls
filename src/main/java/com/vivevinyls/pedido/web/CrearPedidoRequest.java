package com.vivevinyls.pedido.web;

import java.util.List;
import java.util.UUID;

/**
 * Checkout (CU-03). El {@code clienteId} NO viaja en el cuerpo: sale del
 * {@code sub} del JWT. {@code direccionId} debe ser de la libreta del cliente.
 */
public record CrearPedidoRequest(List<ItemPedidoRequest> items, UUID direccionId) {

    /** Línea solicitada: qué vinilo y cuántas unidades. */
    public record ItemPedidoRequest(UUID viniloId, int cantidad) {
    }
}
