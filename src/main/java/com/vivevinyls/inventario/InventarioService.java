package com.vivevinyls.inventario;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.catalogo.ViniloNoEncontradoException;
import com.vivevinyls.catalogo.ViniloRepository;

/**
 * Registro de movimientos de stock desde el back-office (Frontend 3): por
 * ahora solo importaciones. Escribe el ledger durable (fuente de verdad,
 * {@link MovimientoStock}) y actualiza el contador en caliente del
 * {@link ArbitroStock} para que el disponible que ve el cliente refleje la
 * importación de inmediato, sin esperar a la siembra perezosa.
 */
@Service
@Transactional
public class InventarioService {

    private final ViniloRepository vinilos;
    private final MovimientoStockRepository movimientos;
    private final StockService stock;
    private final ArbitroStock arbitro;

    public InventarioService(ViniloRepository vinilos, MovimientoStockRepository movimientos,
            StockService stock, ArbitroStock arbitro) {
        this.vinilos = vinilos;
        this.movimientos = movimientos;
        this.stock = stock;
        this.arbitro = arbitro;
    }

    public int registrarImportacion(UUID viniloId, int cantidad) {
        if (cantidad <= 0) {
            throw new CantidadInvalidaException("La cantidad a importar debe ser mayor que 0");
        }
        if (viniloId == null || !vinilos.existsById(viniloId)) {
            throw new ViniloNoEncontradoException(viniloId);
        }

        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setVinilo(vinilos.getReferenceById(viniloId));
        movimiento.setTipo(TipoMovimiento.IMPORTACION);
        movimiento.setCantidad(cantidad);
        movimientos.saveAndFlush(movimiento);

        arbitro.incrementarDisponible(viniloId, cantidad);
        return stock.disponible(viniloId);
    }
}
