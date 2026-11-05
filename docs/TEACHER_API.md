# Teacher API UC53–UC70

See [UC mapping and verification](teacher-backend-progress.md) for every method/path and its test evidence. All routes require a Bearer JWT with TEACHER role. Identity comes from the principal; client teacher IDs cannot grant access. Responses use `{ "code": 200, "message": "...", "data": ... }`. Errors use the project ApiResponse convention: 400 invalid input, 401 unauthenticated, 403 denied scope, 404 missing resource, 409 duplicate or invalid state transition.

## Academic operations

Create an item: `POST /api/teacher/grade-items`

```json
{"teachingAssignmentId":1,"title":"Quiz 1","gradeType":"REGULAR","weight":1,"maxScore":10,"assessmentDate":"2026-11-05"}
```

Types: REGULAR, MIDTERM, FINAL, OTHER. Weight/maxScore must be positive DECIMAL(6,2); title is required, at most 200 characters. Assessment date, if supplied, must be in assignment semester. Only the assigned teacher in the same school can write grades.

Enter scores: `POST /api/teacher/grades`

```json
{"gradeItemId":1,"grades":[{"studentId":1,"score":0,"comment":"Submitted"},{"studentId":2,"score":null}]}
```

Batch size 1–200; student IDs must be unique and enrolled on assessment date (or the service's semester-bounded current date if omitted). Existing student/item scores cause 409. Zero is an entered score; null is missing. Scores must be between zero and the item's maxScore with at most two decimal places. Edit with `PUT /api/teacher/grades/{gradeId}` and body `{"score":8.5,"comment":"Corrected"}`. Writes use database locks.

Gradebook accepts optional semesterId; response data contains classId/className, subject, semester, gradeItems and students with their stored grades. No average is inferred.

Evaluation: `POST /api/teacher/evaluations`

```json
{"studentId":1,"classId":1,"semesterId":1,"academicResult":"Recorded result","conductResult":"Recorded conduct","overallComment":"Progressing"}
```

Homeroom only. At least one result/comment required; result fields max 50, comment max 10000. Student/semester evaluation is unique.

Remark: `POST /api/teacher/remarks`

```json
{"studentId":1,"classId":1,"semesterId":1,"teachingAssignmentId":1,"content":"Good participation","remarkDate":"2026-11-05","visibility":"PARENTS_VISIBLE"}
```

Visibility is STAFF_ONLY or PARENTS_VISIBLE; omit teachingAssignmentId for a homeroom remark. Content required, max 10000; date and matching semester/class required. Evaluation and remark are separate records.

## Homeroom and statistics

Homeroom classes accepts academicYearId. Contacts return a list of all linked parents with relationship and primary-contact indicator; only the student's homeroom teacher may access it. Academic summary accepts classId, academicYearId, semesterId, page=0, size=20 (1–100). It returns stored subject items, scores and evaluation per student.

Class/homeroom attendance summaries accept inclusive from/to dates; homeroom accepts classId. Response data includes totalSessions, totalRecords and counts for PRESENT, ABSENT, LATE, EXCUSED. Cancelled sessions are excluded. Empty data returns zero counts; missing marks do not become absences. No attendance-policy percentage is invented.

Activities accepts from/to; reversed ranges or ranges over 3660 days are rejected. It reports assigned classes/subjects overlapping the range and persisted attendance sessions, completed sessions, grade items, entered grade rows, published announcements and processed leave requests for the authenticated teacher. These are record counts, not performance ratings.

## Announcements and leave

Create draft with `POST /api/teacher/announcements`:

```json
{"classId":1,"title":"Class meeting","content":"Please check the class schedule","expiresAt":"2026-12-01T18:00:00"}
```

Homeroom only; title max 255, content max 100000, expiry must be future. Publishing is a separate POST; its response includes status=PUBLISHED and publishedAt. This makes the CLASS announcement available in the Parent application's existing feed. It does not claim email/push delivery. Repeating publish on an already published announcement is idempotent.

Announcement list filters: classId/status/page/size. Reads accepts page/size and returns recipientCount, readCount and paged recipients containing parentId/fullName/read/readAt. Parent recipients are deduplicated; reading is recorded through the existing Parent markRead API, never by publish.

Leave list filters: classId/status/from/to/page/size, matching overlapping leave dates. Homeroom authorization is checked again for detail, decision and history. Decide with:

```json
{"decision":"REJECTED","reason":"Please correct the requested dates"}
```

Only PENDING → APPROVED or REJECTED; rejection requires a reason (max 5000). Repeat/concurrent decisions conflict; one transition commits. Response includes reviewer, decisionAt and reviewNote. History returns prior/new status, actor, changedAt and note. Approval does not rewrite attendance.

Run `mvn -f backend/pom.xml clean verify` with JDK 21. Tests exercise persistence, MVC/security, Parent integration and concurrent leave decisions without external delivery. See progress document for environment limitations and exact latest results.
