# Flutter Parent UC71–UC88 implementation

Continued the existing Git working tree. No backend code or database schema was changed. Production Parent requests use the existing ApiClient, TokenStorage and JWT login flow. The original ParentHomeScreen remains the login destination; TeacherHomeScreen routing is preserved.

## Status and API-to-screen mapping

All routes below are prefixed with `/api/parent`. API success data is read from the existing `{code,message,data}` envelope. Paginated data uses `{content,page,size,totalElements,totalPages}`.

| UC | Status | Reachable UI and actual API |
| --- | --- | --- |
| UC71 | COMPLETE | Trang chủ: `GET /dashboard?studentId=...`, greeting/profile, selected child, class/teacher, stored academic data, attendance counts, recent announcements, pending leave count. |
| UC72 | COMPLETE | Các con: `GET /children`, authorized linked children, selection highlight and profile navigation. |
| UC73 | COMPLETE | Shared child selector: `GET /children/{id}/context`; selection stays in memory, and child-specific screens are recreated when the child changes. |
| UC74 | COMPLETE | Học tập → Hồ sơ học sinh: `GET /children/{id}`. |
| UC75 | COMPLETE | Học tập → Lớp và giáo viên chủ nhiệm: `GET /children/{id}/class`; missing class/contact fields are handled. |
| UC76 | PARTIAL | Học tập → Thời khóa biểu: `GET /children/{id}/timetable` using backend defaults, sorted day/period, today highlighted. Historical year/semester picker awaits a backend period lookup contract. |
| UC77 | PARTIAL | Học tập → Kết quả học tập và điểm môn: `GET /children/{id}/academic-results`; grouped stored grades and comments, no invented average. Semester labels and historical filters await backend period metadata. |
| UC78 | PARTIAL | Subject detail shows and refreshes actual grades supplied by UC77. The dedicated `GET /children/{id}/subjects/{subjectId}/grades?semesterId=...` service is implemented and contract-tested, but its required semester ID is not returned by any Parent API. |
| UC79 | COMPLETE | Học tập → Nhận xét của giáo viên: `GET /children/{id}/evaluations?page=...`, evaluations/remarks, content, teacher/date where returned, academic/conduct result, load more. |
| UC80 | COMPLETE | Học tập → Lịch sử điểm danh: `GET /children/{id}/attendance?from=...&to=...&page=...`; valid date ranges, pagination, Vietnamese statuses, late responses discarded. |
| UC81 | COMPLETE | Attendance detail/dashboard and Tổng hợp điểm danh: `GET /children/{id}/attendance/summary`; actual PRESENT/ABSENT/EXCUSED/LATE counts. No rate is fabricated. Summary is all-time when filters are omitted, as defined by the backend. |
| UC82 | COMPLETE | Thông báo: `GET /announcements?page=...`, preview/sender/date, unread distinction, load more and refresh after details. |
| UC83 | COMPLETE | Announcement detail: `GET /announcements/{id}`, plain selectable content, sender/date/read status. Backend provides no attachments. Opening alone does not mark read. |
| UC84 | COMPLETE | Explicit Đánh dấu đã đọc: `POST /announcements/{id}/read`; button disabled during request, status refreshed only after success, errors retained. |
| UC85 | PARTIAL | Device registration/removal and injectable notification lifecycle are integrated after Parent login and before logout; `POST /devices` and `DELETE /devices`. Firebase/provider configuration is absent, so remote delivery is unavailable. See PARENT_PUSH_SETUP.md. |
| UC86 | COMPLETE | Nghỉ học → +: `POST /children/{id}/leave-requests` with exact `{startDate,endDate,reason}` body, required/date/4000-character validation, duplicate-submit prevention, real success/error handling. |
| UC87 | COMPLETE | Leave detail: `GET /leave-requests/{id}`, student/dates/reason/status/reviewer/decision/rejection/history, refresh action. |
| UC88 | COMPLETE | Nghỉ học: `GET /children/{id}/leave-requests?status=...&page=...`, pagination, actual status enum filters, refresh after creation/details and child switching. |

Fourteen use cases are complete against the available contracts. Four are partial; no use case is wholly blocked. COMPLETE describes supported API behavior and automated integration coverage, not a live production database test.

## Navigation and consistency

Bottom navigation: Trang chủ, Học tập, Thông báo, Nghỉ học, Các con. Student profile/class/timetable/results/evaluations/attendance are available through Học tập. Subject grades, announcement details, leave creation/details are reached from their lists. Logout is in the account menu. Existing password-reset functionality remains on the login screen; no unsupported change-password API was invented.

The shared ParentContextController validates selection against refreshed linked children, auto-selects an authorized default, and never persists student profiles. Resource pages have child selectors and keys per child. Request generations and disposed-widget checks prevent earlier child responses from updating the current selection. Logout invalidates context, removes device registration before JWT cleanup, and returns to login. HTTP 401 expires the Parent session; HTTP 403 leaves authentication intact.

The old feature service constructor injection remains for compatibility with existing feature tests. Normal Parent routes use the new authorized `/api/parent` services, including dashboard shortcuts.

Vietnamese Material/Cupertino date-dialog localization is configured through the Flutter SDK `flutter_localizations` dependency. Existing AppColors and theme are reused.

## Backend contract gaps

- The Parent controller accepts academicYearId/semesterId filters, but exposes no list of academic periods or current semester ID. Child context only returns the class academic year's ID/name. Academic results and timetable return raw lists without resolved semester metadata. A compatible follow-up backend contract must supply authorized year/semester IDs, names and current selection before historical pickers and the dedicated UC78 request can be enabled. No IDs are guessed.
- No grading average formula or publication flags are returned. The frontend displays stored nullable scores, weights, dates and comments only.
- There is no Firebase configuration or delivery provider. The push abstraction is wired but its default source is explicitly unavailable. The proposed typed notification payload contract must also be agreed with a real sender.
- Backend responses contain no attachment/avatar or timetable start/end-time fields today. Optional rendering does not fabricate these values.

## Files created

- `lib/features/parent/data/models/parent_models.dart`
- `lib/features/parent/data/services/parent_api_service.dart`
- `lib/features/parent/data/services/parent_notification_service.dart`
- `lib/features/parent/presentation/parent_context_controller.dart`
- `test/parent_features_test.dart`
- `test/parent_api_contract_test.dart`
- `test/parent_interactions_test.dart`
- `test/parent_notification_test.dart`
- `test/teacher_login_navigation_test.dart`
- `docs/PARENT_IMPLEMENTATION.md`
- `docs/PARENT_PUSH_SETUP.md`

## Existing files modified in the working tree

- `lib/core/config/api_config.dart`: Parent route prefix.
- `lib/core/network/api_client.dart`: authenticated POST/DELETE through the shared client.
- `lib/features/auth/data/models/login_request.dart`, `login_response.dart`: existing backend-compatible username/source and role parsing changes retained.
- `lib/features/auth/presentation/screens/login_screen.dart`: Parent user/token/push-source propagation; teacher routing retained.
- `lib/features/home/presentation/screens/parent_home_screen.dart`: integrated Parent views, routes and states.
- `lib/main.dart`, `pubspec.yaml`, `pubspec.lock`: Vietnamese SDK localization and injectable notification source.
- `lib/features/teacher/data/services/teacher_api_service.dart`, `lib/features/teacher/presentation/screens/teacher_home_screen.dart`: retained mechanical lint/deprecation fixes; teacher regression test passes.
- `test/auth_api_service_test.dart`, `test/widget_test.dart`: backend-compatible request assertions and asynchronous home navigation expectations.

## Verification

Verified on 2026-11-04 with the configured Flutter SDK:

- `dart format lib test`: completed; final formatting changed only the edited Parent screen.
- `flutter analyze`: **No issues found**, exit 0.
- `flutter test`: **72 passed**, exit 0.
- `flutter build web --debug`: **succeeded**, output `build/web`, exit 0. The compiler also completed its Wasm dry run.
- `git diff --check`: no whitespace errors. Git reports only the repository's existing LF/CRLF normalization notices.

The 36 tests in the new Parent/teacher test files cover one/multiple/no children, actual dashboard model parsing, stale context/dashboard/academic/attendance responses, missing optional fields, exact REST request contracts, null scores, session expiry versus forbidden access, announcement read success/failure/deduplication, form date/reason/loading/error/success states, leave pagination/status filtering, evaluation pagination, device lifecycle and teacher login routing. Mocks are confined to tests. Production code has no mock data, hardcoded student IDs, or token logging.

## Running against the backend

Configure the existing base URL, for example for an emulator/debug build:

```sh
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

Use the actual reachable backend host for a physical device. Release configuration requires HTTPS, as enforced by existing ApiConfig. JWTs come from the existing login form. No parent/user identity parameter is sent as authorization proof.

There were no database migrations and no backend changes in this Flutter continuation. Verification does not establish Android/iOS device behavior or actual remote push delivery; Firebase setup and real-device checks remain as documented in PARENT_PUSH_SETUP.md.