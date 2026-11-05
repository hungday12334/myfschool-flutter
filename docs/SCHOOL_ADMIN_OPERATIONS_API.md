# School admin UC33–UC42

Base path `/api/admin-school`, Bearer JWT with schema role `SCHOOL_ADMIN` (ADMIN_SCHOOL in the use cases). Services check active admin/account/school and school ownership of every supplied reference. No migration or external delivery service is introduced.

| UC | Method/path relative to base | Behavior |
|---|---|---|
|33|GET /timetable, GET /timetable/{id}, POST /timetable, PUT /timetable/{id}, DELETE /timetable/{id}|Schedule management and conflict detection|
|34|GET /teaching-assignments|Existing assignment API enhanced with filters and display names; not duplicated|
|35|GET /school, PUT /school|School information supported by current entity|
|36|POST /announcements|Create draft with SCHOOL target|
|37|POST /announcements/{id}/publish|Publish draft to SCHOOL, CLASS or USER audiences|
|38|GET /announcements, GET /announcements/{id}|School notification history/detail; POST /announcements/{id}/retract withdraws a published notice|
|39|GET /statistics/students|Student counts by status/gender|
|40|GET /statistics/teachers-classes|Teacher status, class status and class grade counts|
|41|GET /statistics/attendance, GET /statistics/academics|Actual attendance records, entered/missing stored scores, recorded evaluation groups|
|42|GET /audit-logs|School-scoped persisted operation history|

Responses use existing ApiResponse: `{"code":200,"message":"Success","data":...}`. POST timetable/announcements return HTTP 201. Lists return `content`, `page`, `size`, `totalElements`; page starts at 0, size defaults to 20 and must be 1–100. Errors follow existing convention: 400 invalid input/date/scope relationship, 401 authentication, 403 forbidden school/role, 404 absent resource, 409 schedule conflict or invalid state transition. Creation/update uses transactions and school-row locking; audit metadata commits/rolls back with changes.

## Timetable

POST/PUT body:

```json
{"teachingAssignmentId":1,"dayOfWeek":1,"periodNumber":2,"shift":"MORNING","room":"A101","effectiveFrom":"2026-09-01","effectiveTo":"2027-01-15"}
```

dayOfWeek 1–7 (Monday–Sunday), period 1–15, shift MORNING/AFTERNOON/EVENING; room optional max 50. Active teaching assignment, active teacher/class and active subject required. Assignment semester must belong to class year. Effective dates must fit within semester, end >= start; omitted end is stored as semester end. Overlapping effective ranges in the same day/shift/period cannot share class, teacher, or nonblank room. School lock serializes timetable edits to prevent concurrent double booking. Delete removes schedule entry, not historical attendance sessions.

GET filters: classId, teacherId, semesterId, date, page, size. Date selects entries effective on that date; returned rows retain their weekday (the result is not restricted to that date's weekday).

Example response data:

```json
{"id":10,"teachingAssignmentId":1,"classId":1,"teacherId":2,"subjectId":1,"semesterId":1,"dayOfWeek":1,"periodNumber":2,"shift":"MORNING","room":"A101","effectiveFrom":"2026-09-01","effectiveTo":"2027-01-15"}
```

## Assignments

GET filters: classId, teacherId, subjectId, semesterId, academicYearId, status (ACTIVE/INACTIVE), page, size. All references checked within admin school. Rows include className, teacherName, subjectName and assignment IDs. The earlier UC32 create/update/detail routes remain available.

## School information

PUT body:

```json
{"name":"Example School","schoolLevel":"COMBINED","address":"School district","phone":"0900000000","email":"office@example.org"}
```

Name required max 255, schoolLevel SECONDARY/HIGH_SCHOOL/COMBINED, address max 500, phone max 20, valid optional email max 255. School ID/code/status are read-only here. Current schema has no separate settings store, so this API manages existing school fields only.

## Notifications

Create a whole-school draft:

```json
{"title":"School meeting","content":"Please check the schedule","expiresAt":"2027-06-01T18:00:00","targets":[{"type":"SCHOOL"}]}
```

For selected audiences replace targets with e.g. `[{"type":"CLASS","id":1},{"type":"USER","id":2}]`. SCHOOL target ID may be omitted; when provided it must be current school ID. CLASS/USER require existing in-school IDs. Title/content required, limits 255/100000. Targets 1–200, deduplicated by type/ID. Optional expiry must be future at both draft creation and publication.

Publish separately using POST /announcements/{id}/publish (no body). Publication is in-application availability through existing audience/Parent announcement queries; it does not claim email/SMS/push delivery. Drafts are private. PublishedAt is stored only on publish; repeated publish is idempotent. Retracted announcements cannot be republished. Admin can manage school-wide announcements including those authored by teachers in the same school.

Response detail includes id/authorId/title/content/status/publishedAt/expiresAt/targets. History accepts status, authorId, from/to (inclusive created dates), page/size. It returns summary rows without full content or one query per recipient. Read receipts remain controlled by the existing actual Parent markRead action; sending never marks anything read. Audience types are exactly SCHOOL/CLASS/USER from schema, not a new role-based notification system.

## Statistics definitions

- Students: optional academicYearId. Without year, all student profiles in school; with year, distinct students having enrollment history in that year (not enrollment-row count). Groups by stored status and gender; null gender is a separate group.
- Teachers/classes: optional academicYearId. With year, teachers having either teaching assignments or homeroom classes in that year; teacher counted once. Class groups include stored ACTIVE/ARCHIVED status and grade. Without year includes all school profiles/classes.
- Attendance: optional classId/from/to inclusive session dates. Excludes CANCELLED sessions, includes remaining session states. Returns sessionCount, recordCount, and PRESENT/ABSENT/LATE/EXCUSED counts including zero entries. Missing marks are not converted into absences. No policy percentage is invented; if calculating status shares, recorded rows are the denominator and zero records means no percentage.
- Academics: optional classId/semesterId, matching class year when both supplied. Per subject returns gradeRows, enteredScores (non-null including zero), missingScores (stored null score rows). Students with no StudentGrade row are not counted as stored missing-score rows. Also groups saved StudentEvaluation academicResult/conductResult. No inferred weighted average or academic classification. Empty data yields empty group arrays.

Example attendance response data:

```json
{"sessionCount":0,"recordCount":0,"counts":{"PRESENT":0,"ABSENT":0,"LATE":0,"EXCUSED":0}}
```

## Audit history

Filters: actorId, action, entityType, from/to inclusive, page/size. Returns id, actorId, action, entityType, optional entityId, createdAt. Only rows belonging to admin school are visible. Raw old/new values, password/hash, request bodies and import credentials are not exposed.

New successful admin mutations in accounts, school records, enrollment, timetable, school info, announcements and import start/finish write audit metadata transactionally. Earlier historical actions cannot be reconstructed; Teacher/Parent flows are not retroactively instrumented by this change. Some school-record create events record action/type with null entityId, which the schema permits. Failed transactions do not leave successful-action audit rows.

## Build and verification limits

JDK 21, Maven `-f backend/pom.xml -DskipTests package` compiled and packaged these changes successfully. Automated tests and live MySQL execution were not run, following the user's earlier preference; compilation is not evidence that API/business integration tests passed. Existing schema entities were reused. No commit/push/deploy performed.
