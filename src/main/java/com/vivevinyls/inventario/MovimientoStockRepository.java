package com.vivevinyls.inventario;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, UUID> {

    List<MovimientoStock> findByViniloId(UUID viniloId);

    /**
     * Stock físico de un vinilo = suma con signo del ledger. {@code coalesce}
     * devuelve 0 cuando el vinilo no tiene movimientos.
     */
    @Query("""
            select coalesce(sum(m.cantidad), 0)
            from MovimientoStock m
            where m.vinilo.id = :viniloId
            """)
    long sumarCantidadPorVinilo(@Param("viniloId") UUID viniloId);

    /**
     * Misma suma para varios vinilos en una sola consulta (lote). Solo devuelve
     * filas para vinilos con movimientos; los demás se asumen en 0 en la capa
     * de servicio.
     */
    @Query("""
            select m.vinilo.id as viniloId, coalesce(sum(m.cantidad), 0) as cantidad
            from MovimientoStock m
            where m.vinilo.id in :viniloIds
            group by m.vinilo.id
            """)
    List<StockPorVinilo> sumarCantidadPorVinilos(@Param("viniloIds") Collection<UUID> viniloIds);
}
