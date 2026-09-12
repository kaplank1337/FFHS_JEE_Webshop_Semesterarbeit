# JPA & JPQL Documentation

## Übersicht

Diese Dokumentation beschreibt die 6 JPQL-Queries, die in der Anwendung verwendet werden, sowie die JPA-Entities und deren Zuordnung.

**Lernziel:** JPQL als Standard Object Query Language für Jakarta Persistence demonstrieren.

## JPA Entities

### User Service Entity

#### UserEntity
```
Schema: app_user
Table: "user"
Columns: id (UUID, PK), username (VARCHAR, UNIQUE), email (VARCHAR, UNIQUE), 
         passwordHash (VARCHAR), firstName (VARCHAR), lastName (VARCHAR),
         role (ENUM: CUSTOMER/ADMIN), createdAt (TIMESTAMP), updatedAt (TIMESTAMP)
```

### Order Service Entities

#### ProductEntity
```
Schema: orders
Table: product
Columns: id (UUID, PK), name (VARCHAR), description (VARCHAR), 
         category (ENUM: WATER/SOFT_DRINKS/JUICE/BEER/COFFEE),
         price (NUMERIC), stockQuantity (INT), active (BOOLEAN),
         createdAt (TIMESTAMP), updatedAt (TIMESTAMP)
```

#### OrderEntity
```
Schema: orders
Table: "order"
Columns: id (UUID, PK), userId (UUID, FK-ref app_user), 
         status (ENUM: CREATED/CONFIRMED/SHIPPING/SHIPPED/CANCELLED),
         totalPrice (NUMERIC), createdAt (TIMESTAMP), updatedAt (TIMESTAMP)

Relationships:
  - OneToMany: OrderEntity.items → OrderItemEntity (cascade ALL, orphanRemoval=true)
```

#### OrderItemEntity
```
Schema: orders
Table: order_item
Columns: id (UUID, PK), order_id (UUID, FK), 
         productId (UUID), productName (VARCHAR), quantity (INT),
         unitPrice (NUMERIC), totalPrice (NUMERIC)

Relationships:
  - ManyToOne: OrderItemEntity.order → OrderEntity (fetch LAZY)
```

### Shipping Service Entity

#### ShipmentEntity
```
Schema: shipping
Table: shipment
Columns: id (UUID, PK), orderId (UUID, FK-ref orders.order),
         userId (UUID, FK-ref app_user),
         status (ENUM: PREPARING/READY_FOR_SHIPMENT/SHIPPED/DELIVERED/CANCELLED),
         trackingNumber (VARCHAR), createdAt (TIMESTAMP), updatedAt (TIMESTAMP)
```

## JPQL Queries

### Query #1: Benutzer nach Username suchen

**Klasse:** `UserRepository`

**Zweck:** Benutzer bei Login-Versuch finden

```jpql
SELECT u FROM UserEntity u WHERE u.username = :username
```

**Verwendung:**
```java
Optional<UserEntity> user = userRepository.findByUsername("alice");
if (user.isPresent()) {
    // User exists, verify password
    UserEntity entity = user.get();
    boolean passwordMatches = passwordEncoder.matches(plainPassword, entity.getPasswordHash());
}
```

**Lernziel:** 
- Einfache SELECT-Abfrage
- WHERE-Klausel mit Parameter
- Named Parameter `:username`
- Optional-Pattern

**Index-Unterstützung:** `idx_user_username` für Performance

---

### Query #2: Produkte nach Kategorie filtern (aktiv)

**Klasse:** `ProductRepository`

**Zweck:** Produktkatalog nach Kategorie anzeigen

```jpql
SELECT p FROM ProductEntity p 
WHERE p.category = :category AND p.active = true 
ORDER BY p.name ASC
```

**Verwendung:**
```java
List<ProductEntity> beverages = productRepository.findByCategoryAndActive(ProductCategory.WATER);
// Returns: [Mineralwasser 1.5L, Mineralwasser 0.5L, ...] sorted by name
```

**Lernziel:**
- Mehrere Bedingungen in WHERE (AND)
- Enum-Filtering (p.category = :category)
- ORDER BY Clause
- Sorting von Queries

**Performance:** `idx_product_category` + `idx_product_active`

---

### Query #3: Alle aktiven Produkte (sortiert nach Erstellungsdatum)

**Klasse:** `ProductRepository`

**Zweck:** Neue Produkte zuerst anzeigen (What's New?)

```jpql
SELECT p FROM ProductEntity p 
WHERE p.active = true 
ORDER BY p.createdAt DESC
```

**Verwendung:**
```java
List<ProductEntity> newProducts = productRepository.findAllActive();
// Returns newest products first (DESC = descending, newest first)
```

**Lernziel:**
- Sorting mit Timestamp-Spalten
- DESC vs. ASC
- Pagination-vorbereitung (mit setFirstResult/setMaxResults)

---

### Query #4: Bestellungen eines Benutzers (sortiert nach Datum)

**Klasse:** `OrderRepository`

**Zweck:** Meine Bestellungen anzeigen

```jpql
SELECT o FROM OrderEntity o 
WHERE o.userId = :userId 
ORDER BY o.createdAt DESC
```

**Verwendung:**
```java
List<OrderEntity> myOrders = orderRepository.findByUserId(userId);
// Returns: [Order vom 12.9, Order vom 11.9, ...] (newest first)
```

**Lernziel:**
- UUID-basierte Queries
- Filtering nach Foreign Key
- Zeitgestempelte Daten

**Wichtig:** userId ist UUID, NICHT Foreign Key in DB (Microservice-Isolation)

---

### Query #5: Bestellungen nach Status

**Klasse:** `OrderRepository`

**Zweck:** Admin: Bestellungen nach Verarbeitungsstand filtern

```jpql
SELECT o FROM OrderEntity o 
WHERE o.status = :status 
ORDER BY o.createdAt DESC
```

**Verwendung:**
```java
List<OrderEntity> pendingOrders = orderRepository.findByStatus(OrderStatus.SHIPPING);
// Returns: All orders currently in shipping (for admin dashboard)
```

**Lernziel:**
- Enum-Filtering
- State Machine Queries
- Admin-Reports

**Mögliche Status-Filter:**
- CREATED: Neue Bestellungen
- CONFIRMED: Bestätigte Bestellungen
- SHIPPING: Versendet
- SHIPPED: Zugestellt
- CANCELLED: Abgebrochen

---

### Query #6: Shipment nach Order-ID suchen

**Klasse:** `ShipmentRepository`

**Zweck:** Versandstatus für eine spezifische Bestellung anzeigen

```jpql
SELECT s FROM ShipmentEntity s 
WHERE s.orderId = :orderId
```

**Verwendung:**
```java
Optional<ShipmentEntity> shipment = shipmentRepository.findByOrderId(orderId);
if (shipment.isPresent()) {
    ShippingStatus status = shipment.get().getShippingStatus();
    String tracking = shipment.get().getTrackingNumber();
    // Display shipment details to user
}
```

**Lernziel:**
- 1:1 Relationship Query
- SingleResult (nicht List)
- Handling NoResultException
- Microservice-übergreifende Abfrage (orderId ist externe UUID)

---

## JPA Best Practices in dieser Anwendung

### 1. Entity vs. DTO Trennung

❌ **NICHT machen:**
```java
@RestController
public class UserController {
    @GetMapping("/users/{id}")
    public UserEntity getUser(UUID id) {  // Entity direkt als Response!
        return userRepository.findById(id).orElseThrow();
    }
}
```

✅ **Besser:**
```java
@Path("/users/{id}")
public class UserResource {
    @GET
    public Response getUser(@PathParam("id") UUID id) {
        UserEntity entity = userRepository.findById(id).orElseThrow();
        UserDto dto = userMapper.toDto(entity);  // Entity → DTO Mapping
        return Response.ok(dto).build();
    }
}
```

**Grund:** 
- Entity hat interne Struktur (z.B. passwordHash soll NICHT über REST gehen)
- DTO definiert öffentliche API
- Entity kann ändern, ohne REST-API zu brechen

### 2. Lazy Loading und Fetch Strategies

```java
@Entity
public class OrderEntity {
    @OneToMany(mappedBy = "order", fetch = FetchType.EAGER)
    private List<OrderItemEntity> items;
    
    @ManyToOne(fetch = FetchType.LAZY)  // Nicht sofort laden
    private OrderEntity order;
}
```

**EAGER:** Wird sofort mitgeladen (bei Order → auch Items)
**LAZY:** Wird nur bei Zugriff geladen (Performance!)

### 3. Cascade und Orphan Removal

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItemEntity> items;
```

- **cascade = CascadeType.ALL:** Wenn Order gelöscht wird → auch Items löschen
- **orphanRemoval = true:** Wenn Item aus der List entfernt wird → aus DB löschen

### 4. Named Queries (Alternative zu @Query)

Klassischer JPA-Ansatz (in Entity definieren):
```java
@Entity
@NamedQuery(
    name = "UserEntity.findByUsername",
    query = "SELECT u FROM UserEntity u WHERE u.username = ?1"
)
public class UserEntity { ... }
```

Dann verwenden:
```java
TypedQuery<UserEntity> query = em.createNamedQuery("UserEntity.findByUsername", UserEntity.class);
query.setParameter(1, "alice");
UserEntity user = query.getSingleResult();
```

**In dieser Anwendung:** Queries sind in Repository-Klassen als Strings definiert (einfacher zu testen)

### 5. Transient Fields

```java
@Entity
public class OrderEntity {
    @Transient  // Nicht persistent
    private BigDecimal cachedTotalPrice;
    
    public BigDecimal getTotalPrice() {
        if (cachedTotalPrice == null) {
            calculateTotalPrice();  // Dynamisch berechnet
        }
        return totalPrice;
    }
}
```

### 6. Validation Annotations

```java
@Entity
public class ProductEntity {
    @Positive(message = "Price must be positive")
    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;
    
    @Min(0)
    @Max(100000)
    @Column(name = "stock_quantity")
    private int stockQuantity;
}
```

Wird automatisch validiert wenn:
- `entityManager.persist()` aufgerufen wird
- `@Valid` auf REST-Parameter liegt

### 7. Lifecycle Callbacks

```java
@Entity
public class UserEntity {
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

Wird automatisch aufgerufen vor persist/update (kein manueller Code nötig)

## Pagination

```java
public List<ProductEntity> findAllActivePaginated(int pageNumber, int pageSize) {
    int offset = pageNumber * pageSize;
    
    return entityManager.createQuery(
        "SELECT p FROM ProductEntity p WHERE p.active = true ORDER BY p.createdAt DESC",
        ProductEntity.class)
        .setFirstResult(offset)      // Skip offset Einträge
        .setMaxResults(pageSize)     // Limit pageSize
        .getResultList();
}
```

## Performance-Tipps

1. **Indizes:** Auf Spalten mit WHERE-Bedingungen setzen
   ```sql
   CREATE INDEX idx_user_username ON app_user."user"(username);
   CREATE INDEX idx_order_user ON orders."order"(user_id);
   ```

2. **Fetch vs. Batch:** Bei vielen Items besser LAZY laden
   ```java
   @OneToMany(fetch = FetchType.LAZY)
   private List<OrderItemEntity> items;
   ```

3. **Projection:** Nur benötigte Spalten laden
   ```jpql
   SELECT NEW com.jee.shop.order.dto.OrderSummaryDto(
       o.id, o.userId, o.totalPrice, o.status
   ) FROM OrderEntity o WHERE o.userId = :userId
   ```

4. **N+1 Problem:** Vorsicht bei Lazy Loading in Loops
   ```java
   List<OrderEntity> orders = repository.findAll();  // Query 1
   for (OrderEntity order : orders) {
       // ❌ Wird 1 Query pro Order ausgelöst!
       order.getItems().size();
   }
   ```
   
   Lösung: EAGER Loading oder separate JOIN Fetch Query
   ```jpql
   SELECT DISTINCT o FROM OrderEntity o 
   LEFT JOIN FETCH o.items 
   WHERE o.userId = :userId
   ```

## JPQL vs. SQL

| Aspekt | JPQL | SQL |
|--------|------|-----|
| Datenbank-unabhängig | ✅ Ja | ❌ Nein |
| Entities direkt | ✅ Ja (Entity-basiert) | ❌ Nein (Row-basiert) |
| Refactoring-sicher | ✅ Ja (Compile-time checks möglich) | ❌ Nein (String) |
| Performance | ≈ Gleich (wird zu SQL) | ≈ Gleich |
| Komplexe Queries | 📊 Gut | 📊 Sehr gut |
| Komplexe Joins | 📊 Etwas verbose | 📊 Natural |

**Diese Anwendung nutzt JPQL ** für alle Standard-Queries. Native SQL nur wenn absolut nötig (nicht vorhanden).

