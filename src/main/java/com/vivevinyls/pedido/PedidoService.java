package com.vivevinyls.pedido;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.catalogo.ViniloNoEncontradoException;
import com.vivevinyls.catalogo.ViniloRepository;
import com.vivevinyls.cuenta.DireccionRepository;
import com.vivevinyls.inventario.ArbitroStock;
import com.vivevinyls.inventario.ItemReserva;
import com.vivevinyls.inventario.ResultadoReserva;
import com.vivevinyls.pago.Pago;
import com.vivevinyls.pago.PagoRepository;
import com.vivevinyls.pedido.web.CrearPedidoRequest;
import com.vivevinyls.pedido.web.CrearPedidoRequest.ItemPedidoRequest;
import com.vivevinyls.pedido.web.PedidoResponse;

/**
 * Orquesta el checkout (CU-03) y la consulta del pedido (RF-13).
 *
 * <p><b>Orden del checkout (diagrama 7.1):</b> validar entrada y existencia →
 * reservar atómicamente en el árbitro → si hay stock, persistir el pedido en una
 * transacción. La reserva ocurre <b>antes</b> de tocar Postgres (RN-05). Si la
 * persistencia falla tras una reserva exitosa, se compensa el árbitro para no
 * dejarlo descuadrado.</p>
 *
 * <p>La existencia del vinilo se valida <b>antes</b> de reservar: un vinilo
 * inexistente debe dar 404, no un 409 de "agotado".</p>
 */
@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final ArbitroStock arbitro;
    private final PedidoCreacionService creacion;
    private final PedidoRepository pedidos;
    private final ViniloRepository vinilos;
    private final DireccionRepository direcciones;
    private final PagoRepository pagos;

    public PedidoService(ArbitroStock arbitro, PedidoCreacionService creacion,
            PedidoRepository pedidos, ViniloRepository vinilos, DireccionRepository direcciones,
            PagoRepository pagos) {
        this.arbitro = arbitro;
        this.creacion = creacion;
        this.pedidos = pedidos;
        this.vinilos = vinilos;
        this.direcciones = direcciones;
        this.pagos = pagos;
    }

    public PedidoResponse checkout(UUID clienteId, CrearPedidoRequest req) {
        List<ItemReserva> lineas = normalizar(req);
        validarExistencias(clienteId, lineas, req.direccionId());

        ResultadoReserva resultado = arbitro.reservar(lineas);
        if (!resultado.exitoso()) {
            // RN-05: sin stock, no se persiste pedido ni se cobra.
            throw new StockInsuficienteException(resultado.agotados());
        }

        try {
            return creacion.crear(clienteId, lineas, req.direccionId());
        } catch (RuntimeException e) {
            // La reserva en el árbitro tuvo éxito pero la transacción falló:
            // devolvemos las unidades para no descuadrar el disponible en caliente.
            compensar(lineas);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(UUID clienteId, UUID pedidoId) {
        Pedido pedido = pedidos.findById(pedidoId)
                .filter(p -> p.getCliente().getId().equals(clienteId))
                .orElseThrow(() -> new PedidoNoEncontradoException(pedidoId));
        return PedidoResponseMapper.aResponse(pedido, ultimoPago(pedidoId));
    }

    /** Último intento de pago del pedido (el más reciente), o null si no hay. */
    private Pago ultimoPago(UUID pedidoId) {
        return pagos.findByPedidoId(pedidoId).stream()
                .max(java.util.Comparator.comparing(Pago::getFechaCreacion))
                .orElse(null);
    }

    /** Valida forma del request y fusiona líneas duplicadas (mismo vinilo). */
    private List<ItemReserva> normalizar(CrearPedidoRequest req) {
        if (req == null || req.items() == null || req.items().isEmpty()) {
            throw new PedidoInvalidoException("El pedido debe tener al menos un ítem");
        }
        if (req.direccionId() == null) {
            throw new PedidoInvalidoException("Falta la dirección de envío");
        }
        Map<UUID, Integer> porVinilo = new LinkedHashMap<>();
        for (ItemPedidoRequest item : req.items()) {
            if (item == null || item.viniloId() == null) {
                throw new PedidoInvalidoException("Cada ítem requiere un viniloId");
            }
            if (item.cantidad() <= 0) {
                throw new PedidoInvalidoException("La cantidad debe ser mayor que 0");
            }
            porVinilo.merge(item.viniloId(), item.cantidad(), Integer::sum);
        }
        List<ItemReserva> lineas = new ArrayList<>(porVinilo.size());
        porVinilo.forEach((viniloId, cantidad) -> lineas.add(new ItemReserva(viniloId, cantidad)));
        return lineas;
    }

    /**
     * Existencia de vinilos y de la dirección, antes de reservar: garantiza 404
     * para vinilo inexistente y 400 para dirección inválida, en lugar de un 409.
     */
    private void validarExistencias(UUID clienteId, List<ItemReserva> lineas, UUID direccionId) {
        for (ItemReserva linea : lineas) {
            if (!vinilos.existsById(linea.viniloId())) {
                throw new ViniloNoEncontradoException(linea.viniloId());
            }
        }
        if (!direcciones.existsByIdAndClienteId(direccionId, clienteId)) {
            throw new PedidoInvalidoException("La dirección no existe o no pertenece al cliente");
        }
    }

    private void compensar(List<ItemReserva> lineas) {
        try {
            arbitro.compensar(lineas);
        } catch (RuntimeException e) {
            log.error("Fallo al compensar la reserva tras error de persistencia: {}", lineas, e);
        }
    }
}
