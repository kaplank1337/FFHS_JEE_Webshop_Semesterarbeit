package com.jee.shop.order.adapter.out.messaging;

import jakarta.enterprise.context.ApplicationScoped;
import com.jee.shop.order.application.event.OrderCreatedEvent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Publishes committed orders as JSON text messages to the order.created queue. */
@ApplicationScoped
public class OrderCreatedEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedEventPublisher.class);

    public void publishAfterCommit(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrderCreatedEvent event) {
        String brokerUrl = "tcp://" + System.getenv().getOrDefault("JMS_HOST", "localhost")
                + ":" + System.getenv().getOrDefault("JMS_PORT", "61616");

        try (Jsonb jsonb = JsonbBuilder.create()) {
            ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
            try (Connection connection = factory.createConnection();
                 Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)) {
                Queue queue = session.createQueue("order.created");
                MessageProducer producer = session.createProducer(queue);
                producer.setDeliveryMode(DeliveryMode.PERSISTENT);
                TextMessage message = session.createTextMessage(jsonb.toJson(event));
                message.setStringProperty("eventType", "OrderCreated");
                connection.start();
                producer.send(message);
                producer.close();
                logger.info("Published OrderCreated event {} to order.created", event.getEventId());
            }
        } catch (Exception e) {
            logger.error("Order {} was committed, but its OrderCreated event could not be published", event.getOrderId(), e);
        }
    }
}
