package com.vivevinyls.pago.web;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Resultado de un intento de pago (CU-04). Tanto la captura como el rechazo se
 * devuelven con 200: el cliente interpreta el desenlace desde el cuerpo
 * ({@code estado} = CAPTURADO/FALLIDO), de forma uniforme. En el rechazo,
 * {@code estadoPedido} sigue {@code PENDIENTE_PAGO} y se puede reintentar.
 */
public record PagoResponse(
        UUID pagoId,
        String estado,
        UUID pedidoId,
        String estadoPedido,
        String referenciaExterna,
        BigDecimal monto) {
}
