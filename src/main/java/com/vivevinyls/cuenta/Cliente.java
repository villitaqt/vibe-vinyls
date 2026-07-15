package com.vivevinyls.cuenta;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CLIENTE: persona registrada que compra.
 *
 * <p>La autenticación la gestiona Cognito (el backend solo valida el JWT),
 * por lo que aquí NO se almacenan credenciales ni contraseñas (RNF-03).</p>
 */
@Entity
@Table(name = "cliente")
@Getter
@Setter
@NoArgsConstructor
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String nombre;

    /**
     * Rol del usuario (default {@code CLIENTE}). Se expone en el claim
     * {@code role} del JWT. Asignar STAFF/ADMIN es tarea de back-office (post-MVP).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Rol rol = Rol.CLIENTE;

    /** Identificador del sujeto en Cognito (sub del JWT). */
    @Column(name = "cognito_sub", unique = true)
    private String cognitoSub;

    @CreationTimestamp
    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    /** Libreta de direcciones (1 cliente → N direcciones). */
    @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Direccion> direcciones = new ArrayList<>();
}
