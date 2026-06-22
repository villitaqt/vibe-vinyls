package com.vivevinyls.catalogo;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Clave compuesta del puente VINILO_GENERO.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ViniloGeneroId implements Serializable {

    private UUID viniloId;
    private UUID generoId;

    public ViniloGeneroId(UUID viniloId, UUID generoId) {
        this.viniloId = viniloId;
        this.generoId = generoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ViniloGeneroId that)) {
            return false;
        }
        return Objects.equals(viniloId, that.viniloId)
                && Objects.equals(generoId, that.generoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(viniloId, generoId);
    }
}
