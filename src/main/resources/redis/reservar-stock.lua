-- Reserva atómica multi-ítem, todo o nada (RN-02, RNF-05).
-- KEYS[i] = clave del contador disponible de un vinilo (stock:disp:{id}).
-- ARGV[i] = unidades a reservar de ese vinilo (entero positivo).
--
-- Se ejecuta de forma atómica en Redis (un solo hilo): primero verifica que
-- TODAS las claves tengan saldo suficiente; si alguna no alcanza, no toca
-- ninguna y devuelve la lista de claves agotadas. Solo si todas alcanzan,
-- decrementa todas. Así un pedido con varios ítems nunca queda reservado a
-- medias.

local faltantes = {}
for i = 1, #KEYS do
    local disponible = tonumber(redis.call('GET', KEYS[i]))
    local pedido = tonumber(ARGV[i])
    if disponible == nil or disponible < pedido then
        faltantes[#faltantes + 1] = KEYS[i]
    end
end

if #faltantes > 0 then
    return faltantes
end

for i = 1, #KEYS do
    redis.call('DECRBY', KEYS[i], ARGV[i])
end

return {}
