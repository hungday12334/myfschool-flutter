import 'dart:async';

import 'package:myschoolse1947/core/config/api_config.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/core/storage/token_storage.dart';

import '../models/parent_models.dart';

abstract interface class ParentApi {
  Future<List<ParentChild>> children();
  Future<ParentDashboard> dashboard(int? studentId);
  Future<StudentProfile> childContext(int studentId);
  Future<StudentProfile> studentProfile(int studentId);
  Future<Map<String, dynamic>?> classInformation(int studentId);
  Future<List<TimetableEntry>> timetable(
    int studentId, {
    int? academicYearId,
    int? semesterId,
  });
  Future<List<AcademicResult>> academicResults(
    int studentId, {
    int? academicYearId,
    int? semesterId,
  });
  Future<List<StudentGrade>> subjectGrades(
    int studentId,
    int subjectId,
    int semesterId,
  );
  Future<Map<String, dynamic>> evaluations(
    int studentId, {
    int? academicYearId,
    int? semesterId,
    int page,
    int size,
  });
  Future<Map<String, dynamic>> attendance(
    int studentId, {
    DateTime? from,
    DateTime? to,
    int page,
    int size,
  });
  Future<AttendanceSummary> attendanceSummary(
    int studentId, {
    int? academicYearId,
    int? semesterId,
  });
  Future<Map<String, dynamic>> announcements({int page, int size});
  Future<ParentAnnouncement> announcement(int announcementId);
  Future<void> markAnnouncementRead(int announcementId);
  Future<LeaveRequest> submitLeave(
    int studentId, {
    required DateTime startDate,
    required DateTime endDate,
    required String reason,
  });
  Future<Map<String, dynamic>> leaveRequests(
    int studentId, {
    String? status,
    int page,
    int size,
  });
  Future<LeaveRequest> leaveRequest(int requestId);
  Future<void> registerDevice({
    required String token,
    required String platform,
    String? deviceName,
  });
  Future<void> removeDevice(String token);
}

/// Kept separate from [ParentApi] so service fakes need only implement requests.
abstract interface class ParentSessionEvents {
  Stream<void> get sessionExpired;
}

class ParentApiService implements ParentApi, ParentSessionEvents {
  ParentApiService({ApiClient? apiClient, TokenStorage? tokenStorage})
    : _api = apiClient ?? ApiClient(),
      _tokens = tokenStorage ?? TokenStorage();

  final ApiClient _api;
  final TokenStorage _tokens;
  final _expired = StreamController<void>.broadcast();
  bool _didExpire = false;

  @override
  Stream<void> get sessionExpired => _expired.stream;
  String _path(String suffix) => '${ApiConfig.parentApiPath}/$suffix';

  Future<Map<String, dynamic>> _get(
    String path, {
    Map<String, String>? query,
  }) async {
    final token = await _requiredToken();
    try {
      return await _api.getJson(
        path,
        queryParameters: query,
        bearerToken: token,
      );
    } on ApiException catch (error) {
      await _expireToken(error);
      throw _localizedError(error);
    }
  }

  Future<Map<String, dynamic>> _post(
    String path,
    Map<String, dynamic> body,
  ) async {
    final token = await _requiredToken();
    try {
      return await _api.postJson(path, body: body, bearerToken: token);
    } on ApiException catch (error) {
      await _expireToken(error);
      throw _localizedError(error);
    }
  }

  Future<Map<String, dynamic>> _delete(
    String path,
    Map<String, dynamic> body,
  ) async {
    final token = await _requiredToken();
    try {
      return await _api.deleteJson(path, body: body, bearerToken: token);
    } on ApiException catch (error) {
      await _expireToken(error);
      throw _localizedError(error);
    }
  }

  Future<String> _requiredToken() async {
    if (_didExpire) {
      throw const ApiException(
        message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        statusCode: 401,
      );
    }
    final token = (await _tokens.readAccessToken())?.trim();
    if (token == null || token.isEmpty) {
      await _expireSession();
      throw const ApiException(
        message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        statusCode: 401,
      );
    }
    return token;
  }

  Future<void> _expireToken(ApiException error) async {
    if (error.statusCode == 401) await _expireSession();
  }

  Future<void> _expireSession() async {
    if (_didExpire) return;
    _didExpire = true;
    try {
      await _tokens.clearAccessToken();
    } catch (_) {
      // A keychain failure must not keep an expired session on screen.
      // Further requests from this service are blocked by _didExpire.
    } finally {
      if (!_expired.isClosed) _expired.add(null);
    }
  }

  ApiException _localizedError(ApiException error) {
    final known = switch (error.message) {
      'A pending leave request overlaps these dates' =>
        'Đã có đơn nghỉ học chờ duyệt trùng khoảng thời gian này.',
      'Student has no current class' =>
        'Học sinh chưa được xếp lớp. Vui lòng liên hệ nhà trường.',
      'endDate must be on or after startDate' =>
        'Ngày kết thúc phải từ ngày bắt đầu trở đi.',
      'to must be on or after from' =>
        'Ngày kết thúc phải từ ngày bắt đầu trở đi.',
      'No matching academic year' || 'No semester found' =>
        'Nhà trường chưa thiết lập năm học hoặc học kỳ phù hợp.',
      'Academic year is outside this school' ||
      'Semester does not belong to requested school/year' =>
        'Năm học hoặc học kỳ không thuộc trường của học sinh.',
      "Subject does not match this student's semester/class" ||
      "Subject is outside this student's school" =>
        'Môn học không thuộc lớp hoặc học kỳ của học sinh.',
      'Device token is already registered to another account' => 'Thiết bị đang được đăng ký với tài khoản khác. Vui lòng đăng xuất tài khoản đó trước.',
      _ => null,
    };
    if (known == null && RegExp('[À-ỹĐđ]').hasMatch(error.message)) {
      return error;
    }
    final message =
        known ??
        switch (error.statusCode) {
          400 => 'Thông tin không hợp lệ. Vui lòng kiểm tra dữ liệu đã nhập.',
          401 => 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
          403 => 'Bạn không có quyền xem hoặc thay đổi thông tin này.',
          404 => 'Không tìm thấy thông tin yêu cầu.',
          409 => 'Dữ liệu bị trùng hoặc xung đột. Vui lòng tải lại và thử lại.',
          int status when status >= 500 =>
            'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.',
          _ => error.message,
        };
    return ApiException(
      message: message,
      statusCode: error.statusCode,
      code: error.code,
    );
  }

  Map<String, dynamic> _data(Map<String, dynamic> response) {
    final value = response['data'];
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    throw const ApiException(message: 'Dữ liệu máy chủ không đúng định dạng.');
  }

  List<Map<String, dynamic>> _dataList(Map<String, dynamic> response) {
    final value = response['data'];
    if (value is! List) {
      throw const ApiException(
        message: 'Dữ liệu máy chủ không đúng định dạng.',
      );
    }
    return value
        .map(
          (item) => item is Map<String, dynamic>
              ? item
              : item is Map
              ? Map<String, dynamic>.from(item)
              : null,
        )
        .whereType<Map<String, dynamic>>()
        .toList();
  }

  Map<String, dynamic> _page(Map<String, dynamic> response) {
    final data = _data(response);
    if (data['content'] is! List) {
      throw const ApiException(message: 'Dữ liệu trang không đúng định dạng.');
    }
    return data;
  }

  String _date(DateTime value) =>
      '${value.year.toString().padLeft(4, '0')}-${value.month.toString().padLeft(2, '0')}-${value.day.toString().padLeft(2, '0')}';

  @override
  Future<List<ParentChild>> children() async =>
      _dataList(await _get(_path('children')))
          .map(ParentChild.fromJson)
          .toList();

  @override
  Future<ParentDashboard> dashboard(int? studentId) async =>
      ParentDashboard.fromJson(
        _data(
          await _get(
            _path('dashboard'),
            query: studentId == null ? null : {'studentId': '$studentId'},
          ),
        ),
      );

  @override
  Future<StudentProfile> childContext(int studentId) async =>
      StudentProfile.fromJson(
        _data(await _get(_path('children/$studentId/context'))),
      );

  @override
  Future<StudentProfile> studentProfile(int studentId) async =>
      StudentProfile.fromJson(_data(await _get(_path('children/$studentId'))));

  @override
  Future<Map<String, dynamic>?> classInformation(int studentId) async {
    final response = await _get(_path('children/$studentId/class'));
    final value = response['data'];
    if (value == null) return null;
    if (value is Map<String, dynamic>) return value;
    if (value is Map) return Map<String, dynamic>.from(value);
    throw const ApiException(message: 'Dữ liệu máy chủ không đúng định dạng.');
  }

  @override
  Future<List<TimetableEntry>> timetable(
    int studentId, {
    int? academicYearId,
    int? semesterId,
  }) async {
    final query = <String, String>{
      if (academicYearId != null) 'academicYearId': '$academicYearId',
      if (semesterId != null) 'semesterId': '$semesterId',
    };
    return _dataList(
      await _get(_path('children/$studentId/timetable'), query: query),
    ).map(TimetableEntry.fromJson).toList()..sort(
      (a, b) => (a.weekday ?? 0) != (b.weekday ?? 0)
          ? (a.weekday ?? 0).compareTo(b.weekday ?? 0)
          : (a.period ?? 0).compareTo(b.period ?? 0),
    );
  }

  @override
  Future<List<AcademicResult>> academicResults(
    int studentId, {
    int? academicYearId,
    int? semesterId,
  }) async {
    final query = <String, String>{
      if (academicYearId != null) 'academicYearId': '$academicYearId',
      if (semesterId != null) 'semesterId': '$semesterId',
    };
    return _dataList(
      await _get(_path('children/$studentId/academic-results'), query: query),
    ).map(AcademicResult.fromJson).toList();
  }

  @override
  Future<List<StudentGrade>> subjectGrades(
    int studentId,
    int subjectId,
    int semesterId,
  ) async => _dataList(
    await _get(
      _path('children/$studentId/subjects/$subjectId/grades'),
      query: {'semesterId': '$semesterId'},
    ),
  ).map(StudentGrade.fromJson).toList();

  @override
  Future<Map<String, dynamic>> evaluations(
    int studentId, {
    int? academicYearId,
    int? semesterId,
    int page = 0,
    int size = 20,
  }) async => _page(
    await _get(
      _path('children/$studentId/evaluations'),
      query: {
        'page': '$page',
        'size': '$size',
        if (academicYearId != null) 'academicYearId': '$academicYearId',
        if (semesterId != null) 'semesterId': '$semesterId',
      },
    ),
  );

  @override
  Future<Map<String, dynamic>> attendance(
    int studentId, {
    DateTime? from,
    DateTime? to,
    int page = 0,
    int size = 20,
  }) async => _page(
    await _get(
      _path('children/$studentId/attendance'),
      query: {
        'page': '$page',
        'size': '$size',
        if (from != null) 'from': _date(from),
        if (to != null) 'to': _date(to),
      },
    ),
  );

  @override
  Future<AttendanceSummary> attendanceSummary(
    int studentId, {
    int? academicYearId,
    int? semesterId,
  }) async => AttendanceSummary.fromJson(
    _data(
      await _get(
        _path('children/$studentId/attendance/summary'),
        query: {
          if (academicYearId != null) 'academicYearId': '$academicYearId',
          if (semesterId != null) 'semesterId': '$semesterId',
        },
      ),
    ),
  );

  @override
  Future<Map<String, dynamic>> announcements({
    int page = 0,
    int size = 20,
  }) async => _page(
    await _get(
      _path('announcements'),
      query: {'page': '$page', 'size': '$size'},
    ),
  );

  @override
  Future<ParentAnnouncement> announcement(int announcementId) async =>
      ParentAnnouncement.fromJson(
        _data(await _get(_path('announcements/$announcementId'))),
      );

  @override
  Future<void> markAnnouncementRead(int announcementId) async {
    await _post(_path('announcements/$announcementId/read'), const {});
  }

  @override
  Future<LeaveRequest> submitLeave(
    int studentId, {
    required DateTime startDate,
    required DateTime endDate,
    required String reason,
  }) async => LeaveRequest.fromJson(
    _data(
      await _post(_path('children/$studentId/leave-requests'), {
        'startDate': _date(startDate),
        'endDate': _date(endDate),
        'reason': reason.trim(),
      }),
    ),
  );

  @override
  Future<Map<String, dynamic>> leaveRequests(
    int studentId, {
    String? status,
    int page = 0,
    int size = 20,
  }) async => _page(
    await _get(
      _path('children/$studentId/leave-requests'),
      query: {'page': '$page', 'size': '$size', 'status': ?status},
    ),
  );

  @override
  Future<LeaveRequest> leaveRequest(int requestId) async =>
      LeaveRequest.fromJson(
        _data(await _get(_path('leave-requests/$requestId'))),
      );

  @override
  Future<void> registerDevice({
    required String token,
    required String platform,
    String? deviceName,
  }) async {
    await _post(_path('devices'), {
      'token': token,
      'platform': platform,
      'deviceName': ?deviceName,
    });
  }

  @override
  Future<void> removeDevice(String token) async {
    await _delete(_path('devices'), {'token': token});
  }

  void close() {
    _api.close();
    _expired.close();
  }
}
