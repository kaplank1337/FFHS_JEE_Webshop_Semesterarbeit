# Programmierung – Nachweis der Bewertungskriterien

Diese Dokumentation ordnet die Programmierungskriterien dem Quellcode zu. Der Git-/Repository-Punkt wird auf Wunsch des Projektautors hier nicht bewertet.

## Bewertung

| Bewertungspunkt | Ergebnis | Nachweis |
|---|---|---|
| Code lesbar, dokumentiert und nach Layern/Modulen strukturiert | **Im Quellcode erkennbar** | Vier Maven-WAR-Module; jedes hat `domain`, `application`, `adapter/in` und `adapter/out`. Inbound-/Outbound-Ports sind benannt und fachliche Abläufe liegen in Anwendungsservices. Details siehe [arc42 Bausteinsicht](../arc42.md#5-bausteinsicht). |
| Jakarta EE und moderne Java-Konzepte berücksichtigt | **Im Quellcode erkennbar** | JAX-RS, CDI, Jakarta Transactions, EJB, JPA, Bean Validation, JMS und WebSocket werden in den Modulen eingesetzt. Java-21-Features und konkrete Fundstellen stehen in [Technologien und Sprachmittel](#technologien-und-sprachmittel). |
| Anwendung funktioniert und lässt sich bauen | **Build bestätigt; Laufzeit aktuell nicht neu bestätigt** | `mvn -DskipTests -Daether.syncContext.named.factory=rwlock-local clean package` erzeugte alle vier WAR-Dateien erfolgreich. Tests waren dabei übersprungen. Bei der Prüfung liefen keine Services auf den lokalen Health-Ports; Docker war über den lokalen Socket nicht erreichbar. Das ist kein Nachweis eines Laufzeitfehlers, aber auch kein aktueller Laufzeitnachweis. |
| Erkenntnisse aus der Programmierung dokumentiert | **Dokumentiert** | [Erkenntnisse und technische Erfahrungen](lernerfahrungen.md) |

## Modul- und Layeraufbau

| Modul | Verantwortlichkeit | Eingangsschicht | Anwendungslogik / Ports | Technische Adapter |
|---|---|---|---|---|
| `api-gateway` | REST-Routing und CORS | `adapter/in/rest` | `GatewayRoutingUseCases`, `GatewayRoutingService`, Downstream-Ports | `adapter/out/http/HttpDownstreamAdapter` |
| `user-service` | Konto, Anmeldung und Token | `adapter/in/rest` | `UserAccountUseCases`, `AuthService`, User-/Token-/Hash-Ports | JPA-Repository, Token- und Passwortadapter |
| `order-service` | Produktkatalog, Checkout und Orders | REST-Ressourcen und JMS-Listener | Checkout-/Order-/Catalog-Use-Cases | JPA, Identität, CDI/JMS-Eventadapter |
| `shipping-service` | Lieferung, Status und Live-Updates | REST, JMS-Listener, WebSocket | `ShippingUseCases` und Outbound-Ports | JPA, JMS, WebSocket, EJB-Timer |

Die Runtime- und Interaktionsabläufe sind in [arc42 Kapitel 6](../arc42.md#6-laufzeitsicht) beschrieben. Die vollständige Verteilung steht in [Kapitel 7](../arc42.md#7-verteilungssicht).

## Jakarta EE und Sprachmittel

| Konzept | Verwendung im Projekt | Beispiel-Fundstellen |
|---|---|---|
| JAX-RS | REST-Ressourcen, JSON-Request/Response, Health-Endpunkte | `adapter/in/rest/*Resource.java` in allen Diensten |
| Bean Validation | Registrierung, Login und Order-Requests validieren | `RegisterRequest`, `LoginRequest`, `CreateOrderRequest`, `OrderItemRequest` |
| CDI | Dependency Injection und Application-Scoped-Beans | `@Inject`, `@ApplicationScoped`, Adapter und Anwendungsservices |
| EJB / Jakarta Transactions | Transaktionsgrenzen für Checkout und Statusänderungen; Singleton-Listener und Timer | `CheckoutService`, `ShippingService`, `OrderCreatedEventListener`, `ShipmentDeliverySimulator` |
| CDI-Transaktionsereignis | OrderCreated wird erst nach erfolgreichem Transaktionsabschluss weitergegeben | `OrderCreatedEventPublisher.publishAfterCommit` mit `TransactionPhase.AFTER_SUCCESS` |
| JPA | Persistenz von User, Product, Order, OrderItem und Shipment | `domain/model/*Entity.java`, `adapter/out/persistence/Jpa*Repository.java` |
| Jakarta Messaging | Queue `order.created` und Topic `delivery.status.changed` | `OrderCreatedEventPublisher`, `OrderCreatedEventListener`, `DeliveryStatusChangedListener` |
| Jakarta WebSocket | Authentifizierter Statuskanal für Browser-Sitzungen | `ShippingStatusWebSocketEndpoint` |
| Java Records | Kompakte DTOs und Event-Daten | Request-/Response-DTOs und `OrderCreatedEvent` |
| Moderne Java-Syntax/API | Switch-Ausdrücke, Pattern Matching für `instanceof`, Streams und `toList()` | Statusabbildung, JMS Listener und Anwendungsservices |
| Maven Multi-Module | Vier einzeln paketierte WAR-Dateien und gemeinsame Dependency-Verwaltung | Root-`pom.xml` und die Service-POMs |

## Buildnachweis

Der folgende Build lief am **22.11.2026** erfolgreich durch:

```bash
mvn -DskipTests -Daether.syncContext.named.factory=rwlock-local clean package
```

Ergebnis: `user-service`, `order-service`, `shipping-service` und `api-gateway` wurden jeweils als WAR gebaut. Der Lock-Parameter umgeht die auf diesem Rechner aufgetretene Maven-Lock-Fehlermeldung aus dem lokalen Repository. `-DskipTests` bedeutet ausdrücklich, dass dieser Build kein Testergebnis liefert.

Das Root-POM verwendet `maven.compiler.release=21`. Damit wird der Bytecode gegen Java 21 und dessen API-Ziel kompiliert, auch wenn lokal ein neueres JDK installiert ist.

## Testumfang im Repository

Unit-Tests liegen derzeit im User-Service für Passwort-Hashing, JWT-Grundfunktionen und AuthService-Abläufe. Order-Service, Shipping-Service, Gateway und die vollständige JMS-/WebSocket-Kette haben in den Service-Modulen keine entsprechenden Testquellen. In diesem Arbeitsschritt wurden keine Tests ausgeführt.

## Bekannte Grenzen

- `domain/model` enthält aktuell JPA-annotierte Entities; das Domain-Modell ist deshalb nicht vollständig ORM-unabhängig.
- Das Passwort-Hashing verwendet salted SHA-256; für produktive Zugangsdaten wäre ein langsames Passwort-KDF erforderlich.
- `AuthService.login` liefert `expiresIn=3600` fest zurück, obwohl die Token-Laufzeit separat konfigurierbar ist. Bei abweichender `JWT_EXPIRATION_SECONDS`-Konfiguration kann Antwort und Token-Claim auseinanderlaufen.
- Für das `order.created`-Ereignis gibt es keine Transactional Outbox. Ein Prozessabbruch nach dem Order-Commit kann daher die JMS-Übergabe verlieren.
- Der aktuelle Build bestätigt Kompilierung und WAR-Paketierung. Ein End-to-End-Lauf mit erreichbarer Datenbank, Broker und Server muss separat demonstriert bzw. protokolliert werden.
