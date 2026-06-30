package com.vivevinyls.catalogo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.catalogo.web.ViniloDetalleDTO;
import com.vivevinyls.catalogo.web.ViniloResumenDTO;
import com.vivevinyls.inventario.StockService;

/**
 * Lectura del catálogo (CU-02): listado con búsqueda/filtros/paginación y ficha.
 * Es solo lectura; el alta y edición de vinilos son tareas de staff fuera del MVP.
 */
@Service
@Transactional(readOnly = true)
public class CatalogoService {

    private final ViniloRepository vinilos;
    private final StockService stock;

    public CatalogoService(ViniloRepository vinilos, StockService stock) {
        this.vinilos = vinilos;
        this.stock = stock;
    }

    /**
     * Listado paginado del catálogo. Todos los criterios son opcionales: los que
     * vengan vacíos/nulos no restringen. El stock disponible de la página se
     * resuelve en una sola consulta al ledger (lote) para evitar N+1.
     */
    public Page<ViniloResumenDTO> listar(String q, String genero, String artista,
            Integer anio, String sello, Pageable pageable) {

        List<Specification<Vinilo>> criterios = new ArrayList<>();
        criterios.add(ViniloSpecifications.texto(q));
        criterios.add(ViniloSpecifications.genero(genero));
        criterios.add(ViniloSpecifications.artista(artista));
        criterios.add(ViniloSpecifications.anio(anio));
        criterios.add(ViniloSpecifications.sello(sello));

        Specification<Vinilo> spec = Specification.allOf(criterios);
        Page<Vinilo> pagina = vinilos.findAll(spec, pageable);

        List<UUID> ids = pagina.getContent().stream().map(Vinilo::getId).toList();
        Map<UUID, Integer> disponibles = stock.disponiblePorVinilos(ids);

        return pagina.map(v -> aResumen(v, disponibles.getOrDefault(v.getId(), 0)));
    }

    /** Ficha completa de un vinilo (RF-06). Lanza 404 si no existe. */
    public ViniloDetalleDTO ficha(UUID id) {
        Vinilo vinilo = vinilos.findById(id)
                .orElseThrow(() -> new ViniloNoEncontradoException(id));
        return aDetalle(vinilo, stock.disponible(id));
    }

    private ViniloResumenDTO aResumen(Vinilo v, int stockDisponible) {
        return new ViniloResumenDTO(
                v.getId(), v.getTitulo(), v.getAnio(), v.getPrecio(), v.getPortadaUrl(),
                v.getSello().getNombre(), nombresArtistas(v), stockDisponible);
    }

    private ViniloDetalleDTO aDetalle(Vinilo v, int stockDisponible) {
        List<String> generos = v.getGeneros().stream()
                .map(vg -> vg.getGenero().getNombre())
                .sorted()
                .toList();
        return new ViniloDetalleDTO(
                v.getId(), v.getTitulo(), v.getAnio(), v.getPrecio(), v.getPortadaUrl(),
                v.getSello().getNombre(), nombresArtistas(v), generos, stockDisponible);
    }

    private List<String> nombresArtistas(Vinilo v) {
        return v.getArtistas().stream()
                .map(va -> va.getArtista().getNombre())
                .sorted()
                .toList();
    }
}
