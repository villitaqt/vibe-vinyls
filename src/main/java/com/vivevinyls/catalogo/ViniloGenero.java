package com.vivevinyls.catalogo;

import jakarta.persistence.Entity;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Puente VINILO_GENERO: resuelve la relación N:M vinilo–género.
 */
@Entity
@Table(name = "vinilo_genero")
@Getter
@Setter
@NoArgsConstructor
public class ViniloGenero {

    @EmbeddedId
    private ViniloGeneroId id = new ViniloGeneroId();

    @MapsId("viniloId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinilo_id", nullable = false)
    private Vinilo vinilo;

    @MapsId("generoId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "genero_id", nullable = false)
    private Genero genero;

    public ViniloGenero(Vinilo vinilo, Genero genero) {
        this.vinilo = vinilo;
        this.genero = genero;
        this.id = new ViniloGeneroId(vinilo.getId(), genero.getId());
    }
}
