package com.vivevinyls.pedido;

import java.util.List;

import com.vivevinyls.pedido.web.PedidoResponse;
import com.vivevinyls.pedido.web.PedidoResponse.DireccionEnvioResponse;
import com.vivevinyls.pedido.web.PedidoResponse.ItemResponse;

/**
 * Mapea la entidad {@link Pedido} a su DTO de respuesta. Debe invocarse dentro
 * de una transacción activa: recorre las asociaciones perezosas (ítems y su
 * vinilo). Expone únicamente la copia congelada del pedido, nunca lecturas
 * vivas del vinilo o de la libreta.
 */
final class PedidoResponseMapper {

    private PedidoResponseMapper() {
    }

    static PedidoResponse aResponse(Pedido pedido) {
        List<ItemResponse> items = pedido.getItems().stream()
                .map(PedidoResponseMapper::aItem)
                .toList();

        DireccionEnvioResponse direccion = new DireccionEnvioResponse(
                pedido.getEnvioDestinatario(),
                pedido.getEnvioCalle(),
                pedido.getEnvioCiudad(),
                pedido.getEnvioRegion(),
                pedido.getEnvioPais(),
                pedido.getEnvioCodigoPostal(),
                pedido.getEnvioTelefono());

        return new PedidoResponse(
                pedido.getId(),
                pedido.getEstado().name(),
                pedido.getTotal(),
                items,
                direccion,
                pedido.getFechaCreacion());
    }

    private static ItemResponse aItem(ItemPedido item) {
        return new ItemResponse(
                item.getVinilo().getId(),
                item.getVinilo().getTitulo(),
                item.getCantidad(),
                item.getPrecioUnitario(),
                CalculoPedido.subtotal(item.getPrecioUnitario(), item.getCantidad()));
    }
}
