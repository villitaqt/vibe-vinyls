package com.vivevinyls.cuenta;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Emite los JWT de sesión (HS256) con Nimbus, vía el {@link JwtEncoder} que
 * configura la cadena de seguridad. Los claims se eligen para que la migración a
 * Cognito sea transparente para el resto del backend: {@code sub} = id del
 * cliente (el mismo lugar donde Cognito pone su {@code sub}), de modo que los
 * controladores que leen {@code jwt.getSubject()} no cambian.
 */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final long expiracionSegundos;

    public TokenService(JwtEncoder encoder,
            @Value("${app.jwt.issuer}") String issuer,
            @Value("${app.jwt.expiracion-segundos}") long expiracionSegundos) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.expiracionSegundos = expiracionSegundos;
    }

    /** Emite un token firmado para el cliente; devuelve el JWT compacto. */
    public String emitir(Cliente cliente) {
        Instant ahora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(ahora)
                .expiresAt(ahora.plusSeconds(expiracionSegundos))
                .subject(cliente.getId().toString())
                .claim("email", cliente.getEmail())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpiracionSegundos() {
        return expiracionSegundos;
    }
}
