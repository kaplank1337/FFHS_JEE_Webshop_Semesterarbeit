package com.jee.shop.order.adapter.out.messaging;

import com.jee.shop.order.application.event.OrderCreatedEvent;
import com.jee.shop.order.application.port.out.OrderCreatedEventPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/** Bridges the application event port to CDI; the JMS adapter publishes after commit. */
@ApplicationScoped
public class CdiOrderCreatedEventAdapter implements OrderCreatedEventPort {
    @Inject
    private Event<OrderCreatedEvent> events;

    @Override
    public void publish(OrderCreatedEvent event) {
        events.fire(event);
    }
}
