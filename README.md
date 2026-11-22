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

- Docker und Docker Compose
- Java 25 und IntelliJ IDEA für die lokale Entwicklung
- Node.js und npm für das UI
- Maven 3.9+ zum manuellen Bauen der Services

Es gibt zwei Abläufe: lokal mit IntelliJ-Debugger und Docker nur für PostgreSQL
und ActiveMQ, oder vollständig in Docker mit nur dem API-Gateway als
veröffentlichtem Port.

### Umgebung 1: lokal entwickeln und debuggen

PostgreSQL und ActiveMQ laufen in Docker. Die vier Java-Services startest du
über die vorhandenen GlassFish-Konfigurationen in IntelliJ mit **Debug**.
Das UI läuft separat mit `npm run dev` auf Port `3000`.

Im Projektverzeichnis startest du zuerst die Infrastruktur:

```bash
docker compose up -d postgres activemq
docker compose ps
```

Prüfe, dass `postgres` und `activemq` den Status `running` oder `healthy` haben.
Starte dann in IntelliJ **API-Gateway**, **User-Service**, **Order-Service** und
**Shipping-Service** über ihre Debug-Konfigurationen.

Starte das UI in einem zweiten Terminal:

```bash
cd webshop-ui
npm install
cp .env.example .env.local
npm run dev
```

Öffne [http://localhost:3000](http://localhost:3000). Lokal verwendet das UI
das API-Gateway unter
`http://localhost:8080/api-gateway-1.0.0/api`; die Live-Lieferupdates kommen
per WebSocket vom lokal gestarteten Shipping-Service.

| Komponente | Lokale Adresse |
|---|---|
| UI | `http://localhost:3000` |
| API-Gateway (IntelliJ) | `http://localhost:8080/api-gateway-1.0.0/api` |
| User-Service (IntelliJ) | `http://localhost:10080/user-service-1.0.0/users` |
| Order-Service (IntelliJ) | `http://localhost:11080/order-service-1.0.0/api` |
| Shipping-Service WebSocket (IntelliJ) | `ws://localhost:12080/shipping-service-1.0.0/ws/orders` |
| PostgreSQL (Docker) | `localhost:5432` |
| ActiveMQ (Docker) | `localhost:61616` |
| ActiveMQ Web Console | `http://localhost:8161` |

Wenn du mit dem lokalen Debuggen fertig bist, stoppst du nur die beiden
Infrastruktur-Container:

```bash
docker compose stop postgres activemq
```

### Umgebung 2: alles mit Docker starten (`prod`)

Das Produktionsprofil baut UI und Services als Images und startet zusätzlich
PostgreSQL und ActiveMQ. Von aussen ist nur das API-Gateway auf Port `8080`
erreichbar. Die UI läuft intern auf Port `3000` und öffnet sich über das
Gateway unter [http://localhost:8080](http://localhost:8080). REST geht über den
Java-Gateway-Kern; WebSocket-Verbindungen werden intern an den Shipping-Service
weitergeleitet.

Die lokale Datei `.env.prod` enthält die Zugangsdaten für PostgreSQL und JWT.
Sie wird nicht ins Git-Repository eingecheckt. Starte den vollständigen Stack:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up --build -d
```

Status, Logs und Beenden:

```bash
# Container-Status
docker compose --env-file .env.prod -f docker-compose.prod.yml ps

# Logs aller Container verfolgen
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f

# Logs eines einzelnen Services, zum Beispiel Shipping-Service
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f shipping-service

# Stack stoppen (die Datenbankdaten im Volume bleiben erhalten)
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

Prüfe das Gateway und den Produkt-Endpunkt:

```bash
curl http://localhost:8080/health
curl http://localhost:8080/api/products
```

`docker-compose.yml` ist für die lokale Entwicklungsumgebung gedacht und
veröffentlicht zusätzliche Ports für IntelliJ und lokale Tools.

Falls `.env.prod` in einer frischen Kopie des Repositories noch fehlt, erstelle
sie mit `cp .env.prod.example .env.prod` und ersetze darin `DB_PASSWORD` sowie
`JWT_SECRET` durch eigene Werte.

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

Der Shipping Service stellt den authentifizierten WebSocket für Lieferupdates bereit.

**Docker-Produktionsendpoint:** `ws://localhost:8080/ws/orders`

**Authentifizierung:** Das UI sendet das JWT nach dem Öffnen als erste JSON-Nachricht.

**Example (JavaScript):**

```javascript
const token = "your-jwt-token";
const ws = new WebSocket("ws://localhost:8080/ws/orders");
ws.onopen = () => ws.send(JSON.stringify({ token }));

ws.onopen = () => {
  console.log("Connected to order status updates");
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log("Status Update:", message);
  // { "type": "ORDER_STATUS_CHANGED", "orderId": "...", "status": "DELIVERED" }
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
