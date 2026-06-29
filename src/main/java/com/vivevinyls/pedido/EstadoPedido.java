package com.vivevinyls.pedido;

/**
 * Ciclo de vida del PEDIDO (máquina de estados, patrón e-commerce simplificado).
 *
 * <p>Semántica de cada estado:</p>
 * <ul>
 *   <li>{@code PENDIENTE_PAGO}: checkout realizado, stock reservado; se espera la
 *       captura del cobro.</li>
 *   <li>{@code PAGADO}: la pasarela capturó el cobro; venta real. El staff puede
 *       preparar el pedido y el cliente ya no cancela fácilmente.</li>
 *   <li>{@code CONFIRMADO}: el staff confirmó el despacho/envío (punto de no
 *       retorno logístico).</li>
 *   <li>{@code CANCELADO}: reserva expirada (desde {@code PENDIENTE_PAGO}) o
 *       cancelación con reembolso (desde {@code PAGADO}).</li>
 * </ul>
 *
 * <p>Transiciones válidas:</p>
 * <pre>
 *   PENDIENTE_PAGO → PAGADO       (pasarela captura el cobro)      — MVP
 *   PENDIENTE_PAGO → CANCELADO    (reserva expirada)               — MVP
 *   PAGADO         → CONFIRMADO   (staff confirma despacho)        — back-office [+]
 *   PAGADO         → CANCELADO    (cancelación con reembolso)      — back-office [+]
 * </pre>
 *
 * <p>Nota: el pago capturado lleva a {@code PAGADO}, NO directamente a
 * {@code CONFIRMADO}. Las transiciones desde {@code PAGADO} están soportadas
 * estructuralmente pero se implementan en el back-office (post-MVP).</p>
 */
public enum EstadoPedido {
    PENDIENTE_PAGO,
    PAGADO,
    CONFIRMADO,
    CANCELADO
}
