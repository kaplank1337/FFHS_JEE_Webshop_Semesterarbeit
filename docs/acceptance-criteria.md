# Akzeptanzkriterien

Diese Dokumentation definiert messbare Akzeptanzkriterien für die Semesterarbeit. Jedes Kriterium ist im BDD-Format (Given-When-Then) formuliert und testbar.

---

## AC-01: Benutzer kann sich registrieren

**Ziel:** Ein neuer Benutzer kann ein Konto erstellen und erhält bestätigte Daten zurück.

**Given:**
- Es existiert noch kein Benutzer mit username "alice_test"
- Es existiert noch keine Registrierung mit email "alice@test.local"

**When:**
```http
POST /api/auth/register HTTP/1.1
Content-Type: application/json

{
  "username": "alice_test",
  "email": "alice@test.local",
  "password": "SecurePass123",
  "firstName": "Alice",
  "lastName": "Test"
}
```

**Then:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440001",
  "username": "alice_test",
  "email": "alice@test.local",
  "firstName": "Alice",
  "lastName": "Test",
  "role": "CUSTOMER",
  "createdAt": "2024-09-12T10:30:00Z"
}
```

**Akzeptanzkriterium erfüllt wenn:**
- ✅ Response Status 201 Created
- ✅ Benutzer in DB gespeichert (JPQL Query findet ihn)
- ✅ Passwort ist gehashed (PBKDF2), nicht im Klartext
- ✅ Role ist standardmäßig CUSTOMER
- ✅ Timestamps gesetzt

---

## AC-02: Registrierter Benutzer kann sich anmelden

**Ziel:** Login mit Username/Passwort liefert JWT Token.

**Given:**
- Benutzer "alice_test" existiert mit Passwort "SecurePass123"

**When:**
```http
POST /api/auth/login HTTP/1.1
Content-Type: application/json

{
  "username": "alice_test",
  "password": "SecurePass123"
}
```

**Then:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI6IjU1MGU4NDAwLWUyOWItNDFkNC1hNzE2LWhRNjY2NTU0NDAwMDEiLCJ1c2VybmFtZSI6ImFsaWNlX3Rlc3QiLCJyb2xlIjoiQ1VTVE9NRVIiLCJpYXQiOjE2OTQ1MjA2MDAsImV4cCI6MTY5NDUyNDIwMH0.SIGNATURE",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

**Akzeptanzkriterium erfüllt wenn:**
- ✅ Response Status 200 OK
- ✅ accessToken ist gültig JWT (3 Teile, separator ".")
- ✅ tokenType ist "Bearer"
- ✅ expiresIn ist 3600 (Sekunden)
- ✅ Token-Payload enthalté sub (userId), username, role

---

## AC-03: Request ohne Token auf geschützte API liefert 401

**Ziel:** Unauthentifizierte Requests werden abgewiesen.

**Given:**
- User existiert

**When:**
```http
GET /api/orders HTTP/1.1
(kein Authorization header)
```

**Then:**
```http
HTTP/1.1 401 Unauthorized
Content-Type: application/json

{
  "timestamp": "2024-09-12T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "No authorization header"
}
```

**Akzeptanzkriterium erfüllt wenn:**
- ✅ Response Status 401
- ✅ Error-Response Format standardisiert
- ✅ Keine Stacktrace
- ✅ Gateway blockiert unauthentifiziert

---

## AC-04: Benutzer kann Produkte anzeigen (öffentlich)

**Ziel:** Produktkatalog ist ohne Authentifizierung abrufbar.

**Given:**
- 5 Produkte existieren (Wasser, Cola, Saft, Bier, Kaffee)

**When:**
```http
GET /api/products HTTP/1.1
(kein Token erforderlich)
```

**Then:**
```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": "...",
    "name": "Mineralwasser 1.5L",
    "category": "WATER",
    "price": 1.49,
    "stockQuantity": 100,
    "active": true,
    "createdAt": "2024-01-01T12:00:00Z"
  },
  ...
]
```

**Akzeptanzkriterium erfüllt wenn:**
- ✅ Response Status 200 OK
- ✅ Mindestens 5 Produkte zurückgegeben
- ✅ Nur aktive Produkte (active = true)
- ✅ JPQL Query #2 oder #3 verwendet
- ✅ Produktdetails vollständig

---

## AC-05: Benutzer kann Bestellung erstellen

**Ziel:** Authentifizierter Benutzer kann Produkte in Warenkorb und Bestellung platzieren.

**Given:**
- Benutzer "alice_test" ist authentifiziert (Token vorhanden)
- Produkt "Mineralwasser" (ID: abc123) existiert mit Preis 1.49 und Bestand 100
- Produkt "Cola" (ID: def456) existiert mit Preis 2.49 und Bestand 50

**When:**
```http
POST /api/orders HTTP/1.1
Authorization: Bearer eyJhbGc...
Content-Type: application/json

{
  "items": [
    {
      "productId": "abc123",
      "quantity": 2
    },
    {
      "productId": "def456",
      "quantity": 1
    }
  ]
}
```

**Then:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440002",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "status": "CREATED",
  "totalPrice": 5.47,
  "items": [
    {
      "productId": "abc123",
      "productName": "Mineralwasser 1.5L",
      "quantity": 2,
      "unitPrice": 1.49,
      "totalPrice": 2.98
    },
    {
      "productId": "def456",
      "productName": "Cola 0.5L",
      "quantity": 1,
      "unitPrice": 2.49,
      "totalPrice": 2.49
    }
  ],
  "createdAt": "2024-09-12T10:30:00Z"
}
```

**Akzeptanzkriterium erfüllt wenn:**
- ✅ Response Status 201 Created
- ✅ Order in DB gespeichert (mit userId, status=CREATED)
- ✅ OrderItems korrekt verlinkt (@OneToMany)
- ✅ totalPrice serverseitig berechnet (nicht vom Client übernommen) = 1.49*2 + 2.49*1 = 5.47
- ✅ Produktpreise werden vom Server geladen (nicht vertraut Clientdaten)
- ✅ Validierung: Mindestens 1 Item erforderlich
- ✅ Bestand nicht negativ (Order ablehnen wenn Bestand < Menge)

---

## AC-06: Nach Bestellung wird OrderCreatedEvent publiziert

**Ziel:** Order Service publiziert Event nach erfolgreichem Checkout.

**Given:**
- Order mit 2 Items wurde erfolgreich erstellt (AC-05)

**When:**
- (OrderCreatedEvent wird implizit nach Bestellung publiziert)

**Then:**
- ✅ JMS Message an Queue `order.created` versendet
- ✅ Event-DTO enthält: eventId, orderId, userId, items, totalPrice, createdAt
- ✅ Message ist persistent (wird bei Broker-Fehler nicht verloren)
- ✅ Message ist serialisierbar (ObjectMessage)

**Verifikation (im Test):**
```java
// Mock JMS Producer
verify(jmsTemplate).send(
    argThat(msg -> msg instanceof ObjectMessage),
    any(OrderCreatedEvent.class)
);
```

---

## AC-07: Shipping Service erzeugt Shipment aus OrderCreatedEvent

**Ziel:** Shipping Service empfängt Order-Event und erstellt automatisch Shipment.

**Given:**
- Order wurde erstellt und OrderCreatedEvent wurde publiziert
- Shipping Service läuft und listet auf Queue `order.created`

**When:**
- Message-Driven Bean empfängt OrderCreatedEvent
- (Automatische Verarbeitung)

**Then:**
- ✅ Shipment-Entity wird erstellt mit:
  - orderId = von Event
  - userId = von Event
  - status = PREPARING
  - trackingNumber = generiert
- ✅ Shipment wird in shipping-Schema persistiert
- ✅ JPQL Query #6 findet Shipment nach orderId
- ✅ Keine Fehler in Logs

---

## AC-08: Versandstatusänderung erzeugt DeliveryStatusChangedEvent

**Ziel:** Bei Statusänderung am Shipment wird Event publiziert.

**Given:**
- Shipment existiert mit status=PREPARING

**When:**
- Shipping Service aktualisiert Status zu SHIPPED

**Then:**
- ✅ DeliveryStatusChangedEvent wird an Topic `delivery.status.changed` publiziert
- ✅ Event enthält: orderId, shipmentId, status, trackingNumber, timestamp
- ✅ Order Service empfängt Event und aktualisiert seine Order.status
- ✅ WebSocket Broadcaster empfängt Event und sendet Update an Clients

---

## AC-09: Statusänderungen können über WebSocket übertragen werden

**Ziel:** Client erhält Real-Time-Updates zum Versandstatus.

**Given:**
- Benutzer ist authentifiziert (Token vorhanden)
- Client ist über WebSocket `/ws/orders/{userId}?token=...` verbunden
- Order wurde erstellt

**When:**
- Shipping Service ändert Shipment-Status zu SHIPPED
- DeliveryStatusChangedEvent wird publiziert

**Then:**
```json
{
  "type": "DELIVERY_STATUS_CHANGED",
  "orderId": "550e8400-e29b-41d4-a716-446655440002",
  "shipmentId": "...",
  "status": "SHIPPED",
  "trackingNumber": "SHIP-...",
  "timestamp": "2024-09-12T11:45:00Z"
}
```

**Akzeptanzkriterium erfüllt wenn:**
- ✅ WebSocket Connection authentifiziert (Token validiert)
- ✅ Message an Client gesendet
- ✅ Nur relevante Clients erhalten Update (userId-Filterung)
- ✅ Session-Handling korrekt
- ✅ Keine Serialisierungsfehler

---

## AC-10: Admin kann Produkte verwalten

**Ziel:** ADMIN-Rolle kann Produkte erstellen, bearbeiten, löschen.

**Given:**
- Admin-Benutzer existiert (role=ADMIN)
- Admin-Benutzer ist authentifiziert (Token mit role=ADMIN)

**When:**
```http
POST /api/products HTTP/1.1
Authorization: Bearer <admin-token>
Content-Type: application/json

{
  "name": "Neue Limonade",
  "description": "Hausgemachte Limonade",
  "category": "SOFT_DRINKS",
  "price": 3.99,
  "stockQuantity": 50
}
```

**Then:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440010",
  "name": "Neue Limonade",
  "category": "SOFT_DRINKS",
  "price": 3.99,
  "stockQuantity": 50,
  "active": true
}
```

**Akzeptanzkriterium erfüllt wenn:**
- ✅ Response Status 201 Created
- ✅ Product wird in DB gespeichert
- ✅ Nur ADMIN kann POST /products (Customer erhält 403)
- ✅ PUT/DELETE /products nur für ADMIN
- ✅ Validation: price > 0, stockQuantity >= 0, name not blank

---

## Teststrategie: Abnahmetest-Implementierung

**Technologie:** JUnit 5 + Testcontainers + RestAssured

```java
@DisplayName("Acceptance Tests - Complete Workflow")
@Tag("acceptance")
class AcceptanceTest {
    
    private static final GenericContainer<?> postgres = new GenericContainer<>(...)
        .withExposedPorts(5432)
        .withEnv("POSTGRES_PASSWORD", "test123");
    
    @BeforeAll
    static void setupDatabase() {
        postgres.start();
    }
    
    @Test
    @DisplayName("AC-01 bis AC-10: Complete User Journey")
    void testCompleteUserJourney() {
        // AC-01: Register
        UserDto user = registerUser("alice_test", "alice@test.local", "SecurePass123");
        assertNotNull(user.id());
        
        // AC-02: Login
        TokenResponse token = loginUser("alice_test", "SecurePass123");
        assertNotNull(token.accessToken());
        
        // AC-03: Unauthorized access
        Response unauthorized = get("/api/orders")
            .then()
            .statusCode(401);
        
        // AC-04: View products
        List<ProductDto> products = getProducts();
        assertTrue(products.size() >= 5);
        
        // AC-05: Create order
        OrderDto order = createOrder(token.accessToken(), products);
        assertNotNull(order.id());
        
        // AC-06: OrderCreatedEvent published (verify in logs/JMS)
        // AC-07: Shipment created (wait + query)
        waitFor(() -> shipmentExists(order.id()));
        
        // AC-08: Status change triggers event
        // AC-09: WebSocket receives update
        // (Async test with WebSocket client)
        
        // AC-10: Admin product management
        // (Login as admin, create/update/delete product)
    }
}
```

---

## Durchführung der Abnahmetests

```bash
# Alle Tests laufen
mvn clean verify

# Nur Akzeptanztest
mvn clean verify -Dgroups=acceptance

# Mit Coverage-Report
mvn clean verify jacoco:report
open target/site/jacoco/index.html
```

---

## Lernziel

Diese Akzeptanzkriterien demonstrieren:
- ✅ Testgetriebene Entwicklung (TDD)
- ✅ Spezifikation vs. Implementierung
- ✅ End-to-End User Workflows
- ✅ Microservice-Interaktionen
- ✅ JMS Event Flow
- ✅ WebSocket Real-Time
- ✅ Security & Authorization
- ✅ Data Consistency

