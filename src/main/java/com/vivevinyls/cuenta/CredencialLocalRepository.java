package com.vivevinyls.cuenta;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CredencialLocalRepository extends JpaRepository<CredencialLocal, UUID> {

    /** Busca la credencial por el correo de su cliente (join al {@link Cliente}). */
    Optional<CredencialLocal> findByCliente_Email(String email);
}
