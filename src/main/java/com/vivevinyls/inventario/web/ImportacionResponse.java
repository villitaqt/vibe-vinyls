package com.vivevinyls.inventario.web;

import java.util.UUID;

/** Respuesta de {@code POST /admin/inventario/importacion}: stock ya actualizado. */
public record ImportacionResponse(UUID viniloId, int cantidad, int stockDisponible) {
}
