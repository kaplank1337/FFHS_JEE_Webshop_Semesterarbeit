# JEE Drink Shop - Microservice Backend

Vollständiges Jakarta EE Microservice-Backend für einen webbasierten Getränkeshop. Diese Semesterarbeit demonstriert zentrale Jakarta-EE-Konzepte in einer realistischen, produktionsähnlichen Architektur.

## 🎯 Projektübersicht

**Technologie-Stack:**
- **Runtime:** Payara Micro 6 (Jakarta EE 11)
- **Language:** Java 25
- **Build:** Maven Multi-Module
- **Database:** PostgreSQL 16 (3 isolierte Schemas)
- **Messaging:** Apache ActiveMQ (JMS)
- **WebSocket:** Jakarta WebSocket
- **Container:** Docker & Docker Compose

**Microservices:**
1. **API Gateway** (Port 8080) — REST Entry Point, Token Validation, CORS
2. **User Service** (Port 8081) — Authentifizierung, Benutzerverwaltung, JWT
3. **Order Service** (Port 8082) — Produktkatalog, Bestellungen, Order Events
4. **Shipping Service** (Port 8083) — Versandverwaltung, Event Listener

## 🏗️ Architektur

```
┌─────────────────────────────────────────────────────────────┐
│                        React UI (Client)                    │
└────────────────────────────┬────────────────────────────────┘
                             │ HTTP + WebSocket
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                       API Gateway                            │
│  • Token Validation  • Routing  • CORS  • Error Handling   │
└────────┬─────────────────────────┬──────────────────┬───────┘
         │                         │                  │
    REST │                     REST │              REST│
         ▼                         ▼                  ▼
    ┌────────────┐         ┌────────────┐     ┌────────────┐
    │User Service│         │Order Service     │Shipping Svc│
    │            │         │                  │            │
    │ • Login    │         │ • Products       │ • Shipments│
    │ • Register │         │ • Orders         │ • Tracking │
    │ • JWT      │         │ • Cart/Checkout  │ • Status   │
    └────────────┘         └────────┬─────────┘ ◄──┘
         │                          │
         │ JPA               JMS Events (async)
         │                   ├─ order.created
         ▼                   └─ delivery.status.changed
    ┌──────────────────────────────────────┐
    │  PostgreSQL (webshop database)       │
    ├──────────────────────────────────────┤
    │ Schema: app_user   (User Service)    │
    │ Schema: orders     (Order Service)   │
    │ Schema: shipping   (Shipping Service)│
    └──────────────────────────────────────┘
         ▲
         │ JPA
         │
    ┌────────────────────────────────┐
    │  Apache ActiveMQ (JMS Broker)  │
    │  • Queues & Topics             │
    │  • Async Communication         │
    └────────────────────────────────┘
```

## 🚀 Quick Start

### Voraussetzungen

- Docker & Docker Compose
- Java 25 (für lokale Entwicklung)
- Maven 3.9+

### Start mit Service in Intellij und Postgre sowie ActiveMQ lokal
docker compose up -d postgres activemq

### Start mit Docker Compose

```bash
# Konfigurationsdatei kopieren
cp .env.example .env

# Services starten (builds automatisch)
docker compose up --build -d

# Logs folgen
docker compose logs -f

# Services stoppen
docker compose down
```

**Service-URLs nach dem Start:**
- **Webshop UI:** http://localhost:3000
- **API Gateway:** http://localhost:8080
- **User Service:** http://localhost:8081
- **Order Service:** http://localhost:8082
- **Shipping Service:** http://localhost:8083
- **ActiveMQ Web Console:** http://localhost:8161
- **Swagger UI (Order Service):** http://localhost:8082/swagger-ui.html

Das UI spricht in Docker Compose das API-Gateway unter `http://localhost:8080/api` an.
Für lokale Entwicklung mit dem in IntelliJ gestarteten GlassFish-Gateway:

```bash
cd webshop-ui
npm install
cp .env.example .env.local
npm run dev
```

Das UI läuft ebenfalls auf Port `3000` und verwendet dann den GlassFish-Kontext
`http://localhost:8080/api-gateway-1.0.0/api`.

### Health Check

```bash
# Alle Services prüfen
curl http://localhost:8080/health
curl http://localhost:8081/health
curl http://localhost:8082/health
curl http://localhost:8083/health
```

## 📡 API Beispiele

### 1. Benutzer registrieren

```bash
curl -X POST http://localhost:8080/users/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john.doe",
    "email": "john.doe@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### 2. Anmelden (Login)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "alice",
    "password": "secure123"
  }'
```

**Response:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 3. Produkte abrufen (öffentlich)

```bash
curl http://localhost:8080/api/products
```

### 4. Bestellung erstellen (geschützt)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "items": [
      {
        "productId": "550e8400-e29b-41d4-a716-446655440000",
        "quantity": 2
      }
    ]
  }'
```

### 5. Eigene Bestellungen abrufen (geschützt)

```bash
curl http://localhost:8080/api/orders \
  -H "Authorization: Bearer <token>"
```

### 6. Versandstatus abrufen (geschützt)

```bash
curl http://localhost:8080/api/shipments \
  -H "Authorization: Bearer <token>"
```

## 🔌 WebSocket Integration

Der Order Service stellt einen WebSocket Endpoint für Live-Updates bereit.

**Endpoint:** `ws://localhost:8082/ws/orders/{userId}`

**Authentifizierung:** Bearer Token im Query-Parameter oder Header

**Example (JavaScript):**

```javascript
const token = "your-jwt-token";
const userId = "550e8400-e29b-41d4-a716-446655440000";

const ws = new WebSocket(
  `ws://localhost:8082/ws/orders/${userId}?token=${token}`
);

ws.onopen = () => {
  console.log("Connected to order status updates");
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log("Status Update:", message);
  // {
  //   "type": "DELIVERY_STATUS_CHANGED",
  //   "orderId": "...",
  //   "shipmentId": "...",
  //   "status": "SHIPPED",
  //   "timestamp": "..."
  // }
};

ws.onerror = (error) => {
  console.error("WebSocket error:", error);
};

ws.onclose = () => {
  console.log("Disconnected from order status updates");
};
```

## 📊 Datenbank

**Initialisierung:**
Die PostgreSQL-Datenbank wird automatisch beim ersten Start initialisiert mit Flyway Migrations.

**Schemas:**
- `app_user` — User & Authentifizierung (User Service)
- `orders` — Produkte & Bestellungen (Order Service)
- `shipping` — Versand & Tracking (Shipping Service)

**Seed Data:**
Beim Start werden folgende Produkte und ein Admin-Benutzer initialisiert:
- Admin: `admin` / (gehashtes Passwort)
- Produkte: Mineralwasser, Cola, Orangensaft, Eistee, Bier, Kaffeegetränke

## 🔐 Security

**Authentifizierung:**
- Username + Passwort → JWT Bearer Token
- Token Expiration: 3600 Sekunden (konfigurierbar)
- Passwort-Hashing: PBKDF2

**Authorization:**
- Gateway validiert Token auf geschützten Endpunkten
- Rollen: CUSTOMER, ADMIN
- Admin-Endpunkte: POST/PUT/DELETE /api/products

**Environment Secrets:**
```bash
JWT_SECRET=your-secret-key-here
DB_PASSWORD=...
```

## 📬 JMS Event-Driven Architecture

**Destinations:**

1. **order.created (Queue)**
   - Producer: Order Service (nach erfolgreichem Checkout)
   - Consumer: Shipping Service (erzeugt Shipment)

2. **delivery.status.changed (Topic)**
   - Producer: Shipping Service (bei Status-Update)
   - Consumer: Order Service (aktualisiert Order Status)
   - Consumer: WebSocket Broadcaster (sendet Updates an Clients)

**Beispiel Event (OrderCreatedEvent):**
```json
{
  "eventId": "a1b2c3d4-e5f6-47g8-h9i0-j1k2l3m4n5o6",
  "orderId": "550e8400-e29b-41d4-a716-446655440001",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "createdAt": "2024-09-12T10:30:00Z",
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440002",
      "productName": "Mineralwasser 1.5L",
      "quantity": 2,
      "unitPrice": 1.49,
      "totalPrice": 2.98
    }
  ]
}
```

## 🧪 Tests

**Unit Tests:**
```bash
mvn test
```

**Integration Tests:**
```bash
mvn verify
```

**Test-Coverage:**
- User Authentication (Login, Register, Token Validation)
- Order Creation (Validation, Stock Check, Price Calculation)
- JMS Event Publishing & Consumption
- WebSocket Connection & Message Broadcasting
- API Endpoints (REST, Status Codes, Error Handling)

**Testcontainers:**
Integration Tests verwenden Testcontainers für PostgreSQL, keine externe DB-Installation nötig.

## 📚 Dokumentation

Detaillierte Dokumentation unter `docs/`:

- **architecture.md** — Architektur-Übersicht, Layer-Diagramme
- **database-model.md** — ER-Modell (Mermaid), Schema-Details
- **jpa-jpql.md** — 5+ JPQL Queries mit Kontext
- **rest-api.md** — REST API Spezifikation
- **messaging.md** — JMS Event Flows, Message Formats
- **websocket.md** — WebSocket Endpoint, Session Lifecycle
- **security.md** — JWT, Token Validation, Password Hashing
- **testing.md** — Test-Strategie, Coverage
- **acceptance-criteria.md** — AC-01 bis AC-10 (Given/When/Then)
- **decisions.md** — 10+ Architektur-Entscheidungen
- **lessons-learned.md** — Erkenntnisse aus der Implementierung
- **use-cases.md** — UC-01 bis UC-10
- **quality-requirements.md** — 6+ SMART Requirements

## 🏢 Build & Deployment

**Lokales Build:**
```bash
mvn clean verify
```

**Service einzeln bauen:**
```bash
mvn clean package -pl user-service -am
mvn clean package -pl order-service -am
mvn clean package -pl shipping-service -am
mvn clean package -pl api-gateway -am
```

**Docker Build:**
```bash
docker compose build --no-cache
```

**Alle Tests laufen:**
```bash
mvn clean verify
```

## 📋 Projekt-Struktur

```
jee-drink-shop/
├── pom.xml                    # Root Maven (Dependency Management)
├── docker-compose.yml         # Docker Infrastruktur
├── .env.example              # Konfiguration
│
├── user-service/             # User & Auth Service
│   ├── pom.xml
│   ├── src/main/java/com/jee/shop/user/
│   │   ├── entity/           # JPA Entities
│   │   ├── repository/       # JPQL Queries
│   │   ├── service/          # CDI/EJB Services
│   │   ├── resource/         # REST Resources
│   │   └── ...
│   └── src/test/java/
│
├── order-service/            # Order & Product Service
│   ├── pom.xml
│   ├── src/main/java/com/jee/shop/order/
│   │   ├── entity/
│   │   ├── repository/       # JPQL Queries
│   │   ├── service/
│   │   ├── resource/
│   │   ├── messaging/        # JMS Producer/Consumer
│   │   └── ...
│   └── src/test/java/
│
├── shipping-service/         # Shipping Service
│   ├── pom.xml
│   ├── src/main/java/com/jee/shop/shipping/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── resource/
│   │   ├── messaging/        # JMS Listener
│   │   └── ...
│   └── src/test/java/
│
├── api-gateway/              # API Gateway
│   ├── pom.xml
│   ├── src/main/java/com/jee/shop/gateway/
│   │   ├── resource/         # Pass-through Resources
│   │   ├── client/           # REST Clients
│   │   ├── security/         # Auth Filter
│   │   └── ...
│   └── src/test/java/
│
├── docker/
│   ├── Dockerfile.*          # Service Dockerfiles
│   └── postgres-init.sql     # DB Schema Init
│
└── docs/                     # Dokumentation
    ├── architecture.md
    ├── database-model.md
    ├── jpa-jpql.md
    ├── rest-api.md
    ├── messaging.md
    ├── websocket.md
    ├── security.md
    ├── testing.md
    ├── acceptance-criteria.md
    ├── decisions.md
    ├── lessons-learned.md
    ├── use-cases.md
    ├── quality-requirements.md
    ├── openapi/
    │   └── webshop-api.yaml
    └── diagrams/
```

## 🎓 Jakarta EE Lernziele

Diese Semesterarbeit demonstriert umfassend folgende Jakarta-EE-Konzepte:

✅ **Jakarta REST (JAX-RS)** — REST Endpoints, @Path, @GET, @POST, DTOs
✅ **Jakarta Persistence (JPA)** — Entities, Repositories, JPQL Queries
✅ **CDI** — Dependency Injection, @ApplicationScoped, @Inject
✅ **EJB** — @Stateless Services, Container-Managed Transactions
✅ **Bean Validation** — Constraint Annotations auf DTOs/Entities
✅ **Jakarta Messaging (JMS)** — Producer/Consumer, Message-Driven Beans
✅ **Jakarta WebSocket** — Real-Time Updates, Session Management
✅ **Security** — JWT Tokens, Filter-basierte Authentifizierung
✅ **Exception Handling** — JAX-RS ExceptionMappers
✅ **Docker** — Containerisierung, Multi-Stage Builds

## 📝 Lizenz

Diese Semesterarbeit ist Teil des Jakarta-EE-Moduls an der Hochschule.

## 👤 Autor

Kaan Kaplan - FFHS Hochschulmodul Jakarta Enterprise Edition

---

**Weitere Informationen:**
- Dokumentation: Siehe `docs/` Ordner
- Issues & Bugs: Über Git Issues
- Contribution: Nicht verfügbar (Semesterarbeit)



## Projekt starten im Debug!

API Gateway --> 'asadmin start-domain api-gateway-domain'
