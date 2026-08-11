# Inventory System

Spring Boot backend and static HTML/CSS/JS frontend for inventory, allocation, barcode, and reporting workflows.

## Backend

```powershell
cd backend
mvn clean package
mvn spring-boot:run
```

Set these environment variables for PostgreSQL (Supabase or other):

```text
DB_URL=jdbc:postgresql://host:5432/postgres?sslmode=require
DB_USERNAME=...
DB_PASSWORD=...
DB_POOL_SIZE=5            # optional, defaults to 5
JWT_SECRET=replace-with-a-long-random-secret
PORT=8080
```

The backend uses `spring.jpa.hibernate.ddl-auto=update`, so tables are created automatically. The matching SQL is in `database/schema.sql`.

## Frontend

Open `frontend/index.html` in a browser, or serve the folder with any static server. For non-localhost usage the frontend defaults to the deployed backend `https://final-year-project-oref.onrender.com/api`; for local development the default remains `http://localhost:8080/api`. Override it in the browser console with:

```javascript
localStorage.setItem('apiBase', 'https://your-backend.example.com/api')
```

Create the first admin user with `POST /api/auth/register`, then login from the frontend.
