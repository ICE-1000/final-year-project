# Inventory System

Spring Boot backend and static HTML/CSS/JS frontend for inventory, allocation, barcode, and reporting workflows, built for UNZA.

## Backend

```powershell
cd backend
mvn clean package
mvn spring-boot:run
```

Set these environment variables (see `_env.example` for the full list with descriptions):

```text
DB_URL=jdbc:postgresql://host:5432/postgres?sslmode=require
DB_USERNAME=...
DB_PASSWORD=...
DB_POOL_SIZE=5            # optional, defaults to 5
JWT_SECRET=...             # must decode/encode to at least 64 bytes - generate with: openssl rand -base64 64
PORT=8080
CORS_ALLOWED_ORIGINS=...   # comma-separated, no trailing slashes
```

The backend uses `spring.jpa.hibernate.ddl-auto=update`, so tables are created automatically. The reference schema is in `database/schema.sql`; anything not yet applied to your live database is in `database/MIGRATION-add-constraints.sql` - read the comments in that file before running it.

### Creating the first admin account

`POST /api/auth/register` requires an already-authenticated ADMIN caller (see `SecurityConfig` and `CHANGES-IN-THIS-PASS.md`), so on a brand-new deployment with an empty `users` table, it can't be used to create the very first admin - that would be a chicken-and-egg problem.

Instead, set these three environment variables and start the app once:

```text
ADMIN_BOOTSTRAP_USERNAME=admin
ADMIN_BOOTSTRAP_EMAIL=admin@example.com
ADMIN_BOOTSTRAP_PASSWORD=ChangeMeNow123!
```

`AdminBootstrapConfig` runs on startup, checks whether any ADMIN already exists, and creates one from these values only if none does - it's a complete no-op on every subsequent restart, or on a deployment that already has an admin. Once you've logged in as that first admin, you can create further admins normally through the Users page (which calls `POST /api/auth/register` with a valid ADMIN token) and can unset the bootstrap env vars.

## Frontend

Serve the `frontend/` folder with a local static server (e.g. `npx serve frontend`, VS Code Live Server, or similar) rather than opening `index.html` directly by double-clicking it. `frontend/js/api.js` picks the backend URL based on `window.location.hostname`, and a `file://` URL doesn't match `localhost`/`127.0.0.1`, so opening the file directly silently points you at the deployed backend even when you meant to test locally.

For non-localhost usage the frontend defaults to the deployed backend `https://final-year-project-oref.onrender.com/api`; for local development the default is `http://localhost:8080/api`. Override either in the browser console with:

```javascript
localStorage.setItem('apiBase', 'https://your-backend.example.com/api')
```

## Project layout

```
backend/     Spring Boot 2.7.18 API (Java 17)
frontend/    Static HTML/CSS/JS - no build step
database/    Reference schema + migrations to run against Supabase/Postgres
```

See `CHANGES-IN-THIS-PASS.md` for the full backend/frontend/database review-and-fix
pass, and `UNIT-TRACKING-FEATURE.md` for the individual-unit tracking, brand/
specification, and barcode label printing feature.
