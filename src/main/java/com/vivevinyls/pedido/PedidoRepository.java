package com.vivevinyls.pedido;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, UUID> {

    List<Pedido> findByClienteId(UUID clienteId);

    /** Pedidos en un estado dado creados antes de un instante (reservas vencidas). */
    List<Pedido> findByEstadoAndFechaCreacionBefore(EstadoPedido estado, Instant corte);
}
