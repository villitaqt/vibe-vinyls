package com.vivevinyls.pedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cálculo del total del pedido (lógica crítica del MVP → TDD). El total es la
 * suma de los subtotales de cada línea; el subtotal es {@code precioUnitario ×
 * cantidad}. Se opera siempre sobre el precio <b>congelado</b> del ítem
 * (RN-06), nunca sobre el precio vivo del vinilo.
 */
final class CalculoPedido {

    private CalculoPedido() {
    }

    static BigDecimal subtotal(BigDecimal precioUnitario, int cantidad) {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    static BigDecimal total(List<ItemPedido> items) {
        return items.stream()
                .map(i -> subtotal(i.getPrecioUnitario(), i.getCantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
