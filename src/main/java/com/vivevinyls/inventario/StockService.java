package com.vivevinyls.inventario;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cálculo del stock disponible a partir del ledger {@link MovimientoStock}.
 *
 * <p>En esta fase (catálogo, solo lectura) el disponible es simplemente la suma
 * con signo del ledger (verdad histórica en Postgres). La capa de árbitro
 * atómico en Redis —que descuenta reservas en caliente durante la compra— llega
 * en la fase de compra; aquí no se implementa.</p>
 */
@Service
@Transactional(readOnly = true)
public class StockService {

    private final MovimientoStockRepository movimientos;

    public StockService(MovimientoStockRepository movimientos) {
        this.movimientos = movimientos;
    }

    /** Stock disponible de un vinilo = suma con signo de su ledger. */
    public int disponible(UUID viniloId) {
        return (int) movimientos.sumarCantidadPorVinilo(viniloId);
    }

    /**
     * Stock disponible para varios vinilos de una vez (una sola consulta).
     * Devuelve siempre una entrada por cada id solicitado: los vinilos sin
     * movimientos quedan en 0.
     */
    public Map<UUID, Integer> disponiblePorVinilos(Collection<UUID> viniloIds) {
        Map<UUID, Integer> resultado = new HashMap<>();
        for (UUID id : viniloIds) {
            resultado.put(id, 0);
        }
        for (StockPorVinilo fila : movimientos.sumarCantidadPorVinilos(viniloIds)) {
            resultado.put(fila.getViniloId(), (int) fila.getCantidad());
        }
        return resultado;
    }
}
