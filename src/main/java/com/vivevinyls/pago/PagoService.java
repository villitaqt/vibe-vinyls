package com.vivevinyls.pago;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.inventario.MovimientoStock;
import com.vivevinyls.inventario.MovimientoStockRepository;
import com.vivevinyls.inventario.TipoMovimiento;
import com.vivevinyls.pago.PasarelaPago.ResultadoCobro;
import com.vivevinyls.pago.web.PagoRequest;
import com.vivevinyls.pago.web.PagoResponse;
import com.vivevinyls.pedido.EstadoPedido;
import com.vivevinyls.pedido.EstadoPedidoInvalidoException;
import com.vivevinyls.pedido.ItemPedido;
import com.vivevinyls.pedido.Pedido;
import com.vivevinyls.pedido.PedidoNoEncontradoException;
import com.vivevinyls.pedido.PedidoRepository;

/**
 * Pago de un pedido (CU-04). Cada intento es un {@link Pago} nuevo (RN-04: solo
 * estado, monto y referencia; nunca datos de tarjeta).
 *
 * <p>Transición {@code PENDIENTE_PAGO → PAGADO} solo si la pasarela captura. Al
 * capturar, consolida la reserva con un movimiento {@link TipoMovimiento#CONFIRMACION}
 * <b>neutral</b> (cantidad 0): la unidad ya salió del disponible en la reserva
 * (5a) y se queda fuera porque se vendió; el ledger no cambia y <b>no se toca
 * Redis</b>. Si la pasarela rechaza, el pedido sigue {@code PENDIENTE_PAGO} y la
 * reserva NO se libera (el cliente puede reintentar); solo la expiración la
 * libera.</p>
 */
@Service
public class PagoService {

    private final PedidoRepository pedidos;
    private final PagoRepository pagos;
    private final PasarelaPago pasarela;
    private final MovimientoStockRepository movimientos;

    public PagoService(PedidoRepository pedidos, PagoRepository pagos,
            PasarelaPago pasarela, MovimientoStockRepository movimientos) {
        this.pedidos = pedidos;
        this.pagos = pagos;
        this.pasarela = pasarela;
        this.movimientos = movimientos;
    }

    @Transactional
    public PagoResponse pagar(UUID clienteId, UUID pedidoId, PagoRequest req) {
        Pedido pedido = pedidos.findById(pedidoId)
                .filter(p -> p.getCliente().getId().equals(clienteId))
                .orElseThrow(() -> new PedidoNoEncontradoException(pedidoId));

        if (pedido.getEstado() != EstadoPedido.PENDIENTE_PAGO) {
            throw new EstadoPedidoInvalidoException(
                    "Solo se puede pagar un pedido en PENDIENTE_PAGO; está en " + pedido.getEstado());
        }

        ResultadoSimulado simulado = req == null ? null : req.resultadoSimulado();
        ResultadoCobro cobro = pasarela.cobrar(pedido.getTotal(), simulado);

        Pago pago = new Pago();
        pago.setPedido(pedido);
        pago.setMonto(pedido.getTotal());
        pago.setReferenciaExterna(cobro.referenciaExterna());
        pago.setEstado(cobro.capturado() ? EstadoPago.CAPTURADO : EstadoPago.FALLIDO);
        pago = pagos.save(pago);

        if (cobro.capturado()) {
            consolidarReserva(pedido);
            pedido.setEstado(EstadoPedido.PAGADO);
        }

        return new PagoResponse(pago.getId(), pago.getEstado().name(), pedido.getId(),
                pedido.getEstado().name(), pago.getReferenciaExterna(), pago.getMonto());
    }

    /**
     * Marca la venta en el ledger con un {@link TipoMovimiento#CONFIRMACION}
     * neutral (cantidad 0) por ítem: trazabilidad sin alterar el disponible.
     */
    private void consolidarReserva(Pedido pedido) {
        for (ItemPedido item : pedido.getItems()) {
            MovimientoStock confirmacion = new MovimientoStock();
            confirmacion.setVinilo(item.getVinilo());
            confirmacion.setPedido(pedido);
            confirmacion.setTipo(TipoMovimiento.CONFIRMACION);
            confirmacion.setCantidad(0);
            movimientos.save(confirmacion);
        }
    }
}
