package com.vivevinyls.pedido.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vivevinyls.pedido.PedidoService;

/**
 * API de compra (CU-03, RF-13). Sección 8 del contrato: {@code POST /pedidos}
 * (checkout) y {@code GET /pedidos/{id}} (estado y detalle).
 *
 * <p>El cliente se identifica por el {@code sub} del JWT validado por el resource
 * server, nunca por un id del cuerpo: cada cliente solo opera sobre sus propios
 * pedidos. {@code /pedidos/**} exige autenticación (default de SecurityConfig).</p>
 */
@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final PedidoService pedidos;

    public PedidoController(PedidoService pedidos) {
        this.pedidos = pedidos;
    }

    /** Checkout: reserva stock atómicamente y crea el pedido en PENDIENTE_PAGO. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResponse crear(@AuthenticationPrincipal Jwt jwt,
            @RequestBody CrearPedidoRequest req) {
        return pedidos.checkout(clienteId(jwt), req);
    }

    // IMPORTANTE: este mapping debe estar ANTES de GET /{id} en el controlador
    // para que Spring no intente parsear "me" como UUID.
    /** Historial de pedidos del cliente autenticado, más reciente primero. */
    @GetMapping("/me")
    public List<PedidoResumenDTO> misPedidos(@AuthenticationPrincipal Jwt jwt) {
        return pedidos.pedidosDelCliente(clienteId(jwt));
    }

    /** Estado y detalle del pedido del cliente; 404 si no existe o es ajeno. */
    @GetMapping("/{id}")
    public PedidoResponse obtener(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return pedidos.obtener(clienteId(jwt), id);
    }

    private UUID clienteId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
