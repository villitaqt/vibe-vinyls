package com.vivevinyls.cuenta;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.cuenta.web.DireccionRequest;
import com.vivevinyls.cuenta.web.DireccionResponse;

/**
 * Libreta de direcciones del cliente autenticado (RF-03). El {@code clienteId}
 * proviene del {@code sub} del JWT, no del cuerpo, de modo que un cliente solo
 * opera sobre su propia libreta.
 */
@Service
public class DireccionService {

    private final ClienteRepository clientes;
    private final DireccionRepository direcciones;

    public DireccionService(ClienteRepository clientes, DireccionRepository direcciones) {
        this.clientes = clientes;
        this.direcciones = direcciones;
    }

    @Transactional(readOnly = true)
    public List<DireccionResponse> listar(UUID clienteId) {
        return direcciones.findByClienteId(clienteId).stream()
                .map(this::aResponse)
                .toList();
    }

    @Transactional
    public DireccionResponse alta(UUID clienteId, DireccionRequest req) {
        Cliente cliente = clientes.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException(clienteId));

        Direccion direccion = new Direccion();
        direccion.setCliente(cliente);
        direccion.setDestinatario(requerir(req.destinatario(), "destinatario"));
        direccion.setCalle(requerir(req.calle(), "calle"));
        direccion.setCiudad(requerir(req.ciudad(), "ciudad"));
        direccion.setPais(requerir(req.pais(), "pais"));
        direccion.setRegion(req.region());
        direccion.setCodigoPostal(req.codigoPostal());
        direccion.setTelefono(req.telefono());

        return aResponse(direcciones.save(direccion));
    }

    private DireccionResponse aResponse(Direccion d) {
        return new DireccionResponse(d.getId(), d.getDestinatario(), d.getCalle(),
                d.getCiudad(), d.getRegion(), d.getPais(), d.getCodigoPostal(), d.getTelefono());
    }

    private String requerir(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }
}
