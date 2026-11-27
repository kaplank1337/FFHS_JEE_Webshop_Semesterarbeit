package com.jee.shop.shipping.adapter.out.scheduling;

import com.jee.shop.shipping.domain.model.ShippingStatus;
import com.jee.shop.shipping.application.port.in.ShippingUseCases;
import com.jee.shop.shipping.application.port.out.DeliverySchedulerPort;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Timeout;
import jakarta.ejb.Timer;
import jakarta.ejb.TimerConfig;
import jakarta.ejb.TimerService;
import jakarta.inject.Inject;

import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/** Schedules a short, staggered delivery state progression for demos. */
@Singleton
public class ShipmentDeliverySimulator implements DeliverySchedulerPort {

    private static final Logger logger = Logger.getLogger(ShipmentDeliverySimulator.class.getName());

    @Resource
    private TimerService timerService;

    @Inject
    private ShippingUseCases shippingService;

    @Override
    public long schedule(UUID shipmentId) {
        long totalSeconds = ThreadLocalRandom.current().nextLong(1, 11);
        scheduleTransition(shipmentId, ShippingStatus.DELIVERED, totalSeconds);

        logger.info(() -> "Scheduled demo delivery for shipment " + shipmentId + " in " + totalSeconds + " seconds");
        return totalSeconds;
    }

    @Timeout
    public void advanceShipment(Timer timer) {
        ShipmentTransition transition = (ShipmentTransition) timer.getInfo();
        try {
            shippingService.updateShipmentStatus(transition.shipmentId(), transition.status());
            logger.info(() -> "Advanced shipment " + transition.shipmentId() + " to " + transition.status());
        } catch (Exception e) {
            logger.severe("Could not advance shipment " + transition.shipmentId() + " to "
                    + transition.status() + ": " + e.getMessage());
        }
    }

    private void scheduleTransition(UUID shipmentId, ShippingStatus status, long delaySeconds) {
        Date expiry = new Date(System.currentTimeMillis() + delaySeconds * 1_000L);
        TimerConfig config = new TimerConfig(new ShipmentTransition(shipmentId, status), false);
        timerService.createSingleActionTimer(expiry, config);
    }

    private record ShipmentTransition(UUID shipmentId, ShippingStatus status) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
