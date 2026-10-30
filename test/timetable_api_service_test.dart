import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/core/storage/token_storage.dart';
import 'package:myschoolse1947/features/timetable/data/models/timetable_week.dart';
import 'package:myschoolse1947/features/timetable/data/services/timetable_api_service.dart';

void main() {
  test(
    'fetchWeek sends Monday and bearer token then parses timetable',
    () async {
      final httpClient = MockClient((http.Request request) async {
        expect(request.method, 'GET');
        expect(
          request.url,
          Uri.parse(
            'http://localhost:8080/api/v1/students/me/timetable'
            '?weekStart=2026-10-05',
          ),
        );
        expect(request.headers['authorization'], 'Bearer signed-jwt');

        return http.Response(
          jsonEncode(_weekEnvelope(DateTime(2026, 10, 5))),
          200,
          headers: const <String, String>{'content-type': 'application/json'},
        );
      });
      final service = TimetableApiService(
        apiClient: ApiClient(
          httpClient: httpClient,
          baseUri: Uri.parse('http://localhost:8080/'),
        ),
        tokenStorage: _FakeTokenStorage(' signed-jwt '),
      );
      addTearDown(service.close);

      final week = await service.fetchWeek(DateTime(2026, 10, 9));

      expect(week.weekStart, DateTime(2026, 10, 5));
      expect(week.days, hasLength(7));
      expect(week.days.first.lessons.single.subjectName, 'Toán');
      expect(week.days.first.lessons.single.teacherName, 'Cô Nguyễn Thị Lan');
    },
  );

  test('fetchWeek rejects a missing access token before making a request', () {
    final service = TimetableApiService(
      apiClient: ApiClient(
        httpClient: MockClient((_) {
          fail('HTTP must not be called without an access token.');
        }),
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage(null),
    );
    addTearDown(service.close);

    expect(
      service.fetchWeek(DateTime(2026, 10, 5)),
      throwsA(
        isA<ApiException>()
            .having((error) => error.statusCode, 'statusCode', 401)
            .having(
              (error) => error.message,
              'message',
              contains('đăng nhập lại'),
            ),
      ),
    );
  });

  test('fetchWeek rejects a malformed success envelope', () async {
    final httpClient = MockClient(
      (_) async => http.Response(
        jsonEncode(<String, dynamic>{
          'code': 200,
          'message': 'OK',
          'data': <String, dynamic>{
            'weekStart': '2026-10-05',
            'weekEnd': '2026-10-11',
            'days': <Object>[],
          },
        }),
        200,
      ),
    );
    final service = TimetableApiService(
      apiClient: ApiClient(
        httpClient: httpClient,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage('signed-jwt'),
    );
    addTearDown(service.close);

    await expectLater(
      service.fetchWeek(DateTime(2026, 10, 5)),
      throwsA(
        isA<ApiException>().having(
          (error) => error.message,
          'message',
          'Phản hồi từ máy chủ không hợp lệ.',
        ),
      ),
    );
  });

  test('model rejects a non-consecutive week and mismatched range', () {
    final envelope = _weekEnvelope(DateTime(2026, 10, 5));
    final data = envelope['data']! as Map<String, dynamic>;
    data['weekEnd'] = '2026-10-12';
    final days = data['days']! as List<Map<String, dynamic>>;
    days[3]['date'] = '2026-10-09';

    expect(
      () => TimetableWeek.fromEnvelope(envelope),
      throwsA(isA<FormatException>()),
    );
  });

  test('fetchWeek preserves backend authorization error details', () async {
    final httpClient = MockClient(
      (_) async => http.Response(
        jsonEncode(<String, dynamic>{
          'code': 40300,
          'message': 'Only students can view this timetable',
          'data': null,
        }),
        403,
        headers: const <String, String>{'content-type': 'application/json'},
      ),
    );
    final service = TimetableApiService(
      apiClient: ApiClient(
        httpClient: httpClient,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage('signed-jwt'),
    );
    addTearDown(service.close);

    await expectLater(
      service.fetchWeek(DateTime(2026, 10, 5)),
      throwsA(
        isA<ApiException>()
            .having((error) => error.statusCode, 'statusCode', 403)
            .having((error) => error.code, 'code', 40300)
            .having(
              (error) => error.message,
              'message',
              'Only students can view this timetable',
            ),
      ),
    );
  });
}

Map<String, dynamic> _weekEnvelope(DateTime monday) {
  return <String, dynamic>{
    'code': 200,
    'message': 'Timetable retrieved successfully',
    'data': <String, dynamic>{
      'weekStart': _formatDate(monday),
      'weekEnd': _formatDate(monday.add(const Duration(days: 6))),
      'days': List<Map<String, dynamic>>.generate(7, (index) {
        final date = monday.add(Duration(days: index));
        return <String, dynamic>{
          'date': _formatDate(date),
          'dayOfWeek': _dayOfWeek(date.weekday),
          'lessons': index == 0
              ? <Map<String, dynamic>>[
                  <String, dynamic>{
                    'id': 1,
                    'periodNumber': 1,
                    'subjectCode': 'MATH',
                    'subjectName': 'Toán',
                    'teacherName': 'Cô Nguyễn Thị Lan',
                    'room': 'Phòng 101',
                    'startTime': '07:30',
                    'endTime': '08:15',
                  },
                ]
              : <Map<String, dynamic>>[],
        };
      }),
    },
  };
}

String _formatDate(DateTime value) {
  return '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}

String _dayOfWeek(int weekday) => switch (weekday) {
  DateTime.monday => 'MONDAY',
  DateTime.tuesday => 'TUESDAY',
  DateTime.wednesday => 'WEDNESDAY',
  DateTime.thursday => 'THURSDAY',
  DateTime.friday => 'FRIDAY',
  DateTime.saturday => 'SATURDAY',
  _ => 'SUNDAY',
};

class _FakeTokenStorage extends TokenStorage {
  _FakeTokenStorage(this.token);

  final String? token;

  @override
  Future<String?> readAccessToken() async => token;
}
