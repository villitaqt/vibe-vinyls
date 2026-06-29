package com.vivevinyls.pedido.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Pedido tal como se devuelve al cliente (POST /pedidos y GET /pedidos/{id}).
 * No serializa entidades JPA. Los datos de envío y los precios son la copia
 * <b>congelada</b> del pedido (RN-06, RN-07), nunca lecturas dinámicas.
 */
public record PedidoResponse(
        UUID pedidoId,
        String estado,
        BigDecimal total,
        List<ItemResponse> items,
        DireccionEnvioResponse direccionEnvio,
        Instant fechaCreacion) {

    public record ItemResponse(
            UUID viniloId,
            String titulo,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal) {
    }

    public record DireccionEnvioResponse(
            String destinatario,
            String calle,
            String ciudad,
            String region,
            String pais,
            String codigoPostal,
            String telefono) {
    }
}
