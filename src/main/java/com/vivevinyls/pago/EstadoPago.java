package com.vivevinyls.pago;

/**
 * Ciclo de vida del PAGO:
 * pendiente → autorizado → capturado → fallido → reembolsado.
 */
public enum EstadoPago {
    PENDIENTE,
    AUTORIZADO,
    CAPTURADO,
    FALLIDO,
    REEMBOLSADO
}
