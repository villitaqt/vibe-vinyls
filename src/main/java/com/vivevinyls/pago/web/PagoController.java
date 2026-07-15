package com.vivevinyls.pago.web;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.vivevinyls.pago.PagoService;

/**
 * Pago de un pedido (CU-04). Sección 8 del contrato: {@code POST
 * /pedidos/{id}/pago}. El cliente se identifica por el {@code sub} del JWT; solo
 * puede pagar sus propios pedidos (404 si es ajeno).
 */
@RestController
public class PagoController {

    private final PagoService pagos;

    public PagoController(PagoService pagos) {
        this.pagos = pagos;
    }

    @PostMapping("/pedidos/{id}/pago")
    public PagoResponse pagar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestBody(required = false) PagoRequest req) {
        return pagos.pagar(UUID.fromString(jwt.getSubject()), id, req);
    }
}
