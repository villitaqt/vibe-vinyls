package com.vivevinyls.pedido.web;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vivevinyls.pedido.EstadoPedido;
import com.vivevinyls.pedido.PedidoService;

/**
 * Back-office de pedidos (Frontend 3): listado por estado y confirmación del
 * despacho ({@code PAGADO → CONFIRMADO}). Protegido en {@link
 * com.vivevinyls.config.SecurityConfig} para {@code /admin/**} (solo
 * STAFF/ADMIN).
 */
@RestController
@RequestMapping("/admin/pedidos")
public class AdminPedidoController {

    private final PedidoService pedidos;

    public AdminPedidoController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    /** Lista pedidos, opcionalmente filtrados por estado; sin filtro devuelve todos. */
    @GetMapping
    public List<PedidoAdminDTO> listar(@RequestParam(required = false) EstadoPedido estado) {
        return pedidos.listarParaAdmin(estado);
    }

    /** Confirma el despacho de un pedido PAGADO. 409 si no está en ese estado. */
    @PostMapping("/{id}/confirmar")
    public PedidoResponse confirmar(@PathVariable UUID id) {
        return pedidos.confirmarDespacho(id);
    }
}
