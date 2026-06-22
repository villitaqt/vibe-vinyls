package com.vivevinyls.pedido;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.vivevinyls.cuenta.Cliente;
import com.vivevinyls.cuenta.Direccion;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * PEDIDO: compra concreta de un cliente.
 *
 * <p>Guarda una COPIA CONGELADA de la dirección de envío (RN-07): los campos
 * {@code envio*} son propios del pedido, no se leen de la libreta. La FK
 * {@link #direccionOrigen} es solo trazabilidad opcional y puede quedar nula
 * si la dirección de origen se borra después.</p>
 */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado = EstadoPedido.PENDIENTE_PAGO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    // --- Copia congelada de la dirección de envío (RN-07) ---

    /** Referencia de trazabilidad a la dirección de la libreta (opcional). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direccion_origen_id")
    private Direccion direccionOrigen;

    @Column(name = "envio_destinatario")
    private String envioDestinatario;

    @Column(name = "envio_calle")
    private String envioCalle;

    @Column(name = "envio_ciudad")
    private String envioCiudad;

    @Column(name = "envio_region")
    private String envioRegion;

    @Column(name = "envio_pais")
    private String envioPais;

    @Column(name = "envio_codigo_postal")
    private String envioCodigoPostal;

    @Column(name = "envio_telefono")
    private String envioTelefono;

    // --- Composición ---

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemPedido> items = new ArrayList<>();

    /**
     * Copia los datos de una dirección de la libreta a los campos congelados
     * del pedido (RN-07). No mantiene vínculo vivo con la libreta.
     */
    public void copiarDireccionEnvio(Direccion direccion) {
        this.direccionOrigen = direccion;
        this.envioDestinatario = direccion.getDestinatario();
        this.envioCalle = direccion.getCalle();
        this.envioCiudad = direccion.getCiudad();
        this.envioRegion = direccion.getRegion();
        this.envioPais = direccion.getPais();
        this.envioCodigoPostal = direccion.getCodigoPostal();
        this.envioTelefono = direccion.getTelefono();
    }
}
