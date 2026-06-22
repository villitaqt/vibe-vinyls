package com.vivevinyls.pago;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.vivevinyls.cuenta.Cliente;
import com.vivevinyls.pedido.EstadoPedido;
import com.vivevinyls.pedido.Pedido;

/**
 * Persistir y leer PAGO: un pedido puede tener varios intentos de pago, cada
 * uno con su estado y la referencia externa de la pasarela (nunca datos de
 * tarjeta, RN-04).
 */
@DataJpaTest
class PagoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PagoRepository pagoRepository;

    @Test
    void persisteVariosIntentosDePagoSobreUnPedido() {
        Cliente cliente = new Cliente();
        cliente.setEmail("pago@vivevinyls.com");
        cliente.setNombre("Lucía Gómez");
        em.persist(cliente);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.setTotal(new BigDecimal("50.00"));
        em.persist(pedido);
        em.flush();

        Pago fallido = new Pago();
        fallido.setPedido(pedido);
        fallido.setEstado(EstadoPago.FALLIDO);
        fallido.setMonto(new BigDecimal("50.00"));
        fallido.setReferenciaExterna("txn_001_fallida");
        pagoRepository.save(fallido);

        Pago capturado = new Pago();
        capturado.setPedido(pedido);
        capturado.setEstado(EstadoPago.CAPTURADO);
        capturado.setMonto(new BigDecimal("50.00"));
        capturado.setReferenciaExterna("txn_002_ok");
        pagoRepository.save(capturado);

        em.flush();
        em.clear();

        var intentos = pagoRepository.findByPedidoId(pedido.getId());
        assertThat(intentos).hasSize(2)
                .extracting(Pago::getEstado)
                .containsExactlyInAnyOrder(EstadoPago.FALLIDO, EstadoPago.CAPTURADO);
        assertThat(intentos).extracting(Pago::getReferenciaExterna)
                .containsExactlyInAnyOrder("txn_001_fallida", "txn_002_ok");
    }
}
