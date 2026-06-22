package com.vivevinyls.catalogo;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

/**
 * Persistir y leer las entidades núcleo del dominio catálogo, incluyendo los
 * puentes N:M VINILO_ARTISTA y VINILO_GENERO.
 */
@DataJpaTest
class CatalogoRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SelloRepository selloRepository;

    @Autowired
    private ArtistaRepository artistaRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private ViniloRepository viniloRepository;

    @Autowired
    private ViniloArtistaRepository viniloArtistaRepository;

    @Autowired
    private ViniloGeneroRepository viniloGeneroRepository;

    @Test
    void persisteYLeeSelloArtistaGenero() {
        Sello sello = selloRepository.save(new Sello("Blue Note"));
        Artista artista = artistaRepository.save(new Artista("John Coltrane"));
        Genero genero = generoRepository.save(new Genero("Jazz"));

        assertThat(selloRepository.findById(sello.getId())).get()
                .extracting(Sello::getNombre).isEqualTo("Blue Note");
        assertThat(artistaRepository.findById(artista.getId())).get()
                .extracting(Artista::getNombre).isEqualTo("John Coltrane");
        assertThat(generoRepository.findById(genero.getId())).get()
                .extracting(Genero::getNombre).isEqualTo("Jazz");
    }

    @Test
    void persisteYLeeViniloConSello() {
        Sello sello = selloRepository.save(new Sello("Impulse!"));

        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo("A Love Supreme");
        vinilo.setAnio(1965);
        vinilo.setPrecio(new BigDecimal("39.90"));
        vinilo.setSello(sello);
        Vinilo guardado = viniloRepository.save(vinilo);

        em.flush();
        em.clear();

        Vinilo leido = viniloRepository.findById(guardado.getId()).orElseThrow();
        assertThat(leido.getTitulo()).isEqualTo("A Love Supreme");
        assertThat(leido.getAnio()).isEqualTo(1965);
        assertThat(leido.getPrecio()).isEqualByComparingTo("39.90");
        assertThat(leido.getSello().getNombre()).isEqualTo("Impulse!");
    }

    @Test
    void persisteLosPuentesNaMConVinilo() {
        Sello sello = selloRepository.save(new Sello("Verve"));
        Artista artista = artistaRepository.save(new Artista("Ella Fitzgerald"));
        Genero genero = generoRepository.save(new Genero("Vocal Jazz"));

        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo("Ella in Berlin");
        vinilo.setAnio(1960);
        vinilo.setPrecio(new BigDecimal("29.50"));
        vinilo.setSello(sello);
        vinilo = viniloRepository.save(vinilo);
        em.flush();

        viniloArtistaRepository.save(new ViniloArtista(vinilo, artista));
        viniloGeneroRepository.save(new ViniloGenero(vinilo, genero));
        em.flush();
        em.clear();

        ViniloArtista va = viniloArtistaRepository
                .findById(new ViniloArtistaId(vinilo.getId(), artista.getId()))
                .orElseThrow();
        assertThat(va.getArtista().getNombre()).isEqualTo("Ella Fitzgerald");

        ViniloGenero vg = viniloGeneroRepository
                .findById(new ViniloGeneroId(vinilo.getId(), genero.getId()))
                .orElseThrow();
        assertThat(vg.getGenero().getNombre()).isEqualTo("Vocal Jazz");
    }
}
