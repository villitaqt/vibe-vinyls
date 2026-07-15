package com.vivevinyls.catalogo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.catalogo.web.GuardarViniloRequest;
import com.vivevinyls.catalogo.web.ViniloDetalleDTO;

/**
 * Alta y edición de vinilos desde el back-office (Frontend 3). Servicio
 * separado del {@link CatalogoService} de solo lectura porque escribe.
 *
 * <p><b>Simplificación MVP:</b> artistas y géneros llegan como texto libre
 * (nombres separados por coma en el frontend, ya divididos aquí en una
 * lista); se buscan o crean por nombre (find-or-create), igual que hace
 * {@code DevDataSeeder}. La gestión completa de esas entidades (renombrar,
 * fusionar, borrar) es back-office [+], fuera de este MVP.</p>
 */
@Service
@Transactional
public class AdminCatalogoService {

    private final ViniloRepository vinilos;
    private final SelloRepository sellos;
    private final ArtistaRepository artistas;
    private final GeneroRepository generos;
    private final CatalogoService catalogo;

    public AdminCatalogoService(ViniloRepository vinilos, SelloRepository sellos,
            ArtistaRepository artistas, GeneroRepository generos, CatalogoService catalogo) {
        this.vinilos = vinilos;
        this.sellos = sellos;
        this.artistas = artistas;
        this.generos = generos;
        this.catalogo = catalogo;
    }

    public ViniloDetalleDTO crear(GuardarViniloRequest req) {
        validar(req);
        Vinilo vinilo = new Vinilo();
        aplicar(vinilo, req);
        vinilo = vinilos.saveAndFlush(vinilo);
        return catalogo.ficha(vinilo.getId());
    }

    public ViniloDetalleDTO editar(UUID id, GuardarViniloRequest req) {
        validar(req);
        Vinilo vinilo = vinilos.findById(id).orElseThrow(() -> new ViniloNoEncontradoException(id));
        vinilo.getArtistas().clear();
        vinilo.getGeneros().clear();
        aplicar(vinilo, req);
        vinilos.saveAndFlush(vinilo);
        return catalogo.ficha(id);
    }

    private void validar(GuardarViniloRequest req) {
        if (req == null || req.titulo() == null || req.titulo().isBlank()) {
            throw new ViniloInvalidoException("El título es obligatorio");
        }
        if (req.anio() == null) {
            throw new ViniloInvalidoException("El año es obligatorio");
        }
        if (req.precio() == null || req.precio().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ViniloInvalidoException("El precio debe ser mayor que 0");
        }
        if (req.sello() == null || req.sello().isBlank()) {
            throw new ViniloInvalidoException("El sello es obligatorio");
        }
    }

    private void aplicar(Vinilo vinilo, GuardarViniloRequest req) {
        vinilo.setTitulo(req.titulo().trim());
        vinilo.setAnio(req.anio());
        vinilo.setPrecio(req.precio());
        vinilo.setPortadaUrl(blankToNull(req.portadaUrl()));
        vinilo.setSello(selloOrCrear(req.sello()));
        vinilo = vinilos.saveAndFlush(vinilo);

        for (String nombre : nombresValidos(req.artistas())) {
            vinilo.getArtistas().add(new ViniloArtista(vinilo, artistaOrCrear(nombre)));
        }
        for (String nombre : nombresValidos(req.generos())) {
            vinilo.getGeneros().add(new ViniloGenero(vinilo, generoOrCrear(nombre)));
        }
    }

    private List<String> nombresValidos(List<String> nombres) {
        return nombres == null ? List.of() : nombres.stream()
                .map(String::trim)
                .filter(n -> !n.isEmpty())
                .distinct()
                .toList();
    }

    private Sello selloOrCrear(String nombre) {
        return sellos.findByNombreIgnoreCase(nombre.trim())
                .orElseGet(() -> sellos.save(new Sello(nombre.trim())));
    }

    private Artista artistaOrCrear(String nombre) {
        return artistas.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> artistas.save(new Artista(nombre)));
    }

    private Genero generoOrCrear(String nombre) {
        return generos.findByNombreIgnoreCase(nombre)
                .orElseGet(() -> generos.save(new Genero(nombre)));
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
