package com.vivevinyls.pedido;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Lógica crítica (sección 10): cálculo del total del pedido. El total es la suma
 * de {@code precioUnitario × cantidad} de cada línea, sobre el precio congelado.
 */
class CalculoPedidoTest {

    @Test
    void totalSumaSubtotalesConCantidadesDistintas() {
        ItemPedido a = item("39.90", 2); // 79.80
        ItemPedido b = item("34.00", 1); // 34.00
        ItemPedido c = item("10.00", 3); // 30.00

        BigDecimal total = CalculoPedido.total(List.of(a, b, c));

        assertThat(total).isEqualByComparingTo("143.80");
    }

    @Test
    void totalDeUnSoloItem() {
        BigDecimal total = CalculoPedido.total(List.of(item("29.50", 4)));
        assertThat(total).isEqualByComparingTo("118.00");
    }

    @Test
    void subtotalEsPrecioPorCantidad() {
        assertThat(CalculoPedido.subtotal(new BigDecimal("42.00"), 3))
                .isEqualByComparingTo("126.00");
    }

    private ItemPedido item(String precio, int cantidad) {
        ItemPedido item = new ItemPedido();
        item.setPrecioUnitario(new BigDecimal(precio));
        item.setCantidad(cantidad);
        return item;
    }
}
