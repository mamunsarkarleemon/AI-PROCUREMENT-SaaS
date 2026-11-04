# ProcureAI REST API — Authentication

Base URL (local): `http://localhost:8081`
Interactive docs (Swagger UI, dev only): `http://localhost:8081/swagger-ui.html`
Postman collection: [postman/ProcureAI-Auth.postman_collection.json](postman/ProcureAI-Auth.postman_collection.json)

All request and response bodies are JSON. Protected endpoints need the header:

```
Authorization: Bearer <accessToken>
```

## Endpoint summary

| # | Method | Endpoint | Access | Purpose |
|---|--------|----------|--------|---------|
| 1 | POST | `/api/v1/auth/register` | Public | Create a user, their company and an owner membership |
| 2 | POST | `/api/v1/auth/login` | Public | Check credentials, return a JWT access token |
| 3 | GET | `/api/v1/users/me` | Any signed-in user | The current user and their company memberships |
| 4 | GET | `/api/v1/buyer/dashboard` | Role `BUYER` | Buyer workspace (placeholder) — 403 for suppliers |
| 5 | GET | `/api/v1/supplier/dashboard` | Role `SUPPLIER` | Supplier workspace (placeholder) — 403 for buyers |
| 6 | GET | `/actuator/health` | Public | Health check |

---

## 1. Register

`POST /api/v1/auth/register`

```json
{
  "fullName": "Aisha Rahman",
  "email": "aisha@dohafoods.qa",
  "password": "Secret123",
  "accountType": "BUYER",
  "companyName": "Doha Foods LLC",
  "countryCode": "QA",
  "registrationNo": "CR-12345"
}
```

| Field | Rules |
|-------|-------|
| `fullName` | required, max 150 |
| `email` | required, valid email, max 320. Stored lower-case, unique |
| `password` | required, 8–72 chars, at least one letter and one digit |
| `accountType` | `BUYER` or `SUPPLIER` only. Anything else (e.g. `ADMIN`) → 400 |
| `companyName` | required, max 200 |
| `countryCode` | required, 2-letter ISO code (e.g. `QA`) |
| `registrationNo` | optional, max 100. Unique per country |

What the server does:
- Hashes the password with BCrypt (`{bcrypt}$2a$...`). The plain password is never stored.
- Platform role is always `USER`. Company role is `BUYER_OWNER` or `SUPPLIER_OWNER`.
- Buyer companies start `ACTIVE`; supplier companies start `PENDING_REVIEW` (admin review comes later).

**201 Created**

```json
{
  "id": "e0541825-49ab-4c5b-8b64-7254341c22f5",
  "email": "aisha@dohafoods.qa",
  "fullName": "Aisha Rahman",
  "platformRole": "USER",
  "status": "ACTIVE",
  "emailVerified": false,
  "createdAt": "2026-11-04T15:01:29.274699Z",
  "memberships": [
    {
      "companyId": "8417ceca-94f7-498b-82fc-22d44491273d",
      "companyName": "Doha Foods LLC",
      "companyType": "BUYER",
      "companyStatus": "ACTIVE",
      "countryCode": "QA",
      "role": "BUYER_OWNER"
    }
  ]
}
```

Errors: `400 VALIDATION_FAILED`, `400 MALFORMED_REQUEST`, `409 EMAIL_ALREADY_REGISTERED`, `409 COMPANY_ALREADY_REGISTERED`.

## 2. Login

`POST /api/v1/auth/login`

```json
{ "email": "aisha@dohafoods.qa", "password": "Secret123" }
```

**200 OK**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9....",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "...": "same shape as the register response" }
}
```

The token is valid for 60 minutes. Its payload looks like this:

```json
{
  "sub": "e0541825-49ab-4c5b-8b64-7254341c22f5",
  "email": "aisha@dohafoods.qa",
  "roles": ["USER", "BUYER", "BUYER_OWNER"],
  "iss": "procurement-saas",
  "aud": "procurement-api",
  "iat": 1793804490,
  "exp": 1793808090,
  "jti": "6c882304-5e82-4035-b758-6f7499d22348"
}
```

Errors: `401 INVALID_CREDENTIALS` (same response for an unknown email and a wrong password), `403 ACCOUNT_DISABLED`.

## 3. Current user

`GET /api/v1/users/me` with `Authorization: Bearer <token>` → **200** with the user object.
The user id always comes from the verified token, never from the request.

Errors: `401 UNAUTHORIZED` (no token), `401 INVALID_TOKEN` (expired, tampered, wrong issuer or audience).

## 4–5. Role-protected dashboards

`GET /api/v1/buyer/dashboard` → `{"area":"BUYER","message":"Welcome to the buyer workspace."}`
`GET /api/v1/supplier/dashboard` → the same shape for suppliers.
Calling the other role's endpoint returns `403 FORBIDDEN`.

---

## Error format (every error)

```json
{
  "timestamp": "2026-11-04T15:01:29.826Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "One or more fields are invalid.",
  "fieldErrors": [
    { "field": "password", "reason": "must contain at least one letter and one digit" }
  ],
  "traceId": "ea7d217c-f06b-466e-b19b-44ae0f35b03f"
}
```

`traceId` is also sent in the `X-Trace-Id` response header and written to the server logs, so you can find the log lines for any failed request.

| Code | HTTP | Meaning |
|------|------|---------|
| `VALIDATION_FAILED` | 400 | A field broke a rule; see `fieldErrors` |
| `MALFORMED_REQUEST` | 400 | Invalid JSON or an unknown enum value |
| `UNAUTHORIZED` | 401 | No token |
| `INVALID_TOKEN` | 401 | Token expired, tampered with, or not issued by this server |
| `INVALID_CREDENTIALS` | 401 | Wrong email or password |
| `ACCOUNT_DISABLED` | 403 | Correct password, but the account is not active |
| `FORBIDDEN` | 403 | Signed in, but the role is not allowed |
| `EMAIL_ALREADY_REGISTERED` | 409 | Email already in use |
| `COMPANY_ALREADY_REGISTERED` | 409 | Registration number already used in that country |

## Testing with Postman

1. Import `docs/postman/ProcureAI-Auth.postman_collection.json`.
2. Run **1. Register - Buyer**, then **2. Login (saves token)**.
3. Run **3**, **4** and **5**. The token is attached automatically (collection-level Bearer auth).
4. To register a second user, change the `email` collection variable first.
