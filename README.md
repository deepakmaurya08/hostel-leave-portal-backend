# Smart Hostel Leave Management Portal — AKGEC Ghaziabad
### Spring Boot 3.4.1 · Java 24 · PostgreSQL

---

## ⚡ Quick Fix for TypeTag::UNKNOWN Error

Your error log showed: **`javac 24.0.2 was used to compile java sources`**

This is a **Java 24 + old Lombok** incompatibility. This ZIP contains the fix.

### What was fixed

| Item | Old (broken) | New (fixed) | Why |
|------|-------------|-------------|-----|
| Lombok | 1.18.30–1.18.34 | **1.18.36** | First Lombok release supporting Java 24 |
| MapStruct | 1.5.5.Final | **1.6.3** | Java 24 compatible |
| Spring Boot | 3.2.5 | **3.4.1** | Latest stable, tested on Java 24 |
| `<java.version>` | 21 | **24** | Matches your installed JDK |
| compiler `-proc:full` | missing | **added** | Java 24 changed annotation proc defaults |
| `.mvn/jvm.config` | basic | **opens jdk.compiler** | Annotation processors need this on Java 24 |

---

## IntelliJ IDEA — Do These Steps After Opening the Project

### Step 1 — Set Project SDK
```
File → Project Structure (Ctrl+Alt+Shift+S)
  → Project → SDK → Select Java 24  (or whatever you have installed)
  → Language Level → 24
→ Apply → OK
```

### Step 2 — Annotation Processors
```
Settings (Ctrl+Alt+S)
  → Build, Execution, Deployment
    → Compiler
      → Annotation Processors
        ✅ Enable annotation processing
        ● Obtain processors from project classpath   ← select THIS
→ Apply → OK
```

### Step 3 — Reload Maven + Rebuild
```
1. Maven panel (right side) → click "Reload All Maven Projects" (refresh icon)
2. File → Invalidate Caches → Invalidate and Restart
3. After restart: Build → Rebuild Project
```

---

## PostgreSQL Setup

```sql
CREATE DATABASE hostel_leave_db;
CREATE USER hostel_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE hostel_leave_db TO hostel_user;
```

Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/hostel_leave_db
spring.datasource.username=hostel_user
spring.datasource.password=your_password
```

---

## Gmail App Password Setup

1. Google Account → Security → 2-Step Verification → **App Passwords**
2. Create one for "Mail"
3. Update `application.properties`:
```properties
spring.mail.username=your_gmail@gmail.com
spring.mail.password=xxxxxxxxxxxx
spring.mail.from=AKGEC Hostel <your_gmail@gmail.com>
```

---

## Run

```bash
# Option A: Maven
mvn clean spring-boot:run

# Option B: Build JAR
mvn clean package -DskipTests
java -jar target/hostel-leave-portal-1.0.0.jar
```

---

## Default Users (Auto-Seeded on First Start)

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@akgec.ac.in | Admin@1234 |
| Warden | warden@akgec.ac.in | Warden@1234 |
| Dean | dean@akgec.ac.in | Dean@1234 |
| Security | security@akgec.ac.in | Security@1234 |

Students register via `POST /api/auth/register/student`

---

## API Reference

### Public
| Method | URL | Body |
|--------|-----|------|
| POST | `/api/auth/login` | `{email, password}` |
| POST | `/api/auth/register/student` | student fields |
| GET | `/api/parent/approve?token=xxx` | — |
| GET | `/api/parent/reject?token=xxx` | — |

### Student `(Bearer token)`
| Method | URL | Notes |
|--------|-----|-------|
| GET | `/api/student/dashboard` | Stats |
| POST | `/api/student/leave/apply` | Multipart form + docs |
| GET | `/api/student/leaves` | All my leaves |
| GET | `/api/student/leaves/{id}` | With timeline |
| DELETE | `/api/student/leaves/{id}/cancel` | Cancel pending |
| GET | `/api/student/leaves/{id}/download-pdf` | PDF download |

### Warden
| Method | URL | Notes |
|--------|-----|-------|
| GET | `/api/warden/dashboard` | — |
| GET | `/api/warden/leaves/pending` | — |
| POST | `/api/warden/leaves/{id}/approve` | `{remarks, workingDaysCount, wardenParentCommTime}` |
| POST | `/api/warden/leaves/{id}/reject` | `{remarks}` |

### Dean
| Method | URL | Notes |
|--------|-----|-------|
| GET | `/api/dean/dashboard` | — |
| GET | `/api/dean/leaves/pending` | — |
| POST | `/api/dean/leaves/{id}/approve` | Triggers QR + PDF |
| POST | `/api/dean/leaves/{id}/reject` | `{remarks}` |

### Security Guard
| Method | URL | Notes |
|--------|-----|-------|
| GET | `/api/security/dashboard` | — |
| GET | `/api/security/scan?qrToken=xxx` | Verify QR |
| POST | `/api/security/mark-exit` | `{qrToken, remarks}` |
| POST | `/api/security/mark-entry` | `{qrToken, remarks}` |
| GET | `/api/security/on-leave` | Currently outside |

### Admin
| Method | URL | Notes |
|--------|-----|-------|
| POST | `/api/admin/users` | Create staff |
| GET | `/api/admin/users` | All users |
| PATCH | `/api/admin/users/{id}/toggle-active` | Enable/disable |
| PATCH | `/api/admin/users/{id}/reset-password` | `{newPassword}` |
| GET | `/api/admin/reports/system` | System report |
| GET | `/api/admin/audit-logs` | Audit trail |

---

## Leave Status Flow

```
PENDING_PARENT ──► PENDING_WARDEN ──► PENDING_DEAN ──► APPROVED ──► COMPLETED
       │                  │                  │
       ▼                  ▼                  ▼
PARENT_REJECTED    WARDEN_REJECTED    DEAN_REJECTED
```
