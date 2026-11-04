import 'dart:async';
import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/core/storage/token_storage.dart';
import 'package:myschoolse1947/features/parent/data/models/parent_models.dart';
import 'package:myschoolse1947/features/parent/data/services/parent_api_service.dart';
import 'package:myschoolse1947/features/parent/presentation/parent_context_controller.dart';

void main() {
  test(
    'context uses student identity and parses the actual status field',
    () async {
      final api = _service((request) async {
        expect(request.url.path, '/api/parent/children/18/context');
        expect(request.headers['authorization'], 'Bearer test-jwt');
        expect(request.url.queryParameters, isEmpty);
        return _response({
          'id': 18,
          'fullName': 'Minh',
          'studentCode': 'HS18',
          'status': 'ACTIVE',
          'class': {'id': 7, 'name': '8A'},
        });
      });
      addTearDown(api.close);
      final context = await api.childContext(18);
      expect(context.child.enrollmentStatus, 'ACTIVE');
      expect(context.child.studentClass?.name, '8A');
      expect(context.child.school, isNull);
    },
  );

  test(
    'timetable and subject grades send supported filters without invented IDs',
    () async {
      final api = _service((request) async {
        if (request.url.path.endsWith('/timetable')) {
          expect(request.url.queryParameters, {
            'academicYearId': '9',
            'semesterId': '21',
          });
          return _response([
            {'weekday': 3, 'period': 2},
            {'weekday': 2, 'period': 3},
            {'weekday': 2, 'period': 1},
          ]);
        }
        expect(request.url.path, '/api/parent/children/18/subjects/6/grades');
        expect(request.url.queryParameters, {'semesterId': '21'});
        return _response([
          {'gradeItemId': 4, 'name': 'Kiểm tra', 'score': null, 'weight': 1},
        ]);
      });
      addTearDown(api.close);
      final rows = await api.timetable(18, academicYearId: 9, semesterId: 21);
      expect(rows.map((row) => '${row.weekday}/${row.period}'), [
        '2/1',
        '2/3',
        '3/2',
      ]);
      final grades = await api.subjectGrades(18, 6, 21);
      expect(grades.single.score, isNull);
      expect(grades.single.weight, 1);
    },
  );

  test('attendance sends encoded date range and paginates using backend page contract', () async {
    final api = _service((request) async {
      expect(request.url.path, '/api/parent/children/18/attendance');
      expect(request.url.queryParameters, {
        'from': '2026-11-01',
        'to': '2026-11-04',
        'page': '1',
        'size': '15',
      });
      return _response({
        'content': [
          {'date': '2026-11-03', 'status': 'EXCUSED'},
        ],
        'page': 1,
        'size': 15,
        'totalElements': 16,
        'totalPages': 2,
      });
    });
    addTearDown(api.close);
    final page = await api.attendance(
      18,
      from: DateTime(2026, 11, 1),
      to: DateTime(2026, 11, 4),
      page: 1,
      size: 15,
    );
    expect(page['totalPages'], 2);
    expect(
      AttendanceRecord.fromJson((page['content'] as List).single).status,
      'EXCUSED',
    );
  });

  test(
    'leave submission sends dates and reason without approval identities',
    () async {
      final api = _service((request) async {
        expect(request.method, 'POST');
        expect(request.url.path, '/api/parent/children/18/leave-requests');
        expect(jsonDecode(request.body), {
          'startDate': '2026-11-10',
          'endDate': '2026-11-11',
          'reason': 'Học sinh bị ốm',
        });
        return _response({'id': 5, 'status': 'PENDING'});
      });
      addTearDown(api.close);
      final created = await api.submitLeave(
        18,
        startDate: DateTime(2026, 11, 10),
        endDate: DateTime(2026, 11, 11),
        reason: '  Học sinh bị ốm  ',
      );
      expect(created.id, 5);
      expect(created.status, 'PENDING');
    },
  );

  test('mark read and device registration/removal use authenticated write contracts', () async {
    final sent = <http.Request>[];
    final api = _service((request) async {
      sent.add(request);
      expect(request.headers['authorization'], 'Bearer test-jwt');
      return _response(null);
    });
    addTearDown(api.close);
    await api.markAnnouncementRead(33);
    await api.registerDevice(
      token: 'device-token',
      platform: 'ANDROID',
      deviceName: 'Điện thoại',
    );
    await api.removeDevice('device-token');
    expect(sent.map((r) => r.method), ['POST', 'POST', 'DELETE']);
    expect(sent.first.url.path, '/api/parent/announcements/33/read');
    expect(jsonDecode(sent.first.body), isEmpty);
    expect(jsonDecode(sent[1].body), {
      'token': 'device-token',
      'platform': 'ANDROID',
      'deviceName': 'Điện thoại',
    });
    expect(sent.last.url.path, '/api/parent/devices');
    expect(jsonDecode(sent.last.body), {'token': 'device-token'});
  });

  test(
    '401 clears auth and emits expiration once while 403 keeps the session',
    () async {
      final tokens = _MemoryTokens();
      var status = 403;
      final api = _service(
        (_) async => _response(null, status: status),
        tokens: tokens,
      );
      addTearDown(api.close);
      var expirations = 0;
      final subscription = api.sessionExpired.listen((_) => expirations++);
      addTearDown(subscription.cancel);
      await expectLater(
        api.children(),
        throwsA(isA<ApiException>().having((e) => e.statusCode, 'status', 403)),
      );
      expect(tokens.clears, 0);
      expect(expirations, 0);
      status = 401;
      await expectLater(
        api.children(),
        throwsA(isA<ApiException>().having((e) => e.statusCode, 'status', 401)),
      );
      await Future<void>.delayed(Duration.zero);
      expect(tokens.clears, 1);
      expect(expirations, 1);
      await expectLater(api.children(), throwsA(isA<ApiException>()));
      expect(tokens.clears, 1);
    },
  );

  test('missing auth token rejects requests before sending private student queries', () async {
    var sent = false;
    final tokens = _MemoryTokens()..token = null;
    final api = _service((_) async {
      sent = true;
      return _response([]);
    }, tokens: tokens);
    addTearDown(api.close);
    await expectLater(
      api.children(),
      throwsA(isA<ApiException>().having((e) => e.statusCode, 'status', 401)),
    );
    expect(sent, isFalse);
  });

  test('missing required identities reject malformed payloads instead of using student zero', () {
    expect(
      () => ParentChild.fromJson({'fullName': 'Minh'}),
      throwsFormatException,
    );
    expect(
      () => ParentAnnouncement.fromJson({'title': 'Thông báo'}),
      throwsFormatException,
    );
    expect(() => LeaveRequest.fromJson({}), throwsFormatException);
  });

  test(
    'context ignores previous child responses and async work after disposal',
    () async {
      final api = _ContextApi();
      final controller = ParentContextController(api);
      final initial = controller.load();
      await Future<void>.delayed(Duration.zero);
      final next = controller.selectChild(api.linked.last);
      api.contexts[2]!.complete(StudentProfile(child: api.linked.last));
      await next;
      api.contexts[1]!.complete(StudentProfile(child: api.linked.first));
      await initial;
      expect(controller.selectedChild?.id, 2);
      expect(controller.selectedContext?.child.id, 2);
      final pending = controller.selectChild(api.linked.first);
      controller.dispose();
      api.contexts[1]!.complete(StudentProfile(child: api.linked.first));
      await expectLater(pending, completes);
    },
  );
}

ParentApiService _service(
  Future<http.Response> Function(http.Request) send, {
  _MemoryTokens? tokens,
}) => ParentApiService(
  apiClient: ApiClient(
    httpClient: MockClient(send),
    baseUri: Uri.parse('http://localhost:8080/'),
  ),
  tokenStorage: tokens ?? _MemoryTokens(),
);

http.Response _response(Object? data, {int status = 200}) =>
    http.Response.bytes(
      utf8.encode(
        jsonEncode({'code': status, 'message': 'Result', 'data': data}),
      ),
      status,
      headers: {'content-type': 'application/json; charset=utf-8'},
    );

class _MemoryTokens extends TokenStorage {
  String? token = 'test-jwt';
  int clears = 0;
  @override
  Future<String?> readAccessToken() async => token;
  @override
  Future<void> clearAccessToken() async {
    token = null;
    clears++;
  }
}

class _ContextApi implements ParentApi {
  final linked = const [
    ParentChild(id: 1, fullName: 'An', studentCode: 'A'),
    ParentChild(id: 2, fullName: 'Bình', studentCode: 'B'),
  ];
  final contexts = <int, Completer<StudentProfile>>{};
  @override
  Future<List<ParentChild>> children() async => linked;
  @override
  Future<StudentProfile> childContext(int id) {
    final completer = Completer<StudentProfile>();
    contexts[id] = completer;
    return completer.future;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}
