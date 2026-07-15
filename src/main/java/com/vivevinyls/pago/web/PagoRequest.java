package com.vivevinyls.pago.web;

import com.vivevinyls.pago.ResultadoSimulado;

/**
 * Inicio de pago de un pedido (CU-04). <b>No</b> lleva datos de tarjeta (RN-04):
 * el cobro lo realiza la pasarela.
 *
 * <p>{@code resultadoSimulado} es opcional (por defecto {@code CAPTURA}) y es un
 * artefacto de la pasarela simulada para forzar captura/rechazo en demo y tests;
 * desaparece con la pasarela real.</p>
 */
public record PagoRequest(ResultadoSimulado resultadoSimulado) {
}
