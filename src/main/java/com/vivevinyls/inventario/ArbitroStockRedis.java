package com.vivevinyls.inventario;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Árbitro de stock con Redis: contador por vinilo en {@code stock:disp:{id}} y
 * reserva atómica multi-ítem vía script Lua (todo o nada).
 *
 * <p><b>Siembra perezosa:</b> antes del script, cada clave que no exista se
 * inicializa con el disponible del ledger usando {@code SET NX} (seguro ante
 * carrera: si dos requests concurrentes siembran, gana uno y el otro es no-op).
 * El script asume claves ya existentes.</p>
 *
 * <p>No activo bajo el perfil {@code test}: ahí se cablea
 * {@code ArbitroStockEnMemoria} con la misma semántica, evitando depender de un
 * Redis real (Testcontainers no es viable dentro del contenedor de build).</p>
 */
@Component
@Profile("!test")
public class ArbitroStockRedis implements ArbitroStock {

    private static final String PREFIJO = "stock:disp:";

    private final StringRedisTemplate redis;
    private final StockService stock;
    private final RedisScript<List> scriptReserva;

    public ArbitroStockRedis(StringRedisTemplate redis, StockService stock) {
        this.redis = redis;
        this.stock = stock;
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/reservar-stock.lua"));
        script.setResultType(List.class);
        this.scriptReserva = script;
    }

    @Override
    public ResultadoReserva reservar(List<ItemReserva> items) {
        List<String> claves = new ArrayList<>(items.size());
        Object[] cantidades = new Object[items.size()];
        for (int i = 0; i < items.size(); i++) {
            ItemReserva item = items.get(i);
            String clave = clave(item.viniloId());
            // Siembra perezosa e idempotente desde el ledger (verdad durable).
            redis.opsForValue().setIfAbsent(clave, Integer.toString(stock.disponible(item.viniloId())));
            claves.add(clave);
            cantidades[i] = Integer.toString(item.cantidad());
        }

        @SuppressWarnings("unchecked")
        List<String> agotadas = redis.execute(scriptReserva, claves, cantidades);
        if (agotadas != null && !agotadas.isEmpty()) {
            List<UUID> agotados = agotadas.stream().map(this::viniloDeClave).toList();
            return ResultadoReserva.rechazado(agotados);
        }
        return ResultadoReserva.ok();
    }

    @Override
    public void compensar(List<ItemReserva> items) {
        for (ItemReserva item : items) {
            redis.opsForValue().increment(clave(item.viniloId()), item.cantidad());
        }
    }

    private String clave(UUID viniloId) {
        return PREFIJO + viniloId;
    }

    private UUID viniloDeClave(String clave) {
        return UUID.fromString(clave.substring(PREFIJO.length()));
    }
}
