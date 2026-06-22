package com.vivevinyls.inventario;

/**
 * Tipo de movimiento del ledger de inventario. El signo de la cantidad lo
 * fija quien registra el movimiento:
 * <ul>
 *   <li>{@code IMPORTACION} (+): ingreso de stock.</li>
 *   <li>{@code RESERVA} (−): comprometido temporalmente por un pedido.</li>
 *   <li>{@code CONFIRMACION}: consolidación de la reserva en venta.</li>
 *   <li>{@code CANCELACION} (+): liberación de una reserva expirada/cancelada.</li>
 * </ul>
 */
public enum TipoMovimiento {
    IMPORTACION,
    RESERVA,
    CONFIRMACION,
    CANCELACION
}
