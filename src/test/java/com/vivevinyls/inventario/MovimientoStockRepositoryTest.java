package com.vivevinyls.inventario;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.vivevinyls.catalogo.Sello;
import com.vivevinyls.catalogo.Vinilo;
import com.vivevinyls.cuenta.Cliente;
import com.vivevinyls.pedido.EstadoPedido;
import com.vivevinyls.pedido.Pedido;

/**
 * Persistir y leer el ledger MOVIMIENTO_STOCK: movimientos ligados siempre a un
 * vinilo y opcionalmente a un pedido. El stock es la suma con signo del ledger.
 */
@DataJpaTest
class MovimientoStockRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MovimientoStockRepository movimientoRepository;

    @Test
    void persisteLedgerYCalculaStockComoSuma() {
        Sello sello = new Sello("ECM");
        em.persist(sello);
        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo("Köln Concert");
        vinilo.setAnio(1975);
        vinilo.setPrecio(new BigDecimal("42.00"));
        vinilo.setSello(sello);
        em.persist(vinilo);

        Cliente cliente = new Cliente();
        cliente.setEmail("stock@vivevinyls.com");
        cliente.setNombre("Pedro Soto");
        em.persist(cliente);
        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.setTotal(new BigDecimal("42.00"));
        em.persist(pedido);
        em.flush();

        // Importación sin pedido (+10), reserva por venta (−1)
        movimientoRepository.save(movimiento(vinilo, null, TipoMovimiento.IMPORTACION, 10));
        movimientoRepository.save(movimiento(vinilo, pedido, TipoMovimiento.RESERVA, -1));
        em.flush();
        em.clear();

        var ledger = movimientoRepository.findByViniloId(vinilo.getId());
        assertThat(ledger).hasSize(2);
        int stockFisico = ledger.stream().mapToInt(MovimientoStock::getCantidad).sum();
        assertThat(stockFisico).isEqualTo(9);

        // El movimiento de venta referencia al pedido; la importación no.
        assertThat(ledger).filteredOn(m -> m.getTipo() == TipoMovimiento.RESERVA)
                .singleElement()
                .satisfies(m -> assertThat(m.getPedido()).isNotNull());
        assertThat(ledger).filteredOn(m -> m.getTipo() == TipoMovimiento.IMPORTACION)
                .singleElement()
                .satisfies(m -> assertThat(m.getPedido()).isNull());
    }

    private MovimientoStock movimiento(Vinilo vinilo, Pedido pedido, TipoMovimiento tipo, int cantidad) {
        MovimientoStock m = new MovimientoStock();
        m.setVinilo(vinilo);
        m.setPedido(pedido);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        return m;
    }
}
