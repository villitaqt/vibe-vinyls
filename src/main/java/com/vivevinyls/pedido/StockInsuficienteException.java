package com.vivevinyls.pedido;

import java.util.List;
import java.util.UUID;

/**
 * El árbitro rechazó la reserva por falta de stock (RN-02, RN-05): no se crea
 * pedido ni se cobra. Lleva los vinilos agotados para informarlos en el cuerpo
 * de la respuesta 409 (ver {@code PedidoExceptionHandler}).
 */
public class StockInsuficienteException extends RuntimeException {

    private final List<UUID> agotados;

    public StockInsuficienteException(List<UUID> agotados) {
        super("Sin stock suficiente para: " + agotados);
        this.agotados = List.copyOf(agotados);
    }

    public List<UUID> getAgotados() {
        return agotados;
    }
}
