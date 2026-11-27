# Entwurf

Dieses Kapitel beschreibt den Entwurf des SIP Scout Order Shops aus den für die Bewertung geforderten Perspektiven. Es dokumentiert den aktuellen Implementierungsstand des Repositorys und unterscheidet fachliche Abläufe, technische Grenzen und Datenbankregeln.

## Bewertungsnachweis

| Kriterium | Nachweis in diesem Ordner | Inhalt |
|---|---|---|
| Lösungsansatz und Architektur bildlich und textuell | [Architektur und Bausteinsicht](architektur.md) | Systemkontext, Architekturprinzip, Service-Verantwortung und Ports/Adapter mit Grafiken |
| Strukturperspektive | [Architektur und Bausteinsicht](architektur.md#bausteinsicht) | Module, Pakete, Verantwortlichkeiten sowie Ein- und Ausgangsadapter |
| Verhaltensperspektive | [Laufzeit- und Interaktionssicht](laufzeit.md) | Registrierung, Login, Checkout, Lieferstatus und Fehlerpfade |
| Interaktionsperspektive | [Laufzeit- und Interaktionssicht](laufzeit.md) | Sequenzdiagramme für synchrone REST- und asynchrone JMS/WebSocket-Aufrufe |
| Datenbankmodell | [Relationales Datenmodell](datenmodell.md) | ER-Grafik, Tabellen, Attribute, Typen, Constraints, Indizes und Integritätsgrenzen |

## Weiterführende Dokumentation

- [arc42-Gesamtdokumentation](../arc42.md) – Ziele, Randbedingungen, Kontext, Strategie, Sichten, Entscheidungen, Qualitätsszenarien und Risiken.
- [Spezifikation](../spezifikation/README.md) – fachliche Anforderungen, Use Cases und Qualitätsziele.

Die Grafiken werden aus `docs/diagrams` referenziert. Durchgezogene ER-Kanten stehen für Datenbank-Fremdschlüssel; gestrichelte Kanten zeigen fachliche Referenzen zwischen Services, die nicht relational erzwungen werden.
