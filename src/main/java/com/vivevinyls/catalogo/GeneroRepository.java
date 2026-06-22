package com.vivevinyls.catalogo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GeneroRepository extends JpaRepository<Genero, UUID> {
}
