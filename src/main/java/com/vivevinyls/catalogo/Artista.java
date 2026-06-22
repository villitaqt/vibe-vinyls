package com.vivevinyls.catalogo;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dimensión ARTISTA: eje de búsqueda y filtrado del catálogo.
 */
@Entity
@Table(name = "artista")
@Getter
@Setter
@NoArgsConstructor
public class Artista {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    public Artista(String nombre) {
        this.nombre = nombre;
    }
}
