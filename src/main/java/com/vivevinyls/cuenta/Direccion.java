package com.vivevinyls.cuenta;

import java.util.UUID;

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
 * DIRECCION: entrada de la libreta de direcciones de un cliente.
 *
 * <p>Al comprar, sus datos se COPIAN congelados al pedido (RN-07); editar
 * la libreta no altera pedidos pasados.</p>
 */
@Entity
@Table(name = "direccion")
@Getter
@Setter
@NoArgsConstructor
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private String destinatario;

    @Column(nullable = false)
    private String calle;

    @Column(nullable = false)
    private String ciudad;

    /** Región / estado / departamento. */
    private String region;

    @Column(nullable = false)
    private String pais;

    @Column(name = "codigo_postal")
    private String codigoPostal;

    private String telefono;
}
