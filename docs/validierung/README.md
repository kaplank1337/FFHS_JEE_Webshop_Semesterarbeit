# Validierung des Webshops

## Testverfahren und Wiederholung

Die [Abnahmekriterien AC-01 bis AC-10](../spezifikation/use-cases.md#zentrale-abnahmekriterien) definieren die Sollwerte. Auf Komponentenebene prüfen JUnit Jupiter und Mockito die Konto-, Token- und Passwortlogik. Vitest prüft die Startseitenroute. Auf Systemebene arbeitet das [Abnahmeskript](abnahme.cjs) gegen den öffentlichen Docker-Einstieg, verwendet zwei verschiedene Benutzer und öffnet zwei authentifizierte WebSockets vor dem Checkout.

Im Projektstamm mit Java 21, Maven, Node 20, installierten UI-Abhängigkeiten und Docker ausführen:

```sh
mvn -Daether.syncContext.named.factory=rwlock-local test
(cd webshop-ui && npm test)
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
node docs/validierung/abnahme.cjs
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

Für den Systemtest einen Demo-Datenbestand mit verfügbarem Produkt verwenden. Pro erfolgreichem Lauf werden zwei temporäre Konten angelegt und wieder gelöscht. Eine ausgelieferte Bestellung bleibt gespeichert und verbraucht eine Bestandseinheit.

## Ergebnis vom 27.11.2026

| Lauf | Beobachtung |
|---|---|
| Maven, Temurin 21 | 19 von 19 Tests erfolgreich, 0 übersprungen. AuthService 7, PasswordEncoder 6, TokenService 6. Beim ersten Java-21-Lauf meldete Mockito acht Fehler wegen ungenutzter Stubs. Nach Zuordnung der Stubs zu den betroffenen Tests lief dieselbe Suite erfolgreich. |
| UI, Node 20.20.0, Vitest 4.1.12 | 1 von 1 Routingtests erfolgreich. |
| Produktions-Compose, lokal vorhandene Images | Der Systemtest lief über Port 8080. Testbestellung: `1b23b091-d2a0-477b-ab39-6671c53f3b98`. |

| Kriterium | Prüfung und festgestelltes Ergebnis |
|---|---|
| AC-01 Registrierung | Zwei neue Konten lieferten HTTP 201 und CUSTOMER. Die anschliessende Anmeldung fand beide Konten. Hashbildung und Passwortvergleich sind separat durch Unit-Tests belegt. Der Lauf enthielt keine direkte SQL-Prüfung der gespeicherten Hashes. |
| AC-02 Login | Je Konto HTTP 401 bei falschem Passwort, HTTP 200 mit verwendbarem Token und positiver Laufzeit bei korrektem Passwort. |
| AC-03 Katalog | GET /api/products lieferte 200 und ein aktives Produkt mit positivem Bestand. Die vollständige Liste der Seed-Produkte wurde nicht verglichen. |
| AC-04 Checkout | HTTP 201, Status CREATED, Gesamtbetrag entsprach bei Menge eins dem Katalogpreis, Bestand sank um eins. |
| AC-05 Lieferung | Vom Beginn des Checkout-Requests bis zum beobachteten DELIVERED-Ereignis vergingen 6,655 Sekunden. Der Messwert enthält HTTP, Broker und Beobachtungsintervall. |
| AC-06 Benutzerbezogene Updates | A erhielt DELIVERED für die Testbestellung. Auf der angemeldeten Verbindung von B kam während der Beobachtung kein Ereignis zu dieser Bestellung an. Ein separater GET /orders mit As Token bestätigte DELIVERED. |
| AC-07 Fremdzugriff | Abruf und Löschversuch mit Bs Token wurden jeweils abgewiesen. Das Skript akzeptiert gemäss Spezifikation 403 oder 404. |
| AC-08 Löschaktion | Durch [Codeinspektion der Bestellansicht](../../webshop-ui/src/routes/orders.tsx) geprüft. OrderCard setzt den sichtbaren Löschbutton für DELIVERED auf disabled. Ein Browser-Klicktest wurde in diesem Lauf nicht durchgeführt. |
| AC-09 Kontolöschung | Beide Konten antworteten auf DELETE /users/me mit 204. Derselbe Token erhielt danach auf GET /users/me jeweils 401. |
| AC-10 Öffentlicher Zugang | UI antwortete mit HTTP 200. REST und authentifizierter WebSocket liefen über localhost:8080. Compose zeigte als einzige Hostfreigabe 8080 → 80 beim Edge-Gateway. |

Der Systemlauf prüft das Zusammenspiel der gestarteten Services aus Sicht eines API-Clients. Das Rendern der Live-Änderung im Browser ist damit separat vom Empfang der WebSocket-Nachricht zu bewerten.
