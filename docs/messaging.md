# JMS & Event-Driven Architecture Dokumentation

## Überblick

Jakarta Messaging (JMS) wird verwendet für **asynchrone, entkoppelte Kommunikation** zwischen Microservices. Dies demonstriert ein zentrales Konzept verteilter Systeme.

**Lernziel:** Event-Driven Architecture, Asynchrone Messaging, Message-Driven Beans, Entkopplung von Services

## JMS Infrastruktur

### Message Broker: Apache ActiveMQ

**Konfiguration (Docker):**
```yaml
activemq:
  image: apache/activemq:5.18.3
  ports:
    - "61616:61616"    # OpenWire Protocol (Java Client)
    - "8161:8161"      # Web Console
  environment:
    ACTIVEMQ_CONFIG_MAXINACTIVITYDURATION: "30000"
```

**Web Console:** http://localhost:8161/admin/

**Features:**
- Queues (Point-to-Point)
- Topics (Publish-Subscribe)
- Message Persistence
- Transaction Support
- Redelivery Policy

---

## Event-Destinations

### 1. Queue: `order.created`

**Typ:** Point-to-Point (Queue)
- Ein Message wird von einem Consumer konsumiert
- Message wird nicht erneut zugestellt
- Perfekt für Order-Verarbeitung

**Producer:** Order Service (CheckoutService)

**Consumer:** Shipping Service (OrderCreatedEventListener - MDB)

**Event-DTO:**
```java
public class OrderCreatedEvent implements Serializable {
    private UUID eventId;              // Eindeutige Event-ID
    private UUID orderId;              // Die erzeugte Bestellung
    private UUID userId;               // Benutzer
    private LocalDateTime createdAt;   // Ereigniszeit
    private List<OrderItemDto> items;  // Bestellte Items
    private BigDecimal totalPrice;     // Gesamtbetrag
}
```

**Beispiel-Nachricht (serialisiert):**
```
OrderCreatedEvent {
  eventId: a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6
  orderId: 550e8400-e29b-41d4-a716-446655440001
  userId: 550e8400-e29b-41d4-a716-446655440000
  createdAt: 2024-09-12T10:30:00Z
  items: [
    {productId: ..., productName: "Mineralwasser", quantity: 2, unitPrice: 1.49, totalPrice: 2.98},
    {productId: ..., productName: "Cola", quantity: 1, unitPrice: 2.49, totalPrice: 2.49}
  ]
  totalPrice: 5.47
}
```

---

### 2. Topic: `delivery.status.changed`

**Typ:** Publish-Subscribe (Topic)
- Nachricht wird an alle Subscriber gesendet
- Jeder Consumer erhält die Nachricht
- Perfekt für Status-Broadcasts

**Producer:** Shipping Service (ShipmentService nach Status-Update)

**Consumers:**
- Order Service (OrderStatusUpdater - aktualisiert Order-Status)
- WebSocket Broadcaster (sendet Live-Updates an Browser)

**Event-DTO:**
```java
public class DeliveryStatusChangedEvent implements Serializable {
    private UUID eventId;           // Eindeutige Event-ID
    private UUID orderId;           // Original-Bestellung
    private UUID shipmentId;        // Versand-ID
    private ShippingStatus status;  // Neuer Status
    private String trackingNumber;  // Trackingnummer
    private LocalDateTime timestamp; // Ereigniszeit
}
```

**Beispiel-Nachricht:**
```
DeliveryStatusChangedEvent {
  eventId: b2c3d4e5-f6g7-48h9-i0j1-k2l3m4n5o6p7
  orderId: 550e8400-e29b-41d4-a716-446655440001
  shipmentId: 660e8400-e29b-41d4-a716-446655440002
  status: SHIPPED
  trackingNumber: SHIP-1694520600000-1234
  timestamp: 2024-09-12T11:45:00Z
}
```

---

## Producer: OrderCreatedEvent Publishing

**Location:** Order Service, CheckoutService

```java
@Stateless
public class CheckoutService {
    
    @Resource(name = "jms/OrderConnectionFactory")
    private ConnectionFactory connectionFactory;
    
    @Resource(name = "jms/OrderCreatedQueue")
    private Queue orderCreatedQueue;
    
    @Inject
    private OrderRepository orderRepository;
    
    @Transactional(REQUIRED)
    public OrderDto createOrder(CreateOrderRequest request, UUID userId) {
        // 1. Validate & Create Order
        OrderEntity order = new OrderEntity(userId);
        // ... add items, validate stock, calculate total ...
        
        // 2. Persist Order
        order.setStatus(OrderStatus.CREATED);
        orderRepository.save(order);
        entityManager.flush();  // Ensure persisted before messaging
        
        // 3. Publish Event (nach TX-Commit!)
        OrderCreatedEvent event = new OrderCreatedEvent(
            UUID.randomUUID(),
            order.getId(),
            order.getUserId(),
            LocalDateTime.now(),
            order.getItems(),
            order.getTotalPrice()
        );
        
        publishOrderCreatedEvent(event);
        
        return orderMapper.toDto(order);
    }
    
    private void publishOrderCreatedEvent(OrderCreatedEvent event) {
        try (
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        ) {
            ObjectMessage message = session.createObjectMessage(event);
            MessageProducer producer = session.createProducer(orderCreatedQueue);
            
            // Set delivery mode
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.setTimeToLive(86400000);  // 24 hours
            
            producer.send(message);
            logger.info("OrderCreatedEvent published: {}", event.getOrderId());
            
        } catch (JMSException e) {
            logger.error("Failed to publish OrderCreatedEvent", e);
            throw new RuntimeException("JMS Publishing failed", e);
        }
    }
}
```

**Wichtig:** JMS-Send erfolgt **nach** TX-Commit (s. unten Saga-Pattern)

---

## Consumer: Message-Driven Beans (MDB)

### 1. OrderCreatedEventListener (Shipping Service)

**Location:** Shipping Service

```java
@MessageDriven(
    activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "order.created"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "AUTO_ACKNOWLEDGE"),
        @ActivationConfigProperty(propertyName = "maxSession", propertyValue = "10")
    }
)
public class OrderCreatedEventListener implements MessageListener {
    
    @Inject
    private ShippingService shippingService;
    
    private static final Logger logger = LoggerFactory.getLogger(OrderCreatedEventListener.class);
    
    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof ObjectMessage) {
                ObjectMessage objMessage = (ObjectMessage) message;
                OrderCreatedEvent event = (OrderCreatedEvent) objMessage.getObject();
                
                logger.info("Received OrderCreatedEvent: {}", event.getOrderId());
                
                // Create shipment
                ShipmentEntity shipment = shippingService.createShipmentForOrder(
                    event.getOrderId(),
                    event.getUserId()
                );
                
                logger.info("Shipment created: {} for Order: {}", shipment.getId(), event.getOrderId());
                
            } else {
                logger.warn("Invalid message type: {}", message.getClass());
            }
            
        } catch (JMSException e) {
            logger.error("Error processing OrderCreatedEvent", e);
            // Message will be redelivered (redelivery policy)
            throw new RuntimeException("Message processing failed", e);
        }
    }
}
```

**Lernziele:**
- `@MessageDriven` Annotation
- Activation Config (Destination, Acknowledgment Mode)
- `implements MessageListener`
- `onMessage(Message)` Callback

---

### 2. DeliveryStatusChangedListener (Order Service)

**Location:** Order Service

```java
@MessageDriven(
    activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "delivery.status.changed"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "AUTO_ACKNOWLEDGE"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "OrderServiceSubscriber"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "DeliveryStatusChanged")
    }
)
public class DeliveryStatusChangedListener implements MessageListener {
    
    @Inject
    private OrderService orderService;
    
    private static final Logger logger = LoggerFactory.getLogger(DeliveryStatusChangedListener.class);
    
    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof ObjectMessage) {
                ObjectMessage objMessage = (ObjectMessage) message;
                DeliveryStatusChangedEvent event = (DeliveryStatusChangedEvent) objMessage.getObject();
                
                logger.info("Received DeliveryStatusChangedEvent: {}", event.getOrderId());
                
                // Update Order Status based on Shipment Status
                OrderStatus newOrderStatus = mapShippingStatusToOrderStatus(event.getStatus());
                orderService.updateOrderStatus(event.getOrderId(), newOrderStatus);
                
                logger.info("Order status updated to: {} for Order: {}", 
                    newOrderStatus, event.getOrderId());
                
            }
            
        } catch (JMSException e) {
            logger.error("Error processing DeliveryStatusChangedEvent", e);
            throw new RuntimeException("Message processing failed", e);
        }
    }
    
    private OrderStatus mapShippingStatusToOrderStatus(ShippingStatus shippingStatus) {
        return switch (shippingStatus) {
            case PREPARING -> OrderStatus.CONFIRMED;
            case READY_FOR_SHIPMENT -> OrderStatus.SHIPPING;
            case SHIPPED -> OrderStatus.SHIPPED;
            case DELIVERED -> OrderStatus.SHIPPED;  // End state
            case CANCELLED -> OrderStatus.CANCELLED;
        };
    }
}
```

---

### 3. WebSocket Broadcaster (Order Service)

**Location:** Order Service, WebSocket Integration

```java
@MessageDriven(
    activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "jakarta.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "delivery.status.changed"),
        @ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "AUTO_ACKNOWLEDGE")
    }
)
public class WebSocketBroadcasterMDB implements MessageListener {
    
    @Inject
    private WebSocketBroadcaster broadcaster;
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketBroadcasterMDB.class);
    
    @Override
    public void onMessage(Message message) {
        try {
            if (message instanceof ObjectMessage) {
                ObjectMessage objMessage = (ObjectMessage) message;
                DeliveryStatusChangedEvent event = (DeliveryStatusChangedEvent) objMessage.getObject();
                
                logger.info("Broadcasting delivery status update to WebSocket clients");
                
                // Send to all connected clients for this order's user
                broadcaster.broadcastStatusUpdate(
                    event.getOrderId(),
                    event.getShipmentId(),
                    event.getStatus(),
                    event.getTimestamp()
                );
                
            }
        } catch (JMSException e) {
            logger.error("Error broadcasting status update", e);
        }
    }
}
```

---

## Producer: DeliveryStatusChangedEvent Publishing

**Location:** Shipping Service, ShippingService

```java
@Stateless
public class ShippingService {
    
    @Resource(name = "jms/DeliveryStatusChangedConnectionFactory")
    private ConnectionFactory connectionFactory;
    
    @Resource(name = "jms/DeliveryStatusChangedTopic")
    private Topic deliveryStatusChangedTopic;
    
    @Inject
    private ShipmentRepository shipmentRepository;
    
    @Transactional(REQUIRED)
    public void updateShipmentStatus(UUID shipmentId, ShippingStatus newStatus) {
        ShipmentEntity shipment = shipmentRepository.findById(shipmentId)
            .orElseThrow(() -> new ShipmentNotFoundException("Shipment not found: " + shipmentId));
        
        // Validate transition
        if (!shipment.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                "Cannot transition from " + shipment.getShippingStatus() + " to " + newStatus
            );
        }
        
        // Update shipment
        shipment.setShippingStatus(newStatus);
        shipmentRepository.update(shipment);
        entityManager.flush();
        
        // Publish event (nach TX)
        DeliveryStatusChangedEvent event = new DeliveryStatusChangedEvent(
            UUID.randomUUID(),
            shipment.getOrderId(),
            shipment.getId(),
            newStatus,
            shipment.getTrackingNumber(),
            LocalDateTime.now()
        );
        
        publishStatusChangedEvent(event);
    }
    
    private void publishStatusChangedEvent(DeliveryStatusChangedEvent event) {
        try (
            Connection connection = connectionFactory.createConnection();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE)
        ) {
            ObjectMessage message = session.createObjectMessage(event);
            MessageProducer producer = session.createProducer(deliveryStatusChangedTopic);
            
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            producer.send(message);
            
            logger.info("DeliveryStatusChangedEvent published: {} → {}", 
                event.getOrderId(), event.getStatus());
            
        } catch (JMSException e) {
            logger.error("Failed to publish DeliveryStatusChangedEvent", e);
            throw new RuntimeException("JMS Publishing failed", e);
        }
    }
}
```

---

## Transaktionsbetrachtung: Saga Pattern

**Problem:** Distributed Transactions über zwei Services hinweg sind komplex

```
Order Service (TX):        Shipping Service (TX):
├─ CREATE order                     X
├─ PERSIST order
└─ COMMIT TX
    ↓
    JMS SEND (außerhalb TX)
    ↓
                        ORDER RECEIVED
                        ├─ CREATE shipment
                        ├─ PERSIST shipment
                        └─ COMMIT TX
```

**Herausforderungen:**
1. Order persistiert, aber JMS-Send fehlschlag?
2. JMS-Message versendet, aber Shipping-Service Fehler?

**Lösung: Saga Pattern (Choreography-based)**

```
Order Service:
  1. Create & Persist Order (TX)
  2. Send OrderCreatedEvent
  3. Response OK to Client
  
  If OrderCreatedEvent Send fails:
    → Log error
    → Retry (Message Broker Redelivery)
    → Manual intervention (dead letter queue)

Shipping Service:
  1. Receive OrderCreatedEvent
  2. Create & Persist Shipment (TX)
  3. Send DeliveryStatusChangedEvent
  
  If Shipment Creation fails:
    → MDB Redelivery aktiviert
    → Nachricht wird später erneut verarbeitet
```

**Idempotenz (wichtig!):**

```java
@MessageDriven(...)
public class OrderCreatedEventListener implements MessageListener {
    
    @Override
    public void onMessage(Message message) {
        OrderCreatedEvent event = extractEvent(message);
        
        // Check if already processed
        if (shipmentRepository.existsByOrderId(event.getOrderId())) {
            logger.info("Shipment already exists for order: {}", event.getOrderId());
            return;  // Idempotent: Safe to reprocess
        }
        
        // Process
        shippingService.createShipmentForOrder(event.getOrderId(), event.getUserId());
    }
}
```

**Dokumentation in Code:**
```java
/**
 * Deterministic processing:
 * - Same eventId → Same shipment created (checked by orderId)
 * - No side-effects if reprocessed
 * - Message-Driven Bean automatically handles redelivery
 * 
 * This implements the Saga Choreography Pattern:
 * Order Creation Event → Trigger Shipment Creation Event → Trigger WebSocket Update
 * 
 * Lernziel: Distributed transactions, eventual consistency, saga pattern
 */
```

---

## Message Broker Konfiguration

**ActiveMQ Redelivery Policy (docker-compose.yml):**
```xml
<redeliveryPolicy>
  <redeliveryPolicy
    maximumRedeliveries="5"
    initialRedeliveryDelay="1000"
    redeliveryDelay="2000"
    backOffMultiplier="2.0"
    useExponentialBackOff="true"
  />
</redeliveryPolicy>
```

**Bedeutung:**
- Max 5 Versuche
- Erst 1s warten, dann 2s, 4s, 8s, 16s
- Nach 5 Fehler → Dead Letter Queue

---

## Testing JMS Integration

```java
@DisplayName("JMS Message Processing Tests")
class OrderCreatedEventListenerTest {
    
    @Mock
    private ShippingService shippingService;
    
    @InjectMocks
    private OrderCreatedEventListener listener;
    
    @Test
    @DisplayName("Should create shipment when OrderCreatedEvent received")
    void testOrderCreatedEventProcessing() throws JMSException {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(/* ... */);
        
        ObjectMessage message = mock(ObjectMessage.class);
        when(message.getObject()).thenReturn(event);
        
        // Act
        listener.onMessage(message);
        
        // Assert
        verify(shippingService).createShipmentForOrder(orderId, userId);
    }
    
    @Test
    @DisplayName("Should retry on error")
    void testErrorHandling() throws JMSException {
        // Arrange
        ObjectMessage message = mock(ObjectMessage.class);
        when(message.getObject()).thenThrow(new JMSException("Connection lost"));
        
        // Act & Assert
        assertThrows(RuntimeException.class, () -> listener.onMessage(message));
        // MDB framework would handle redelivery
    }
}
```

---

## Zusammenfassung: Warum JMS statt REST?

| Aspekt | JMS | REST |
|--------|-----|------|
| Entkopplung | ✅ Hoch (Broker-vermittelt) | ❌ Direkt (RPC-artig) |
| Asynchronität | ✅ Nativ | ⚠️ Mit Komplexität |
| Fehlertoleranz | ✅ Built-in Redelivery | ❌ Manuell zu implementieren |
| Ordering | ✅ Garant | ❌ HTTP ist ungeordnet |
| Latency | ⚠️ Höher (Queue) | ✅ Niedrig |
| Komplexität | ⚠️ Mehr Code | ✅ Einfacher |

**In dieser Anwendung:** JMS für Order → Shipping (kritisch, fehler-tolerant), REST für UI → Gateway (schnell, synchron)

---

## Lernziele dieser JMS-Integration

✅ Jakarta Messaging (JMS) Fundamentals
✅ Message Broker (ActiveMQ) Deployment
✅ Queues vs. Topics (Point-to-Point vs. Publish-Subscribe)
✅ Message-Driven Beans (@MessageDriven)
✅ Asynchrone Event-Driven Communication
✅ Entkopplung von Microservices
✅ Saga Pattern für verteilte Transaktionen
✅ Idempotente Message-Verarbeitung
✅ Error Handling und Redelivery
✅ Testing von asynchronem Code

