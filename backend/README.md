# MyFSchool parent backend

Java 21 / Spring Boot 3.5 REST API for the parent Flutter application. It provides authentication, linked-student timetables, grades, attendance reports, and tuition information using MySQL 8, Spring Data JPA, BCrypt, and stateless HS256 JWT access tokens.

## Teacher API (implemented subset)

All endpoints below require a bearer JWT. Teacher endpoints require `ROLE_TEACHER` and derive the teacher identity from the token. Principal class assignment requires `ROLE_PRINCIPAL`.

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/v1/teacher/me` | Authenticated teacher profile |
| GET | `/api/v1/teacher/schedule?weekStart=YYYY-MM-DD` | Teaching entries for the requested week (defaults to current week) |
| GET | `/api/v1/teacher/classes` | Classes assigned to the authenticated teacher with roster count |
| GET | `/api/v1/teacher/classes/{assignmentId}/students?search=...` | Search assigned class by student name/code |
| GET | `/api/v1/teacher/classes/{assignmentId}/students/{studentId}` | Student detail, restricted to assigned class |
| GET | `/api/v1/teacher/classes/{assignmentId}/attendance?date=YYYY-MM-DD` | Class roster with attendance for a date |
| PUT | `/api/v1/teacher/classes/{assignmentId}/attendance?date=YYYY-MM-DD` | Upsert attendance marks for roster students |
| POST | `/api/v1/principal/teaching-assignments` | Assign an existing class to a teacher |

Attendance request body is an array of `{ "studentId": 1, "status": "PRESENT", "note": null }`; accepted statuses include `PRESENT`, `ABSENT`, `LATE`, and `EXCUSED` plus the legacy report statuses. Principal assignment body is `{ "teacherId": 1, "className": "10A", "schoolName": "MyFSchool" }`. An assignment can only be created for a teacher account and a class name/school that already has student profiles.

The current schema has no class or subject entities. `StudentProfile.className` and `schoolName` remain the canonical class grouping, and schedule entries store the teacher as free-text `teacherName`. Consequently this API subset does not provide a true teacher-to-subject or teacher-to-schedule foreign-key assignment, and should not be treated as complete school administration functionality.

## Run locally

Requirements: Java 21, Maven 3.9+, and Docker with Compose.

1. Start MySQL:

   ```powershell
   cd backend
   docker compose up -d mysql
   ```

2. Generate a signing key and export the runtime variables. `JWT_SECRET` must be a Base64-encoded value containing at least 32 random bytes.

   ```powershell
   $jwtBytes = New-Object byte[] 32
   $random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   $random.GetBytes($jwtBytes)
   $random.Dispose()
   $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
   $env:SPRING_PROFILES_ACTIVE = "dev"
   $env:DB_HOST = "localhost"
   $env:DB_PORT = "3306"
   $env:DB_NAME = "myfschool"
   $env:DB_USERNAME = "myfschool"
   $env:DB_PASSWORD = "myfschool_dev"
   mvn spring-boot:run
   ```

   The `dev` profile creates the school, parent role, parent profile, and this account once:

   - Username: `parent_demo`
   - Email: `parent@myfschool.local`
   - Password: `Password@123`
   - Role/status: `PARENT` / `ACTIVE`

   Never enable the `dev` profile in production. Supply a fresh production `JWT_SECRET`, database credentials, an HTTPS deployment, and explicit production CORS origins through `CORS_ALLOWED_ORIGIN_PATTERNS`.

3. Run the tests (they use an isolated in-memory H2 database in MySQL compatibility mode):

   ```powershell
   mvn test
   ```

## API examples

Login:

```powershell
curl.exe -i -X POST http://localhost:8080/api/v1/auth/login `
  -H "Content-Type: application/json" `
  -d '{"username":"parent_demo","password":"Password@123"}'
```

Successful response:

```json
{
  "code": 200,
  "message": "Login successful",
  "data": {
    "accessToken": "<signed-jwt>",
    "tokenType": "Bearer",
    "expiresIn": 3600,
    "user": {
      "id": 1,
      "username": "parent_demo",
      "phone": null,
      "email": "parent@myfschool.local",
      "roles": ["PARENT"],
      "status": "ACTIVE",
      "mustChangePassword": false
    }
  }
}
```

Authenticated user (replace the placeholder with the login token):

```powershell
curl.exe -i http://localhost:8080/api/v1/auth/me `
  -H "Authorization: Bearer <signed-jwt>"
```

Parent grade report and subject detail (`period` accepts `1`, `2`, or `all`):

```powershell
curl.exe -i "http://localhost:8080/api/v1/parents/me/grades?period=1" `
  -H "Authorization: Bearer <signed-jwt>"

curl.exe -i "http://localhost:8080/api/v1/parents/me/grades/MATH?period=1" `
  -H "Authorization: Bearer <signed-jwt>"
```

Parent timetable:

```powershell
curl.exe -i "http://localhost:8080/api/v1/parents/me/timetable?weekStart=2026-10-26" `
  -H "Authorization: Bearer <signed-jwt>"
```

Monthly attendance report (`month` uses `YYYY-MM`):

```powershell
curl.exe -i "http://localhost:8080/api/v1/parents/me/attendance?month=2026-11" `
  -H "Authorization: Bearer <signed-jwt>"
```

Paid, partially paid, and outstanding tuition fees:

```powershell
curl.exe -i "http://localhost:8080/api/v1/parents/me/tuition" `
  -H "Authorization: Bearer <signed-jwt>"
```

Invalid credentials return HTTP 401 with a generic response:

```json
{
  "code": 40101,
  "message": "Invalid username or password",
  "data": null
}
```

Request a password-reset code by email:

```powershell
curl.exe -i -X POST http://localhost:8080/api/v1/auth/password-reset/request `
  -H "Content-Type: application/json" `
  -d '{"email":"parent@myfschool.local"}'
```

Verify the six-digit code from the email, then use the returned one-time token to set a new password:

```powershell
curl.exe -i -X POST http://localhost:8080/api/v1/auth/password-reset/verify `
  -H "Content-Type: application/json" `
  -d '{"email":"parent@myfschool.local","code":"123456"}'

curl.exe -i -X POST http://localhost:8080/api/v1/auth/password-reset/confirm `
  -H "Content-Type: application/json" `
  -d '{"resetToken":"<token-from-verify>","newPassword":"new-password","confirmPassword":"new-password"}'
```

Reset codes and reset tokens expire after 10 minutes and can each be used only once. A new code can be requested after 60 seconds. The request endpoint always returns the same success response, including for unknown email addresses.

## Configuration

The documented variables are listed in `.env.example`. The application never embeds a JWT signing secret. Relevant defaults are intended only for the local MySQL container; production should override all database values. JWT lifetime is controlled by `JWT_EXPIRATION_SECONDS` and is returned by the login response. Flutter Web origins are controlled with the comma-separated `CORS_ALLOWED_ORIGIN_PATTERNS` value.

Password-reset email delivery uses the `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_SMTP_AUTH`, and `MAIL_STARTTLS_ENABLE` variables. The local defaults expect a development SMTP server on port 1025; production must provide a real SMTP account and TLS settings.

The phone contract currently accepts exactly 10 decimal digits starting with `0`, matching the supplied development account.
