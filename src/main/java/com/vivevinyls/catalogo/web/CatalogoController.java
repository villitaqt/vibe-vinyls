package com.vivevinyls.catalogo.web;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vivevinyls.catalogo.CatalogoService;

/**
 * API de lectura del catálogo (CU-02). Sección 8 del contrato:
 * {@code GET /vinilos} y {@code GET /vinilos/{id}}.
 */
@RestController
@RequestMapping("/vinilos")
public class CatalogoController {

    private final CatalogoService catalogo;

    public CatalogoController(CatalogoService catalogo) {
        this.catalogo = catalogo;
    }

    /**
     * Listado del catálogo con búsqueda por texto ({@code q}), filtros
     * opcionales (género, artista, año, sello) y paginación (Spring Data
     * resuelve {@code page}, {@code size}, {@code sort} desde el {@link Pageable}).
     */
    @GetMapping
    public Page<ViniloResumenDTO> listar(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String artista,
            @RequestParam(required = false) Integer anio,
            @RequestParam(required = false) String sello,
            Pageable pageable) {
        return catalogo.listar(q, genero, artista, anio, sello, pageable);
    }

    /** Ficha completa de un vinilo (RF-06). Responde 404 si no existe. */
    @GetMapping("/{id}")
    public ViniloDetalleDTO ficha(@PathVariable UUID id) {
        return catalogo.ficha(id);
    }
}
