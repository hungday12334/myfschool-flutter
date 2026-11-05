# School admin accounts: Dashboard and UC08–UC15

Base path `/api/admin-school`. Bearer JWT required, role `SCHOOL_ADMIN`; every service operation checks the actor is active, school active and resource belongs to that school. Password hashes are never returned. Existing schema is reused; no migration.

| UC | Method | Path |
|---|---|---|
|Dashboard|GET|/dashboard|
|08|POST|/accounts/teachers|
|09|GET|/accounts|
|10|PUT|/accounts/{id}|
|11|PATCH|/accounts/{id}/status|
|12|POST|/accounts/{id}/reset-password|
|13|PUT|/accounts/{id}/roles|
|14|POST multipart|/account-imports|
|15|GET|/account-imports/{id}|
|15|GET|/account-imports/{id}/errors|

Create teacher request:

```json
{"username":"teacher.new","password":"InitialPassword123!","teacherCode":"GV0100","fullName":"Nguyen Van A","email":"teacher@example.org","phone":"0900000000"}
```

Creates User + Teacher + TEACHER role atomically. Username globally unique; teacherCode unique within school. Password 8–72 characters and at most 72 UTF-8 bytes; stored using existing BCrypt encoder. Account ACTIVE and mustChangePassword=true. No password is sent by email automatically.

Account response data:

```json
{"id":100,"username":"teacher.new","email":"teacher@example.org","phone":"0900000000","status":"ACTIVE","mustChangePassword":true,"roles":["TEACHER"]}
```

List filters: keyword (username substring, max 100), status (ACTIVE/LOCKED/DISABLED), page=0, size=20, maximum size 100. Response is project Spring Page inside ApiResponse.

Update replaces email/phone and optionally teacher fullName: `{"email":"new@example.org","phone":"0900000001","fullName":"Updated name"}`. Username and school are immutable through this API. Status body `{"status":"LOCKED"}`; unlock with ACTIVE. Self status changes rejected. Password reset body `{"password":"ReplacementPassword123!"}`; sets mustChangePassword=true.

Role replacement body `{"roles":["TEACHER"]}`. Allowed role codes SCHOOL_ADMIN/TEACHER/PARENT/STUDENT must already exist. A TEACHER/PARENT/STUDENT role requires its existing corresponding profile in the same school; this endpoint does not fabricate profiles. Self role replacement rejected. Existing authentication reloads account status and roles from the database on requests.

Dashboard returns school-scoped counts with keys User, Teacher, Student, SchoolClass, reflecting stored rows including inactive records.

Excel import multipart field `file`: .xlsx/.xls supported by Apache POI; first sheet, first row must contain exact headers in this order:

```text
username | password | teacherCode | fullName | email | phone
```

Service limit is 5 MiB and 1000 data rows; the deployment's Spring multipart limit can be lower (default 1 MiB). Formulas rejected. Use text cells for phone/identifiers to preserve leading zeroes. All non-header rows are processed; blank rows are invalid rows. Invalid file/header fails 400 before creating a batch. Each valid account row commits independently; invalid/duplicate rows persist a row number (Excel numbering, first data row=2) and sanitized error without credentials. Original workbook/passwords are not persisted in import history. Batch stores checksum, file name, counters, start/end time and COMPLETED/PARTIAL/FAILED status. Result does not imply email delivery. Errors endpoint supports page/size and returns content and totalElements. Batch/error access is school-scoped.

Response envelope follows existing ApiResponse. Main errors: 400 validation, 401 missing/invalid token, 403 role/school scope, 404 missing account/batch/role configuration, 409 duplicates or self-management conflict. Import row errors use INVALID_OR_DUPLICATE_ACCOUNT / ROW_PERSISTENCE_FAILED and never echo password/input values.

Implementation assumptions: account import creates teacher accounts (schema ImportType.TEACHER and UC08); other account profiles are not created by this use case. Role management supports existing profile types only. No external notification delivery is introduced.

Build: JDK 21, Maven `-f backend/pom.xml -DskipTests package`. Automated tests were not requested for this change and are not claimed as passed.
