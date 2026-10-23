# MyFSchool authentication backend

Java 21 / Spring Boot 3.5 REST API for the Flutter application's login flow. It uses MySQL 8, Spring Data JPA, BCrypt, and stateless HS256 JWT access tokens.

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

   The `dev` profile creates this account once, with a BCrypt hash:

   - Phone: `0987654321`
   - Password: `123456`
   - Role/status: `STUDENT` / `ACTIVE`

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
  -d '{"phoneNumber":"0987654321","password":"123456"}'
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
      "phoneNumber": "0987654321",
      "fullName": "Test Student",
      "role": "STUDENT"
    }
  }
}
```

Authenticated user (replace the placeholder with the login token):

```powershell
curl.exe -i http://localhost:8080/api/v1/auth/me `
  -H "Authorization: Bearer <signed-jwt>"
```

Invalid credentials return HTTP 401 with a generic response:

```json
{
  "code": 40101,
  "message": "Invalid phone number or password",
  "data": null
}
```

## Configuration

The documented variables are listed in `.env.example`. The application never embeds a JWT signing secret. Relevant defaults are intended only for the local MySQL container; production should override all database values. JWT lifetime is controlled by `JWT_EXPIRATION_SECONDS` and is returned by the login response. Flutter Web origins are controlled with the comma-separated `CORS_ALLOWED_ORIGIN_PATTERNS` value.

The phone contract currently accepts exactly 10 decimal digits starting with `0`, matching the supplied development account.
