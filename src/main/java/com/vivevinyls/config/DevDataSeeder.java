package com.vivevinyls.config;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vivevinyls.catalogo.Artista;
import com.vivevinyls.catalogo.ArtistaRepository;
import com.vivevinyls.catalogo.Genero;
import com.vivevinyls.catalogo.GeneroRepository;
import com.vivevinyls.catalogo.Sello;
import com.vivevinyls.catalogo.SelloRepository;
import com.vivevinyls.catalogo.Vinilo;
import com.vivevinyls.catalogo.ViniloArtista;
import com.vivevinyls.catalogo.ViniloGenero;
import com.vivevinyls.catalogo.ViniloRepository;
import com.vivevinyls.inventario.MovimientoStock;
import com.vivevinyls.inventario.MovimientoStockRepository;
import com.vivevinyls.inventario.TipoMovimiento;

// Agregar estos imports
import com.vivevinyls.cuenta.Cliente;
import com.vivevinyls.cuenta.ClienteRepository;
import com.vivevinyls.cuenta.CredencialLocal;
import com.vivevinyls.cuenta.CredencialLocalRepository;
import com.vivevinyls.cuenta.EstadoCredencial;
import com.vivevinyls.cuenta.Rol;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Seed de catálogo SOLO para el entorno de desarrollo, para poder ver el
 * frontend contra datos realistas. Inserta ~8 vinilos con sus dimensiones
 * (artistas, géneros, sello) y movimientos de stock de tipo IMPORTACION.
 *
 * <p><b>Nunca corre en test ni en producción:</b> está acotado al perfil
 * {@code dev} ({@link Profile}) y puede desactivarse con
 * {@code app.seed.enabled=false}. El seed de los tests vive en su propio
 * {@code @BeforeEach} y es independiente de éste.</p>
 *
 * <p><b>Idempotente:</b> si la tabla de vinilos ya tiene filas, no hace nada,
 * por lo que reiniciar la app no duplica datos.</p>
 */
@Component
@Profile("dev")
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final SelloRepository sellos;
    private final ArtistaRepository artistas;
    private final GeneroRepository generos;
    private final ViniloRepository vinilos;
    private final MovimientoStockRepository movimientos;
    private final ClienteRepository clientes;
    private final CredencialLocalRepository credenciales;
    private final PasswordEncoder passwordEncoder;

    public DevDataSeeder(SelloRepository sellos, ArtistaRepository artistas,
                         GeneroRepository generos, ViniloRepository vinilos,
                         MovimientoStockRepository movimientos,
                         ClienteRepository clientes,
                         CredencialLocalRepository credenciales,
                         PasswordEncoder passwordEncoder) {
        this.sellos = sellos;
        this.artistas = artistas;
        this.generos = generos;
        this.vinilos = vinilos;
        this.movimientos = movimientos;
        this.clientes = clientes;
        this.credenciales = credenciales;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (vinilos.count() > 0) {
            log.info("Seed dev: ya hay {} vinilos, no se inserta nada (idempotente).", vinilos.count());
            return;
        }
        log.info("Seed dev: catálogo vacío, insertando datos de ejemplo...");

        // Dimensiones reutilizadas entre vinilos (evita duplicar sellos/artistas/géneros).
        Sello impulse = sello("Impulse!");
        Sello columbia = sello("Columbia");
        Sello blueNote = sello("Blue Note");
        Sello harvest = sello("Harvest");
        Sello island = sello("Island");
        Sello defJam = sello("Def Jam");
        Sello verve = sello("Verve");

        Artista coltrane = artista("John Coltrane");
        Artista miles = artista("Miles Davis");
        Artista pinkFloyd = artista("Pink Floyd");
        Artista marley = artista("Bob Marley & The Wailers");
        Artista kendrick = artista("Kendrick Lamar");
        Artista evans = artista("Bill Evans");
        Artista hancock = artista("Herbie Hancock");
        Artista ella = artista("Ella Fitzgerald");
        Artista louis = artista("Louis Armstrong");

        Genero jazz = genero("Jazz");
        Genero bebop = genero("Bebop");
        Genero rock = genero("Rock");
        Genero reggae = genero("Reggae");
        Genero hipHop = genero("Hip-Hop");
        Genero fusion = genero("Fusion");

        // (título, año, precio, sello) -> stock importado.
        // Se varían cantidades; dos quedan con stock bajo (1-2) para probar ese caso.
        vinilo("A Love Supreme", 1965, "39.90", impulse, art(coltrane), gen(jazz), 12);
        vinilo("Kind of Blue", 1959, "34.00", columbia, art(miles), gen(jazz), 8);
        vinilo("Blue Train", 1957, "29.50", blueNote, art(coltrane), gen(bebop, jazz), 2);
        vinilo("The Dark Side of the Moon", 1973, "42.00", harvest, art(pinkFloyd), gen(rock), 20);
        vinilo("Legend", 1984, "27.90", island, art(marley), gen(reggae), 15);
        vinilo("To Pimp a Butterfly", 2015, "38.50", defJam, art(kendrick), gen(hipHop, jazz), 1);
        vinilo("Head Hunters", 1973, "33.00", columbia, art(hancock), gen(fusion, jazz), 6);

        // Compilación con varios artistas: ejercita el puente N:M vinilo–artista.
        vinilo("Ella and Louis", 1956, "31.00", verve,
                art(ella, louis, evans), gen(jazz), 5);

        log.info("Seed dev: insertados {} vinilos con sus movimientos de stock.", vinilos.count());

        // Usuario de prueba dev — solo si no existe ya (idempotente independiente del catálogo)
        if (credenciales.findByCliente_Email("dev@vivevinyls.com").isEmpty()) {
            Cliente cliente = new Cliente();
            cliente.setNombre("Dev ViveVinyls");
            cliente.setEmail("dev@vivevinyls.com");
            cliente.setRol(Rol.CLIENTE);
            clientes.save(cliente);

            CredencialLocal cred = new CredencialLocal();
            cred.setCliente(cliente);
            cred.setPasswordHash(passwordEncoder.encode("dev12345"));
            cred.setEstado(EstadoCredencial.ACTIVA); // ya verificada, sin pasar por el flujo
            cred.setCodigoVerificacion("000000");
            credenciales.save(cred);

            log.info("Seed dev: usuario de prueba creado — dev@vivevinyls.com / dev12345");
        }

        // Usuario de staff de prueba para el back-office (Frontend 3) — independiente del catálogo.
        if (credenciales.findByCliente_Email("staff@vivevinyls.com").isEmpty()) {
            Cliente staff = new Cliente();
            staff.setNombre("Staff ViveVinyls");
            staff.setEmail("staff@vivevinyls.com");
            staff.setRol(Rol.STAFF);
            clientes.save(staff);

            CredencialLocal credStaff = new CredencialLocal();
            credStaff.setCliente(staff);
            credStaff.setPasswordHash(passwordEncoder.encode("staff12345"));
            credStaff.setEstado(EstadoCredencial.ACTIVA);
            credStaff.setCodigoVerificacion("000000");
            credenciales.save(credStaff);

            log.info("Seed dev: usuario staff creado — staff@vivevinyls.com / staff12345");
        }

        // Usuario admin de prueba para el back-office (Frontend 3) — independiente del catálogo.
        if (credenciales.findByCliente_Email("admin@vivevinyls.com").isEmpty()) {
            Cliente admin = new Cliente();
            admin.setNombre("Admin ViveVinyls");
            admin.setEmail("admin@vivevinyls.com");
            admin.setRol(Rol.ADMIN);
            clientes.save(admin);

            CredencialLocal credAdmin = new CredencialLocal();
            credAdmin.setCliente(admin);
            credAdmin.setPasswordHash(passwordEncoder.encode("admin123"));
            credAdmin.setEstado(EstadoCredencial.ACTIVA);
            credAdmin.setCodigoVerificacion("000000");
            credenciales.save(credAdmin);

            log.info("Seed dev: usuario admin creado — admin@vivevinyls.com / admin123");
        }
    }

    private Sello sello(String nombre) {
        return sellos.save(new Sello(nombre));
    }

    private Artista artista(String nombre) {
        return artistas.save(new Artista(nombre));
    }

    private Genero genero(String nombre) {
        return generos.save(new Genero(nombre));
    }

    private Artista[] art(Artista... a) {
        return a;
    }

    private Genero[] gen(Genero... g) {
        return g;
    }

    private void vinilo(String titulo, int anio, String precio, Sello sello,
            Artista[] artistasDelVinilo, Genero[] generosDelVinilo, int importacion) {
        Vinilo vinilo = new Vinilo();
        vinilo.setTitulo(titulo);
        vinilo.setAnio(anio);
        vinilo.setPrecio(new BigDecimal(precio));
        vinilo.setSello(sello);
        vinilo = vinilos.saveAndFlush(vinilo);

        // Ambos lados de las relaciones N:M; el cascade ALL del vinilo persiste los puentes.
        for (Artista a : artistasDelVinilo) {
            vinilo.getArtistas().add(new ViniloArtista(vinilo, a));
        }
        for (Genero g : generosDelVinilo) {
            vinilo.getGeneros().add(new ViniloGenero(vinilo, g));
        }
        vinilo = vinilos.saveAndFlush(vinilo);

        // Stock inicial: movimiento IMPORTACION en positivo (ledger append-only).
        MovimientoStock m = new MovimientoStock();
        m.setVinilo(vinilo);
        m.setTipo(TipoMovimiento.IMPORTACION);
        m.setCantidad(importacion);
        movimientos.save(m);
    }
}
