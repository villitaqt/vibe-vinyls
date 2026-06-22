package com.vivevinyls.cuenta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Persistir y leer CLIENTE y su libreta de DIRECCION (1 → N).
 */
@DataJpaTest
class CuentaRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DireccionRepository direccionRepository;

    @Test
    void persisteYLeeClientePorEmail() {
        Cliente cliente = new Cliente();
        cliente.setEmail("coleccionista@vivevinyls.com");
        cliente.setNombre("Diego Villa");
        clienteRepository.save(cliente);

        em.flush();
        em.clear();

        assertThat(clienteRepository.findByEmail("coleccionista@vivevinyls.com"))
                .get()
                .extracting(Cliente::getNombre).isEqualTo("Diego Villa");
    }

    @Test
    void persisteLibretaDeDirecciones() {
        Cliente cliente = new Cliente();
        cliente.setEmail("libreta@vivevinyls.com");
        cliente.setNombre("Ana Pérez");
        cliente = clienteRepository.save(cliente);

        Direccion casa = nuevaDireccion(cliente, "Av. Siempre Viva 742", "Lima");
        Direccion oficina = nuevaDireccion(cliente, "Jr. Unión 100", "Lima");
        direccionRepository.save(casa);
        direccionRepository.save(oficina);

        em.flush();
        em.clear();

        List<Direccion> libreta = direccionRepository.findByClienteId(cliente.getId());
        assertThat(libreta).hasSize(2)
                .extracting(Direccion::getCalle)
                .containsExactlyInAnyOrder("Av. Siempre Viva 742", "Jr. Unión 100");
    }

    private Direccion nuevaDireccion(Cliente cliente, String calle, String ciudad) {
        Direccion d = new Direccion();
        d.setCliente(cliente);
        d.setDestinatario(cliente.getNombre());
        d.setCalle(calle);
        d.setCiudad(ciudad);
        d.setPais("Perú");
        return d;
    }
}
