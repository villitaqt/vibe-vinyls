package com.vivevinyls.cuenta.web;

import java.util.UUID;

/**
 * Datos básicos del cliente autenticado, devueltos en el login para que el
 * frontend disponga del rol sin decodificar el JWT en el primer render.
 */
public record ClienteDTO(UUID id, String email, String nombre, String rol) {
}
