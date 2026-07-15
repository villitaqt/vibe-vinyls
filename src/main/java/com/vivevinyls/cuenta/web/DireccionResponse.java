package com.vivevinyls.cuenta.web;

import java.util.UUID;

/**
 * Dirección de la libreta tal como se expone al cliente (RF-03). No serializa
 * la entidad JPA.
 */
public record DireccionResponse(
        UUID id,
        String destinatario,
        String calle,
        String ciudad,
        String region,
        String pais,
        String codigoPostal,
        String telefono) {
}
