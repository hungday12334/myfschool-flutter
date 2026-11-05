# Teacher backend UC53–UC70

Verified workspace: `D:/FULearning/Fall26/PRM392/project/myfschoolse1947`, branch `teacherbe`.
Sources: user UC53–UC70 requirements; `backend/src/main/resources/db.migration/01_myfschool_mysql_schema.sql`; migrations 03/04; existing entities, TeacherAccess, Teacher services and ParentService. No AGENTS.md was found in the workspace.

All paths below start with `/api/teacher`. Implemented means controller, DTO, service and persistence exist; verified means covered by the named integration suite in the latest clean verify.

| UC | Method/path | Review and verification |
|---|---|---|
|53|GET /classes/{classId}/attendance/summary|Implemented; Attendance + Context tests: scope, empty data, no duplicate counting, cancelled sessions|
|54|POST /grade-items|Implemented; Grade tests: assignment, dates, weight/max validation|
|55|POST /grades|Implemented; Grade tests: duplicate batch/stored grade, foreign student, invalid score, transaction rollback|
|56|PUT /grades/{gradeId}|Implemented; Grade tests: persistence and forbidden teacher; pessimistic row lock|
|57|GET /classes/{classId}/subjects/{subjectId}/grades|Implemented; Grade tests: zero versus missing, assignment/semester scope|
|58|POST /evaluations|Implemented; Grade tests: homeroom only, duplicate and empty evaluation|
|59|POST /remarks|Implemented; Grade tests: assignment and Parent visibility|
|60|GET /homeroom/classes|Added API; Context tests: authenticated homeroom, Parent role rejection and foreign school year|
|61|GET /homeroom/academic-summary|Implemented; Grade tests: persisted scores/evaluation and pagination|
|62|GET /homeroom/attendance-summary|Implemented; Attendance tests: actual status counts and homeroom scope|
|63|GET /students/{studentId}/parents|Added API; Context tests: contact success, subject teacher and foreign school denied|
|64|POST /announcements|Implemented; Communication tests: draft privacy, validation and homeroom ownership|
|65|POST /announcements/{announcementId}/publish|Implemented; Communication tests: Parent visibility after publication; in-app delivery only|
|66|GET /announcements and /announcements/{announcementId}/reads|Implemented; Communication tests: deduplicated parents, actual Parent read action, unrelated recipient denied|
|67|GET /leave-requests and /leave-requests/{requestId}|Implemented; Communication tests: Parent submission and teacher scope|
|68|PATCH /leave-requests/{requestId}/decision|Implemented; Communication tests: valid/invalid transition, rejection reason, concurrent decisions|
|69|GET /leave-requests/{requestId}/history|Implemented; Communication tests: persisted actor/status/history|
|70|GET /activities|Added API; Context tests: zero real activity and invalid date range|

## Changes and test diagnosis

- Exposed existing homeroom, parent-contact and activity services through TeacherContextController.
- `enrolled_at` is a required business DATE with no schema default. Existing mapping is read-only; seed migration inserts it explicitly. Fixture now inserts the complete enrollment through JDBC, including its actual start date. The duplicate-enrollment test uses a distinct real date to respect the business unique key. No production constraint/default or mapping was changed.
- Prior Jupiter discovery failure referenced stale compiled StudentProfile classes. Maven clean removed stale output; no tests were disabled.
- First fresh run: 36 tests, 29 errors. Fixture repair: 36 tests, one remaining enrollment fixture error. After repairing that test and adding Context coverage: final `clean verify`: 38 tests, zero failures/errors/skips, BUILD SUCCESS.

## Verification

Use JDK 21. Set JAVA_HOME to the installed JDK for the current shell, then run Maven `-f backend/pom.xml clean verify`. Maven was invoked through its installed absolute `mvn.cmd` path under the user's Maven wrapper distribution cache because mvn is absent from PATH.

Latest run: 2026-11-05, log `backend/teacher-test-current.log`; fresh reports `backend/target/surefire-reports`. Suites: ParentService 7, TeacherAttendance 10, TeacherCommunication 8, TeacherContext 2, TeacherGrade 11. Verify also created the executable backend JAR. No external notifications were sent.

## Assumptions and limits

- Schema and code provide raw grades/evaluations, not an authoritative average/classification formula; summaries return stored results without inventing one.
- Attendance statistics return status counts, not a policy attendance percentage. Missing records are not silently classified absent. If consumers calculate status shares, the denominator is total recorded statuses; empty data yields counts of zero.
- Publication means availability through existing Parent announcement APIs. CLASS targets use current membership; schema has no immutable delivery snapshot. Read receipts arise only from Parent markRead.
- Tests use H2 in MySQL compatibility mode and the test schema. Native MySQL deployment/migration execution and load testing were not performed. No migration was needed for these additions.
- Gradebook and homeroom attendance return complete class views as existing contracts; these views are not paginated. Announcement/leave lists and academic summary support pagination. Large-class performance is not load-tested.

Next step if stronger deployment evidence is needed: execute the same schema and application against a dedicated MySQL test database, then exercise production-specific locking and query plans. No user action is required for the local test suite.
