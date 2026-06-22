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
 * Dimensión SELLO: sello editor de un vinilo (uno por vinilo).
 */
@Entity
@Table(name = "sello")
@Getter
@Setter
@NoArgsConstructor
public class Sello {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    public Sello(String nombre) {
        this.nombre = nombre;
    }
}
