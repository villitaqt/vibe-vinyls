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
 * Puente VINILO_ARTISTA: resuelve la relación N:M vinilo–artista.
 */
@Entity
@Table(name = "vinilo_artista")
@Getter
@Setter
@NoArgsConstructor
public class ViniloArtista {

    @EmbeddedId
    private ViniloArtistaId id = new ViniloArtistaId();

    @MapsId("viniloId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinilo_id", nullable = false)
    private Vinilo vinilo;

    @MapsId("artistaId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artista_id", nullable = false)
    private Artista artista;

    public ViniloArtista(Vinilo vinilo, Artista artista) {
        this.vinilo = vinilo;
        this.artista = artista;
        this.id = new ViniloArtistaId(vinilo.getId(), artista.getId());
    }
}
