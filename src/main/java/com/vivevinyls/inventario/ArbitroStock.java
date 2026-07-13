package com.vivevinyls.inventario;

import java.util.List;
import java.util.UUID;

/**
 * Árbitro de la disputa de stock "en caliente" (RN-02, RN-05, RNF-05): serializa
 * la concurrencia de reservas para prevenir la sobreventa. Abstrae el mecanismo
 * (Redis en producción; doble in-memory en test) tras la misma semántica
 * atómica de check-and-decrement.
 *
 * <p><b>Fuente de verdad:</b> el contador del árbitro es la verdad del
 * "disponible en caliente" para decidir la reserva; el ledger
 * {@link MovimientoStock} en Postgres es la verdad durable y auditable. El
 * primero se siembra perezosamente desde el segundo.</p>
 */
public interface ArbitroStock {

    /**
     * Reserva atómicamente, todo o nada, las unidades de todos los ítems. Si
     * algún vinilo no tiene saldo suficiente, no descuenta ninguno y devuelve los
     * agotados.
     */
    ResultadoReserva reservar(List<ItemReserva> items);

    /**
     * Devuelve unidades previamente reservadas (compensación). Se usa cuando la
     * reserva tuvo éxito pero la persistencia del pedido falló después, para no
     * dejar el contador descuadrado.
     */
    void compensar(List<ItemReserva> items);

    /**
     * Suma unidades al disponible en caliente tras una importación de stock
     * (back-office). Si la clave del vinilo aún no está sembrada, se siembra
     * desde el ledger (que ya incluye la importación) en vez de sumar dos veces.
     */
    void incrementarDisponible(UUID viniloId, int cantidad);
}
