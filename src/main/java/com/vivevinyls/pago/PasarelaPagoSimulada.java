package com.vivevinyls.pago;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Implementación simulada de la {@link PasarelaPago} para el MVP: no hace ningún
 * cobro real ni maneja datos de tarjeta (RN-04). Genera una referencia de
 * transacción ficticia (UUID) y decide capturar o rechazar según
 * {@link ResultadoSimulado} (por defecto {@code CAPTURA}).
 *
 * <p>Devuelve una referencia incluso en el rechazo, como hacen las pasarelas
 * reales (cada intento deja rastro). Al integrar el proveedor real se reemplaza
 * esta clase sin tocar la lógica de pago.</p>
 */
@Component
public class PasarelaPagoSimulada implements PasarelaPago {

    @Override
    public ResultadoCobro cobrar(BigDecimal monto, ResultadoSimulado resultadoSimulado) {
        ResultadoSimulado decision = resultadoSimulado == null ? ResultadoSimulado.CAPTURA : resultadoSimulado;
        boolean capturado = decision == ResultadoSimulado.CAPTURA;
        String referencia = "SIMUL-" + UUID.randomUUID();
        return new ResultadoCobro(capturado, referencia);
    }
}
