package com.vivevinyls.inventario;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.vivevinyls.catalogo.Vinilo;
import com.vivevinyls.pedido.Pedido;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MOVIMIENTO_STOCK: registro append-only del inventario (ledger).
 *
 * <p>El stock es un CÁLCULO sobre estos movimientos, nunca un atributo del
 * vinilo. Siempre referencia a un VINILO; opcionalmente a un PEDIDO cuando el
 * movimiento se origina en una venta (reserva/confirmación/cancelación).</p>
 *
 * <p>Append-only: las filas no se modifican ni se borran; las correcciones se
 * registran como nuevos movimientos.</p>
 */
@Entity
@Table(name = "movimiento_stock")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinilo_id", nullable = false, updatable = false)
    private Vinilo vinilo;

    /** Pedido que origina el movimiento (solo si es una venta). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", updatable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, updatable = false)
    private TipoMovimiento tipo;

    /** Cantidad con signo (ver {@link TipoMovimiento}). */
    @Column(nullable = false, updatable = false)
    private int cantidad;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;
}
