package com.vivevinyls.cuenta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Persistir y leer CREDENCIAL_LOCAL con clave compartida ({@code @MapsId}) con
 * CLIENTE, y la búsqueda por correo del cliente (join). Verifica que la PK de la
 * credencial coincide con el id del cliente.
 */
@DataJpaTest
class CredencialLocalRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CredencialLocalRepository credencialRepository;

    @Test
    void persisteCredencialConClaveCompartidaYBuscaPorEmail() {
        Cliente cliente = new Cliente();
        cliente.setEmail("auth@vivevinyls.com");
        cliente.setNombre("Auth Tester");
        cliente = clienteRepository.save(cliente);

        CredencialLocal credencial = new CredencialLocal();
        credencial.setCliente(cliente);
        credencial.setPasswordHash("$2a$10$hashficticio");
        credencial.setEstado(EstadoCredencial.PENDIENTE_VERIFICACION);
        credencial.setCodigoVerificacion("123456");
        credencialRepository.save(credencial);

        em.flush();
        em.clear();

        CredencialLocal leida = credencialRepository.findByCliente_Email("auth@vivevinyls.com")
                .orElseThrow();
        // @MapsId: la PK de la credencial es el id del cliente.
        assertThat(leida.getId()).isEqualTo(cliente.getId());
        assertThat(leida.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(leida.getEstado()).isEqualTo(EstadoCredencial.PENDIENTE_VERIFICACION);
        assertThat(leida.getCodigoVerificacion()).isEqualTo("123456");
        assertThat(leida.getFechaCreacion()).isNotNull();
    }
}
