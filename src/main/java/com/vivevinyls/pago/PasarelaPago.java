package com.vivevinyls.pago;

import java.math.BigDecimal;

/**
 * Pasarela de cobro tercerizada (CU-04). Abstrae al proveedor real tras una
 * interfaz para poder simularlo en el MVP y sustituirlo después sin tocar la
 * lógica de pago.
 *
 * <p><b>RN-04:</b> el sistema nunca recibe ni pasa datos de tarjeta; la pasarela
 * solo devuelve el estado del cobro y una {@link ResultadoCobro#referenciaExterna()
 * referencia} de la transacción.</p>
 */
public interface PasarelaPago {

    ResultadoCobro cobrar(BigDecimal monto, ResultadoSimulado resultadoSimulado);

    /** Resultado del cobro: si se capturó y la referencia de la transacción. */
    record ResultadoCobro(boolean capturado, String referenciaExterna) {
    }
}
