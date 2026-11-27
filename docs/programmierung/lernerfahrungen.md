# Erkenntnisse aus der Programmierung

Die folgenden Punkte leiten sich aus der Implementierung und den gewählten Technologien des JEE Drink Shop ab. Sie beschreiben konkrete technische Erfahrungen und Grenzen des Projekts.

## 1. Ports und Adapter machen Integrationsgrenzen sichtbar

REST-, JMS- und WebSocket-Eingänge rufen Use-Case-Ports auf. Anwendungsservices verwenden Ausgangs-Ports für Datenhaltung, User-Identität, Timer und Events. Dadurch sind die Integrationen in einzelnen Adaptern auffindbar und die Geschäftsabläufe sind nicht direkt an den HTTP- oder JMS-Client gebunden.

**Erkenntnis:** Diese Struktur hilft beim Verständnis und Austausch technischer Adapter. Sie schafft aber nicht automatisch eine reine Domain: Die Modelle sind derzeit weiterhin mit JPA annotiert.

## 2. Verteilte Abläufe brauchen mehr als eine Datenbanktransaktion

Der Order-Service publiziert `order.created` erst nach erfolgreichem Commit. Das verhindert, dass der Shipping-Service ein Ereignis für eine zurückgerollte Bestellung verarbeitet. Zwischen Order-Commit und JMS-Publikation bleibt trotzdem ein Prozessausfall-Fenster.

**Erkenntnis:** Für eine belastbare Zustellung wäre eine Transactional Outbox mit Wiederholungsmechanismus nötig. Der aktuelle Demo-Ablauf ist asynchron, aber keine garantierte Messaging-Zustellung.

## 3. REST, JMS und WebSocket lösen unterschiedliche Aufgaben

- REST beantwortet Checkout und Produkt-/Orderabfragen direkt.
- JMS entkoppelt Order-Erstellung und Lieferverarbeitung.
- WebSocket überträgt Statusänderungen an bereits verbundene UI-Sitzungen.
- Die Datenbank bleibt die Quelle für den Status, der beim späteren erneuten Öffnen per REST geladen wird.

**Erkenntnis:** Live-Updates ersetzen keinen gespeicherten Zustand. Eine UI muss beim Öffnen zuerst bzw. weiterhin den REST-Zustand laden und WebSocket nur als Änderungsstrom verwenden.

## 4. Benutzeridentität muss an Servicegrenzen mitgeführt werden

Die Services leiten die User-ID aus dem Token ab und filtern eigene Orders bzw. Shipments anhand dieser ID. Der WebSocket authentifiziert das Token beim Verbindungsaufbau und ordnet die Sitzung einer User-ID zu.

**Erkenntnis:** Eine gültige Signatur allein genügt nicht für Datenisolation. Jeder Lese- und Änderungsweg muss zusätzlich den Ressourceneigentümer berücksichtigen.

## 5. Getrennte Schemas lösen keine Integritätsfragen zwischen Services

`app_user`, `orders` und `shipping` sind getrennte Schemas. `user_id` und `order_id` werden serviceübergreifend als UUID übertragen; die Datenbank kann dort keine Fremdschlüssel erzwingen.

**Erkenntnis:** Service-Autonomie verschiebt Integritätsverantwortung in die Schnittstellen, Events und Anwendungslogik. Das Datenmodell dokumentiert deshalb echte Fremdschlüssel getrennt von logischen Referenzen.

## 6. WebSocket erfordert einen eigenen Authentifizierungsablauf

Ein WebSocket-Handshake kann den Bearer-Header im Browser nicht so bequem wie `fetch` setzen. Die UI sendet darum das JWT als erstes Nachrichten-Frame; der Server authentifiziert vor dem Eintragen der Sitzung in die Benutzerzuordnung.

**Erkenntnis:** Der Socket darf keine nutzerbezogenen Nachrichten vor erfolgreicher Authentifizierung erhalten. Reconnects und abgelaufene Tokens müssen als eigener UI-Fall behandelt werden.

## 7. Buildbarkeit und Laufzeitverhalten sind getrennte Nachweise

Maven kompiliert die Services und packt WAR-Dateien. Damit ist die Paketierung bestätigt, nicht aber das Zusammenspiel aus Jakarta-EE-Server, PostgreSQL, ActiveMQ, Gateway und UI.

**Erkenntnis:** Ein vollständiger Nachweis braucht neben dem Build einen Laufzeitdurchgang mit gesunden Abhängigkeiten und konkreten REST-/JMS-/WebSocket-Ergebnissen. Die vorhandenen User-Service-Unit-Tests ersetzen diesen Integrationsnachweis nicht.

## 8. Java-Zielversion explizit festlegen

Das Projekt wird mit Java-21-Release kompiliert, obwohl ein lokales JDK neuer sein kann. Das Root-POM setzt `maven.compiler.release=21`.

**Erkenntnis:** `release` begrenzt sowohl Bytecode als auch verfügbare Java-APIs auf die Zielversion. Das vermeidet, dass eine lokale neuere JDK-Installation versehentlich APIs einführt, die im Java-21-Container fehlen.
