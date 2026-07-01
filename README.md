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
spring.datasource.url=jdbc:postgresql://
spring.datasource.username=
spring.datasource.password=
```

---

## Gmail App Password Setup

1. Google Account → Security → 2-Step Verification → **App Passwords**
2. Create one for "Mail"
3. Update `application.properties`:
```properties
spring.mail.username=
spring.mail.password=xxxxxxxxxxxx
spring.mail.from=
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

## Leave Status Flow

```
PENDING_PARENT ──► PENDING_WARDEN ──► PENDING_DEAN ──► APPROVED ──► COMPLETED
       │                  │                  │
       ▼                  ▼                  ▼
PARENT_REJECTED    WARDEN_REJECTED    DEAN_REJECTED
```
