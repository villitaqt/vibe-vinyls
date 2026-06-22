package com.vivevinyls.pedido;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.vivevinyls.catalogo.Sello;
import com.vivevinyls.catalogo.Vinilo;
import com.vivevinyls.cuenta.Cliente;
import com.vivevinyls.cuenta.Direccion;

/**
 * Persistir y leer PEDIDO e ITEM_PEDIDO, verificando que el pedido guarda la
 * copia congelada de la dirección (RN-07) y el ítem congela precio y cantidad
 * (RN-06).
 */
@DataJpaTest
class PedidoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private ItemPedidoRepository itemPedidoRepository;

    @Test
    void persistePedidoConItemYDireccionCongelada() {
        Cliente cliente = new Cliente();
        cliente.setEmail("compra@vivevinyls.com");
        cliente.setNombre("Carlos Ruiz");
        em.persist(cliente);

        Direccion direccion = new Direccion();
        direccion.setCliente(cliente);
        direccion.setDestinatario("Carlos Ruiz");
        direccion.setCalle("Calle Falsa 123");
        direccion.setCiudad("Bogotá");
        direccion.setPais("Colombia");
        em.persist(direccion);

        Sello sello = new Sello("Atlantic");
        em.persist(sello);
        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo("Giant Steps");
        vinilo.setAnio(1960);
        vinilo.setPrecio(new BigDecimal("34.00"));
        vinilo.setSello(sello);
        em.persist(vinilo);
        em.flush();

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setEstado(EstadoPedido.PENDIENTE_PAGO);
        pedido.setTotal(new BigDecimal("68.00"));
        pedido.copiarDireccionEnvio(direccion);

        ItemPedido item = new ItemPedido();
        item.setPedido(pedido);
        item.setVinilo(vinilo);
        item.setCantidad(2);
        item.setPrecioUnitario(new BigDecimal("34.00"));
        pedido.getItems().add(item);

        Pedido guardado = pedidoRepository.save(pedido);
        em.flush();
        em.clear();

        Pedido leido = pedidoRepository.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getEstado()).isEqualTo(EstadoPedido.PENDIENTE_PAGO);
        assertThat(leido.getTotal()).isEqualByComparingTo("68.00");
        // Copia congelada de la dirección (RN-07)
        assertThat(leido.getEnvioCalle()).isEqualTo("Calle Falsa 123");
        assertThat(leido.getEnvioCiudad()).isEqualTo("Bogotá");
        assertThat(leido.getEnvioPais()).isEqualTo("Colombia");

        var items = itemPedidoRepository.findByPedidoId(leido.getId());
        assertThat(items).hasSize(1);
        // Precio y cantidad congelados (RN-06)
        assertThat(items.get(0).getCantidad()).isEqualTo(2);
        assertThat(items.get(0).getPrecioUnitario()).isEqualByComparingTo("34.00");
    }
}
