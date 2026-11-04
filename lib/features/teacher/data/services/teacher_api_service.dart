import 'package:flutter/foundation.dart';

import '../../../../core/network/api_client.dart';
import '../../../../core/storage/token_storage.dart';

@immutable
class TeacherClass {
  const TeacherClass({
    required this.assignmentId,
    required this.className,
    required this.schoolName,
    required this.studentCount,
  });
  final int assignmentId;
  final String className;
  final String schoolName;
  final int studentCount;
  factory TeacherClass.fromJson(Map<String, dynamic> j) => TeacherClass(
    assignmentId: (j['assignmentId'] as num).toInt(),
    className: j['className'] as String,
    schoolName: j['schoolName'] as String,
    studentCount: (j['studentCount'] as num).toInt(),
  );
}

@immutable
class TeacherStudent {
  const TeacherStudent({
    required this.id,
    required this.studentCode,
    required this.fullName,
    required this.className,
    required this.schoolName,
    this.avatarUrl,
  });
  final int id;
  final String studentCode;
  final String fullName;
  final String className;
  final String schoolName;
  final String? avatarUrl;
  factory TeacherStudent.fromJson(Map<String, dynamic> j) => TeacherStudent(
    id: (j['id'] as num).toInt(),
    studentCode: j['studentCode'] as String,
    fullName: j['fullName'] as String,
    className: j['className'] as String,
    schoolName: j['schoolName'] as String,
    avatarUrl: j['avatarUrl'] as String?,
  );
}

@immutable
class TeacherLesson {
  const TeacherLesson({
    required this.id,
    required this.date,
    required this.periodNumber,
    required this.subjectCode,
    required this.subjectName,
    required this.room,
    required this.startTime,
    required this.endTime,
  });
  final int id;
  final DateTime date;
  final int periodNumber;
  final String subjectCode;
  final String subjectName;
  final String room;
  final String startTime;
  final String endTime;
  factory TeacherLesson.fromJson(Map<String, dynamic> j) => TeacherLesson(
    id: (j['id'] as num).toInt(),
    date: DateTime.parse(j['date'] as String),
    periodNumber: (j['periodNumber'] as num).toInt(),
    subjectCode: j['subjectCode'] as String,
    subjectName: j['subjectName'] as String,
    room: j['room'] as String,
    startTime: j['startTime'] as String,
    endTime: j['endTime'] as String,
  );
}

class TeacherAttendance {
  TeacherAttendance({
    required this.studentId,
    required this.studentCode,
    required this.fullName,
    required this.status,
    this.note,
    required this.date,
  });
  final int studentId;
  final String studentCode;
  final String fullName;
  String? status;
  String? note;
  final DateTime date;
  factory TeacherAttendance.fromJson(Map<String, dynamic> j) =>
      TeacherAttendance(
        studentId: (j['studentId'] as num).toInt(),
        studentCode: j['studentCode'] as String,
        fullName: j['fullName'] as String,
        status: j['status'] as String?,
        note: j['note'] as String?,
        date: DateTime.parse(j['date'] as String),
      );
}

abstract interface class TeacherService {
  Future<Map<String, dynamic>> loadOverview();
  Future<List<TeacherLesson>> loadSchedule(DateTime weekStart);
  Future<List<TeacherClass>> loadClasses();
  Future<List<TeacherStudent>> loadStudents(int assignmentId, {String? search});
  Future<List<TeacherAttendance>> loadAttendance(
    int assignmentId,
    DateTime date,
  );
  Future<List<TeacherAttendance>> saveAttendance(
    int assignmentId,
    DateTime date,
    List<TeacherAttendance> marks,
  );
}

class TeacherApiService implements TeacherService {
  TeacherApiService({ApiClient? apiClient, TokenStorage? tokenStorage})
    : _api = apiClient ?? ApiClient(),
      _storage = tokenStorage ?? TokenStorage();
  final ApiClient _api;
  final TokenStorage _storage;
  static const _base = 'api/v1/teacher';

  Future<Map<String, dynamic>> _get(
    String path, {
    Map<String, String>? query,
  }) async =>
      _api.getJson(path, queryParameters: query, bearerToken: await _token());
  Future<String> _token() async {
    final token = (await _storage.readAccessToken())?.trim();
    if (token == null || token.isEmpty) {
      throw const ApiException(
        message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        statusCode: 401,
      );
    }
    return token;
  }

  Map<String, dynamic> _map(Object? value) => value is Map<String, dynamic>
      ? value
      : Map<String, dynamic>.from(value as Map);
  List<dynamic> _list(Map<String, dynamic> envelope) =>
      envelope['data'] as List<dynamic>;

  @override
  Future<Map<String, dynamic>> loadOverview() async =>
      _map((await _get('$_base/me'))['data']);
  @override
  Future<List<TeacherClass>> loadClasses() async =>
      _list(await _get('$_base/classes'))
          .map((e) => TeacherClass.fromJson(_map(e)))
          .toList();
  @override
  Future<List<TeacherLesson>> loadSchedule(DateTime weekStart) async => _list(
    await _get('$_base/schedule', query: {'weekStart': _date(weekStart)}),
  ).map((e) => TeacherLesson.fromJson(_map(e))).toList();
  @override
  Future<List<TeacherStudent>> loadStudents(int id, {String? search}) async =>
      _list(
        await _get(
          '$_base/classes/$id/students',
          query: search == null || search.trim().isEmpty
              ? null
              : {'search': search.trim()},
        ),
      ).map((e) => TeacherStudent.fromJson(_map(e))).toList();
  @override
  Future<List<TeacherAttendance>> loadAttendance(int id, DateTime date) async =>
      _list(
        await _get(
          '$_base/classes/$id/attendance',
          query: {'date': _date(date)},
        ),
      ).map((e) => TeacherAttendance.fromJson(_map(e))).toList();
  @override
  Future<List<TeacherAttendance>> saveAttendance(
    int id,
    DateTime date,
    List<TeacherAttendance> marks,
  ) async {
    final response = await _api.putJson(
      '$_base/classes/$id/attendance',
      queryParameters: {'date': _date(date)},
      bearerToken: await _token(),
      body: marks
          .map(
            (m) => {
              'studentId': m.studentId,
              'status': m.status ?? 'PRESENT',
              'note': m.note,
            },
          )
          .toList(),
    );
    return _list(response)
        .map((e) => TeacherAttendance.fromJson(_map(e)))
        .toList();
  }

  String _date(DateTime date) =>
      '${date.year.toString().padLeft(4, '0')}-${date.month.toString().padLeft(2, '0')}-${date.day.toString().padLeft(2, '0')}';
}
