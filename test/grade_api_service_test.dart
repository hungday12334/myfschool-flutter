import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/core/storage/token_storage.dart';
import 'package:myschoolse1947/features/grades/data/models/grade_report.dart';
import 'package:myschoolse1947/features/grades/data/services/grade_api_service.dart';

void main() {
  test('fetchReport sends parent endpoint, period and bearer token', () async {
    final client = MockClient((request) async {
      expect(request.method, 'GET');
      expect(
        request.url,
        Uri.parse('http://localhost:8080/api/v1/parents/me/grades?period=1'),
      );
      expect(request.headers['authorization'], 'Bearer signed-jwt');
      return http.Response.bytes(
        utf8.encode(jsonEncode(_reportEnvelope)),
        200,
        headers: const <String, String>{
          'content-type': 'application/json; charset=utf-8',
        },
      );
    });
    final service = GradeApiService(
      apiClient: ApiClient(
        httpClient: client,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage(' signed-jwt '),
    );
    addTearDown(service.close);

    final report = await service.fetchReport(GradePeriod.semester1);

    expect(report.student.fullName, 'Nguyễn Minh Anh');
    expect(report.subjects.single.subjectName, 'Toán');
    expect(report.subjects.single.averageScore, 8.9);
  });

  test('fetchSubject encodes subject code and parses components', () async {
    final client = MockClient((request) async {
      expect(
        request.url,
        Uri.parse(
          'http://localhost:8080/api/v1/parents/me/grades/'
          'MATH%20ADVANCED?period=all',
        ),
      );
      return http.Response.bytes(
        utf8.encode(jsonEncode(_detailEnvelope)),
        200,
        headers: const <String, String>{
          'content-type': 'application/json; charset=utf-8',
        },
      );
    });
    final service = GradeApiService(
      apiClient: ApiClient(
        httpClient: client,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage('signed-jwt'),
    );
    addTearDown(service.close);

    final detail = await service.fetchSubject(
      subjectCode: 'MATH ADVANCED',
      period: GradePeriod.fullYear,
    );

    expect(detail.assessments, hasLength(4));
    expect(detail.assessments.last.coefficient, 3);
    expect(detail.averageScore, 8.9);
  });

  test('fetchReport rejects missing token without HTTP call', () async {
    final service = GradeApiService(
      apiClient: ApiClient(
        httpClient: MockClient((_) {
          fail('HTTP must not be called without an access token.');
        }),
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage(null),
    );
    addTearDown(service.close);

    await expectLater(
      service.fetchReport(GradePeriod.semester1),
      throwsA(
        isA<ApiException>().having(
          (error) => error.statusCode,
          'statusCode',
          401,
        ),
      ),
    );
  });
}

const Map<String, dynamic> _student = <String, dynamic>{
  'studentCode': 'HS001',
  'fullName': 'Nguyễn Minh Anh',
  'className': '10A1',
  'schoolName': 'Trường THPT FPT',
};

const Map<String, dynamic> _reportEnvelope = <String, dynamic>{
  'code': 200,
  'message': 'OK',
  'data': <String, dynamic>{
    'student': _student,
    'academicYear': '2026-2027',
    'period': 'SEMESTER_1',
    'subjects': <Map<String, dynamic>>[
      <String, dynamic>{
        'subjectCode': 'MATH',
        'subjectName': 'Toán',
        'averageScore': 8.9,
      },
    ],
  },
};

const Map<String, dynamic> _detailEnvelope = <String, dynamic>{
  'code': 200,
  'message': 'OK',
  'data': <String, dynamic>{
    'student': _student,
    'academicYear': '2026-2027',
    'period': 'FULL_YEAR',
    'subjectCode': 'MATH',
    'subjectName': 'Toán',
    'averageScore': 8.9,
    'assessments': <Map<String, dynamic>>[
      <String, dynamic>{
        'id': 1,
        'type': 'ORAL',
        'label': 'Miệng',
        'coefficient': 1,
        'score': 8.0,
        'semester': 1,
      },
      <String, dynamic>{
        'id': 2,
        'type': 'FIFTEEN_MINUTES',
        'label': '15 phút',
        'coefficient': 1,
        'score': 7.5,
        'semester': 1,
      },
      <String, dynamic>{
        'id': 3,
        'type': 'ONE_PERIOD',
        'label': '1 tiết',
        'coefficient': 2,
        'score': 9.0,
        'semester': 1,
      },
      <String, dynamic>{
        'id': 4,
        'type': 'SEMESTER',
        'label': 'Học kỳ',
        'coefficient': 3,
        'score': 9.5,
        'semester': 1,
      },
    ],
  },
};

class _FakeTokenStorage extends TokenStorage {
  _FakeTokenStorage(this.token);

  final String? token;

  @override
  Future<String?> readAccessToken() async => token;
}
