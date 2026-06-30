package com.vivevinyls.catalogo.web;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Ficha completa de un vinilo (RF-06): sus datos, artistas, géneros, sello y el
 * stock disponible. No expone la entidad JPA.
 */
public record ViniloDetalleDTO(
        UUID id,
        String titulo,
        Integer anio,
        BigDecimal precio,
        String portadaUrl,
        String sello,
        List<String> artistas,
        List<String> generos,
        int stockDisponible) {
}
