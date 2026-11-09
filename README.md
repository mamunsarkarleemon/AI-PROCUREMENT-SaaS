# ProcureAI — B2B Procurement Marketplace

A web platform for Qatar SMEs. **Buyers** find products, post buying requests, negotiate prices and order.
**Suppliers** list products (with photos and short videos), send quotes and fulfil orders.
**Admins** manage users, menus per role, categories and see platform-wide reports.

| Part | Stack | Folder |
|------|-------|--------|
| Backend | Java 17, Spring Boot 4.1, Spring Security (JWT), JPA, Flyway | [Procurement_Backend](Procurement_Backend) |
| Frontend | React 19, TypeScript, Vite, React Router, Recharts, Lucide icons | [Procurement_Frontend](Procurement_Frontend) |
| Database | PostgreSQL 18 | `procurement_db` |

API reference: [docs/API.md](docs/API.md) · Postman: [docs/postman/ProcureAI.postman_collection.json](docs/postman/ProcureAI.postman_collection.json)

---

## Run it

### 1. One-time setup
- PostgreSQL must have an empty database: `CREATE DATABASE procurement_db;`
- `Procurement_Backend/config/local.properties` (git-ignored) must contain your DB login and a JWT secret.
  Copy `config/local.properties.example` if it does not exist.

### 2. Backend → http://localhost:8081
```powershell
cd D:\Procurement_SaaS\Procurement_Backend
.\mvnw.cmd spring-boot:run
```
On the first start it automatically:
1. creates all tables (Flyway migration `V1__initial_schema.sql`)
2. creates the admin account
3. loads demo data (suppliers, buyers, products, orders…)

Ready when you see `Started ProcurementApplication`.

### 3. Frontend → http://localhost:5173
```powershell
cd D:\Procurement_SaaS\Procurement_Frontend
npm install      # first time only
npm run dev
```

### Logins

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@procureai.com | Admin@123 |
| Buyer (demo) | buyer1@demo.com · buyer2@demo.com | Demo@123 |
| Supplier (demo) | supplier1@demo.com · supplier2@demo.com · supplier3@demo.com | Demo@123 |

Or click **Create account** and register your own Buyer / Supplier.
The login page has one-click buttons that fill in the demo accounts.

Demo data is only loaded into an empty database. To start with no demo data set `app.seed-demo-data=false` in `application.properties`.

---

## What each role can do

| | Buyer | Supplier | Admin |
|---|---|---|---|
| Dashboard with stats, 30-day chart, recent orders | ✔ | ✔ | ✔ |
| **Marketplace** – top rated, top selling, all products (paged) + open buying requests | ✔ | ✔ | ✔ |
| **Buy now** at the listed price | ✔ | | |
| **Negotiate** – offer → counter-offer → accept/reject → order | ✔ | ✔ | view |
| **Post a buying request** (with reference photos) | ✔ | | |
| **Send quotes** on buying requests (price, qty, delivery days, photos/video) | | ✔ | |
| Compare quotes and **accept one → order** | ✔ | | |
| **My products** – add/edit/hide/delete, photos + short video, stock, MOQ | | ✔ | all products |
| **Orders** – pending → confirmed → shipped → delivered / cancelled | own | own | all |
| **Receive partly** – accept the good units, pay for those, send the rest back | ✔ | | |
| **Confirm a return** – returned units go back into stock | | ✔ | ✔ |
| **Rate and review** received products (1–5 stars) | ✔ | | |
| **React** to other people's reviews | ✔ | ✔ | ✔ |
| **Reports** by date range, supplier, buyer, product + CSV export | own | own | all + filters |
| Profile & password | ✔ | ✔ | ✔ |
| **Users** – create, edit, activate/deactivate, delete | | | ✔ |
| **Menu access** – choose which menus buyers / suppliers see | | | ✔ |
| Categories, contact messages | | | ✔ |
| Notifications (bell icon) | ✔ | ✔ | ✔ |

Order rules: the **supplier** confirms (stock is reduced) and ships; the **buyer** marks it received; either can cancel while pending (supplier also while confirmed — stock is given back).

Partial receipt: if 100 were ordered and 20 arrive damaged, the buyer accepts 80 and pays for 80. The other 20 go back to the supplier and return to stock when the supplier confirms them.

---

## Database tables

```
users ─┬─< products ─┬─< media_files            (photos / videos)
       │             └─< negotiations ─< negotiation_messages
       ├─< buy_requests ─┬─< quotes ─< media_files
       │                 └─< media_files
       ├─< orders           (buyer_id, supplier_id, product_id, quote_id, negotiation_id)
       └─< notifications
categories ─< products, buy_requests
menus ─< role_menu_access   (which role sees which menu)
contact_messages            (landing page contact form)
```

| Table | What it stores |
|-------|----------------|
| `users` | Buyers, suppliers, admins + company details. Password is a BCrypt hash. |
| `categories` | Product categories (seeded, admin can edit). |
| `products` | Supplier products: price, unit, stock, MOQ, status. |
| `media_files` | Uploaded photos/videos, linked to a product, request or quote. Files are in `Procurement_Backend/uploads/`. |
| `buy_requests` | What a buyer wants to buy: quantity, budget, needed-by date. |
| `quotes` | A supplier's answer to a buy request. |
| `negotiations` / `negotiation_messages` | Price bargaining on a product and every offer in it. |
| `orders` | Orders with price snapshot, status, and received/returned quantities. |
| `product_reviews` | A buyer's 1–5 star rating and comment on a product they received. |
| `review_reactions` | Like / love / helpful / dislike on a review, one per user. |
| `notifications` | Messages under the bell icon. |
| `menus` / `role_menu_access` | Sidebar menus and which role may use them. |
| `contact_messages` | Messages from the website's contact form. |

---

## Security (kept simple on purpose for now)
- Passwords are stored as **BCrypt hashes**; everything else is plain readable data.
- Login returns a **JWT token** (valid 12 hours) that the frontend sends as `Authorization: Bearer …`.
- Every request re-reads the user from the database, so **deactivating a user or changing menu access works immediately**.
- Roles and menu permissions are checked on the server too, not only hidden in the UI.

## Tests
```powershell
cd Procurement_Backend;  .\mvnw.cmd test      # unit tests (one Docker-based test is skipped without Docker)
cd Procurement_Frontend; npm run lint; npm run build
```

## Next ideas
Email verification & password reset · rate-limiting login · cloud storage for uploads · AI specification extraction · freight & landed-cost estimates · shipment tracking (see the blueprint PDF).

---

## Project vision

*Intelligent Procurement & Supplier Management Platform*

AI Procurement SaaS is a multi-company procurement management platform designed to simplify purchasing operations, supplier discovery, price comparison, and procurement workflows.

The platform aims to connect buyers and suppliers through an intelligent system that improves procurement efficiency, transparency, and cost optimization.

### 🎯 Project Overview

The goal of this project is to develop a centralized procurement platform where businesses can manage their purchasing activities, discover suppliers, compare quotations, and track procurement operations.

The system is designed to support multiple companies, buyers, and suppliers with role-based access and centralized management.

### ✨ Key Features

#### 🏢 Company Management

- Multi-company registration and management.
- Buyer and supplier profiles.
- Company information and verification.

#### 🤝 Supplier Management

- Supplier registration and onboarding.
- Supplier discovery and search.
- Supplier profile management.
- Supplier performance tracking.

#### 📦 Product & Specification Management

- Product and service catalog.
- Technical specification management.
- Product requirement submission.
- Supplier product matching.

#### 💰 Price Comparison

- Multiple supplier quotation comparison.
- MOQ (Minimum Order Quantity) comparison.
- Product price analysis.
- Cost optimization.

#### 📋 RFQ Management

- Request for Quotation (RFQ).
- Supplier quotation submission.
- Quotation comparison.
- Procurement decision management.

#### 🚚 Logistics & Shipment

- Freight cost estimation.
- Landed cost calculation.
- Shipment tracking.
- Delivery status monitoring.

#### 📄 Certificate Management

- Supplier document management.
- Certificate verification.
- Compliance tracking.

#### 🤖 AI-Powered Procurement

- Intelligent supplier recommendations.
- Automated product and specification matching.
- Smart quotation analysis.
- Procurement insights and cost optimization.
