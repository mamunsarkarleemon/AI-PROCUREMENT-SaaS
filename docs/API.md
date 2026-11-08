# ProcureAI REST API

Base URL: `http://localhost:8081/api/v1` · Swagger UI: http://localhost:8081/swagger-ui.html
Postman collection: [postman/ProcureAI.postman_collection.json](postman/ProcureAI.postman_collection.json)

**Auth:** call `POST /auth/login`, then send `Authorization: Bearer <token>` on every other request.
Money is in QAR, sent as numbers (e.g. `85.5`). Dates are `YYYY-MM-DD`.

**Errors** always look like this:
```json
{ "timestamp": "2026-11-08T06:30:00Z", "status": 400, "message": "Please check the highlighted fields.",
  "fieldErrors": { "email": "Enter a valid email" } }
```
`401` = not logged in / token expired / user inactive · `403` = your role or menu does not allow this · `404` = not found or not yours · `409` = conflict (e.g. email already used).

Access column: **Public** = no login · **Any** = any logged-in user · role names = only that role.
`[menu]` = the admin must have that menu switched on for the role (Admin → Menu Access).

---

## Public (landing page)
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/public/stats` | Public | Counts of suppliers, buyers, products, open requests, orders |
| GET | `/public/products?limit=8` | Public | Latest active products |
| GET | `/public/categories` | Public | All categories |
| POST | `/public/contact` | Public | Contact form `{name, email, subject, message}` |

## Auth & profile
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/auth/register` | Public | Create account (role `BUYER` or `SUPPLIER`) |
| POST | `/auth/login` | Public | `{email, password}` → `{token, user}` |
| GET | `/me` | Any | My profile |
| PUT | `/me` | Any | Update profile `{fullName, phone, companyName, address, city, country, about}` |
| PUT | `/me/password` | Any | `{currentPassword, newPassword}` |
| GET | `/me/menus` | Any | Sidebar menus allowed for my role |

Register body:
```json
{ "fullName": "Aisha Rahman", "email": "aisha@dohafoods.qa", "password": "secret1", "role": "BUYER",
  "phone": "+974 5555 1234", "companyName": "Doha Foods LLC", "city": "Doha", "country": "Qatar" }
```

## Dashboard & reports
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/dashboard` | Any | Role-specific stat cards, 30-day chart, recent orders |
| GET | `/reports?from=2026-10-01&to=2026-10-31` | Any `[reports]` | Summary, daily chart, by supplier / buyer / product / status, orders |
| GET | `/reports?...&supplierId=2&buyerId=5` | Admin | Same, filtered by supplier and/or buyer |

## Products
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/products?q=box&categoryId=1` | Buyer/Supplier `[marketplace]`, Admin | Marketplace search |
| GET | `/products/{id}` | Any | Product details with photos/videos |
| GET | `/products/mine` | Supplier `[my_products]` | My products |
| POST | `/products` | Supplier `[my_products]` | Create |
| PUT | `/products/{id}` | Owner supplier, Admin | Update |
| PUT | `/products/{id}/status` | Owner supplier, Admin | `{status: "ACTIVE" \| "INACTIVE"}` |
| DELETE | `/products/{id}` | Owner supplier, Admin | Delete |
| POST | `/products/{id}/media` | Owner supplier, Admin | Upload files (multipart field **files**, repeat for several) |
| DELETE | `/media/{id}` | Uploader, Admin | Delete one photo/video |
| GET | `/admin/products` | Admin | All products incl. hidden |

Product body:
```json
{ "name": "Kraft Paper Food Box 750ml", "description": "Grease resistant", "categoryId": 1,
  "unit": "carton", "price": 85.00, "stockQty": 400, "minOrderQty": 10, "status": "ACTIVE" }
```
Uploads: JPG/PNG/WEBP/GIF up to 5 MB, MP4/WEBM/MOV up to 50 MB. Files are served at `http://localhost:8081/uploads/...`.

## Buying requests & quotes
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/requests?q=&categoryId=` | Buyer/Supplier `[marketplace]`, Admin | Open requests |
| GET | `/requests/mine` | Buyer `[my_requests]` | My requests (with quote counts) |
| GET | `/requests/{id}` | Any | Details. Owner/admin see all quotes; a supplier sees only its own |
| POST | `/requests` | Buyer `[my_requests]` | Post a request (all suppliers are notified) |
| PUT | `/requests/{id}` | Owner buyer, Admin | Edit (while open) |
| POST | `/requests/{id}/close` | Owner buyer, Admin | Close |
| DELETE | `/requests/{id}` | Owner buyer, Admin | Delete with its quotes |
| POST | `/requests/{id}/media` | Owner buyer | Reference photos (multipart **files**) |
| GET | `/admin/requests` | Admin | All requests |
| POST | `/requests/{id}/quotes` | Supplier | Send or update my quote |
| GET | `/quotes/mine` | Supplier `[my_quotes]` | My quotes |
| POST | `/quotes/{id}/media` | Quote owner | Photos/video for the quote |
| POST | `/quotes/{id}/accept` | Request owner | Accept → **creates an order**, closes request, declines other quotes |
| POST | `/quotes/{id}/reject` | Request owner | Decline |
| POST | `/quotes/{id}/withdraw` | Quote owner | Withdraw (while pending) |

Request body: `{ "title", "description", "categoryId", "quantity", "unit", "targetPrice", "neededBy": "2026-12-01", "deliveryCity" }`
Quote body: `{ "pricePerUnit": 44.0, "quantity": 200, "deliveryDays": 14, "productId": null, "message": "..." }`

## Negotiations
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/negotiations` | Buyer/Supplier `[negotiations]`, Admin | My negotiations (admin: all) |
| GET | `/negotiations/{id}` | Party, Admin | With full message history |
| POST | `/negotiations` | Buyer `[negotiations]` | First offer `{productId, quantity, pricePerUnit, message}` |
| POST | `/negotiations/{id}/counter` | The side whose turn it is | `{quantity, pricePerUnit, message}` |
| POST | `/negotiations/{id}/accept` | The side whose turn it is | Accept → **creates an order** at the agreed price |
| POST | `/negotiations/{id}/reject` | Either side | End the negotiation |

The response has `myTurn: true` when you must answer.

## Orders
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/orders?status=PENDING` | Any `[orders]` | Buyer: mine · Supplier: received · Admin: all |
| GET | `/orders/{id}` | Buyer, supplier of the order, Admin | Details. `nextStatuses` = what you may change it to |
| POST | `/orders` | Buyer | Buy now `{productId, quantity, note}` (checks MOQ and stock) |
| PUT | `/orders/{id}/status` | See rules | `{status}` |

Status rules: Supplier `PENDING→CONFIRMED` (stock reduced), `CONFIRMED→SHIPPED`, `PENDING/CONFIRMED→CANCELLED` (stock returned) · Buyer `PENDING→CANCELLED`, `SHIPPED→DELIVERED` · Admin: any.

## Notifications
| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| GET | `/notifications` | Any | `{unread, items[]}` latest 30 |
| POST | `/notifications/{id}/read` | Any | Mark one read |
| POST | `/notifications/read-all` | Any | Mark all read |

## Admin
| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/admin/users?role=SUPPLIER&status=ACTIVE&q=doha` | List / search users |
| POST | `/admin/users` | Create user (any role) |
| PUT | `/admin/users/{id}` | Update (empty `password` keeps the old one) |
| PUT | `/admin/users/{id}/status` | `{status: "ACTIVE" \| "INACTIVE"}` — inactive users are logged out at once |
| DELETE | `/admin/users/{id}` | Delete (409 if the user has orders → deactivate instead) |
| GET | `/admin/menu-access` | Menu × role matrix |
| PUT | `/admin/menu-access/{menuKey}/{BUYER\|SUPPLIER}` | `{enabled: true \| false}` |
| POST / PUT / DELETE | `/admin/categories[/{id}]` | `{name}` |
| GET / DELETE | `/admin/messages[/{id}]` | Contact-form messages |

Menu keys: `dashboard`, `marketplace`, `my_products`, `my_requests`, `my_quotes`, `negotiations`, `orders`, `reports`, `profile` (dashboard and profile are always on).
