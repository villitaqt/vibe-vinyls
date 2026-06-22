package com.vivevinyls.catalogo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ViniloRepository extends JpaRepository<Vinilo, UUID> {
}
