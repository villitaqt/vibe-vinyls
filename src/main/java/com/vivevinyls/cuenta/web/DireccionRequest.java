package com.vivevinyls.cuenta.web;

/**
 * Alta de una dirección en la libreta del cliente (RF-03). {@code region},
 * {@code codigoPostal} y {@code telefono} son opcionales (espejan los campos
 * nullable de la entidad {@code Direccion}).
 */
public record DireccionRequest(
        String destinatario,
        String calle,
        String ciudad,
        String region,
        String pais,
        String codigoPostal,
        String telefono) {
}
