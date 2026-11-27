package com.jee.shop.shipping.adapter.out.websocket;

import com.jee.shop.shipping.adapter.in.websocket.ShippingStatusWebSocketEndpoint;
import com.jee.shop.shipping.application.port.out.LiveDeliveryUpdatesPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/** Outbound adapter that sends application updates through the shipping WebSocket endpoint. */
@ApplicationScoped
public class WebSocketLiveDeliveryUpdatesAdapter implements LiveDeliveryUpdatesPort {
    @Override
    public void publish(UUID userId, UUID orderId, String orderStatus) {
        ShippingStatusWebSocketEndpoint.publishStatusChanged(userId, orderId, orderStatus);
    }
}
