package com.vivevinyls.cuenta;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.cuenta.web.ClienteDTO;
import com.vivevinyls.cuenta.web.LoginRequest;
import com.vivevinyls.cuenta.web.LoginResponse;
import com.vivevinyls.cuenta.web.RegistroRequest;
import com.vivevinyls.cuenta.web.RegistroResponse;
import com.vivevinyls.cuenta.web.VerificacionRequest;

/**
 * Lógica de cuentas con auth local (CU-01, RF-02). Orquesta el agregado
 * {@link Cliente} y su {@link CredencialLocal} aislada.
 *
 * <p><b>Temporal hasta Cognito:</b> el flujo de estados (no verificada →
 * verificada/activa) se implementa de verdad, pero el código de verificación no
 * se envía por correo: se devuelve en el response del registro. Al migrar a
 * Cognito, esta clase y {@link CredencialLocal} desaparecen sin tocar
 * {@code Cliente}.</p>
 */
@Service
public class AuthService {

    private static final int PASSWORD_MIN = 8;

    private final ClienteRepository clientes;
    private final CredencialLocalRepository credenciales;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final SecureRandom random = new SecureRandom();

    public AuthService(ClienteRepository clientes, CredencialLocalRepository credenciales,
            PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.clientes = clientes;
        this.credenciales = credenciales;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    /**
     * Registra al cliente y crea su credencial en {@code PENDIENTE_VERIFICACION}
     * con un código generado. Si el correo ya existe, lo informa (flujo 1a).
     */
    @Transactional
    public RegistroResponse registro(RegistroRequest req) {
        String email = normalizarEmail(req.email());
        String nombre = requerir(req.nombre(), "nombre");
        validarPassword(req.password());

        clientes.findByEmail(email).ifPresent(c -> {
            throw new EmailYaRegistradoException(email);
        });

        Cliente cliente = new Cliente();
        cliente.setEmail(email);
        cliente.setNombre(nombre);
        cliente = clientes.save(cliente);

        CredencialLocal credencial = new CredencialLocal();
        credencial.setCliente(cliente);
        credencial.setPasswordHash(passwordEncoder.encode(req.password()));
        credencial.setEstado(EstadoCredencial.PENDIENTE_VERIFICACION);
        String codigo = generarCodigo();
        credencial.setCodigoVerificacion(codigo);
        credenciales.save(credencial);

        return new RegistroResponse(cliente.getId(), codigo);
    }

    /** Valida el código y activa la cuenta. Idempotente si ya estaba activa. */
    @Transactional
    public void verificar(VerificacionRequest req) {
        String email = normalizarEmail(req.email());
        CredencialLocal credencial = credenciales.findByCliente_Email(email)
                .orElseThrow(VerificacionInvalidaException::new);

        if (credencial.getEstado() == EstadoCredencial.ACTIVA) {
            return;
        }
        if (req.codigo() == null || !req.codigo().equals(credencial.getCodigoVerificacion())) {
            throw new VerificacionInvalidaException();
        }
        credencial.setEstado(EstadoCredencial.ACTIVA);
        credencial.setCodigoVerificacion(null);
    }

    /** Verifica contraseña y estado {@code ACTIVA}; emite un JWT (RF-02). */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest req) {
        String email = normalizarEmail(req.email());
        CredencialLocal credencial = credenciales.findByCliente_Email(email)
                .orElseThrow(CredencialesInvalidasException::new);

        if (req.password() == null
                || !passwordEncoder.matches(req.password(), credencial.getPasswordHash())) {
            throw new CredencialesInvalidasException();
        }
        if (credencial.getEstado() != EstadoCredencial.ACTIVA) {
            throw new CuentaNoVerificadaException();
        }

        Cliente cliente = credencial.getCliente();
        String token = tokenService.emitir(cliente);
        ClienteDTO datos = new ClienteDTO(cliente.getId(), cliente.getEmail(),
                cliente.getNombre(), cliente.getRol().name());
        return new LoginResponse(token, "Bearer", cliente.getId(),
                tokenService.getExpiracionSegundos(), datos);
    }

    private String generarCodigo() {
        return String.format("%06d", random.nextInt(1_000_000));
    }

    private String normalizarEmail(String email) {
        return requerir(email, "email").toLowerCase();
    }

    private void validarPassword(String password) {
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new DatosInvalidosException(
                    "La contraseña debe tener al menos " + PASSWORD_MIN + " caracteres");
        }
    }

    private String requerir(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new DatosInvalidosException("El campo '" + campo + "' es obligatorio");
        }
        return valor.trim();
    }
}
