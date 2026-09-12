# Security Dokumentation

## Überblick

Diese Dokumentation beschreibt die Security-Architektur der Anwendung:
- **Authentifizierung:** Username + Passwort → JWT Token
- **Autorisierung:** Role-Based Access Control (RBAC)
- **Token-Validierung:** Vor geschützten Endpoints
- **Passwort-Hashing:** PBKDF2

**Lernziel:** Sichere Authentifizierung ohne Spring Security, JWT, Token-basierte Stateless-Authentication

---

## Authentifizierungs-Fluss

### 1. Registrierung (Registration)

```
Client
  │
  ├─ POST /api/auth/register
  │  {
  │    "username": "alice",
  │    "email": "alice@example.com",
  │    "password": "SecurePassword123",
  │    "firstName": "Alice",
  │    "lastName": "Smith"
  │  }
  │
  └─→ API Gateway
       │
       └─→ User Service
            │
            ├─ AuthService.register()
            │  ├─ Check username exists (JPQL Query)
            │  ├─ Check email exists (JPQL Query)
            │  ├─ Hash password (PBKDF2)
            │  ├─ Create UserEntity
            │  └─ Persist to DB
            │
            └─→ Response: 201 Created
                {
                  "id": "550e8400-...",
                  "username": "alice",
                  "email": "alice@example.com",
                  "firstName": "Alice",
                  "lastName": "Smith",
                  "role": "CUSTOMER",
                  "createdAt": "2024-09-12T10:30:00"
                }
```

**Bean Validation:**
```java
public record RegisterRequest(
    @NotBlank @Size(min=3, max=50) String username,
    @NotBlank @Email String email,
    @NotBlank @Size(min=8) String password,  // Minimum 8 chars
    @NotBlank @Size(min=1, max=50) String firstName,
    @NotBlank @Size(min=1, max=50) String lastName
) {}
```

**Fehlerbehandlung:**
- Duplicate username → 409 Conflict
- Duplicate email → 409 Conflict
- Invalid email format → 400 Bad Request

---

### 2. Login (Authentication)

```
Client
  │
  ├─ POST /api/auth/login
  │  {
  │    "username": "alice",
  │    "password": "SecurePassword123"
  │  }
  │
  └─→ API Gateway
       │
       └─→ User Service
            │
            ├─ AuthService.login()
            │  ├─ Find user by username (JPQL Query #1)
            │  ├─ Verify password (PasswordEncoder.matches())
            │  ├─ TokenService.generateToken()
            │  │  ├─ Create JWT payload
            │  │  ├─ Sign with secret (HMAC-SHA256)
            │  │  └─ Base64 encode
            │  │
            │  └─→ Return TokenResponse
            │
            └─→ Response: 200 OK
                {
                  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                  "tokenType": "Bearer",
                  "expiresIn": 3600
                }
```

**Fehlerbehandlung:**
- User not found → 401 Unauthorized
- Password wrong → 401 Unauthorized
- Account locked (optional) → 403 Forbidden

---

### 3. Token-Validierung auf geschützten Endpoints

```
Client
  │
  ├─ GET /api/orders
  │  Headers: Authorization: Bearer eyJhbGciOi...
  │
  └─→ API Gateway
       │
       ├─ AuthFilter (@Provider)
       │  ├─ Extract token from Authorization header
       │  ├─ Strip "Bearer " prefix
       │  ├─ Call TokenService.validateToken(token)
       │  │  ├─ Verify signature (HMAC-SHA256)
       │  │  ├─ Check expiration (exp claim)
       │  │  ├─ Extract user info (sub, username, role)
       │  │  └─ Return TokenPayload or null
       │  │
       │  ├─ If valid: Set UserContext in RequestScope
       │  └─ If invalid: Throw AuthenticationException (401)
       │
       └─→ Order Service
            │
            ├─ REST Resource erhält Request
            │  └─ @Context HttpHeaders kann Authorization abrufen
            │
            └─→ PostgreSQL
                 └─ Benutzer-spezifische Daten
```

---

## Passwort-Sicherheit

### Hashing-Algorithmus: PBKDF2

**Standard:** PBKDF2WithHmacSHA256

**Konfiguration:**
```java
public class PasswordEncoder {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100000;  // Rechenintensiv
    private static final int KEY_LENGTH = 256;     // 256 bits
    private static final int SALT_LENGTH = 16;     // 16 bytes salt
}
```

**Prozess:**

```
Passwort in Klartext: "MySecurePassword123"
         ↓
    PBKDF2(
        password = "MySecurePassword123".toCharArray(),
        salt = randomBytes(16),
        iterations = 100000,
        algorithm = HMAC-SHA256,
        keyLength = 256 bits
    )
         ↓
Hash (256 bits) + Salt (16 bytes) kombiniert
         ↓
Base64 Encoded: "AAAAAA...==+o3RvXPLZYKIuCnJQqAE4Pq4Y1pFZcNJ/zZpBdQzNQzh9GDjt5cpZKfpvHv3Y6dJvP8Fw=="
         ↓
In Database gespeichert
```

**Verifikation:**

```
Client Passwort: "MySecurePassword123"
         ↓
    Extract salt from stored hash
         ↓
    PBKDF2(password, extracted_salt, ...)
         ↓
    Compare generated hash with stored hash
         ↓
    If equal: Password correct ✅
    If different: Password wrong ❌
```

**Wichtig:** Passwörter werden NIEMALS in Klartext gespeichert!

### Sichere Defaults

```properties
# .env
JWT_SECRET=your-super-secret-jwt-key-change-in-production
JWT_EXPIRATION_SECONDS=3600

# Nie hardcodiert, immer aus Environment!
```

---

## JWT (JSON Web Token)

### Token-Struktur

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC0uLi4iLCJ1c2VybmFtZSI6ImFsaWNlIiwicm9sZSI6IkNVU1RPTUVSIiwiaWF0IjoxNjk0NTIwNjAwLCJleHAiOjE2OTQ1MjQyMDB9.signature...
```

**3 Teile (Base64-encoded):**

1. **Header:** Algoritmus & Token-Typ
   ```json
   {
     "alg": "HS256",
     "typ": "JWT"
   }
   ```

2. **Payload:** Claims (Benutzer-Info)
   ```json
   {
     "sub": "550e8400-e29b-41d4-a716-446655440000",  // Subject (User ID)
     "username": "alice",
     "role": "CUSTOMER",
     "iat": 1694520600,                               // Issued At
     "exp": 1694524200                                // Expiration (1 hour later)
   }
   ```

3. **Signature:** HMAC-SHA256(header.payload, secret)
   ```
   HMAC-SHA256(
       "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI6Li4uIn0=",
       "your-jwt-secret-key"
   ) = Base64(signature_bytes)
   ```

### Token-Validierung

```java
public TokenPayload validateToken(String token) {
    String[] parts = token.split("\\.");
    if (parts.length != 3) return null;
    
    // 1. Verify signature
    String message = parts[0] + "." + parts[1];
    String providedSignature = parts[2];
    String expectedSignature = generateSignature(message);
    
    if (!providedSignature.equals(expectedSignature)) {
        return null;  // Signature invalid (tampered token)
    }
    
    // 2. Decode and parse payload
    String payloadJson = new String(
        Base64.getUrlDecoder().decode(parts[1])
    );
    TokenPayload payload = parsePayload(payloadJson);
    
    // 3. Check expiration
    if (payload.getExp() < System.currentTimeMillis() / 1000) {
        return null;  // Token expired
    }
    
    return payload;  // Valid!
}
```

### Token-Claims (Payload)

| Claim | Typ | Bedeutung | Beispiel |
|-------|-----|----------|----------|
| `sub` | UUID | Subject (User ID) | `550e8400-...` |
| `username` | String | Username | `alice` |
| `role` | String | User Role | `CUSTOMER` oder `ADMIN` |
| `iat` | Long | Issued At (Unix timestamp) | `1694520600` |
| `exp` | Long | Expiration (Unix timestamp) | `1694524200` |
| `eventId` | String | (Optional) Event-ID für Idempotenz | (optional) |

---

## Autorisierung (Role-Based Access Control)

### Rollen

| Rolle | REST-Endpoints | Berechtigungen |
|-------|----------------|----------------|
| **CUSTOMER** (default) | GET /products, POST /orders, GET /orders, GET /shipments | Produkte anzeigen, Bestellungen, Status |
| **ADMIN** | + POST/PUT/DELETE /products | Produktverwaltung |

### Implementierung

**Option 1: Manual Role Check in Resource**
```java
@Path("/products")
public class ProductResource {
    
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createProduct(@Valid ProductDto dto, @Context HttpHeaders headers) {
        // Extract token from Authorization header
        String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
        TokenPayload payload = tokenService.validateToken(authHeader);
        
        // Check role
        if (payload.getRole() != UserRole.ADMIN) {
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("Only ADMIN can create products"))
                .build();
        }
        
        // Process creation
        ProductDto result = productService.createProduct(dto);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }
}
```

**Option 2: Custom @AdminOnly Annotation (Future)**
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AdminOnly {}

// In Interceptor/Filter:
@Provider
@Priority(Priorities.AUTHORIZATION)
public class AdminAuthorizationFilter implements ContainerRequestFilter {
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        Method method = requestContext.getResourceMethod().getInvocedMethod();
        if (method.isAnnotationPresent(AdminOnly.class)) {
            String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
            TokenPayload payload = tokenService.validateToken(authHeader);
            
            if (payload == null || payload.getRole() != UserRole.ADMIN) {
                requestContext.abortWith(
                    Response.status(Response.Status.FORBIDDEN)
                        .build()
                );
            }
        }
    }
}
```

---

## API Gateway: AuthFilter

```java
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {
    
    @Inject
    private TokenService tokenService;
    
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();
        
        // Public endpoints (no token required)
        if (isPublicEndpoint(path)) {
            return;
        }
        
        // Protected endpoints (token required)
        String authHeader = requestContext.getHeaderString(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isEmpty()) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("No authorization header"))
                    .build()
            );
            return;
        }
        
        TokenPayload payload = tokenService.validateToken(authHeader);
        if (payload == null) {
            requestContext.abortWith(
                Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("Invalid or expired token"))
                    .build()
            );
            return;
        }
        
        // Token is valid → set request context
        requestContext.setProperty("user.id", payload.getSub());
        requestContext.setProperty("user.username", payload.getUsername());
        requestContext.setProperty("user.role", payload.getRole());
    }
    
    private boolean isPublicEndpoint(String path) {
        return path.contains("/api/auth/") ||
               path.contains("/api/products") && requestContext.getMethod().equals("GET") ||
               path.contains("/health");
    }
}
```

---

## CORS (Cross-Origin Resource Sharing)

**Problem:** React Frontend (localhost:3000) ruft Backend (localhost:8080) auf

**Lösung: CORS Filter**

```java
@Provider
public class CorsFilter implements ContainerResponseFilter {
    
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) {
        response.getHeaders().add("Access-Control-Allow-Origin", "*");  // In dev: *, in prod: specific origin
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        response.getHeaders().add("Access-Control-Max-Age", "86400");  // 24 hours
    }
}
```

**Verwendung:**
```bash
# Client Request (Browser)
curl -X GET http://localhost:8080/api/products \
  -H "Origin: http://localhost:3000"

# Server Response headers:
# Access-Control-Allow-Origin: *
# Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS
```

---

## Häufige Sicherheitsprobleme (und Vermeidung)

| Problem | ❌ Falsch | ✅ Richtig |
|---------|-----------|-----------|
| Klartext-Passwort | `user.password = "abc123"` | `user.passwordHash = encoder.encode("abc123")` |
| Token hardcodiert | Secret in Code | Secret aus Environment: `System.getenv("JWT_SECRET")` |
| Token in URL | `http://...?token=abc123` | Header: `Authorization: Bearer abc123` |
| Keine Expiration | JWT ohne `exp` claim | Immer `exp` setzen (z.B. 1 hour) |
| Falsche Signatur | Keine Verifikation | HMAC-SHA256 verifizieren |
| SQL Injection | `"SELECT * FROM user WHERE username='" + input + "'"` | JPQL mit Parametern: `:username` |
| Sensitive Daten in Response | `{..., "passwordHash": "..."}` | Nur DTO mit notwendigen Feldern |
| CORS zu offen | `Access-Control-Allow-Origin: *` (in prod) | Spezifische Origins in Production |

---

## Best Practices (diese Anwendung)

✅ **PBKDF2 mit 100.000 Iterationen:** Rechenintensiv, schwer zu brute-forcen
✅ **JWT mit HMAC-SHA256:** Tamper-proof, stateless
✅ **Token Expiration:** Kurze Gültig (1 hour), nach Expiration neu einloggen
✅ **Bearer-Token in Authorization Header:** HTTP-Standard
✅ **Keine Passwörter in Logs:** Sensitive Data ist gekürzt
✅ **Separate DTO für REST:** Entity hat passwordHash, DTO nicht
✅ **Role-Based Authorization:** CUSTOMER vs. ADMIN Rollen
✅ **Bean Validation:** @NotBlank, @Email, @Size Constraints
✅ **Fehlerbehandlung:** Keine Stacktraces an Client, generische Error-Messages

---

## Deployment-Sicherheit

```env
# .env (NOT IN GIT!)
JWT_SECRET=<very-long-random-secret-key-min-32-chars>
DB_PASSWORD=<secure-db-password>

# In production:
# - Use secret management (HashiCorp Vault, AWS Secrets Manager, etc.)
# - Rotate secrets regularly
# - Use HTTPS/TLS for all communication
# - Set secure HTTP headers (CSP, HSTS, etc.)
```

---

## Testing Security

```java
@DisplayName("Security Tests")
class AuthSecurityTest {
    
    @Test
    void testPasswordHashingIsDifferent() {
        String plain = "password123";
        String hash1 = encoder.encode(plain);
        String hash2 = encoder.encode(plain);
        
        // Different salts → different hashes (but both valid)
        assertNotEquals(hash1, hash2);
        assertTrue(encoder.matches(plain, hash1));
        assertTrue(encoder.matches(plain, hash2));
    }
    
    @Test
    void testInvalidTokenIsRejected() {
        String invalidToken = "invalid.token.here";
        assertNull(tokenService.validateToken(invalidToken));
    }
    
    @Test
    void testExpiredTokenIsRejected() {
        long pastExpiration = (System.currentTimeMillis() / 1000) - 3600;
        String expiredToken = createTokenWithExpiration(pastExpiration);
        
        assertNull(tokenService.validateToken(expiredToken));
    }
    
    @Test
    void testUnauthorizedAccessRejected() {
        Response response = client.target(baseURI)
            .path("orders")
            .request()
            .get();
        
        assertEquals(401, response.getStatus());
    }
    
    @Test
    void testAdminOnlyEndpointRequiresRole() {
        Response response = client.target(baseURI)
            .path("products")
            .request()
            .header("Authorization", "Bearer " + customerToken)
            .post(Entity.json(productDto));
        
        assertEquals(403, response.getStatus());
    }
}
```

---

## Lernziele

✅ Authentifizierung (Login mit Username/Password)
✅ JWT Tokens (Stateless Authentication)
✅ Passwort-Hashing (PBKDF2)
✅ Token-Validierung (HMAC-SHA256)
✅ Bearer Token Authentication
✅ Role-Based Authorization (CUSTOMER/ADMIN)
✅ CORS Configuration
✅ Exception Handling für Security
✅ Keine Verwendung von Spring Security (Jakarta Standard)

