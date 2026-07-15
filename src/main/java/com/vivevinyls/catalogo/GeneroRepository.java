package com.vivevinyls.catalogo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneroRepository extends JpaRepository<Genero, UUID> {

    /** Para el alta/edición admin: reutiliza el género si ya existe (find-or-create). */
    Optional<Genero> findByNombreIgnoreCase(String nombre);
}
