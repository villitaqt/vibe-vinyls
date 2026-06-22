package com.vivevinyls.pedido;

/**
 * Ciclo de vida del PEDIDO: pendiente_pago → pagado → confirmado → cancelado.
 */
public enum EstadoPedido {
    PENDIENTE_PAGO,
    PAGADO,
    CONFIRMADO,
    CANCELADO
}
