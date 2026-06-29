package com.vivevinyls.inventario;

import java.util.List;
import java.util.UUID;

/**
 * Resultado de una reserva atómica multi-ítem (todo o nada). Si {@code exitoso},
 * todas las unidades quedaron reservadas; si no, {@code agotados} lista los
 * vinilos que no tenían saldo suficiente y <b>no se reservó ninguno</b>.
 */
public record ResultadoReserva(boolean exitoso, List<UUID> agotados) {

    public static ResultadoReserva ok() {
        return new ResultadoReserva(true, List.of());
    }

    public static ResultadoReserva rechazado(List<UUID> agotados) {
        return new ResultadoReserva(false, List.copyOf(agotados));
    }
}
