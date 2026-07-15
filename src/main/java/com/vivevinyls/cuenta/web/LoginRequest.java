package com.vivevinyls.cuenta.web;

/**
 * Credenciales de inicio de sesión (RF-02).
 */
public record LoginRequest(String email, String password) {
}
