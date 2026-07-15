package com.vivevinyls.pedido.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.vivevinyls.pedido.EstadoPedido;
import com.vivevinyls.pedido.web.PedidoResponse.DireccionEnvioResponse;

/**
 * Pedido tal como lo necesita el back-office ({@code GET /admin/pedidos}):
 * suma el resumen del cliente y el detalle de ítems al resumen del cliente
 * final, para que el staff pueda decidir sin pedir el detalle aparte.
 */
public record PedidoAdminDTO(
        UUID pedidoId,
        Instant fechaCreacion,
        EstadoPedido estado,
        BigDecimal total,
        ClienteResumen cliente,
        int cantidadItems,
        List<ItemResumen> items,
        DireccionEnvioResponse direccionEnvio) {

    public record ClienteResumen(String nombre, String email) {
    }

    public record ItemResumen(String titulo, int cantidad) {
    }
}
