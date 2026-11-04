# ProcureAI — AI Procurement SaaS

A multi-company B2B platform for Qatar SMEs: buyers and suppliers register; the system will support specification extraction, supplier discovery, quote/MOQ comparison, certificate checks, freight and landed cost, RFQs and shipment tracking.

| Part | Stack | Folder |
|------|-------|--------|
| Backend | Java 17, Spring Boot 4.1, Spring Security (JWT resource server), JPA, Flyway | [Procurement_Backend](Procurement_Backend) |
| Frontend | React 19, TypeScript, Vite, React Router | [Procurement_Frontend](Procurement_Frontend) |
| Database | PostgreSQL 18 | `procurement_db` |

**Status:** user sign-up, login, JWT authentication and role-based access are done. API reference: [docs/API.md](docs/API.md).

## Run it locally

### 1. Database (one time)

Create the database in PostgreSQL (pgAdmin or psql):

```sql
CREATE DATABASE procurement_db;
```

Tables are created automatically by Flyway when the backend starts (`src/main/resources/db/migration`).

### 2. Backend secrets (one time)

Copy `Procurement_Backend/config/local.properties.example` to `Procurement_Backend/config/local.properties` (git-ignored) and set:

- `spring.datasource.username` / `spring.datasource.password` — your PostgreSQL login
- `app.jwt.secret` — at least 32 random characters, e.g. the output of `openssl rand -base64 48`

### 3. Start the backend (port 8081)

```powershell
cd Procurement_Backend
.\mvnw.cmd spring-boot:run
```

Ready when the log says `Started ProcurementApplication`. Check: http://localhost:8081/actuator/health → `{"status":"UP"}`

### 4. Start the frontend (port 5173)

```powershell
cd Procurement_Frontend
npm install
npm run dev
```

Open http://localhost:5173 → **Create an account** → sign in → dashboard.
In development, Vite forwards `/api/*` to `http://localhost:8081`.

### 5. Test the API with Postman

Import [docs/postman/ProcureAI-Auth.postman_collection.json](docs/postman/ProcureAI-Auth.postman_collection.json). Details in [docs/API.md](docs/API.md).

## Tests

```powershell
cd Procurement_Backend
.\mvnw.cmd test          # unit tests always run; integration tests need Docker (Testcontainers) and skip without it

cd ..\Procurement_Frontend
npm run lint
npm run build            # includes the TypeScript type-check
```

## Security design (blueprint Appendix A–K)

- Passwords: BCrypt via Spring Security `DelegatingPasswordEncoder`; never stored or logged in plain text.
- Tokens: HS256 JWT, 60-minute lifetime, issued and validated with Spring Security's Nimbus support. Signature, expiry, issuer and audience are all checked.
- Roles: public sign-up only ever gets platform role `USER` plus `BUYER_OWNER` or `SUPPLIER_OWNER`. `ADMIN` cannot be requested.
- Authorization: `@PreAuthorize("hasRole('BUYER')")` for role checks. Services take identity from the token, never from request bodies.
- API: stateless (no session cookie), so CSRF is disabled; CORS allows only `http://localhost:5173` (`CORS_ALLOWED_ORIGINS`).
- Errors: one JSON format with a `traceId`; no stack traces or SQL leak out.
- Browser: the access token is kept in `sessionStorage` and cleared on logout, on expiry, or when the tab closes.

## Next steps (roadmap)

1. Email verification and forgot/reset password (Day 12). `users.status` already has `PENDING_VERIFICATION`.
2. Refresh tokens in an HttpOnly cookie, so the access token can become shorter-lived and stop living in `sessionStorage`.
3. Rate limiting on login and register.
4. Company profile APIs and object-level tenant checks (Day 13, Day 22).
