package com.vivevinyls.inventario.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vivevinyls.inventario.InventarioService;

/**
 * Back-office de inventario (Frontend 3): registro de importaciones de stock.
 * Protegido para STAFF/ADMIN en {@link com.vivevinyls.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/admin/inventario")
public class AdminInventarioController {

    private final InventarioService inventario;

    public AdminInventarioController(InventarioService inventario) {
        this.inventario = inventario;
    }

    @PostMapping("/importacion")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportacionResponse importar(@RequestBody ImportacionRequest req) {
        int stockDisponible = inventario.registrarImportacion(req.viniloId(), req.cantidad());
        return new ImportacionResponse(req.viniloId(), req.cantidad(), stockDisponible);
    }
}
