package com.vivevinyls.catalogo.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Vinilo tal como aparece en el listado del catálogo (RF-05). Incluye el stock
 * disponible calculado desde el ledger. No expone la entidad JPA.
 */
public record ViniloResumenDTO(
        UUID id,
        String titulo,
        Integer anio,
        BigDecimal precio,
        String portadaUrl,
        String sello,
        List<String> artistas,
        int stockDisponible) {
}
