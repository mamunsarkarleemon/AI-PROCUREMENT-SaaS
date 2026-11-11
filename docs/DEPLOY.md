# Deploy ProcureAI for free

| Part | Free host | What you get |
|---|---|---|
| Database | [Supabase](https://supabase.com) | PostgreSQL, 500 MB |
| Backend (Spring Boot) | [Render](https://render.com) | Docker web service, 512 MB |
| Frontend (React) | [Vercel](https://vercel.com) | Static site + forwards `/api` and `/uploads` to Render |

The browser only talks to Vercel. Vercel passes `/api/...` and `/uploads/...` on to Render (see `Procurement_Frontend/vercel.json`), so no CORS setup and no code change is needed.

Files used: `Procurement_Backend/Dockerfile`, `Procurement_Frontend/vercel.json`, and `render.yaml` (only if you use a Render Blueprint).

Sign up to all three with **Continue with GitHub**. No credit card is needed.

---

## 1. Database: Supabase (about 3 minutes)

1. Supabase → **New project**.
   - Name: `procureai`
   - Database password: click **Generate a password** and **save it somewhere**. You need it in step 2.
   - Region: **Central EU (Frankfurt)**, the same region as the Render backend.
   - Under **Security options**, **untick "Enable Data API"**. This app does not use it. If it stays on, anyone with the project's public key can read tables through Supabase's REST API. If the project already exists: **Project Settings → Data API → disable**.
2. Wait until the project is ready, then click **Connect** at the top.
3. Pick **Session pooler**. Do not pick Direct connection: it only works over IPv6, which Render does not have. Do not pick Transaction pooler either: database migrations do not work through it.
4. Open **View parameters** and note four values:

| Value | Example |
|---|---|
| Host | `aws-0-eu-central-1.pooler.supabase.com` (it may start with `aws-1-`) |
| Port | `5432` |
| Database | `postgres` |
| User | `postgres.abcdefghijklmnop` (with the project id after the dot) |

5. Write the **JDBC URL** for the backend (Java needs this form, not the `postgresql://user:pass@...` one):

```
jdbc:postgresql://<Host>:5432/postgres?sslmode=require
```

The tables are created automatically when the backend starts. Leave the database empty.

## 2. Backend: Render (about 10 minutes, mostly waiting)

1. Render → **New** → **Web Service** → choose the `AI-PROCUREMENT-SaaS` repository.
2. Fill in:

| Field | Value |
|---|---|
| Name | `procureai-api` |
| Region | **Frankfurt (EU Central)** |
| Branch | `main` |
| Root Directory | `Procurement_Backend` |
| Language | **Docker** |
| Dockerfile Path | `./Dockerfile` (keep the default) |
| Instance Type | **Free** |

3. **Environment Variables** → add:

| Key | Value |
|---|---|
| `DB_URL` | the JDBC URL from step 1 |
| `SPRING_DATASOURCE_USERNAME` | Supabase user, `postgres.xxxxxxxx` |
| `DB_PASSWORD` | Supabase database password |
| `APP_ADMIN_PASSWORD` | a new strong password for `admin@procureai.com` (not `Admin@123`, which is public in the README) |
| `JWT_SECRET` | click **Generate**, or paste any random text of 32+ characters |
| `SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE` | `5` |

4. **Advanced** → **Health Check Path**: `/actuator/health`.
5. Click **Deploy Web Service**. The first build takes 5–10 minutes.
6. When it shows **Live**, open `https://procureai-api.onrender.com/actuator/health`. It should show `{"status":"UP"}`.

> **Check the address.** Render shows the service address at the top left of the service page. If it is not exactly `procureai-api.onrender.com` (for example `procureai-api-x1y2.onrender.com`), put that address in both `/api` and `/uploads` lines of `Procurement_Frontend/vercel.json`, then commit and push.

*Blueprint alternative:* Render → **New** → **Blueprint** → this repo. Render reads `render.yaml` and asks only for the four secret values above.

## 3. Frontend: Vercel (about 3 minutes)

1. Vercel → **Add New… → Project** → import `AI-PROCUREMENT-SaaS`.
2. **Root Directory**: click Edit and choose `Procurement_Frontend`. The framework is detected as **Vite**; keep the build settings as they are.
3. Click **Deploy**.

Vercel redeploys on every push to `main`, so a push that changes `vercel.json` is picked up automatically.

## 4. Check it

1. Open the backend health link from step 2.6 first; that wakes the backend up.
2. Open the Vercel address. The numbers on the landing page should appear.
3. Log in as `buyer1@demo.com` / `Demo@123` and as `admin@procureai.com` with **your** admin password.
4. In Supabase → **Table Editor**, you should see the app's tables (`users`, `products`, `orders`, …).

From now on, every `git push` to `main` redeploys the backend and the frontend automatically.

---

## Good to know (free plan limits)

- **Render sleep:** Render stops the backend after 15 minutes with no visitors. The next visitor waits about a minute while it wakes up; the first request may fail once, so try again.
- **Supabase pause:** a free project with no activity for 7 days is paused. Resume it in the Supabase dashboard; data is kept.
- **Uploaded photos and videos are not kept.** Render's free disk is wiped whenever the service restarts, sleeps or redeploys. All other data (users, products, orders, reviews) is safe in Supabase. Keeping files needs cloud storage (for example Supabase Storage or Cloudinary); that is a later change.
- **Demo data** (demo suppliers, buyers, products and orders) is loaded on the first start, and the demo logins are public. To start empty, add `APP_SEED_DEMO_DATA` = `false` in Render → Environment **before the first start**.
- The **Admin** button in the demo box on the login page fills in `Admin@123`, so it will not work online. That is intended: the real admin password is the one you chose.
- Supabase's **Security Advisor** may warn "RLS disabled" on the app's tables. With the Data API turned off, nothing outside the backend can reach them, so the warning does not apply.
- **Vercel Hobby** is meant for personal, non-commercial projects.
- Your local setup does not change: `config/local.properties` and your local database are still used when you run on your PC.

## If something goes wrong

| Problem | Where to look / fix |
|---|---|
| Render build fails | Render → service → **Logs**. Check Root Directory is `Procurement_Backend` and Language is Docker. |
| `Network is unreachable` / `UnknownHostException` in the logs | You used the Direct connection host (`db.xxx.supabase.co`). Use the **Session pooler** host. |
| `password authentication failed` / `Tenant or user not found` | The user must be `postgres.<project id>` (with the dot part), and the password the database password, not your Supabase login. |
| `prepared statement "S_1" already exists` | You used the Transaction pooler (port `6543`). Use the Session pooler (port `5432`). |
| `Found non-empty schema(s) "public"` | Tables were created in Supabase by hand. Use a fresh project, or delete those tables. |
| `max clients reached` | `SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE` is missing; set it to `5`. |
| `app.jwt.secret must be at least 32 characters` | `JWT_SECRET` is missing or too short. |
| Site loads but login says "Something went wrong" | The backend is asleep or the address in `vercel.json` is wrong. Open the health link from step 2.6. |
| Page refresh shows 404 on Vercel | Root Directory must be `Procurement_Frontend` so `vercel.json` is used. |
