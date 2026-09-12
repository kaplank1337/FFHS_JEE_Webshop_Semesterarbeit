# Architektur-Dokumentation

## Überblick

Das JEE Drink Shop Backend folgt einer **Microservice-Architektur** mit vier isolierten Services, die über REST und asynchrone JMS-Events kommunizieren. Jeder Service besitzt seine eigene Persistierungs-Schicht mit separatem PostgreSQL-Schema.

## Schichtenarchitektur pro Service

```
┌─────────────────────────────────────────────────┐
│         REST Resources (@Path)                  │  JAX-RS Layer
├─────────────────────────────────────────────────┤
│    DTOs, Mappers, Exception Handling            │  API Layer
├─────────────────────────────────────────────────┤
│    Service Layer (CDI @ApplicationScoped)       │  Business Logic
│    EJB @Stateless für Transaktionen            │  
├─────────────────────────────────────────────────┤
│    Repository Layer (JPQL Queries)              │  Data Access
├─────────────────────────────────────────────────┤
│    JPA Entities (@Entity)                       │  ORM Mapping
├─────────────────────────────────────────────────┤
│    PostgreSQL (getrennte Schemas)               │  Datenbank
└─────────────────────────────────────────────────┘
```

## Microservice-Komponenten

### 1. API Gateway (Port 8080)

**Verantwortung:**
- Zentraler Einstiegspunkt für alle Client-Requests
- Token-Validierung auf geschützten Endpoints
- CORS-Konfiguration für React-Frontend
- Request-Routing zu Backend-Services
- Standardisierte Error-Responses

**Architektur:**
- REST Resources für Pass-through Requests
- JAX-RS Client (internal) für Service-Kommunikation
- Authentication Filter (@Provider)
- CORS Filter

**Lernziel:** API Gateway Pattern, Request Routing, Security Filtering

### 2. User Service (Port 8081, Schema: app_user)

**Verantwortung:**
- Benutzer-Registrierung & Authentifizierung
- JWT Token-Generierung & Validierung
- Passwort-Hashing (PBKDF2)
- Benutzerprofile & Rollen (CUSTOMER, ADMIN)

**Entities:**
- `UserEntity` (UUID id, username, email, passwordHash, firstName, lastName, role, timestamps)

**JPQL Queries:**
- **Query #1:** Benutzer nach Username suchen
  ```jpql
  SELECT u FROM UserEntity u WHERE u.username = :username
  ```

**Services:**
- `AuthService` (@ApplicationScoped CDI)
  - `register(RegisterRequest)` → UserDto
  - `login(LoginRequest)` → TokenResponse
  - `getUserFromToken(token)` → UserDto
- `TokenService` (@ApplicationScoped CDI)
  - JWT Token Generation & Validation

**REST Endpoints:**
- `POST /users/auth/register` — Registrierung
- `POST /users/auth/login` — Login
- `GET /users/me` — Aktuelles Profil

**Lernziele:** CDI, EJB, JWT, Security, Password Hashing, JPQL

### 3. Order Service (Port 8082, Schema: orders)

**Verantwortung:**
- Produktkatalog & Produktverwaltung
- Bestellungsverwaltung (CRUD)
- Checkout & Bestellvalidierung
- Bestands-Management
- Publizierung von OrderCreatedEvent (JMS)
- Konsumierung von DeliveryStatusChangedEvent (JMS)

**Entities:**
- `ProductEntity` (UUID id, name, category, price, stockQuantity, active, timestamps)
- `OrderEntity` (UUID id, userId, status, totalPrice, timestamps)
- `OrderItemEntity` (UUID id, productId, productName, quantity, unitPrice, totalPrice)

**JPQL Queries:**
- **Query #2:** Produkte nach Kategorie filtern (aktiv)
  ```jpql
  SELECT p FROM ProductEntity p 
  WHERE p.category = :category AND p.active = true 
  ORDER BY p.name ASC
  ```
- **Query #3:** Alle aktiven Produkte
  ```jpql
  SELECT p FROM ProductEntity p 
  WHERE p.active = true 
  ORDER BY p.createdAt DESC
  ```
- **Query #4:** Bestellungen eines Benutzers
  ```jpql
  SELECT o FROM OrderEntity o 
  WHERE o.userId = :userId 
  ORDER BY o.createdAt DESC
  ```
- **Query #5:** Bestellungen nach Status
  ```jpql
  SELECT o FROM OrderEntity o 
  WHERE o.status = :status
  ```

**Services:**
- `ProductService` (@Stateless EJB)
  - `findAll()`, `findById()`, `findByCategory()`
  - `createProduct()`, `updateProduct()`, `deleteProduct()`
- `OrderService` (@ApplicationScoped CDI)
  - `findUserOrders()`, `findOrderById()`
  - `updateOrderStatus()`
- `CheckoutService` (@Stateless EJB)
  - `createOrder(CreateOrderRequest, userId)` → OrderDto
  - Validierung: Mindestanzahl Items, Bestand-Check, Server-seitige Preis-Berechnung
  - **Transaktionale Schritte:**
    1. Produkt-Details laden & Preise validieren
    2. Bestand prüfen
    3. Order & OrderItems erzeugen
    4. Bestand aktualisieren
    5. Order persistieren
    6. OrderCreatedEvent publizieren

**JMS Integration:**
- **Producer:** OrderCreatedEventPublisher → Queue `order.created`
  - Event-DTO: `OrderCreatedEvent` (eventId, orderId, userId, items, createdAt)
- **Consumer:** DeliveryStatusChangedListener (MDB)
  - Topic: `delivery.status.changed`
  - Action: OrderStatus aktualisieren

**REST Endpoints:**
- `GET /products` — Produktliste
- `GET /products/{id}` — Produktdetails
- `POST /products` (ADMIN) — Produkt erstellen
- `PUT /products/{id}` (ADMIN) — Produkt aktualisieren
- `DELETE /products/{id}` (ADMIN) — Produkt löschen
- `POST /orders` — Bestellung erstellen (Checkout)
- `GET /orders` — Benutzers Bestellungen
- `GET /orders/{id}` — Bestelldetails

**Lernziele:** JPA @OneToMany/@ManyToOne, JPQL, JMS Producer/Consumer, MDB, Transaktionen, Bean Validation

### 4. Shipping Service (Port 8083, Schema: shipping)

**Verantwortung:**
- Shipment-Verwaltung (für Orders)
- Versand-Tracking & Status-Updates
- Automatische Shipment-Erstellung aus OrderCreatedEvent
- Publizierung von DeliveryStatusChangedEvent

**Entities:**
- `ShipmentEntity` (UUID id, orderId, userId, status, trackingNumber, timestamps)

**JPQL Queries:**
- **Query #6:** Shipment nach Order-ID suchen
  ```jpql
  SELECT s FROM ShipmentEntity s 
  WHERE s.orderId = :orderId
  ```

**Services:**
- `ShippingService` (@Stateless EJB)
  - `createShipment()`, `updateStatus()`
  - `findByOrderId()`, `findByUserId()`, `findByStatus()`
- `DeliveryStatusChangedEventPublisher` (CDI)
  - Publiziert Status-Changes zum Topic

**JMS Integration:**
- **Consumer:** OrderCreatedEventListener (MDB)
  - Queue: `order.created`
  - Action: Neuen Shipment erzeugen, Status = PREPARING
- **Producer:** DeliveryStatusChangedEventPublisher → Topic `delivery.status.changed`
  - Event-DTO: `DeliveryStatusChangedEvent`

**REST Endpoints:**
- `GET /shipments` — Benutzer-Shipments
- `GET /shipments/{id}` — Shipment-Details
- `PUT /shipments/{id}/status` (optional) — Status aktualisieren

**Lernziele:** JMS Message-Driven Beans, Event-Driven Architecture, Async Communication

## Persistierungs-Architektur

### PostgreSQL Schemas

```
Database: webshop
├── Schema: app_user       (User Service)
│   └── Table: "user"
│
├── Schema: orders         (Order Service)
│   ├── Table: product
│   ├── Table: "order"
│   └── Table: order_item
│
└── Schema: shipping       (Shipping Service)
    └── Table: shipment
```

**Wichtig:** Keine Foreign Keys über Schema-Grenzen. Microservices sind über UUID-Referenzen logisch gekoppelt, aber datenbank-technisch isoliert.

**Datenhoheit:**
- User Service: Besitzt user-Daten
- Order Service: Besitzt product, order, order_item
- Shipping Service: Besitzt shipment

### JPA Konfiguration

Jeder Service hat eigenes Persistence Unit mit eigenem DataSource:

```xml
<!-- User Service: persistence.xml -->
<persistence-unit name="userServicePU">
  <jta-data-source>jdbc/userDS</jta-data-source>
  <class>com.jee.shop.user.entity.UserEntity</class>
</persistence-unit>

<!-- Order Service: persistence.xml -->
<persistence-unit name="orderServicePU">
  <jta-data-source>jdbc/orderDS</jta-data-source>
  <class>com.jee.shop.order.entity.ProductEntity</class>
  <class>com.jee.shop.order.entity.OrderEntity</class>
  <class>com.jee.shop.order.entity.OrderItemEntity</class>
</persistence-unit>

<!-- Shipping Service: persistence.xml -->
<persistence-unit name="shippingServicePU">
  <jta-data-source>jdbc/shippingDS</jta-data-source>
  <class>com.jee.shop.shipping.entity.ShipmentEntity</class>
</persistence-unit>
```

## Kommunikationsmuster

### Synchron (REST)

```
Client
  → API Gateway
    → User Service (auth)
    → Order Service (products, orders)
    → Shipping Service (shipments)
      → PostgreSQL
```

**Use Cases:**
- Produktkatalog abrufen
- Bestellung abrufen
- Benutzer-Profil abrufen

### Asynchron (JMS)

```
Order Service
  → OrderCreatedEvent
    → JMS Queue: order.created
      → Shipping Service (MDB Listener)
        → Erzeugt Shipment
        → DeliveryStatusChangedEvent
          → JMS Topic: delivery.status.changed
            → Order Service (aktualisiert Order Status)
            → WebSocket Broadcaster (sendet an Client)
```

**Vorteile asynchrone Kommunikation:**
- Services sind entkoppelt
- Höhere Ausfalltoleranz
- Bessere Skalierbarkeit
- Eventuelle Konsistenz (eventual consistency)

## WebSocket Integration

**Endpoint:** `/ws/orders/{userId}`

**Authentifizierung:** Bearer Token aus Query-Param oder Header

**Message Format (Server → Client):**
```json
{
  "type": "DELIVERY_STATUS_CHANGED",
  "orderId": "...",
  "shipmentId": "...",
  "status": "SHIPPED",
  "trackingNumber": "SHIP-...",
  "timestamp": "2024-09-12T10:30:00Z"
}
```

**Flow:**
1. Client connected zu `/ws/orders/{userId}?token=...`
2. Server validiert Token
3. Server speichert Session in SessionManager
4. JMS DeliveryStatusChangedEvent wird empfangen
5. WebSocket Broadcaster filtert nach userId
6. Message wird an alle verbundenen Sessions gesendet

## Security-Architektur

```
Client Credentials
  ↓
POST /users/auth/login
  ↓
User Service: Password Verify (PBKDF2)
  ↓
TokenService: Generate JWT
  ↓
Response: { accessToken, tokenType: "Bearer", expiresIn }
  ↓
Client: Authorization: Bearer <token>
  ↓
API Gateway: AuthFilter validiert Token
  ↓
RequestScoped UserContext gesetzt
  ↓
Backend-Service kann Current User abrufen
```

**JWT Payload Beispiel:**
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "username": "alice",
  "role": "CUSTOMER",
  "iat": 1694520600,
  "exp": 1694524200
}
```

**Rollen:**
- CUSTOMER: Kann Produkte anzeigen, Bestellungen erstellen/abrufen, Shipments abrufen
- ADMIN: Zusätzlich: Produkte verwalten (POST/PUT/DELETE)

## Exception Handling

**Standardisierte Error-Response (alle Services):**
```json
{
  "timestamp": "2024-09-12T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation error details",
  "path": "/api/orders"
}
```

**ExceptionMappers (JAX-RS @Provider):**
- `AuthenticationExceptionMapper` → 401 Unauthorized
- `UserAlreadyExistsExceptionMapper` → 409 Conflict
- `ProductNotFoundExceptionMapper` → 404 Not Found
- `OrderValidationExceptionMapper` → 400 Bad Request
- `InsufficientStockExceptionMapper` → 409 Conflict
- (Global) `GeneralExceptionMapper` → 500 Internal Server Error (ohne Stack Trace)

## Transaktionen

**Container-Managed Transactions (CMT):**

Jeder @Stateless EJB hat automatisch CMT-Support. Methoden laufen in Transaktion:
- **REQUIRED** (default): Nutzt existierende TX oder startet neue
- **REQUIRES_NEW**: Startet immer neue TX
- **MANDATORY**: TX muss existieren
- **NEVER**: Keine TX erlaubt

**Wichtige Transaktions-Grenzen:**

1. **UserService.register()**
   ```
   BEGIN TX
     - Check username exists
     - Check email exists
     - Hash password
     - CREATE user
   COMMIT
   
   ROLLBACK bei Fehler → keine Daten halb-persistent
   ```

2. **CheckoutService.createOrder()**
   ```
   BEGIN TX
     - Load products
     - Validate prices (server-side)
     - Validate stock
     - CREATE order & items
     - UPDATE product stock
     - Calculate total
     - PERSIST order
   COMMIT
   
   ROLLBACK bei Fehler → Order, Items & Stock sind wieder konsistent
   
   NACH TX:
     - JMS Message send (außerhalb TX!)
   ```

3. **ShippingService bei OrderCreatedEvent**
   ```
   BEGIN TX (Message-Driven Bean)
     - Receive OrderCreatedEvent
     - CREATE shipment
     - Set status PREPARING
     - PERSIST
   COMMIT oder ROLLBACK
   
   Message Redelivery bei Fehler (ActiveMQ-Konfiguration)
   ```

**Herausforderung:** Transaktion und JMS

Problem: JMS-Message könnte nach Order-Persist aber vor TX-Commit versendet werden.

Lösung: **Outbox Pattern** (dokumentiert in `messaging.md`)
- Alternative: Saga-Pattern
- Akzeptabel für Hochschularbeit: einfache Dokumentation der Limitation

## Abhängigkeits-Struktur (Maven)

```
jee-drink-shop (Root POM)
├── api-gateway/
├── user-service/
├── order-service/
└── shipping-service/
```

**Build-Reihenfolge (Reactor):**
```bash
mvn clean verify  # Builds alle Modules in korrekter Reihenfolge

# Oder einzeln:
mvn clean package -pl user-service -am  # -am = also make (Abhängigkeiten)
mvn clean package -pl order-service -am
mvn clean package -pl shipping-service -am
mvn clean package -pl api-gateway -am
```

## Deployment

**Docker Compose Orchestration:**

Jeder Service als eigenständiger Container:
- Payara Micro Runtime
- WAR-File als Deployment-Artifact
- Environment-basierte Konfiguration
- Health Checks für Readiness/Liveness

**Service-Dependencies:**
```yaml
api-gateway:
  depends_on:
    - user-service (ready)
    - order-service (ready)
    - shipping-service (ready)

order-service:
  depends_on:
    - postgres (healthy)
    - activemq (healthy)
    - user-service (ready)

shipping-service:
  depends_on:
    - postgres (healthy)
    - activemq (healthy)
```

## Lernziele dieser Architektur

✅ Microservices: Separate Services, eigene Datenbanken, asynchrone Kommunikation
✅ REST: JAX-RS für synchrone API-Kommunikation
✅ JPA/JPQL: Datenbank-ORM mit 6+ JPQL Queries
✅ CDI: Dependency Injection, @ApplicationScoped, Lifecycle Management
✅ EJB: @Stateless für transaktionale Business-Services
✅ JMS: Asynchrone Event-Driven Communication, Message-Driven Beans
✅ WebSocket: Real-Time Updates vom Server zum Client
✅ Security: JWT Tokens, Password Hashing, Role-Based Access
✅ Bean Validation: @Valid auf DTOs, Custom Validators
✅ Exception Handling: Standardisierte Error-Responses via ExceptionMappers
✅ Transactions: CMT, TX-Boundaries, Saga-Pattern Documentation
✅ Docker: Multi-Stage Builds, Compose Orchestration, Health Checks

