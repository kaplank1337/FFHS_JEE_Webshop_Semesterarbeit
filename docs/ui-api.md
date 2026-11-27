# REST-Schnittstelle für das UI

Diese Übersicht beschreibt die aktuell implementierten REST-Routen, Live-Statusmeldungen und die Seed-Getränke. Das API-Gateway ist der Browser-Einstiegspunkt für REST-Aufrufe und leitet den WebSocket transparent an den Shipping-Service weiter, der Statusupdates an angemeldete Benutzer sendet.

## Lokale URLs

| Anwendung | Basis-URL |
|---|---|
| API-Gateway | `http://localhost:8080/api-gateway-1.0.0/api` |
| User-Service direkt (nur intern/Debugging) | `http://localhost:10080/user-service-1.0.0/users` |
| Order-Service direkt (nur intern/Debugging) | `http://localhost:11080/order-service-1.0.0/api` |
| Lieferstatus-WebSocket | `ws://localhost:12080/shipping-service-1.0.0/ws/orders` |
| Shipping-Service direkt (nur intern/Debugging) | `http://localhost:12080/shipping-service-1.0.0/shipments` |

Die Services müssen in den zugehörigen GlassFish-Domains laufen. PostgreSQL und ActiveMQ müssen ebenfalls erreichbar sein. Das Gateway leitet Benutzer- und Bestellaufrufe sowie Produkt-Leseaufrufe an die Services weiter. Browser-Aufrufe sollen ausschliesslich die Gateway-URL verwenden.

Für die HTTP-Beispiele kann das UI diese lokale Basisvariable verwenden:

```text
baseUrl = http://localhost:8080/api-gateway-1.0.0/api
```

`accessToken` in den Beispielen ist der Wert aus dem `accessToken`-Feld der Login-Antwort.

## Anmeldung und Benutzer

Alle Beispiele gehen vom Gateway aus. JSON-Anfragen verwenden `Content-Type: application/json`.

### Benutzer registrieren

`POST /users/auth/register` — erwartet Username (3–50 Zeichen), gültige E-Mail, Passwort (mindestens 8 Zeichen), Vor- und Nachname. Bei Erfolg kommt `201 Created` mit einem Benutzerobjekt zurück.

```http
POST {{baseUrl}}/users/auth/register
Content-Type: application/json

{
  "username": "ui.demo@example.com",
  "email": "ui.demo@example.com",
  "password": "change-me-123",
  "firstName": "UI",
  "lastName": "Demo"
}
```

Antwort (Felder gekürzt):

```json
{
  "id": "<uuid>",
  "username": "ui.demo@example.com",
  "email": "ui.demo@example.com",
  "firstName": "UI",
  "lastName": "Demo",
  "role": "CUSTOMER",
  "createdAt": "<date-time>",
  "updatedAt": "<date-time>"
}
```

Ein bereits verwendeter Username oder eine E-Mail führt zu `409 Conflict`.

### Login

`POST /users/auth/login` — meldet den Benutzer mit Username und Passwort an. Die Antwort enthält das JWT.

```http
POST {{baseUrl}}/users/auth/login
Content-Type: application/json

{
  "username": "ui.demo@example.com",
  "password": "change-me-123"
}
```

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

Das UI sollte `accessToken` sicher im Arbeitsspeicher halten und für geschützte Anfragen den Header `Authorization: Bearer <accessToken>` senden. Bei ungültigen Zugangsdaten kommt `401 Unauthorized`.

### Eigenes Profil abrufen

`GET /users/me` — benötigt ein gültiges JWT.

```http
GET {{baseUrl}}/users/me
Authorization: Bearer {{accessToken}}
```

Antwort ist ein Benutzerobjekt wie bei der Registrierung.

### Eigenes Konto löschen

`DELETE /users/me` — benötigt das JWT des Kontoinhabers. Erfolg: `204 No Content`; das UI sollte den Token anschließend löschen und den Benutzer abmelden.

## Produkte

Der Order-Service implementiert `GET /products` und `GET /products/{id}` öffentlich. Für Browser-Aufrufe stellt das API-Gateway diese Lese-Endpunkte bereit; das Frontend soll den Order-Service nicht direkt ansprechen. Die Gateway-Basis-URL lautet:

`http://localhost:8080/api-gateway-1.0.0/api`

### Produktliste laden

```http
GET http://localhost:8080/api-gateway-1.0.0/api/products
Accept: application/json
```

Die Antwort ist eine Liste. Ein Produkt enthält `id`, `name`, `description`, `category`, `price`, `stockQuantity`, `active`, `createdAt` und `updatedAt`. Die Liste enthält nur aktive Produkte. CORS wird am API-Gateway behandelt; die Kommunikation zwischen Gateway und Order-Service läuft serverseitig.

### Vorhandene Seed-Getränke

Die Migration `V2__Insert_products.sql` legt diese sieben Produkte an. Die Werte sind Startbestand; Bestände und Preise können sich durch Laufzeitänderungen unterscheiden. `category` ist der Enum-Wert, den das JSON liefert.

Die Tabelle zeigt die Startwerte aus der Seed-Migration. Laufende Bestände können sich durch Bestellungen ändern.

| ID | Getränk | Kategorie | Preis | Startbestand |
|---|---|---|---:|---:|
| `550e8400-e29b-41d4-a716-446655440011` | Mineralwasser 1.5L | `WATER` | 1.49 | 100 |
| `550e8400-e29b-41d4-a716-446655440012` | Mineralwasser 0.5L | `WATER` | 0.79 | 150 |
| `550e8400-e29b-41d4-a716-446655440013` | Cola 0.5L | `SOFT_DRINKS` | 2.49 | 80 |
| `550e8400-e29b-41d4-a716-446655440014` | Orangensaft 1L | `JUICE` | 3.99 | 60 |
| `550e8400-e29b-41d4-a716-446655440015` | Eistee 0.5L | `SOFT_DRINKS` | 2.29 | 70 |
| `550e8400-e29b-41d4-a716-446655440016` | Lager Bier 0.5L | `BEER` | 1.99 | 120 |
| `550e8400-e29b-41d4-a716-446655440017` | Espresso Cold Brew | `COFFEE` | 3.49 | 50 |

Preise sind Dezimalwerte in der Projektwährung (aktuell ohne Währungsfeld im DTO). Kategorien: `WATER`, `SOFT_DRINKS`, `JUICE`, `BEER`, `COFFEE`.

### Produkt schreiben

`POST /products` erstellt ein Produkt, `PUT /products/{id}` ersetzt dessen Produktdaten, `DELETE /products/{id}` löscht es. JSON-Felder zum Schreiben sind `name`, `description`, `category`, `price`, `stockQuantity` und `active`. Das Gateway bietet derzeit nur die Lese-Endpunkte `GET /products` und `GET /products/{id}` an. Die Schreiboperationen sind nicht über das Gateway verfügbar.

```http
POST http://localhost:11080/order-service-1.0.0/api/products
Content-Type: application/json

{
  "name": "Ingwer-Limonade 0.33L",
  "description": "Limonade mit Ingwer",
  "category": "SOFT_DRINKS",
  "price": 2.50,
  "stockQuantity": 30,
  "active": true
}
```

Hinweis: Der Order-Service prüft für Produkt-Schreiboperationen aktuell keine Admin-Rolle. Diese Endpunkte sind daher keine sichere Admin-Funktion und sollten nicht extern freigegeben werden.

## Bestellungen

Bestellungen laufen über das Gateway und benötigen ein gültiges JWT. Das Backend ordnet die Bestellung dem Benutzer aus dem Token zu; eine `userId` gehört deshalb nicht in den Request.

### Bestellung erstellen

`POST /orders` — jede Position enthält eine Produkt-ID und positive Anzahl.

```http
POST {{baseUrl}}/orders
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "items": [
    {
      "productId": "550e8400-e29b-41d4-a716-446655440011",
      "quantity": 2
    },
    {
      "productId": "550e8400-e29b-41d4-a716-446655440013",
      "quantity": 1
    }
  ]
}
```

Erfolg: `201 Created` mit `id`, `userId`, `status`, `totalPrice`, `items`, `createdAt` und `updatedAt`. Eine Bestellposition enthält unter anderem Produktname, Menge, Einzel- und Positionspreis. Ein nicht existierendes Produkt oder zu kleiner Bestand wird als Fehler zurückgegeben.

### Eigene Bestellungen abrufen

```http
GET {{baseUrl}}/orders
Authorization: Bearer {{accessToken}}
```

Antwort: Liste der Bestellungen des angemeldeten Benutzers. Einzelne Bestellung: `GET {{baseUrl}}/orders/{orderId}`.

### Bestellstatus und Lieferung

Nach erfolgreichem `POST /orders` antwortet der Order-Service mit `201 Created` und speichert den Status `CREATED`. Erst nach dieser Backend-Antwort zeigt das UI die Bestellung als erstellt an. Danach wird die Bestellung über JMS an den Shipping-Service übergeben. Sobald die Lieferung beginnt, steht der Status auf `SHIPPED` (im UI „Wird geliefert“); nach einer zufälligen Wartezeit von 1 bis 10 Sekunden wechselt er auf `DELIVERED` („Geliefert“).

Die Bestellliste lädt beim Öffnen den gespeicherten Status über `GET /orders`. Während die Seite geöffnet ist, verbindet sich das UI mit dem WebSocket des Shipping-Service und sendet nach dem Verbindungsaufbau das JWT als erstes JSON-Frame:

```json
{"token":"<accessToken>"}
```

Nach erfolgreicher Authentifizierung erhält nur der angemeldete Bestellbesitzer Statusereignisse:

```json
{"type":"ORDER_STATUS_CHANGED","orderId":"<uuid>","status":"DELIVERED"}
```

In der Docker-Produktionskonfiguration nutzt der Browser ausschliesslich das Gateway: `ws://localhost:8080/ws/orders`. Das Gateway leitet den WebSocket intern an den Shipping-Service weiter. Die lokale IntelliJ-Konfiguration verbindet das UI direkt mit `ws://localhost:12080/shipping-service-1.0.0/ws/orders`; die URL kann über `VITE_ORDER_WS_URL` angepasst werden.

### Status ändern und Bestellung löschen

Die UI ändert Lieferstatus nicht manuell; die Statuswerte kommen aus dem JMS-Ablauf zwischen Order- und Shipping-Service. Die REST-Route für eine geschützte Statusänderung bleibt für interne/administrative Verwendung vorhanden:

```http
PUT {{baseUrl}}/orders/{orderId}/status
Authorization: Bearer {{accessToken}}
Content-Type: application/json

{
  "status": "CANCELLED"
}
```

Zulässige Statuswerte laut Enum: `CREATED`, `CONFIRMED`, `SHIPPING`, `SHIPPED`, `DELIVERED`, `CANCELLED`.

```http
DELETE {{baseUrl}}/orders/{orderId}
Authorization: Bearer {{accessToken}}
```

Erfolg beim Löschen: `204 No Content`. Lesen, Statusänderung und Löschen sind auf eigene Bestellungen beschränkt.

## Lieferungen

Die Shipping-REST-Routen verwenden den JAX-RS-Präfix `/shipments` und die Ressourcenroute `/shipments`. Lokal ist der direkte Pfad deshalb `http://localhost:12080/shipping-service-1.0.0/shipments/shipments`. Diese REST-Routen sind derzeit nicht über das API-Gateway geroutet. Sie verlangen ein Bearer-JWT und beschränken Lese- und Statusänderungen auf Lieferungen des angemeldeten Benutzers. Die UI verwendet für Live-Updates den WebSocket; der aktuelle Status kann über die Bestellabfrage gelesen werden.

## Health Checks

```http
GET {{baseUrl}}/health
GET {{baseUrl}}/health/ready
```

Antworten sind derzeit `{"status":"UP"}` beziehungsweise `{"status":"READY"}`.

## Gateway-Routen im Überblick

| Methode | Gateway-Pfad | Authentifizierung | Zweck |
|---|---|---|---|
| `GET` | `/health` | nein | Gateway-Prozessstatus |
| `GET` | `/health/ready` | nein | Gateway-Readiness |
| `POST` | `/users/auth/register` | nein | Registrierung |
| `POST` | `/users/auth/login` | nein | Login/JWT |
| `GET` | `/users/me` | Bearer JWT | Eigenes Profil |
| `DELETE` | `/users/me` | Bearer JWT | Eigenes Konto löschen |
| `GET` | `/orders` | Bearer JWT | Eigene Bestellungen |
| `POST` | `/orders` | Bearer JWT | Bestellung erstellen |
| `GET` | `/orders/{id}` | Bearer JWT | Eigene Bestellung lesen |
| `PUT` | `/orders/{id}/status` | Bearer JWT | Status der eigenen Bestellung ändern |
| `DELETE` | `/orders/{id}` | Bearer JWT | Eigene Bestellung löschen |

Das Gateway stellt Produkt-Lesezugriffe bereit. Shipping-REST ist nicht über das Gateway geroutet; die UI benötigt dafür aktuell keinen direkten REST-Aufruf, da Bestellstatus über `/orders` gelesen und Statusänderungen per WebSocket zugestellt werden.
