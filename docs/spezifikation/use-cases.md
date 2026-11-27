# Use Cases und fachliche Anforderungen

## Akteure und Systemgrenze

| Akteur | Rolle |
|---|---|
| Kunde | Registriert sich, meldet sich an, betrachtet Produkte, erstellt und verwaltet eigene Bestellungen. |
| UI | Präsentiert Funktionen und sendet Browser-REST über das API-Gateway; verbindet sich für Lieferstatus mit dem WebSocket des Shipping-Service. |
| API-Gateway | Nimmt Browser-REST an, behandelt CORS und leitet Requests an User- oder Order-Service weiter. |
| User-Service | Verifiziert Zugangsdaten, erzeugt JWT und liefert Identität des Token-Subjekts. |
| Order-Service | Verwaltet Produktkatalog und Bestellungen; überprüft Benutzeridentität bei geschützten Funktionen. |
| Shipping-Service | Konsumiert Bestellereignisse, verwaltet simulierte Lieferungen und sendet authentifizierte Live-Statusereignisse. |

Die fachliche Grenze umfasst UI, Gateway und die drei Geschäftsdienste. PostgreSQL und ActiveMQ sind technische Nachbarsysteme. Die Services greifen nicht direkt auf die Tabellen anderer Service-Schemas zu.

## Funktionale Anforderungen

| ID | Anforderung | Priorität | Status im Code |
|---|---|---:|---|
| FR-01 | Das System muss neue Kunden mit Username, E-Mail, Passwort, Vor- und Nachname registrieren und doppelte Usernames/E-Mail-Adressen ablehnen. | Muss | Umgesetzt |
| FR-02 | Ein registrierter Kunde muss sich mit Username und Passwort anmelden und ein signiertes Bearer-JWT erhalten. | Muss | Umgesetzt |
| FR-03 | Geschützte Funktionen müssen die Identität aus einem gültigen Token beziehen; der Client darf keine fremde User-ID für eigene Daten vorgeben. | Muss | Umgesetzt |
| FR-04 | Der Kunde muss aktive Seed-Produkte mit Preis, Kategorie und verfügbarem Bestand abrufen können. | Muss | Umgesetzt; Lesen ist über Gateway verfügbar |
| FR-05 | Ein angemeldeter Kunde muss mit mindestens einer gültigen Produktposition eine Bestellung erstellen können. | Muss | Umgesetzt |
| FR-06 | Checkout muss Produkt, Menge und Bestand prüfen, Preise serverseitig berechnen, Bestand anpassen und die Bestellung mit `CREATED` speichern. | Muss | Umgesetzt |
| FR-07 | Das UI darf eine Bestellung erst dann als erstellt anzeigen, wenn das Backend die Speicherung erfolgreich bestätigt hat. | Muss | Umgesetzt im UI-Ablauf |
| FR-08 | Nach erfolgreicher Speicherung löst ein Bestellereignis die Lieferabwicklung asynchron aus. | Muss | Quellcode bestätigt: CDI-Ereignis mit `AFTER_SUCCESS`-Observer und JMS-Publisher |
| FR-09 | Der Shipping-Service muss die Lieferung persistieren, die Demo-Lieferung nach zufällig 1–10 Sekunden als zugestellt markieren und Statusänderungen publizieren. | Muss | Umgesetzt |
| FR-10 | Ein angemeldeter Kunde muss seine eigenen Bestellungen und den gespeicherten aktuellen Status abrufen können. | Muss | Umgesetzt |
| FR-11 | Während eine angemeldete Bestellansicht geöffnet ist, verarbeitet das UI Statusänderungen für eigene Bestellungen über WebSocket. | Muss | Quellcode bestätigt: UI-WebSocket-Handler und Shipping-Endpunkt |
| FR-12 | Die UI deaktiviert bei einer Bestellung mit `DELIVERED` die Löschaktion. | Soll | Quellcode bestätigt: `webshop-ui/src/routes/orders.tsx` |
| FR-13 | Ein Kunde kann sein eigenes Konto löschen; nach dem Löschen wird das Token-Subjekt vom User-Service nicht mehr aufgelöst. | Soll | Quellcode bestätigt: `user-service/.../AuthService.java` |
| FR-14 | Die vollständige Docker-Compose-Konfiguration veröffentlicht nur den Edge-Gateway als Host-Port. | Muss | Konfiguration bestätigt: in `docker-compose.prod.yml` veröffentlicht nur `api-gateway` den Port `8080:80` |

## Geschäftsregeln

- Benutzername und E-Mail sind eindeutig.
- Username muss 3–50 Zeichen umfassen; E-Mail muss gültig sein; Passwort mindestens 8 Zeichen; Vor- und Nachname jeweils 1–50 Zeichen.
- Eine Bestellung muss mindestens eine Position enthalten. Jede Menge muss positiv sein.
- Produktpreis und Bestandsprüfung werden im Backend vorgenommen. Der Client ist nicht massgebend für Preis oder User-ID.
- Eine Bestellung gehört genau dem Benutzer, dessen Identität das gültige Token ergibt.
- Lieferstatus wird durch den asynchronen Shipping-Ablauf bestimmt. Die Browseroberfläche soll ihn nicht unabhängig davon frei setzen.
- Statusbezeichnungen in der UI: `CREATED` und `CONFIRMED` → „Erstellt“; `SHIPPING` und `SHIPPED` → „Wird geliefert“; `DELIVERED` → „Geliefert“; `CANCELLED` → „Storniert“.
- `DELIVERED` und `CANCELLED` sind terminale Lieferzustände. Die UI deaktiviert für den Status `DELIVERED` die sichtbare Löschaktion (FR-12).

## Use Cases

### UC-01 – Kundenkonto registrieren

**Primärakteur:** Kunde  
**Vorbedingung:** Kunde ist nicht mit demselben Username bzw. derselben E-Mail registriert.  
**Auslöser:** Kunde sendet das Registrierungsformular.

**Hauptablauf:**

1. Kunde gibt Username, E-Mail, Passwort, Vor- und Nachname ein.
2. UI sendet `POST /users/auth/register` an das Gateway.
3. Gateway leitet an den User-Service weiter.
4. User-Service validiert die Eingaben und prüft die Eindeutigkeit.
5. User-Service speichert den Benutzer mit Standardrolle `CUSTOMER` und gehashtem Passwort.
6. User-Service antwortet `201 Created`; die UI kann den Benutzer zum Login weiterführen.

**Alternativen/Fehler:** ungültige Eingabe → `400`; Username/E-Mail bereits vergeben → `409`; Service nicht erreichbar → UI zeigt Fehler, ohne Erfolg zu behaupten.

**Nachbedingung:** Ein Benutzerkonto ist dauerhaft gespeichert; das Klartextpasswort wird nicht zurückgegeben.

### UC-02 – Anmelden und Token erhalten

**Primärakteur:** registrierter Kunde  
**Vorbedingung:** Konto existiert.  
**Hauptablauf:**

1. Kunde sendet Username und Passwort.
2. UI ruft `POST /users/auth/login` über das Gateway auf.
3. User-Service prüft Passwort und erzeugt ein JWT.
4. UI speichert das Token für die Sitzung und ruft geschützte Ressourcen mit `Authorization: Bearer <JWT>` auf.

**Alternativen/Fehler:** unbekannter Benutzer oder falsches Passwort → `401`; fehlende Pflichtfelder → Validierungsfehler.

### UC-03 – Aktive Getränke ansehen

**Primärakteur:** Besucher oder Kunde  
**Vorbedingung:** Order-Service und Datenbank sind verfügbar.  
**Hauptablauf:** UI ruft `GET /products` am Gateway auf; Order-Service liefert aktive Produkte mit Preis, Kategorie und Bestand.

**Alternativen/Fehler:** leerer Katalog → leere Liste; Gateway/Service nicht verfügbar → UI meldet, dass der Katalog nicht geladen werden konnte.

### UC-04 – Bestellung erstellen

**Primärakteur:** angemeldeter Kunde  
**Vorbedingungen:** gültiges JWT; Produkt ist aktiv und ausreichend verfügbar.  
**Hauptablauf:**

1. Kunde wählt Produkte und positive Mengen aus.
2. UI sendet `POST /orders` mit Positionen und Bearer-JWT an das Gateway.
3. Gateway leitet an den Order-Service weiter.
4. Order-Service ermittelt die User-ID aus dem Token, prüft Produkt und Bestand, berechnet Positions- und Gesamtpreis, reduziert Bestand und speichert die Bestellung mit Status `CREATED`.
5. Nach erfolgreichem Commit wird `order.created` ausgelöst.
6. Order-Service antwortet `201 Created` mit der gespeicherten Bestellung.
7. Erst nach dieser Antwort zeigt die UI die Bestellung als erstellt an.

**Alternativen/Fehler:** fehlendes/ungültiges Token → Zugriff verweigert; ungültige Menge oder leere Bestellung → `400`; Produkt fehlt → `404`; Bestand zu klein → Fehlerantwort ohne Bestellbestätigung.

**Nachbedingung:** Order und reduzierte Produktbestände sind persistiert. Die Lieferabwicklung kann asynchron starten.

### UC-05 – Eigene Bestellungen ansehen

**Primärakteur:** angemeldeter Kunde  
**Vorbedingung:** gültiges JWT.  
**Hauptablauf:** UI ruft `GET /orders` auf; Order-Service gibt ausschliesslich Orders der Token-Identität zurück. Für Details wird `GET /orders/{orderId}` verwendet.

**Alternativen/Fehler:** kein Token → `401`; fremde oder nicht vorhandene Order-ID → keine fremden Daten (aktuell als `404` modelliert).

### UC-06 – Lieferstatus verfolgen

**Primärakteur:** angemeldeter Kunde  
**Vorbedingung:** Bestellung angenommen; Shipping-Service und Broker verfügbar.  
**Hauptablauf:**

1. Shipping-Service konsumiert `order.created` und legt die Lieferung an.
2. Er publiziert Statusereignisse über `delivery.status.changed`; Order-Service aktualisiert damit den gespeicherten Bestellstatus.
3. Shipping-Service simuliert das Zustellen mit Verzögerung von zufällig 1 bis 10 Sekunden.
4. Das UI lädt beim Öffnen der Ansicht den aktuellen Stand über `GET /orders`.
5. Für Live-Updates öffnet das UI `/ws/orders` und sendet das JWT als erstes JSON-Frame.
6. Shipping-Service authentifiziert die Verbindung und sendet `ORDER_STATUS_CHANGED` nur an Sitzungen des betroffenen Kunden.

**Alternativen/Fehler:** WebSocket nicht verfügbar → zuletzt per REST geladener Status bleibt sichtbar; erneutes Laden der Ansicht liest den Datenbankstand. Ungültiges Token → WebSocket-Verbindung wird geschlossen.

### UC-07 – Löschaktion in der Bestellansicht

**Primärakteur:** angemeldeter Kunde  
**Vorbedingung:** Order gehört zur Token-Identität.  
**Hauptablauf:** Die UI stellt bei eigenen Bestellungen eine Löschaktion dar. Bei Status `DELIVERED` ist die Aktion deaktiviert; bei anderen Status kann der Kunde die Löschung bestätigen. Nach erfolgreichem `DELETE /orders/{id}` aktualisiert die UI die Liste.

**Geltungsbereich:** Dieser Use Case beschreibt die sichtbare UI-Interaktion. Der Order-Service prüft die Zugehörigkeit zur Token-Identität und gibt bei nicht ausgelieferten Bestellungen den reservierten Bestand zurück.

### UC-08 – Eigenes Konto löschen

**Primärakteur:** angemeldeter Kunde  
**Vorbedingung:** gültiges JWT.  
**Hauptablauf:** UI sendet `DELETE /users/me`; User-Service prüft das Token und löscht das zugehörige Benutzerkonto. Die UI entfernt danach das lokale Token und meldet ab. Da weitere Tokenprüfungen das Benutzerkonto laden, wird das Token nach der Löschung nicht mehr akzeptiert.

## Zentrale Abnahmekriterien

| ID | Given / When | Then |
|---|---|---|
| AC-01 | Benutzer mit noch nicht vergebenen Daten sendet gültige Registrierung | `201`, Rolle `CUSTOMER`, persistierter Account, Passwort nicht im Klartext |
| AC-02 | Registrierter Benutzer sendet richtige Zugangsdaten | `200`, Bearer-JWT mit endlicher Laufzeit; falsche Zugangsdaten ergeben `401` |
| AC-03 | Kunde ruft aktive Produkte ab | Gateway gibt Produktliste mit Seed-Produkten und Bestandswerten zurück |
| AC-04 | Authentifizierter Kunde bestellt verfügbare Positionen | `201`, Orderstatus `CREATED`, Summe serverseitig berechnet, Bestand reduziert |
| AC-05 | Bestellung ist angelegt und abhängige Services laufen | Shipping-Service verarbeitet Ereignis und Status erreicht im Demo nach 1–10 Sekunden `DELIVERED` |
| AC-06 | Zwei Benutzer sind angemeldet; eine Order wechselt Status | Nur der Besitzer erhält das zugehörige WebSocket-Event; `GET /orders` liefert den aktuellen persistenten Status |
| AC-07 | Benutzer versucht fremde Order zu lesen oder zu löschen | Keine fremden Daten; Operation wird mit `404` oder `403` verweigert |
| AC-08 | Order ist `DELIVERED`; Benutzer ruft UI auf | Die sichtbare Löschaktion ist deaktiviert. |
| AC-09 | User löscht sein Konto und verwendet das alte JWT erneut | Konto-Löschung antwortet erfolgreich; nachfolgende Identitätsprüfung verweigert das Token |
| AC-10 | Vollständiger Docker-Stack ist gestartet | UI, REST und WebSocket sind über Host-Port 8080 erreichbar; keine anderen Serviceports sind am Host veröffentlicht |

Die Abnahmekriterien fassen umgesetztes Verhalten als nachvollziehbare Prüffälle zusammen. Testergebnisse und Datum sind getrennt zu protokollieren; siehe [SMART-Qualitätsanforderungen](quality-requirements.md) und die technische Architektur in [arc42](../arc42.md).

## Rückverfolgbarkeit

| Funktionale Anforderungen | Use Case | Abnahmekriterium |
|---|---|---|
| FR-01 | UC-01 | AC-01 |
| FR-02 | UC-02 | AC-02 |
| FR-03 | UC-04, UC-05, UC-07 | AC-04, AC-06, AC-07 |
| FR-04 | UC-03 | AC-03 |
| FR-05–FR-07 | UC-04 | AC-04 |
| FR-08–FR-09 | UC-06 | AC-05 |
| FR-10–FR-11 | UC-05, UC-06 | AC-06 |
| FR-12 | UC-07 | AC-08 |
| FR-13 | UC-08 | AC-09 |
| FR-14 | Betriebsvariante Docker | AC-10 |
