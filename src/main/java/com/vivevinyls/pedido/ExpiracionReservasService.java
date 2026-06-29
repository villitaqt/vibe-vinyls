package com.vivevinyls.pedido;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Expiración de reservas (CU-03, flujo 4a): cancela los pedidos que siguen en
 * {@code PENDIENTE_PAGO} pasada la ventana de reserva, liberando su stock.
 *
 * <p>El método {@link #expirarReservasVencidas()} es invocable directamente
 * (también desde tests); el {@link Scheduled} solo lo dispara periódicamente. La
 * planificación se activa fuera del perfil de test (ver {@code SchedulingConfig}),
 * de modo que en tests el job no corre solo y se invoca a mano.</p>
 *
 * <p><b>Límite conocido:</b> en single-instance la guardia de estado basta para
 * la idempotencia; en multi-instancia haría falta un lock distribuido (fuera del
 * MVP).</p>
 */
@Service
public class ExpiracionReservasService {

    private static final Logger log = LoggerFactory.getLogger(ExpiracionReservasService.class);

    private final PedidoRepository pedidos;
    private final CancelacionPedidoService cancelacion;
    private final long reservaTtlSegundos;

    public ExpiracionReservasService(PedidoRepository pedidos, CancelacionPedidoService cancelacion,
            @Value("${app.pedido.reserva-ttl-seconds:900}") long reservaTtlSegundos) {
        this.pedidos = pedidos;
        this.cancelacion = cancelacion;
        this.reservaTtlSegundos = reservaTtlSegundos;
    }

    /** Cancela las reservas creadas hace más de la ventana configurada. */
    public int expirarReservasVencidas() {
        return expirarAnterioresA(Instant.now().minusSeconds(reservaTtlSegundos));
    }

    /**
     * Cancela las reservas {@code PENDIENTE_PAGO} creadas antes de {@code corte}.
     * Separado para poder fijar el corte de forma determinista en tests.
     *
     * @return número de pedidos efectivamente cancelados.
     */
    public int expirarAnterioresA(Instant corte) {
        List<UUID> vencidos = pedidos
                .findByEstadoAndFechaCreacionBefore(EstadoPedido.PENDIENTE_PAGO, corte)
                .stream().map(Pedido::getId).toList();

        int cancelados = 0;
        for (UUID pedidoId : vencidos) {
            // Cada cancelación en su propia transacción (idempotente vía estado).
            if (cancelacion.cancelarSiVencido(pedidoId)) {
                cancelados++;
            }
        }
        if (cancelados > 0) {
            log.info("Expiración de reservas: {} pedido(s) cancelado(s) de {} candidato(s).",
                    cancelados, vencidos.size());
        }
        return cancelados;
    }

    /** Disparo periódico; solo cuando la planificación está activa (no en test). */
    @Scheduled(fixedDelayString = "${app.pedido.expiracion-check-ms:60000}")
    public void job() {
        expirarReservasVencidas();
    }
}
