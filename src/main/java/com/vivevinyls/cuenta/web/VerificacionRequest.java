package com.vivevinyls.cuenta.web;

/**
 * Verificación de la cuenta (CU-01): el correo y el código recibido.
 */
public record VerificacionRequest(String email, String codigo) {
}
