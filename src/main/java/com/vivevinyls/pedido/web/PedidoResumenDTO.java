package com.vivevinyls.pedido.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.vivevinyls.pedido.EstadoPedido;

/**
 * Resumen de un pedido para el historial del cliente ({@code GET /pedidos/me}).
 * No serializa entidades JPA.
 */
public record PedidoResumenDTO(
        UUID pedidoId,
        Instant fechaCreacion,
        EstadoPedido estado,
        BigDecimal total,
        int cantidadItems,
        String estadoPagoUltimo) {
}
