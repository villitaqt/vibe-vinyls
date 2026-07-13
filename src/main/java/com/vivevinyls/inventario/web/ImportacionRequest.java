package com.vivevinyls.inventario.web;

import java.util.UUID;

/** Cuerpo de {@code POST /admin/inventario/importacion}. */
public record ImportacionRequest(UUID viniloId, int cantidad) {
}
