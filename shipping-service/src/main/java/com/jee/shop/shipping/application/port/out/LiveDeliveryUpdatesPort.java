package com.jee.shop.shipping.application.port.out;

import java.util.UUID;

/** Sends live status updates to clients connected to the shipping WebSocket. */
public interface LiveDeliveryUpdatesPort {
    void publish(UUID userId, UUID orderId, String orderStatus);
}
