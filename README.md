# Banking App

## Project Structure

```
banking-app/
├── frontend/   # Vue 3 + Vite
├── backend/    # Spring Boot + Maven (REST API + H2 Database)
```

---

## Getting Started

### Frontend (Vue 3 + Vite)

```bash
cd frontend
npm install
npm run dev
```

App runs at:  
[http://localhost:5173](http://localhost:5173)

---

### Backend (Spring Boot + H2)

```bash
cd backend
mvn spring-boot:run
```

API runs at:  
[http://localhost:8080](http://localhost:8080)

---

### Accessing the Database (H2 Console)

Once the backend is running, open:

[http://localhost:8080/h2-console](http://localhost:8080/h2-console)

**Login settings:**

| Field    | Value                        |
| -------- | ---------------------------- |
| JDBC URL | `jdbc:h2:file:./data/bankdb` |
| Username | `sa`                         |
| Password | _(leave blank)_              |

> The H2 database is file-based. Meaning data will not reset on restart.

> The database file is created at: `backend/data/bankdb.mv.db`

---

## Git Flow

1. Create feature branches from `development`
2. Commit and push your work regularly
3. Merge finished features into `development`
4. When `development` is stable, merge it into `main`

---

## Notes

- CORS is enabled for local frontend-backend integration (`http://localhost:5173`)
- Auth endpoints:
  - `POST /api/auth/register`
  - `POST /api/auth/login`
- Passwords are stored securely using BCrypt
- H2 console is enabled for easy development and debugging