package com.vivevinyls.cuenta;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * El correo ya tiene cuenta (flujo alternativo 1a de CU-01). Responde 409 para
 * que el cliente sepa que debe iniciar sesión en lugar de registrarse.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException(String email) {
        super("Ya existe una cuenta con el correo " + email);
    }
}
