package com.vivevinyls.cuenta;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CREDENCIAL_LOCAL: credencial de autenticación local (password + verificación
 * de correo) <b>aislada</b> del agregado {@link Cliente}.
 *
 * <p><b>Por qué vive aparte:</b> {@code Cliente} se diseñó sin contraseña a
 * propósito (RNF-03; la autenticación la asumirá Cognito vía
 * {@link Cliente#getCognitoSub() cognitoSub}). Esta entidad concentra toda la
 * deuda del auth local temporal para no contaminar el modelo de cuenta.</p>
 *
 * <p><b>Migración a Cognito (fase de IaC):</b> se elimina la tabla
 * {@code CREDENCIAL_LOCAL} completa y {@code Cliente.cognitoSub} toma el relevo;
 * {@code Cliente} no se toca. La relación se mapea desde este lado para que
 * {@code Cliente} permanezca intacto.</p>
 *
 * <p>Relación {@code @OneToOne} con {@code @MapsId}: la PK de esta tabla
 * <b>es</b> la FK al cliente (clave compartida), de modo que cada cliente tiene
 * a lo sumo una credencial local.</p>
 */
@Entity
@Table(name = "credencial_local")
@Getter
@Setter
@NoArgsConstructor
public class CredencialLocal {

    /** Igual al id del cliente (clave compartida vía {@link MapsId}). */
    @Id
    private UUID id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id")
    private Cliente cliente;

    /** Hash BCrypt de la contraseña; nunca se guarda en claro. */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCredencial estado;

    /**
     * Código de verificación de correo (6 dígitos). Mecanismo temporal: hoy se
     * devuelve en el response del registro (no se envía correo); con SES/Cognito
     * se reemplazará el medio de entrega sin cambiar el contrato de la API. Se
     * limpia al verificar.
     */
    @Column(name = "codigo_verificacion")
    private String codigoVerificacion;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;
}
