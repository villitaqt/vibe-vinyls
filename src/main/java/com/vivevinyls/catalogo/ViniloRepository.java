package com.vivevinyls.catalogo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Repositorio del catálogo. Extiende {@link JpaSpecificationExecutor} porque la
 * búsqueda por texto y los filtros (género, artista, año, sello) son todos
 * opcionales y combinables: las Specifications permiten armar el WHERE solo con
 * los criterios presentes, sin multiplicar query methods por cada combinación.
 */
public interface ViniloRepository
        extends JpaRepository<Vinilo, UUID>, JpaSpecificationExecutor<Vinilo> {
}
