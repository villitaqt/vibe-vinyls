package com.vivevinyls.catalogo;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SelloRepository extends JpaRepository<Sello, UUID> {

    /** Para el alta/edición admin: reutiliza el sello si ya existe (find-or-create). */
    Optional<Sello> findByNombreIgnoreCase(String nombre);
}
