package com.vivevinyls.cuenta.web;

import java.util.UUID;

/**
 * Token de sesión emitido tras un login correcto (RF-02). {@code tokenType} es
 * {@code "Bearer"}; el cliente lo envía en la cabecera {@code Authorization}.
 * {@code cliente} trae los datos básicos (incluido el rol) para enrutar en el
 * frontend sin decodificar el JWT; {@code clienteId} se mantiene por
 * compatibilidad.
 */
public record LoginResponse(String token, String tokenType, UUID clienteId,
        long expiraEnSegundos, ClienteDTO cliente) {
}
