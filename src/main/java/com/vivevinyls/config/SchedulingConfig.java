package com.vivevinyls.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Activa la planificación de tareas ({@code @Scheduled}) fuera del perfil de
 * test. Así el job de expiración de reservas corre periódicamente en dev/prod,
 * pero en los tests no dispara solo: el servicio se invoca manualmente para que
 * las pruebas sean deterministas.
 */
@Configuration
@EnableScheduling
@Profile("!test")
public class SchedulingConfig {
}
