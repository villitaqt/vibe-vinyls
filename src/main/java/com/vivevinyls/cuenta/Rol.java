package com.vivevinyls.cuenta;

/**
 * Rol del usuario, expuesto en el claim {@code role} del JWT para que el
 * frontend enrute (cliente final vs. panel de administración).
 *
 * <ul>
 *   <li>{@code CLIENTE}: comprador (rol por defecto al registrarse).</li>
 *   <li>{@code STAFF}: operador interno (back-office, post-MVP).</li>
 *   <li>{@code ADMIN}: administrador (back-office, post-MVP).</li>
 * </ul>
 *
 * <p>En este MVP todo registro nace {@code CLIENTE}; no hay endpoint para
 * asignar roles (eso es back-office). Para un usuario de prueba con otro rol, ver
 * la nota en {@code PROGRESO.md}.</p>
 */
public enum Rol {
    CLIENTE,
    STAFF,
    ADMIN
}
