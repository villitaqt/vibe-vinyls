package com.vivevinyls.cuenta;

/**
 * Ciclo de vida de la credencial local (auth temporal previo a Cognito):
 * recién registrada está {@code PENDIENTE_VERIFICACION}; tras validar el código
 * pasa a {@code ACTIVA} y solo entonces puede iniciar sesión (CU-01).
 */
public enum EstadoCredencial {
    PENDIENTE_VERIFICACION,
    ACTIVA
}
