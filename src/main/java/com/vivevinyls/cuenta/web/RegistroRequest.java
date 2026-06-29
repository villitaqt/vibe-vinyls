package com.vivevinyls.cuenta.web;

/**
 * Datos de registro de un nuevo cliente (CU-01). No expone la entidad JPA.
 */
public record RegistroRequest(String email, String nombre, String password) {
}
