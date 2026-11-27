# JEE Drink Shop

Semesterarbeit für das Modul Jakarta EE: ein Getränkeshop mit React-UI und vier Jakarta-EE-Modulen. Die aktuelle Architektur, Laufzeitabläufe, Qualitätsziele und Risiken stehen in der [arc42-Dokumentation](docs/arc42.md). Der [UI-Schnittstellenvertrag](docs/ui-api.md) enthält REST-/WebSocket-Beispiele und die Seed-Produkte.

## Aufbau

- **API-Gateway:** REST-Einstieg, CORS und HTTP-Routing
- **User-Service:** Registrierung, Login, JWT und Benutzerkonto
- **Order-Service:** Produkte, Checkout und Bestellstatus
- **Shipping-Service:** JMS-Empfang, Lieferstatus und authentifizierter WebSocket
- **PostgreSQL:** Datenbank `webshop`, Schemas `app_user`, `orders`, `shipping`
- **ActiveMQ:** Queue `order.created`, Topic `delivery.status.changed`
- **UI:** React/Vite im Verzeichnis `webshop-ui`

Alle Backend-Module verwenden Ports und Adapter (`domain`, `application/port/in`, `application/service`, `application/port/out`, `adapter/in`, `adapter/out`). Die JPA-Annotationen liegen aktuell noch auf den Domain-Modellen.

## Lokal entwickeln und debuggen

Voraussetzungen: Docker Compose, Java 21+, Maven 3.9+, Node.js/npm und IntelliJ mit den vorhandenen GlassFish-Konfigurationen.

1. PostgreSQL und ActiveMQ starten:

   ```bash
   docker compose up -d postgres activemq
   docker compose ps
   ```

2. In IntelliJ die Run-Konfigurationen **User-Service**, **Order-Service**, **Shipping-Service** und **API-Gateway** einzeln mit **Debug** starten. Die Domains werden durch die IDE-Konfiguration gestartet.
3. UI in einem zweiten Terminal starten:

   ```bash
   cd webshop-ui
   npm install
   npm run dev
   ```

   UI: `http://localhost:3000`.

| Komponente | Adresse |
|---|---|
| API-Gateway | `http://localhost:8080/api-gateway-1.0.0/api` |
| User-Service (Debug) | `http://localhost:10080/user-service-1.0.0/users` |
| Order-Service (Debug) | `http://localhost:11080/order-service-1.0.0/api` |
| Shipping-WebSocket (Debug) | `ws://localhost:12080/shipping-service-1.0.0/ws/orders` |
| PostgreSQL | `localhost:5432` |
| ActiveMQ OpenWire | `localhost:61616` |
| ActiveMQ-Konsole | `http://localhost:8161` |

Browser-REST wird über das Gateway gesendet. Die direkten Service-Adressen sind für lokales Debugging und interne Kommunikation gedacht. Für den lokalen UI-Betrieb kann `webshop-ui/.env.local` die bereits vorhandenen Variablen `VITE_API_BASE_URL` und `VITE_ORDER_WS_URL` überschreiben.

Infrastruktur beenden:

```bash
docker compose stop postgres activemq
```

## Gesamten Stack mit Docker starten

Die Compose-Datei `docker-compose.prod.yml` baut UI, Gateway und Services. `.env.prod` wird nicht eingecheckt; die Vorlage ist `.env.prod.example`. Einmalig lokal anlegen und zwei eigene Werte setzen:

```bash
cp .env.prod.example .env.prod
```

Dann den Stack starten:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up --build -d
```

- UI: `http://localhost:8080`
- REST: `http://localhost:8080/api`
- WebSocket: `ws://localhost:8080/ws/orders`

Nur der Edge-Gateway veröffentlicht einen Host-Port (`8080`). Die anderen Services und die Datenbank sind über interne Compose-Netzwerke erreichbar.

```bash
# Status
docker compose --env-file .env.prod -f docker-compose.prod.yml ps

# Logs
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f

# Stack stoppen (Datenvolume bleibt bestehen)
docker compose --env-file .env.prod -f docker-compose.prod.yml down

# Stack samt Datenvolume entfernen
docker compose --env-file .env.prod -f docker-compose.prod.yml down -v
```

## REST-Beispiele

Für den vollständigen Docker-Stack ist die Basis-URL `http://localhost:8080/api`. Lokal lautet sie `http://localhost:8080/api-gateway-1.0.0/api`.

Benutzer registrieren:

```bash
curl -i -X POST http://localhost:8080/api/users/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo@example.com","email":"demo@example.com","password":"change-me-123","firstName":"Demo","lastName":"User"}'
```

Produkte abrufen:

```bash
curl http://localhost:8080/api/products
```

Für Login, geschützte Bestellungen, Löschen und WebSocket-Nachrichten siehe [UI-API-Dokumentation](docs/ui-api.md) sowie die ausführlichen Sequenzen in [arc42, Kapitel 6](docs/arc42.md#6-laufzeitsicht).

## Build und Validierung

```bash
# Alle Module bauen und Tests ausführen
mvn clean verify

# Module bauen, Tests überspringen
mvn -DskipTests package

# UI-Produktionsbuild
cd webshop-ui && npm run build
```

Derzeit existieren Unit-Tests insbesondere für Token-/Passwortlogik und User-Service. Die vollständigen JMS-, WebSocket- und Service-Integrationsabläufe brauchen zusätzliche automatisierte Tests. Ein Build mit `-DskipTests` bestätigt nur Kompilierung und Paketierung.

## Dokumentation

- [Programmierung – Codeaufbau, Technologien, Buildnachweis und Erkenntnisse](docs/programmierung/README.md)
- [Entwurf – Nachweis der Bewertungskriterien und Datenmodell](docs/entwurf/README.md)
- [Spezifikation – Vision, Anforderungen, Use Cases und SMART-Qualitätsziele](docs/spezifikation/README.md)
- [arc42 Architekturdokumentation](docs/arc42.md)
- [REST-/WebSocket-Vertrag, Beispiele und Produkte](docs/ui-api.md)
- [JPA/JPQL-Beispiele](docs/jpa-jpql.md)


### Produktionstart

## Images bauen und Container starten
```bash
docker-compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

## Status prüfen
```bash
docker-compose -f docker-compose.prod.yml --env-file .env.prod ps
```

## Container herunterfahren
```bash
docker-compose -f docker-compose.prod.yml --env-file .env.prod down
```
## Validierung

[Abnahmekriterien, Testverfahren und protokollierte Ergebnisse](docs/validierung/README.md)
