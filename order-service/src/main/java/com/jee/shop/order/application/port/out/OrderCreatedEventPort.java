package com.jee.shop.order.application.port.out;

import com.jee.shop.order.application.event.OrderCreatedEvent;

/** Publishes committed order creation events to the messaging adapter. */
public interface OrderCreatedEventPort {
    void publish(OrderCreatedEvent event);
}
