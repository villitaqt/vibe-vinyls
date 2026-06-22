package com.vivevinyls.pedido;

import java.math.BigDecimal;
import java.util.UUID;

import com.vivevinyls.catalogo.Vinilo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * ITEM_PEDIDO: línea de un pedido.
 *
 * <p>Congela cantidad y {@code precioUnitario} al momento de la compra (RN-06):
 * cambios de precio posteriores en el vinilo no alteran este ítem.</p>
 */
@Entity
@Table(name = "item_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinilo_id", nullable = false)
    private Vinilo vinilo;

    @Column(nullable = false)
    private int cantidad;

    /** Precio unitario congelado al momento de la compra (RN-06). */
    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;
}
