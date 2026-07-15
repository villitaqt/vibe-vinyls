package com.vivevinyls.cuenta.web;

import java.util.UUID;

/**
 * Respuesta del registro (CU-01).
 *
 * <p>El {@code codigoVerificacion} se devuelve aquí como <b>mecanismo
 * temporal</b>: todavía no se envía correo. Cuando llegue SES/Cognito el código
 * dejará de viajar en el cuerpo, pero el resto del contrato se mantiene.</p>
 */
public record RegistroResponse(UUID clienteId, String codigoVerificacion) {
}
