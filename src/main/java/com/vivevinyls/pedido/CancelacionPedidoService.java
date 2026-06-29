package com.vivevinyls.pedido;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.inventario.ArbitroStock;
import com.vivevinyls.inventario.ItemReserva;
import com.vivevinyls.inventario.MovimientoStock;
import com.vivevinyls.inventario.MovimientoStockRepository;
import com.vivevinyls.inventario.TipoMovimiento;

/**
 * Cancela un pedido con reserva vencida, <b>cada uno en su propia transacción</b>
 * (CU-03 flujo 4a). Libera el stock en Redis ({@link ArbitroStock#compensar}),
 * registra un movimiento {@link TipoMovimiento#CANCELACION} (positivo) por ítem
 * —de modo que el disponible vuelve al valor previo a la reserva— y pasa el
 * pedido a {@code CANCELADO}.
 *
 * <p><b>Idempotencia:</b> la guardia es la transición {@code PENDIENTE_PAGO →
 * CANCELADO}. Si el pedido ya no está en {@code PENDIENTE_PAGO}, se omite; correr
 * el job dos veces no doble-libera.</p>
 */
@Service
public class CancelacionPedidoService {

    private final PedidoRepository pedidos;
    private final MovimientoStockRepository movimientos;
    private final ArbitroStock arbitro;

    public CancelacionPedidoService(PedidoRepository pedidos,
            MovimientoStockRepository movimientos, ArbitroStock arbitro) {
        this.pedidos = pedidos;
        this.movimientos = movimientos;
        this.arbitro = arbitro;
    }

    /** @return true si canceló el pedido; false si ya no estaba en PENDIENTE_PAGO. */
    @Transactional
    public boolean cancelarSiVencido(UUID pedidoId) {
        Pedido pedido = pedidos.findById(pedidoId).orElse(null);
        if (pedido == null || pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            return false; // idempotente: ya pagado/cancelado o inexistente
        }

        List<ItemReserva> reservas = pedido.getItems().stream()
                .map(i -> new ItemReserva(i.getVinilo().getId(), i.getCantidad()))
                .toList();

        // Devuelve las unidades al disponible "en caliente" (Redis).
        arbitro.compensar(reservas);

        // CANCELACION (+) compensa la RESERVA (−) en el ledger durable.
        for (ItemPedido item : pedido.getItems()) {
            MovimientoStock cancelacion = new MovimientoStock();
            cancelacion.setVinilo(item.getVinilo());
            cancelacion.setPedido(pedido);
            cancelacion.setTipo(TipoMovimiento.CANCELACION);
            cancelacion.setCantidad(item.getCantidad());
            movimientos.save(cancelacion);
        }

        pedido.setEstado(EstadoPedido.CANCELADO);
        return true;
    }
}
