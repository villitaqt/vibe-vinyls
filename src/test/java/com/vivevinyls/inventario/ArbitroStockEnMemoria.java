package com.vivevinyls.inventario;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Doble de test del {@link ArbitroStock} con la <b>misma semántica atómica</b>
 * que la implementación Redis (check-and-decrement todo-o-nada), pero en memoria
 * y thread-safe vía un cerrojo único: como Redis ejecuta el script Lua en un
 * solo hilo, serializar con {@code synchronized} reproduce exactamente esa
 * atomicidad. Evita depender de un Redis real en el contenedor de build.
 *
 * <p>Activo solo bajo el perfil {@code test} (component-scan lo recoge desde las
 * clases de test); en ese perfil {@code ArbitroStockRedis} no se carga.</p>
 */
@Component
@Profile("test")
public class ArbitroStockEnMemoria implements ArbitroStock {

    private final StockService stock;
    private final Map<UUID, Integer> disponibles = new HashMap<>();

    public ArbitroStockEnMemoria(StockService stock) {
        this.stock = stock;
    }

    @Override
    public synchronized ResultadoReserva reservar(List<ItemReserva> items) {
        for (ItemReserva item : items) {
            // Siembra perezosa desde el ledger, igual que el SET NX de Redis.
            disponibles.computeIfAbsent(item.viniloId(), stock::disponible);
        }
        List<UUID> agotados = new ArrayList<>();
        for (ItemReserva item : items) {
            if (disponibles.get(item.viniloId()) < item.cantidad()) {
                agotados.add(item.viniloId());
            }
        }
        if (!agotados.isEmpty()) {
            return ResultadoReserva.rechazado(agotados);
        }
        for (ItemReserva item : items) {
            disponibles.merge(item.viniloId(), -item.cantidad(), Integer::sum);
        }
        return ResultadoReserva.ok();
    }

    @Override
    public synchronized void compensar(List<ItemReserva> items) {
        for (ItemReserva item : items) {
            disponibles.merge(item.viniloId(), item.cantidad(), Integer::sum);
        }
    }

    @Override
    public synchronized void incrementarDisponible(UUID viniloId, int cantidad) {
        if (!disponibles.containsKey(viniloId)) {
            // El ledger (leído aquí) ya incluye la importación recién persistida.
            disponibles.put(viniloId, stock.disponible(viniloId));
        } else {
            disponibles.merge(viniloId, cantidad, Integer::sum);
        }
    }

    /** Limpia el estado en caliente entre tests (la BD se revierte por @Transactional). */
    public synchronized void vaciar() {
        disponibles.clear();
    }
}
