# MyFSchool

MyFSchool is a Flutter client backed by a Java 21 / Spring Boot 3 authentication API. Login credentials are validated by the backend against MySQL, passwords are stored as BCrypt hashes, and authenticated requests use stateless JWT bearer tokens.

## Project layout

```text
.
|-- backend/                         Spring Boot REST API
|   |-- compose.yaml                 Local MySQL 8.4 service
|   `-- src/
|       |-- main/java/com/example/myfschool/
|       |   |-- controller/          HTTP endpoints
|       |   |-- service/             Authentication business logic
|       |   |-- repository/          JPA data access
|       |   |-- entity/              users table mapping
|       |   |-- dto/ and mapper/     API contracts
|       |   |-- security/            BCrypt, JWT, and Spring Security
|       |   |-- exception/           Standard API errors
|       |   `-- config/              Development data initializer
|       `-- test/                    Authentication integration tests
`-- lib/
    |-- core/config/                 Platform-aware API URL
    |-- core/network/                JSON HTTP client
    |-- core/storage/                Access-token storage
    `-- features/auth/               Login models, service, and existing UI
```

## Prerequisites

- Java 21 and Maven 3.9+
- Flutter compatible with Dart `^3.13.2`
- Docker with Compose, or a local MySQL 8.x server

## 1. Start MySQL and the backend

From PowerShell:

```powershell
cd backend
Copy-Item .env.example .env
# Edit .env and replace every change-me/placeholder value.
docker compose up -d mysql

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
$env:DB_PASSWORD = "<the DB_PASSWORD from .env>"
mvn spring-boot:run
```

`JWT_SECRET` is required and must be Base64 encoding of at least 32 random bytes. The `dev` profile idempotently creates this local-only account with a BCrypt password hash:

```text
Phone:    0987654321
Password: 123456
Role:     STUDENT
Status:   ACTIVE
```

Do not enable `dev` in production. Production must use a fresh secret, production database credentials, explicit CORS origins, and HTTPS.

If MySQL is already installed, skip Compose and set the `DB_*` variables for that instance. More backend configuration and curl details are in [backend/README.md](backend/README.md).

## 2. Start Flutter

From the repository root in a second terminal:

```powershell
flutter pub get
flutter run
```

The debug defaults are platform-aware:

| Target | Default API URL |
|---|---|
| Android emulator | `http://10.0.2.2:8080` |
| iOS simulator | `http://localhost:8080` |
| Flutter Web | `http://localhost:8080` |
| Other desktop targets | `http://localhost:8080` |

Override the URL for a physical device or another environment:

```powershell
flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8080
```

The device and development machine must be on the same network, and the backend port must be reachable through the host firewall. Release builds reject non-HTTPS API URLs:

```powershell
flutter build apk --dart-define=API_BASE_URL=https://api.example.com
```

The token is stored with `flutter_secure_storage`. On Flutter Web, the package uses tab-scoped session storage; browser storage cannot provide native keychain-level isolation, so production Web deployments must use HTTPS and should apply HSTS and a restrictive CSP.

## API smoke test

With the backend running:

```powershell
$login = Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/auth/login `
  -ContentType application/json `
  -Body '{"phoneNumber":"0987654321","password":"123456"}'

$token = $login.data.accessToken
Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/auth/me `
  -Headers @{ Authorization = "Bearer $token" }
```

A successful login returns HTTP 200 with `accessToken`, `tokenType`, configured `expiresIn`, and a password-free user DTO. Invalid credentials return HTTP 401:

```json
{
  "code": 40101,
  "message": "Invalid phone number or password",
  "data": null
}
```

## Verification

Run the automated checks:

```powershell
cd backend
mvn test

cd ..
flutter analyze
flutter test
```

To verify the full flow manually:

1. Start MySQL and the backend with the `dev` profile.
2. Confirm the login PowerShell request succeeds.
3. Start Flutter for the desired target, overriding `API_BASE_URL` for a physical device.
4. Enter `0987654321` and `123456` in the unchanged login screen.
5. Confirm the loading state appears, the token is stored, and the app replaces the login route with the existing student home screen.
6. Retry with an incorrect password and confirm the app remains on login and shows the backend error.
