package com.jee.shop.shipping.application.port.out;

import com.jee.shop.shipping.domain.model.ShipmentEntity;

/** Publishes delivery state changes to the JMS broker. */
public interface DeliveryStatusEventPort {
    void publish(ShipmentEntity shipment);
}
