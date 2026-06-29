package com.vivevinyls.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Seguridad del backend como <b>resource server</b>: valida en cada request
 * protegida un JWT HS256 firmado con un secreto simétrico (mismo secreto para
 * emitir y validar). Stateless: no hay sesión de servidor.
 *
 * <p><b>Migración a Cognito (fase de IaC):</b> esta clase es el único punto que
 * cambia. El decoder pasa de {@code SecretKeySpec} HS256 a la JWK Set URI de
 * Cognito (RS256) y se retira el {@link JwtEncoder} (Cognito emite los tokens);
 * la cadena de autorización, los controladores y los servicios no se tocan.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SecretKeySpec claveHs256;

    public SecurityConfig(@Value("${app.jwt.secret}") String secreto) {
        // HS256 exige una clave de >=256 bits; el secreto debe tener >=32 bytes.
        this.claveHs256 = new SecretKeySpec(
                secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // API sin estado consumida por SPA + clientes: CSRF no aplica y la
                // sesión es stateless (la identidad viaja en el JWT).
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Reutiliza las reglas CORS declaradas en CorsConfig (WebMvc).
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Auth y catálogo (CU-02) son públicos; también las sondas.
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/vinilos", "/vinilos/**").permitAll()
                        .requestMatchers("/health", "/actuator/**").permitAll()
                        // Todo lo demás (p. ej. /clientes/**) exige un JWT válido.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /** Valida el JWT entrante (resource server). */
    @Bean
    JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(claveHs256)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Firma el JWT que emite {@code TokenService} (mismo secreto simétrico). */
    @Bean
    JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(claveHs256));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
