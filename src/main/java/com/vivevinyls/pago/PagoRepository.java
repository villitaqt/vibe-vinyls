package com.vivevinyls.pago;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoRepository extends JpaRepository<Pago, UUID> {

    List<Pago> findByPedidoId(UUID pedidoId);
}
