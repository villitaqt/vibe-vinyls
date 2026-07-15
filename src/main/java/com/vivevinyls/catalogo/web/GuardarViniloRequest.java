package com.vivevinyls.catalogo.web;

import java.math.BigDecimal;
import java.util.List;

/**
 * Alta/edición de un vinilo desde el back-office ({@code POST/PUT
 * /admin/vinilos}). Artistas y géneros son nombres en texto libre: el MVP
 * admin no gestiona las entidades N:M directamente (ver nota en
 * {@code AdminCatalogoService}).
 */
public record GuardarViniloRequest(
        String titulo,
        Integer anio,
        BigDecimal precio,
        String sello,
        List<String> artistas,
        List<String> generos,
        String portadaUrl) {
}
