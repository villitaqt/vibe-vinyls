package com.vivevinyls.pedido.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vivevinyls.pedido.StockInsuficienteException;

/**
 * Traduce {@link StockInsuficienteException} a un 409 cuyo cuerpo identifica los
 * vinilos agotados (sección 4 del alcance). El resto de errores de dominio usan
 * el patrón {@code @ResponseStatus} y no necesitan handler.
 */
@RestControllerAdvice
public class PedidoExceptionHandler {

    @ExceptionHandler(StockInsuficienteException.class)
    public ResponseEntity<ErrorStock> sinStock(StockInsuficienteException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorStock(ex.getMessage(), ex.getAgotados()));
    }

    /** Cuerpo del 409: mensaje y lista de vinilos sin stock suficiente. */
    public record ErrorStock(String mensaje, List<UUID> agotados) {
    }
}
