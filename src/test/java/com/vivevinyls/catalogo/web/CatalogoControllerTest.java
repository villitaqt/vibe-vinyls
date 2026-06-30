package com.vivevinyls.catalogo.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.catalogo.Artista;
import com.vivevinyls.catalogo.ArtistaRepository;
import com.vivevinyls.catalogo.Genero;
import com.vivevinyls.catalogo.GeneroRepository;
import com.vivevinyls.catalogo.Sello;
import com.vivevinyls.catalogo.SelloRepository;
import com.vivevinyls.catalogo.Vinilo;
import com.vivevinyls.catalogo.ViniloArtista;
import com.vivevinyls.catalogo.ViniloGenero;
import com.vivevinyls.catalogo.ViniloRepository;
import com.vivevinyls.inventario.MovimientoStock;
import com.vivevinyls.inventario.MovimientoStockRepository;
import com.vivevinyls.inventario.TipoMovimiento;

/**
 * Tests de la capa web del catálogo (CU-02) end-to-end contra H2: listado con
 * filtro, búsqueda por texto, paginación, ficha existente y ficha 404. El stock
 * de cada respuesta se calcula desde el ledger, no desde un atributo.
 *
 * <p>El seed vive solo aquí (perfil de test) y se revierte por test gracias a
 * {@link Transactional}; no toca datos de dev/producción.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private SelloRepository sellos;
    @Autowired private ArtistaRepository artistas;
    @Autowired private GeneroRepository generos;
    @Autowired private ViniloRepository vinilos;
    @Autowired private MovimientoStockRepository movimientos;

    private UUID idLoveSupreme;

    @BeforeEach
    void seed() {
        Sello impulse = sellos.save(new Sello("Impulse!"));
        Sello columbia = sellos.save(new Sello("Columbia"));
        Sello blueNote = sellos.save(new Sello("Blue Note"));

        Artista coltrane = artistas.save(new Artista("John Coltrane"));
        Artista miles = artistas.save(new Artista("Miles Davis"));

        Genero jazz = generos.save(new Genero("Jazz"));
        Genero bebop = generos.save(new Genero("Bebop"));

        // A Love Supreme: stock disponible = +10 - 3 = 7
        Vinilo loveSupreme = guardarVinilo("A Love Supreme", 1965, "39.90", impulse, coltrane, jazz);
        idLoveSupreme = loveSupreme.getId();
        movimiento(loveSupreme, TipoMovimiento.IMPORTACION, 10);
        movimiento(loveSupreme, TipoMovimiento.RESERVA, -3);

        // Kind of Blue: stock disponible = +5
        Vinilo kindOfBlue = guardarVinilo("Kind of Blue", 1959, "34.00", columbia, miles, jazz);
        movimiento(kindOfBlue, TipoMovimiento.IMPORTACION, 5);

        // Blue Train: sin movimientos -> stock disponible = 0
        guardarVinilo("Blue Train", 1957, "29.50", blueNote, coltrane, bebop);
    }

    @Test
    void listadoCalculaStockDisponibleDesdeLedger() throws Exception {
        // Filtra a un solo vinilo por sello para aislar la aserción de stock.
        mockMvc.perform(get("/vinilos").param("sello", "Impulse!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("A Love Supreme"))
                .andExpect(jsonPath("$.content[0].artistas[0]").value("John Coltrane"))
                .andExpect(jsonPath("$.content[0].stockDisponible").value(7));
    }

    @Test
    void filtraPorGenero() throws Exception {
        mockMvc.perform(get("/vinilos").param("genero", "Bebop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("Blue Train"))
                .andExpect(jsonPath("$.content[0].stockDisponible").value(0));
    }

    @Test
    void buscaPorTextoEnTituloYEnArtista() throws Exception {
        // Por artista: "coltrane" aparece en A Love Supreme y Blue Train.
        mockMvc.perform(get("/vinilos").param("q", "coltrane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        // Por título: "love" solo en A Love Supreme.
        mockMvc.perform(get("/vinilos").param("q", "love"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titulo").value("A Love Supreme"));
    }

    @Test
    void paginaElListado() throws Exception {
        mockMvc.perform(get("/vinilos").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/vinilos").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void fichaDevuelveDetalleCompleto() throws Exception {
        mockMvc.perform(get("/vinilos/{id}", idLoveSupreme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(idLoveSupreme.toString()))
                .andExpect(jsonPath("$.titulo").value("A Love Supreme"))
                .andExpect(jsonPath("$.anio").value(1965))
                .andExpect(jsonPath("$.sello").value("Impulse!"))
                .andExpect(jsonPath("$.artistas[0]").value("John Coltrane"))
                .andExpect(jsonPath("$.generos[0]").value("Jazz"))
                .andExpect(jsonPath("$.stockDisponible").value(7));
    }

    @Test
    void fichaDeViniloInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/vinilos/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void portadaUrlViajaEnElDtoCuandoEstaSeteadaYEsNullCuandoNo() throws Exception {
        // Sin portada: el campo viaja como null.
        mockMvc.perform(get("/vinilos/{id}", idLoveSupreme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portadaUrl").isEmpty());

        // Al setear la portada, el DTO la expone.
        Vinilo loveSupreme = vinilos.findById(idLoveSupreme).orElseThrow();
        loveSupreme.setPortadaUrl("https://cdn.vivevinyls.test/love-supreme.jpg");
        vinilos.saveAndFlush(loveSupreme);

        mockMvc.perform(get("/vinilos/{id}", idLoveSupreme))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.portadaUrl")
                        .value("https://cdn.vivevinyls.test/love-supreme.jpg"));
    }

    private Vinilo guardarVinilo(String titulo, int anio, String precio,
            Sello sello, Artista artista, Genero genero) {
        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo(titulo);
        vinilo.setAnio(anio);
        vinilo.setPrecio(new BigDecimal(precio));
        vinilo.setSello(sello);
        vinilo = vinilos.saveAndFlush(vinilo);
        // Mantiene ambos lados de la relación para que la instancia gestionada
        // que el servicio relee en esta misma transacción tenga sus puentes; el
        // cascade ALL del vinilo persiste los puentes.
        vinilo.getArtistas().add(new ViniloArtista(vinilo, artista));
        vinilo.getGeneros().add(new ViniloGenero(vinilo, genero));
        return vinilos.saveAndFlush(vinilo);
    }

    private void movimiento(Vinilo vinilo, TipoMovimiento tipo, int cantidad) {
        MovimientoStock m = new MovimientoStock();
        m.setVinilo(vinilo);
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        movimientos.save(m);
    }
}
