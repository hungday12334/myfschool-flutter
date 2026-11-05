# School administration UC16–UC32

Base `/api/admin-school`, Bearer JWT role **SCHOOL_ADMIN** (the schema's code for ADMIN_SCHOOL). Identity and school come from the principal. All referenced IDs must belong to that school; wrong school returns 403, absent ID 404, invalid input 400, duplicate or conflicting history 409. Existing ApiResponse envelope: `{ "code": 200, "message": "Success", "data": ... }`. Create endpoints return HTTP 201 except synchronous import (200 with final batch result).

## UC mapping

| UC | Methods and paths relative to base |
|---|---|
|16 teacher profiles|GET /teachers, GET /teachers/{id}, PUT /teachers/{id}; create account/profile through existing POST /accounts/teachers|
|17 add student|POST /students|
|18 student list/detail|GET /students, GET /students/{id}|
|19 update student|PUT /students/{id}|
|20 Excel students|POST /student-imports, GET /student-imports/{id}, GET /student-imports/{id}/errors|
|21 parent profiles|GET /parents, GET /parents/{id}, POST /parents, PUT /parents/{id}|
|22 link parent/student|POST /parents/{parentId}/students/{studentId}, GET /parents/{parentId}/students|
|23 adjust/unlink|PUT or DELETE /parents/{parentId}/students/{studentId}|
|24 academic years|GET /academic-years, GET /academic-years/{id}, POST /academic-years, PUT /academic-years/{id}|
|25 semesters|GET /semesters, GET /semesters/{id}, POST /semesters, PUT /semesters/{id}|
|26 subjects|GET /subjects, GET /subjects/{id}, POST /subjects, PUT /subjects/{id}|
|27 create class|POST /classes|
|28 classes|GET /classes, GET /classes/{id}, PUT /classes/{id}|
|29 enroll student|POST /classes/{id}/students|
|30 roster/transfer/end|GET /classes/{id}/students, POST /enrollments/{id}/transfer, PATCH /enrollments/{id}/end|
|31 homeroom assignment|PUT /classes/{id}/homeroom-teacher|
|32 teaching assignment|GET /teaching-assignments, GET /teaching-assignments/{id}, POST /teaching-assignments, PUT /teaching-assignments/{id}|

Lists support page=0, size=20 (1–100) and return content/page/size/totalElements. Entity lists support keyword substring on name (not teaching assignments); semesters/classes/assignments also support academicYearId. Other lists validate a supplied year scope but do not filter by year. Roster supports optional date (inclusive membership on that date); omit date for full enrollment history. Parent links and import errors support page/size.

## Request examples (POST/PUT replace editable fields)

Teacher profile:

```json
{"teacherCode":"GV100","fullName":"Nguyen Van A","dateOfBirth":"1990-01-01","gender":"MALE","phone":"0900000000","email":"teacher@example.org","status":"ACTIVE"}
```

Teacher creation remains UC08, creating user/profile/role together. Teacher profile update synchronizes contact fields with the linked account. Use TeacherStatus enum for status; deactivation preserves historical records.

Student profile (no login account required by schema):

```json
{"studentCode":"HS100","fullName":"Tran Van B","dateOfBirth":"2013-01-01","gender":"MALE","admissionDate":"2026-09-01","address":"School district","status":"ACTIVE"}
```

Codes max 40, names max 150, address max 500; date of birth must be past; admission cannot precede birth. Student code is unique within school. Optional dates/gender may be null. Updates preserve linked user and class history.

Parent profile:

```json
{"userId":100,"fullName":"Tran Van C","phone":"0900000001","email":"parent@example.org","address":"School district"}
```

Supply userId to attach an existing account in the same school. To create a new parent account atomically, omit userId and supply username/password (8–72 characters, at most 72 UTF-8 bytes). New accounts are ACTIVE with mustChangePassword=true; passwords are BCrypt hashes and never returned. Do not supply credentials together with userId. Create attaches a unique parent profile and ensures PARENT role; update requires the original userId and cannot change it. Contact fields synchronize with the account.

Link body:

```json
{"relationshipType":"GUARDIAN","primaryContact":true,"canViewAcademic":true,"canSubmitLeave":true}
```

Multiple parents per student are supported. Parent/student pair is unique; POST on an existing pair conflicts, PUT on missing pair returns 404. DELETE removes that relationship. Primary contact is stored per link; no unsupported single-primary constraint is invented. Parent visibility/leave permissions use the existing flags.

Academic year:

```json
{"name":"2026-2027","startDate":"2026-09-01","endDate":"2027-06-01","current":true}
```

End must follow start. Setting current clears other current flags within the school under a school-row lock. Date changes are rejected once classes reference the year; existing semesters must fit inside it. Name max 20 and unique per school.

Semester:

```json
{"academicYearId":1,"semesterNumber":1,"name":"Semester 1","startDate":"2026-09-01","endDate":"2027-01-15"}
```

Number 1–3, unique per year, name max 60. Dates strictly ordered and within year. Existing semester cannot be moved to another year; date changes conflict once teaching assignments exist.

Subject:

```json
{"subjectCode":"MATH","name":"Mathematics","description":"School subject","active":true}
```

Code max 40 unique per school, name max 150, description max 10000; inactive subjects cannot receive new active assignments. Use active=false to retain history.

Class:

```json
{"academicYearId":1,"className":"6A","gradeLevel":6,"status":"ACTIVE"}
```

Grade level 6–12 per schema, name max 50 unique per academic year. Class year is immutable after creation. Use ARCHIVED to retain history. Enrollments, transfers and homeroom assignments require active class.

Enrollment: `{"studentId":1,"enrolledAt":"2026-09-01"}`. Dates are business DATEs provided explicitly; no default is invented for enrolled_at. Student must be active. Overlapping class memberships in the same academic year are rejected. Writes serialize through school/student locks.

Transfer: `{"targetClassId":2,"effectiveDate":"2026-11-10"}`. Source enrollment must still be ACTIVE/open, destination different and within the same academic year. Source closes with TRANSFERRED and left_at=effectiveDate minus one day; target opens on effectiveDate atomically. Historical grades or attendance at/after the transfer date block a retroactive move; undated grades also block it. No historical attendance/grades are rewritten.

End enrollment: `{"leftAt":"2027-06-01","status":"COMPLETED"}` or WITHDRAWN. Left date is inclusive and within year. ACTIVE/TRANSFERRED are not accepted here; use transfer endpoint for TRANSFERRED. Existing future attendance/grades block retroactive closure. Repeated closure is rejected.

Homeroom: `{"teacherId":1}`; null removes assignment. Teacher must have active profile/account and TEACHER role in this school. Schema stores current teacher only, not assignment history.

Teaching assignment:

```json
{"classId":1,"teacherId":1,"subjectId":1,"semesterId":1,"status":"ACTIVE"}
```

All references must belong to school; semester must belong to class year. Active assignment requires active class, teacher/account/role and subject. Tuple class/teacher/subject/semester is unique. PUT changes status while keeping the tuple immutable, preserving grade/timetable references; use a new assignment for a different tuple. Multiple teachers per class/subject are permitted by schema.

## Student Excel import

POST multipart field `file`. First sheet headers in exact order:

```text
studentCode | fullName | dateOfBirth | gender | admissionDate | address | status
```

Dates accept ISO yyyy-MM-dd strings or Excel date cells. Gender/status use entity enum names; blank optional birth/gender/admission allowed, status required. Use text cells for codes. Same upload limits as account import (service 5 MiB, 1000 rows; deployment multipart limit may be lower). Formula cells are rejected. Each student row has its own transaction; valid rows commit, invalid/duplicate rows are recorded with Excel row numbers and sanitized errors. Existing ImportBatch/ImportError use type STUDENT and final COMPLETED/PARTIAL/FAILED counts. No new tables or external services.

## Build evidence and limits

Implemented in AdminSchoolDtos, SchoolAdminSchoolController, SchoolAdminSchoolService; reused SchoolAdminImportService for student workbooks and existing tenant/security checks. Existing ClassEnrollment timestamp mapping remains read-only; membership writes explicitly use DATE native queries matching the migration. No schema migration required.

JDK 21: `mvn -f backend/pom.xml -DskipTests package` compiled and packaged successfully. No automated tests were run for this change, following the user's earlier preference. Runtime/MySQL integration is not claimed verified.
