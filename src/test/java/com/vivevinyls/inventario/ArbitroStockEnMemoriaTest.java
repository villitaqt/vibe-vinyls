package com.vivevinyls.inventario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Prevención de sobreventa (RN-02, RNF-05): el test más importante de la fase.
 * Corre contra el doble in-memory del árbitro, que reproduce la atomicidad del
 * script Lua de Redis (check-and-decrement serializado). Si el árbitro fuera
 * incorrecto bajo concurrencia, estos tests fallarían de forma no determinista.
 */
class ArbitroStockEnMemoriaTest {

    @Test
    void dosReservasConcurrentesDeLaUltimaUnidadSoloUnaGana() throws Exception {
        UUID vinilo = UUID.randomUUID();
        StockService stock = mock(StockService.class);
        when(stock.disponible(any())).thenReturn(1); // una sola unidad
        ArbitroStockEnMemoria arbitro = new ArbitroStockEnMemoria(stock);

        int exitos = reservarEnParalelo(arbitro, vinilo, 1, 2);

        assertThat(exitos).isEqualTo(1);
    }

    @Test
    void conStockNyMasSolicitudesLasReservasExitosasNuncaSuperanN() throws Exception {
        UUID vinilo = UUID.randomUUID();
        int n = 5;
        int solicitudes = 50;
        StockService stock = mock(StockService.class);
        when(stock.disponible(any())).thenReturn(n);
        ArbitroStockEnMemoria arbitro = new ArbitroStockEnMemoria(stock);

        int exitos = reservarEnParalelo(arbitro, vinilo, 1, solicitudes);

        // Exactamente N reservas de 1 unidad pueden tener éxito; ni una más
        // (sobreventa) ni una menos (el disponible no quedó negativo).
        assertThat(exitos).isEqualTo(n);
    }

    /** Lanza {@code hilos} reservas concurrentes de {@code cantidad} y cuenta los éxitos. */
    private int reservarEnParalelo(ArbitroStockEnMemoria arbitro, UUID vinilo,
            int cantidad, int hilos) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        AtomicInteger exitos = new AtomicInteger();
        List<ItemReserva> items = List.of(new ItemReserva(vinilo, cantidad));

        for (int i = 0; i < hilos; i++) {
            pool.submit(() -> {
                listos.countDown();
                try {
                    salida.await(); // todos arrancan a la vez para forzar la carrera
                    if (arbitro.reservar(items).exitoso()) {
                        exitos.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        listos.await();
        salida.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        return exitos.get();
    }
}
