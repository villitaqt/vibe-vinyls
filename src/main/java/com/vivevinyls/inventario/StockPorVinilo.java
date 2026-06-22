package com.vivevinyls.inventario;

import java.util.UUID;

/**
 * Proyección para sumar el stock del ledger en lote (una sola consulta para
 * varios vinilos), evitando el problema N+1 al construir el listado del
 * catálogo.
 */
public interface StockPorVinilo {

    UUID getViniloId();

    long getCantidad();
}
