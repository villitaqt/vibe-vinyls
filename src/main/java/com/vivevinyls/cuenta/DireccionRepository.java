package com.vivevinyls.cuenta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DireccionRepository extends JpaRepository<Direccion, UUID> {

    List<Direccion> findByClienteId(UUID clienteId);

    /** Dirección por id solo si pertenece al cliente (libreta del dueño, RF-03). */
    Optional<Direccion> findByIdAndClienteId(UUID id, UUID clienteId);

    boolean existsByIdAndClienteId(UUID id, UUID clienteId);
}
