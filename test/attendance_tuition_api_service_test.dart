import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/core/storage/token_storage.dart';
import 'package:myschoolse1947/features/attendance/data/models/attendance_report.dart';
import 'package:myschoolse1947/features/attendance/data/services/attendance_api_service.dart';
import 'package:myschoolse1947/features/tuition/data/models/tuition_report.dart';
import 'package:myschoolse1947/features/tuition/data/services/tuition_api_service.dart';

void main() {
  test('attendance service sends month and parses monthly report', () async {
    final client = MockClient((request) async {
      expect(
        request.url,
        Uri.parse(
          'http://localhost:8080/api/v1/parents/me/attendance?month=2026-11',
        ),
      );
      expect(request.headers['authorization'], 'Bearer signed-jwt');
      return _jsonResponse(_attendanceEnvelope);
    });
    final service = AttendanceApiService(
      apiClient: ApiClient(
        httpClient: client,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage(' signed-jwt '),
    );
    addTearDown(service.close);

    final report = await service.fetchMonth(DateTime(2026, 11, 20));

    expect(report.month, DateTime(2026, 11));
    expect(report.attendanceRate, 50);
    expect(report.records, hasLength(4));
    expect(report.records[1].status, AttendanceStatus.late);
  });

  test('tuition service parses paid, partial and due items', () async {
    final client = MockClient((request) async {
      expect(
        request.url,
        Uri.parse('http://localhost:8080/api/v1/parents/me/tuition'),
      );
      expect(request.headers['authorization'], 'Bearer signed-jwt');
      return _jsonResponse(_tuitionEnvelope);
    });
    final service = TuitionApiService(
      apiClient: ApiClient(
        httpClient: client,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage('signed-jwt'),
    );
    addTearDown(service.close);

    final report = await service.fetchReport();

    expect(report.totalPaid, 140000);
    expect(report.totalDue, 260000);
    expect(report.paidItems.single.status, TuitionItemStatus.paid);
    expect(report.dueItems.first.status, TuitionItemStatus.partiallyPaid);
  });

  test('new parent services reject a missing token before HTTP', () async {
    final client = MockClient((_) {
      fail('HTTP must not be called without a token.');
    });
    final attendance = AttendanceApiService(
      apiClient: ApiClient(
        httpClient: client,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage(null),
    );
    final tuition = TuitionApiService(
      apiClient: ApiClient(
        httpClient: client,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
      tokenStorage: _FakeTokenStorage(null),
    );
    addTearDown(attendance.close);
    addTearDown(tuition.close);

    await expectLater(
      attendance.fetchMonth(DateTime(2026, 11)),
      throwsA(
        isA<ApiException>().having((error) => error.statusCode, 'status', 401),
      ),
    );
    await expectLater(
      tuition.fetchReport(),
      throwsA(
        isA<ApiException>().having((error) => error.statusCode, 'status', 401),
      ),
    );
  });
}

http.Response _jsonResponse(Map<String, dynamic> body) {
  return http.Response.bytes(
    utf8.encode(jsonEncode(body)),
    200,
    headers: const <String, String>{
      'content-type': 'application/json; charset=utf-8',
    },
  );
}

const _student = <String, dynamic>{
  'studentCode': 'HS001',
  'fullName': 'Nguyễn Minh Anh',
  'className': '10A1',
  'schoolName': 'Trường THPT FPT',
};

const _attendanceEnvelope = <String, dynamic>{
  'code': 200,
  'message': 'OK',
  'data': <String, dynamic>{
    'student': _student,
    'month': '2026-11',
    'totalSchoolDays': 4,
    'presentDays': 1,
    'lateDays': 1,
    'excusedAbsenceDays': 1,
    'unexcusedAbsenceDays': 1,
    'attendanceRate': 50.0,
    'records': <Map<String, dynamic>>[
      <String, dynamic>{
        'id': 1,
        'date': '2026-11-03',
        'status': 'PRESENT',
        'checkInTime': '07:25',
        'note': null,
      },
      <String, dynamic>{
        'id': 2,
        'date': '2026-11-04',
        'status': 'LATE',
        'checkInTime': '07:48',
        'note': 'Đi muộn',
      },
      <String, dynamic>{
        'id': 3,
        'date': '2026-11-05',
        'status': 'EXCUSED_ABSENCE',
        'checkInTime': null,
        'note': 'Nghỉ ốm',
      },
      <String, dynamic>{
        'id': 4,
        'date': '2026-11-06',
        'status': 'UNEXCUSED_ABSENCE',
        'checkInTime': null,
        'note': null,
      },
    ],
  },
};

const _tuitionEnvelope = <String, dynamic>{
  'code': 200,
  'message': 'OK',
  'data': <String, dynamic>{
    'student': _student,
    'academicYear': '2026-2027',
    'totalAmount': 400000,
    'totalPaid': 140000,
    'totalDue': 260000,
    'paidItems': <Map<String, dynamic>>[
      <String, dynamic>{
        'id': 1,
        'feeCode': 'PAID',
        'title': 'Học phí tháng 9',
        'description': null,
        'academicYear': '2026-2027',
        'dueDate': '2026-09-15',
        'amount': 100000,
        'paidAmount': 100000,
        'remainingAmount': 0,
        'paidDate': '2026-09-10',
        'status': 'PAID',
      },
    ],
    'dueItems': <Map<String, dynamic>>[
      <String, dynamic>{
        'id': 2,
        'feeCode': 'PARTIAL',
        'title': 'Phí bán trú',
        'description': 'Đã nộp một phần',
        'academicYear': '2026-2027',
        'dueDate': '2026-11-15',
        'amount': 100000,
        'paidAmount': 40000,
        'remainingAmount': 60000,
        'paidDate': '2026-10-25',
        'status': 'PARTIALLY_PAID',
      },
      <String, dynamic>{
        'id': 3,
        'feeCode': 'DUE',
        'title': 'Học phí học kỳ 2',
        'description': null,
        'academicYear': '2026-2027',
        'dueDate': '2027-01-15',
        'amount': 200000,
        'paidAmount': 0,
        'remainingAmount': 200000,
        'paidDate': null,
        'status': 'DUE',
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
