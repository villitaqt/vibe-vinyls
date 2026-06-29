package com.vivevinyls.inventario;

import java.util.UUID;

/**
 * Petición de reserva de unidades de un vinilo para el árbitro de stock.
 * Cantidad siempre positiva (el árbitro la descuenta del disponible).
 */
public record ItemReserva(UUID viniloId, int cantidad) {
}
