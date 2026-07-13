package com.vivevinyls.catalogo.web;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vivevinyls.catalogo.AdminCatalogoService;

/**
 * Back-office de catálogo (Frontend 3): alta y edición de vinilos. El listado
 * reutiliza el {@code GET /vinilos} público (ya paginado); no hay un
 * {@code GET /admin/vinilos} separado. Protegido para STAFF/ADMIN en
 * {@link com.vivevinyls.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/admin/vinilos")
public class AdminCatalogoController {

    private final AdminCatalogoService admin;

    public AdminCatalogoController(AdminCatalogoService admin) {
        this.admin = admin;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ViniloDetalleDTO crear(@RequestBody GuardarViniloRequest req) {
        return admin.crear(req);
    }

    @PutMapping("/{id}")
    public ViniloDetalleDTO editar(@PathVariable UUID id, @RequestBody GuardarViniloRequest req) {
        return admin.editar(id, req);
    }
}
