import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/core/storage/token_storage.dart';
import 'package:myschoolse1947/features/auth/presentation/screens/login_screen.dart';
import 'package:myschoolse1947/features/home/presentation/screens/parent_home_screen.dart';
import 'package:myschoolse1947/features/parent/data/models/parent_models.dart';
import 'package:myschoolse1947/features/parent/data/services/parent_api_service.dart';
import 'package:myschoolse1947/features/parent/presentation/parent_context_controller.dart';

void main() {
  testWidgets('session expiry returns to login and clears the parent session', (
    tester,
  ) async {
    final api = _SessionApi();
    final tokens = _SessionTokens();
    addTearDown(api.expired.close);
    await tester.pumpWidget(
      MaterialApp(
        home: ParentHomeScreen(parentApi: api, tokenStorage: tokens),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.byType(ParentHomeScreen), findsOneWidget);
    expect(api.expired.hasListener, isTrue);
    api.expired.add(null);
    for (
      var frame = 0;
      frame < 20 && find.byType(LoginScreen).evaluate().isEmpty;
      frame++
    ) {
      await tester.pump(const Duration(milliseconds: 100));
    }
    await tester.pumpAndSettle();
    expect(tokens.clearCalls, 1);
    expect(find.byType(LoginScreen), findsOneWidget);
    expect(find.byType(ParentHomeScreen), findsNothing);
  });

  testWidgets(
    'leave history loads later pages and resets pagination for a status filter',
    (tester) async {
      final api = _InteractionApi();
      api.leaveResponse = (studentId, status, page) async => {
        'content': [
          {
            'id': page + 20,
            'status': status ?? 'APPROVED',
            'reason': status == null ? 'Lý do trang $page' : 'Lý do chờ duyệt',
          },
        ],
        'page': page,
        'totalPages': status == null ? 2 : 1,
        'totalElements': 2,
      };
      await _home(tester, api);
      await tester.tap(find.byIcon(Icons.event_note_outlined));
      await tester.pumpAndSettle();
      expect(find.textContaining('Lý do trang 0'), findsOneWidget);
      await tester.tap(find.text('Tải thêm'));
      await tester.pumpAndSettle();
      expect(find.textContaining('Lý do trang 0'), findsOneWidget);
      expect(find.textContaining('Lý do trang 1'), findsOneWidget);
      expect(api.leaveRequestsLog.map((r) => r['page']), [0, 1]);
      await tester.tap(find.text('Tất cả'));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Chờ duyệt').last);
      await tester.pumpAndSettle();
      expect(api.leaveRequestsLog.last, {
        'studentId': 1,
        'status': 'PENDING',
        'page': 0,
      });
      expect(find.textContaining('Lý do chờ duyệt'), findsOneWidget);
      expect(find.textContaining('Lý do trang 1'), findsNothing);
      expect(find.text('Tải thêm'), findsNothing);
    },
  );

  testWidgets(
    'evaluations render the backend page and append subsequent remarks',
    (tester) async {
      final api = _InteractionApi();
      api.evaluationResponse = (studentId, page) async => {
        'content': [
          {
            'type': 'REMARK',
            'teacher': {'fullName': 'Cô Lan'},
            'subject': {'name': 'Toán'},
            'content': 'Nhận xét trang $page',
            'date': '2026-11-04',
          },
        ],
        'page': page,
        'totalPages': 2,
        'totalElements': 2,
      };
      await _home(tester, api);
      await tester.tap(find.byIcon(Icons.menu_book_outlined));
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byIcon(Icons.rate_review_outlined));
      await tester.tap(find.byIcon(Icons.rate_review_outlined));
      await tester.pumpAndSettle();
      expect(find.textContaining('Nhận xét trang 0'), findsOneWidget);
      expect(find.textContaining('Cô Lan'), findsOneWidget);
      await tester.tap(find.text('Tải thêm'));
      await tester.pumpAndSettle();
      expect(api.evaluationRequests, [0, 1]);
      expect(find.textContaining('Nhận xét trang 0'), findsOneWidget);
      expect(find.textContaining('Nhận xét trang 1'), findsOneWidget);
    },
  );

  test(
    'a late context response cannot replace the newly selected child',
    () async {
      final api = _InteractionApi();
      final context = ParentContextController(api);
      addTearDown(context.dispose);
      await context.load();
      final oldResponse = Completer<StudentProfile>();
      api.contextResponse = (id) => id == 2
          ? oldResponse.future
          : Future.value(StudentProfile(child: api.linkedChildren.first));
      final oldSelection = context.selectChild(api.linkedChildren.last);
      await context.selectChild(api.linkedChildren.first);
      oldResponse.complete(StudentProfile(child: api.linkedChildren.last));
      await oldSelection;
      expect(context.selectedChild?.id, 1);
      expect(context.selectedContext?.child.id, 1);
    },
  );

  testWidgets('late dashboard attendance never replaces a switched child', (
    tester,
  ) async {
    final api = _InteractionApi();
    final oldResponse = Completer<ParentDashboard>();
    api.dashboardResponse = (id) => id == 1
        ? oldResponse.future
        : Future.value(api.dashboardFor(id!, present: 2));
    await _home(tester, api, settle: false);
    await tester.tap(find.byType(DropdownButton<int>).first);
    await tester.pump(const Duration(milliseconds: 400));
    await tester.tap(find.text('Bình · S2').last);
    await tester.pumpAndSettle();
    oldResponse.complete(api.dashboardFor(1, present: 99));
    await tester.pumpAndSettle();
    expect(find.text('99'), findsNothing);
    expect(find.text('Bình'), findsWidgets);
    expect(find.text('2'), findsOneWidget);
  });

  testWidgets('announcement list distinguishes unread and read items', (
    tester,
  ) async {
    final api = _InteractionApi();
    await _home(tester, api);
    await tester.tap(find.byIcon(Icons.notifications_outlined));
    await tester.pumpAndSettle();
    expect(find.byIcon(Icons.mark_email_unread_outlined), findsOneWidget);
    expect(find.byIcon(Icons.mark_email_read_outlined), findsOneWidget);
    expect(find.text('Thông báo mới'), findsOneWidget);
  });

  testWidgets(
    'mark as read disables duplicate taps and only updates after success',
    (tester) async {
      final api = _InteractionApi();
      api.pendingRead = Completer<void>();
      await _openAnnouncement(tester, api);
      await tester.tap(find.text('Đánh dấu đã đọc'));
      await tester.pump();
      expect(api.markReadCalls, 1);
      final button = tester.widget<FilledButton>(find.byType(FilledButton));
      expect(button.onPressed, isNull);
      await tester.tap(find.text('Đánh dấu đã đọc'));
      await tester.pump();
      expect(api.markReadCalls, 1);
      api.pendingRead!.complete();
      await tester.pumpAndSettle();
      expect(find.text('Đánh dấu đã đọc'), findsNothing);
      expect(api.detailCalls, 2);
    },
  );

  testWidgets('mark as read failure keeps the unread action available', (
    tester,
  ) async {
    final api = _InteractionApi()..readError = true;
    await _openAnnouncement(tester, api);
    await tester.tap(find.text('Đánh dấu đã đọc'));
    await tester.pumpAndSettle();
    expect(find.text('Không thể đánh dấu thông báo.'), findsOneWidget);
    expect(find.text('Đánh dấu đã đọc'), findsOneWidget);
    expect(api.detailCalls, 1);
    expect(api.read, isFalse);
  });

  testWidgets('announcement API error displays retry and retry reloads data', (
    tester,
  ) async {
    final api = _InteractionApi()..announcementListError = true;
    await _home(tester, api);
    await tester.tap(find.byIcon(Icons.notifications_outlined));
    await tester.pumpAndSettle();
    expect(find.text('Không thể tải thông báo.'), findsOneWidget);
    api.announcementListError = false;
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();
    expect(find.text('Thông báo mới'), findsOneWidget);
  });

  testWidgets('leave form rejects missing reason and dates', (tester) async {
    final api = _InteractionApi();
    await _openLeaveForm(tester, api);
    await tester.tap(find.text('Gửi đơn'));
    await tester.pumpAndSettle();
    expect(find.text('Vui lòng nhập lý do nghỉ.'), findsOneWidget);
    await tester.enterText(find.byType(TextFormField), 'Học sinh bị ốm');
    await tester.tap(find.text('Gửi đơn'));
    await tester.pumpAndSettle();
    expect(
      find.text('Vui lòng chọn ngày bắt đầu và kết thúc.'),
      findsOneWidget,
    );
    expect(api.submitCalls, 0);
  });

  testWidgets('leave form rejects an end date before the start date', (
    tester,
  ) async {
    final api = _InteractionApi();
    await _openLeaveForm(tester, api);
    final today = DateUtils.dateOnly(DateTime.now());
    await _pickDate(tester, 'Ngày bắt đầu', today);
    await _pickDate(
      tester,
      'Ngày kết thúc',
      today.subtract(const Duration(days: 1)),
    );
    await tester.enterText(find.byType(TextFormField), 'Học sinh bị ốm');
    await tester.tap(find.text('Gửi đơn'));
    await tester.pumpAndSettle();
    expect(
      find.text('Ngày kết thúc phải từ ngày bắt đầu trở đi.'),
      findsOneWidget,
    );
    expect(api.submitCalls, 0);
  });

  testWidgets(
    'leave submission prevents duplicates and retains form on API failure',
    (tester) async {
      final api = _InteractionApi()..pendingSubmit = Completer<LeaveRequest>();
      await _openLeaveForm(tester, api);
      final today = DateUtils.dateOnly(DateTime.now());
      await _pickDate(tester, 'Ngày bắt đầu', today);
      await _pickDate(tester, 'Ngày kết thúc', today);
      await tester.enterText(find.byType(TextFormField), 'Học sinh bị ốm');
      await tester.tap(find.text('Gửi đơn'));
      await tester.pump();
      expect(api.submitCalls, 1);
      expect(
        tester.widget<FilledButton>(find.byType(FilledButton)).onPressed,
        isNull,
      );
      api.pendingSubmit!.completeError(
        const ApiException(message: 'Yêu cầu bị trùng.', statusCode: 409),
      );
      await tester.pumpAndSettle();
      expect(find.text('Yêu cầu bị trùng.'), findsOneWidget);
      expect(find.text('Học sinh bị ốm'), findsOneWidget);
      expect(find.text('Gửi đơn'), findsOneWidget);
    },
  );

  testWidgets(
    'successful leave submission refreshes history and shows its real status',
    (tester) async {
      final api = _InteractionApi();
      await _openLeaveForm(tester, api);
      final today = DateUtils.dateOnly(DateTime.now());
      await _pickDate(tester, 'Ngày bắt đầu', today);
      await _pickDate(tester, 'Ngày kết thúc', today);
      await tester.enterText(find.byType(TextFormField), 'Học sinh bị ốm');
      await tester.tap(find.text('Gửi đơn'));
      await tester.pumpAndSettle();
      expect(api.submitCalls, 1);
      expect(api.submittedStudentId, 1);
      expect(find.text('Gửi đơn'), findsNothing);
      expect(find.textContaining('Chờ duyệt'), findsOneWidget);
      expect(find.textContaining('Học sinh bị ốm'), findsOneWidget);
    },
  );

  testWidgets(
    'switching children on academic results reloads and drops late grades',
    (tester) async {
      final api = _InteractionApi();
      final oldResponse = Completer<List<AcademicResult>>();
      api.academicResponse = (id) => id == 1
          ? oldResponse.future
          : Future.value(const [
              AcademicResult(
                subject: SubjectInfo(id: 8, name: 'Ngữ văn của Bình'),
                grades: [],
              ),
            ]);
      await _home(tester, api);
      await tester.tap(find.byIcon(Icons.menu_book_outlined));
      await tester.pumpAndSettle();
      await tester.tap(find.byIcon(Icons.bar_chart_outlined));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 400));
      await tester.tap(find.byType(DropdownButton<int>).hitTestable().first);
      await tester.pump(const Duration(milliseconds: 400));
      await tester.tap(find.text('Bình · S2').last);
      await tester.pumpAndSettle();
      oldResponse.complete(const [
        AcademicResult(
          subject: SubjectInfo(name: 'Điểm cũ của An'),
          grades: [],
        ),
      ]);
      await tester.pumpAndSettle();
      expect(api.academicRequests, [1, 2]);
      expect(find.text('Ngữ văn của Bình'), findsOneWidget);
      expect(find.text('Điểm cũ của An'), findsNothing);
    },
  );

  testWidgets(
    'switching children on attendance discards a late history response',
    (tester) async {
      final api = _InteractionApi();
      final oldResponse = Completer<Map<String, dynamic>>();
      api.attendanceResponse = (id) => id == 1
          ? oldResponse.future
          : Future.value(
              api._page([
                {
                  'date': '2026-11-04',
                  'status': 'LATE',
                  'remarks': 'Điểm danh của Bình',
                },
              ]),
            );
      await _home(tester, api);
      await tester.tap(find.byIcon(Icons.menu_book_outlined));
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byIcon(Icons.fact_check_outlined));
      await tester.tap(find.byIcon(Icons.fact_check_outlined));
      await tester.pump();
      await tester.pump(const Duration(milliseconds: 400));
      await tester.tap(find.byType(DropdownButton<int>).hitTestable().first);
      await tester.pump(const Duration(milliseconds: 400));
      await tester.tap(find.text('Bình · S2').last);
      await tester.pumpAndSettle();
      oldResponse.complete(
        api._page([
          {'status': 'PRESENT', 'remarks': 'Điểm danh cũ của An'},
        ]),
      );
      await tester.pumpAndSettle();
      expect(api.attendanceRequests, [1, 2]);
      expect(find.textContaining('Điểm danh của Bình'), findsOneWidget);
      expect(find.textContaining('Điểm danh cũ của An'), findsNothing);
    },
  );

  testWidgets(
    'new academic and attendance screens call selected-child Parent APIs',
    (tester) async {
      final api = _InteractionApi();
      await _home(tester, api);
      await tester.tap(find.byIcon(Icons.menu_book_outlined));
      await tester.pumpAndSettle();
      await tester.tap(find.byIcon(Icons.bar_chart_outlined));
      await tester.pumpAndSettle();
      expect(api.academicRequests, [1]);
      expect(find.text('Toán'), findsOneWidget);
      await tester.pageBack();
      await tester.pumpAndSettle();
      await tester.ensureVisible(find.byIcon(Icons.fact_check_outlined));
      await tester.tap(find.byIcon(Icons.fact_check_outlined));
      await tester.pumpAndSettle();
      expect(api.attendanceRequests, [1]);
      expect(find.textContaining('Đi học đầy đủ'), findsOneWidget);
    },
  );
}

Future<void> _home(
  WidgetTester tester,
  _InteractionApi api, {
  bool settle = true,
}) async {
  await tester.pumpWidget(MaterialApp(home: ParentHomeScreen(parentApi: api)));
  if (settle) {
    await tester.pumpAndSettle();
  } else {
    await tester.pump();
    await tester.pump();
  }
}

Future<void> _openAnnouncement(WidgetTester tester, _InteractionApi api) async {
  await _home(tester, api);
  await tester.tap(find.byIcon(Icons.notifications_outlined));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Thông báo mới'));
  await tester.pumpAndSettle();
}

Future<void> _openLeaveForm(WidgetTester tester, _InteractionApi api) async {
  await _home(tester, api);
  await tester.tap(find.byIcon(Icons.event_note_outlined));
  await tester.pumpAndSettle();
  await tester.tap(find.byIcon(Icons.add));
  await tester.pumpAndSettle();
}

Future<void> _pickDate(WidgetTester tester, String label, DateTime date) async {
  await tester.tap(find.text(label));
  await tester.pumpAndSettle();
  tester
      .widget<CalendarDatePicker>(find.byType(CalendarDatePicker))
      .onDateChanged(date);
  await tester.pump();
  await tester.tap(find.text('OK'));
  await tester.pumpAndSettle();
}

class _InteractionApi implements ParentApi {
  final linkedChildren = const [
    ParentChild(id: 1, fullName: 'An', studentCode: 'S1'),
    ParentChild(id: 2, fullName: 'Bình', studentCode: 'S2'),
  ];
  Future<StudentProfile> Function(int)? contextResponse;
  Future<ParentDashboard> Function(int?)? dashboardResponse;
  Future<List<AcademicResult>> Function(int)? academicResponse;
  Future<Map<String, dynamic>> Function(int)? attendanceResponse;
  Future<Map<String, dynamic>> Function(int, String?, int)? leaveResponse;
  Future<Map<String, dynamic>> Function(int, int)? evaluationResponse;
  Completer<void>? pendingRead;
  Completer<LeaveRequest>? pendingSubmit;
  LeaveRequest? createdLeave;
  int? submittedStudentId;
  bool read = false, readError = false, announcementListError = false;
  int markReadCalls = 0, detailCalls = 0, submitCalls = 0;
  final academicRequests = <int>[];
  final attendanceRequests = <int>[];
  final leaveRequestsLog = <Map<String, dynamic>>[];
  final evaluationRequests = <int>[];

  ParentDashboard dashboardFor(int studentId, {int present = 1}) =>
      ParentDashboard(
        parentName: 'Phụ huynh',
        children: linkedChildren,
        selectedStudent: StudentProfile(
          child: linkedChildren.singleWhere((c) => c.id == studentId),
        ),
        attendanceSummary: AttendanceSummary(
          totalSessions: present,
          present: present,
        ),
        recentAnnouncements: const [],
        academicSummary: const [],
        pendingLeaveRequests: 0,
      );
  Map<String, dynamic> _page(List<Map<String, dynamic>> content) => {
    'content': content,
    'page': 0,
    'size': 20,
    'totalPages': 1,
    'totalElements': content.length,
  };

  @override
  Future<List<ParentChild>> children() async => linkedChildren;
  @override
  Future<StudentProfile> childContext(int id) async => contextResponse == null
      ? StudentProfile(child: linkedChildren.singleWhere((c) => c.id == id))
      : contextResponse!(id);
  @override
  Future<ParentDashboard> dashboard(int? id) async =>
      dashboardResponse == null ? dashboardFor(id!) : dashboardResponse!(id);
  @override
  Future<Map<String, dynamic>> announcements({
    int page = 0,
    int size = 20,
  }) async {
    if (announcementListError) {
      throw const ApiException(message: 'Không thể tải thông báo.');
    }
    return _page([
      {
        'id': 10,
        'title': 'Thông báo mới',
        'preview': 'Nội dung mới',
        'read': read,
      },
      {'id': 11, 'title': 'Thông báo cũ', 'read': true},
    ]);
  }

  @override
  Future<ParentAnnouncement> announcement(int id) async {
    detailCalls++;
    return ParentAnnouncement(
      id: id,
      title: 'Thông báo mới',
      content: 'Nội dung đầy đủ',
      read: read,
    );
  }

  @override
  Future<void> markAnnouncementRead(int id) async {
    markReadCalls++;
    if (readError) {
      throw const ApiException(message: 'Không thể đánh dấu thông báo.');
    }
    if (pendingRead != null) await pendingRead!.future;
    read = true;
  }

  @override
  Future<Map<String, dynamic>> leaveRequests(
    int studentId, {
    String? status,
    int page = 0,
    int size = 20,
  }) async {
    leaveRequestsLog.add({
      'studentId': studentId,
      'status': status,
      'page': page,
    });
    if (leaveResponse != null) return leaveResponse!(studentId, status, page);
    return _page(
      createdLeave == null
          ? []
          : [
              {
                'id': createdLeave!.id,
                'status': createdLeave!.status,
                'startDate': createdLeave!.startDate?.toIso8601String(),
                'endDate': createdLeave!.endDate?.toIso8601String(),
                'reason': createdLeave!.reason,
              },
            ],
    );
  }

  @override
  Future<Map<String, dynamic>> evaluations(
    int studentId, {
    int? academicYearId,
    int? semesterId,
    int page = 0,
    int size = 20,
  }) async {
    evaluationRequests.add(page);
    return evaluationResponse == null
        ? _page([])
        : evaluationResponse!(studentId, page);
  }

  @override
  Future<LeaveRequest> submitLeave(
    int studentId, {
    required DateTime startDate,
    required DateTime endDate,
    required String reason,
  }) async {
    submitCalls++;
    submittedStudentId = studentId;
    if (pendingSubmit != null) return pendingSubmit!.future;
    createdLeave = LeaveRequest(
      id: 12,
      status: 'PENDING',
      startDate: startDate,
      endDate: endDate,
      reason: reason,
    );
    return createdLeave!;
  }

  @override
  Future<List<AcademicResult>> academicResults(
    int studentId, {
    int? academicYearId,
    int? semesterId,
  }) async {
    academicRequests.add(studentId);
    if (academicResponse != null) return academicResponse!(studentId);
    return const [
      AcademicResult(
        subject: SubjectInfo(id: 5, name: 'Toán'),
        grades: [],
      ),
    ];
  }

  @override
  Future<Map<String, dynamic>> attendance(
    int studentId, {
    DateTime? from,
    DateTime? to,
    int page = 0,
    int size = 20,
  }) async {
    attendanceRequests.add(studentId);
    if (attendanceResponse != null) return attendanceResponse!(studentId);
    return _page([
      {
        'id': 1,
        'date': '2026-11-04',
        'status': 'PRESENT',
        'remarks': 'Đi học đầy đủ',
      },
    ]);
  }

  @override
  Future<AttendanceSummary> attendanceSummary(
    int studentId, {
    int? academicYearId,
    int? semesterId,
  }) async => const AttendanceSummary(totalSessions: 1, present: 1);
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _SessionApi extends _InteractionApi implements ParentSessionEvents {
  final expired = StreamController<void>.broadcast();
  @override
  Stream<void> get sessionExpired => expired.stream;
}

class _SessionTokens extends TokenStorage {
  int clearCalls = 0;
  @override
  Future<void> clearAccessToken() async {
    clearCalls++;
  }
}
