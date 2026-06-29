package com.vivevinyls.pedido;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.catalogo.Vinilo;
import com.vivevinyls.catalogo.ViniloNoEncontradoException;
import com.vivevinyls.catalogo.ViniloRepository;
import com.vivevinyls.cuenta.Cliente;
import com.vivevinyls.cuenta.ClienteRepository;
import com.vivevinyls.cuenta.Direccion;
import com.vivevinyls.cuenta.DireccionRepository;
import com.vivevinyls.inventario.ItemReserva;
import com.vivevinyls.inventario.MovimientoStock;
import com.vivevinyls.inventario.MovimientoStockRepository;
import com.vivevinyls.inventario.TipoMovimiento;
import com.vivevinyls.pedido.web.PedidoResponse;

/**
 * Persistencia del pedido tras una reserva atómica exitosa, en <b>una sola
 * transacción</b> (RN-06, RN-07). Vive en un bean aparte de la orquestación del
 * checkout a propósito: así el {@code @Transactional} se aplica vía proxy y la
 * compensación del árbitro puede envolver el commit/rollback desde fuera.
 *
 * <p>Registra un movimiento {@link TipoMovimiento#RESERVA} (negativo) por ítem,
 * de modo que el invariante {@code disponible = Σ ledger} sigue siendo correcto.</p>
 */
@Service
public class PedidoCreacionService {

    private final ClienteRepository clientes;
    private final ViniloRepository vinilos;
    private final DireccionRepository direcciones;
    private final PedidoRepository pedidos;
    private final MovimientoStockRepository movimientos;

    public PedidoCreacionService(ClienteRepository clientes, ViniloRepository vinilos,
            DireccionRepository direcciones, PedidoRepository pedidos,
            MovimientoStockRepository movimientos) {
        this.clientes = clientes;
        this.vinilos = vinilos;
        this.direcciones = direcciones;
        this.pedidos = pedidos;
        this.movimientos = movimientos;
    }

    @Transactional
    public PedidoResponse crear(java.util.UUID clienteId, List<ItemReserva> lineas,
            java.util.UUID direccionId) {
        Cliente cliente = clientes.getReferenceById(clienteId);

        // Dirección de la libreta del propio cliente (RF-03); se copia congelada.
        Direccion direccion = direcciones.findByIdAndClienteId(direccionId, clienteId)
                .orElseThrow(() -> new PedidoInvalidoException(
                        "La dirección no existe o no pertenece al cliente"));

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.copiarDireccionEnvio(direccion); // RN-07: copia congelada

        List<Vinilo> vinilosDelPedido = new ArrayList<>(lineas.size());
        for (ItemReserva linea : lineas) {
            Vinilo vinilo = vinilos.findById(linea.viniloId())
                    .orElseThrow(() -> new ViniloNoEncontradoException(linea.viniloId()));
            ItemPedido item = new ItemPedido();
            item.setPedido(pedido);
            item.setVinilo(vinilo);
            item.setCantidad(linea.cantidad());
            // RN-06: precio congelado al momento de la compra (copia, no referencia viva).
            item.setPrecioUnitario(vinilo.getPrecio());
            pedido.getItems().add(item);
            vinilosDelPedido.add(vinilo);
        }

        pedido.setTotal(CalculoPedido.total(pedido.getItems()));
        Pedido guardado = pedidos.save(pedido); // cascade persiste los ItemPedido

        // Movimientos RESERVA (negativos) enlazados al pedido ya persistido.
        List<MovimientoStock> reservas = new ArrayList<>(lineas.size());
        for (int i = 0; i < lineas.size(); i++) {
            MovimientoStock movimiento = new MovimientoStock();
            movimiento.setVinilo(vinilosDelPedido.get(i));
            movimiento.setPedido(guardado);
            movimiento.setTipo(TipoMovimiento.RESERVA);
            movimiento.setCantidad(-lineas.get(i).cantidad());
            reservas.add(movimiento);
        }
        movimientos.saveAll(reservas);

        return PedidoResponseMapper.aResponse(guardado);
    }
}
