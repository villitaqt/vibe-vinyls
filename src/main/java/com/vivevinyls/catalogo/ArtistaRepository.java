package com.vivevinyls.catalogo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistaRepository extends JpaRepository<Artista, UUID> {

    /** Para el alta/edición admin: reutiliza el artista si ya existe (find-or-create). */
    Optional<Artista> findByNombreIgnoreCase(String nombre);
}
