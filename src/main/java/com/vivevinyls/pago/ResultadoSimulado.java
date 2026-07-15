package com.vivevinyls.pago;

/**
 * Control de la pasarela simulada para demo y tests: fuerza el camino del cobro
 * de forma determinista.
 *
 * <p><b>Artefacto temporal:</b> viaja en el request de pago como campo opcional
 * (por defecto {@code CAPTURA}) y desaparece cuando se integre una pasarela real,
 * sin cambiar el resto del contrato.</p>
 */
public enum ResultadoSimulado {
    CAPTURA,
    RECHAZO
}
