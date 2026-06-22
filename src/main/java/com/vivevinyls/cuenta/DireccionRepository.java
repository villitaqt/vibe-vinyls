package com.vivevinyls.cuenta;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionRepository extends JpaRepository<Direccion, UUID> {

    List<Direccion> findByClienteId(UUID clienteId);
}
