package com.vivevinyls.catalogo;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clave compuesta del puente VINILO_ARTISTA.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ViniloArtistaId implements Serializable {

    private UUID viniloId;
    private UUID artistaId;

    public ViniloArtistaId(UUID viniloId, UUID artistaId) {
        this.viniloId = viniloId;
        this.artistaId = artistaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ViniloArtistaId that)) {
            return false;
        }
        return Objects.equals(viniloId, that.viniloId)
                && Objects.equals(artistaId, that.artistaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(viniloId, artistaId);
    }
}
