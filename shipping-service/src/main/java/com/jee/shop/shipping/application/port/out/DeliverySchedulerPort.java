package com.jee.shop.shipping.application.port.out;

import java.util.UUID;

/** Schedules the simulated delivery completion. */
public interface DeliverySchedulerPort {
    long schedule(UUID shipmentId);
}
