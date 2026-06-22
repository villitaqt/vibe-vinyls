package com.vivevinyls.catalogo;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * VINILO: producto vendible y entidad bisagra del modelo.
 * Cada edición es un vinilo independiente (no se modelan variantes).
 *
 * <p>El stock NO es un atributo: se calcula sobre el ledger
 * {@link com.vivevinyls.inventario.MovimientoStock}.</p>
 */
@Entity
@Table(name = "vinilo")
@Getter
@Setter
@NoArgsConstructor
public class Vinilo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String titulo;

    /** Año de edición (eje de filtrado). */
    @Column(name = "anio")
    private Integer anio;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    /** Sello editor: uno por vinilo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sello_id", nullable = false)
    private Sello sello;

    /** Puente N:M con ARTISTA (compilaciones, colaboraciones). */
    @OneToMany(mappedBy = "vinilo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ViniloArtista> artistas = new HashSet<>();

    /** Puente N:M con GENERO. */
    @OneToMany(mappedBy = "vinilo", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ViniloGenero> generos = new HashSet<>();
}
