import '../../../../core/network/api_client.dart';

/// Teacher endpoints are not present in the checked-in Spring Boot backend.
/// Keep this boundary explicit so teacher UI never reads parent data or fakes records.
abstract interface class TeacherService {
  Future<void> loadOverview();
  Future<void> loadSchedule();
  Future<void> loadClasses();
  Future<void> loadStudents();
  Future<void> saveAttendance();
}

class TeacherApiService implements TeacherService {
  const TeacherApiService();

  ApiException _missing(String feature) => ApiException(
    message: 'Backend chưa có API giáo viên cho chức năng $feature.',
    statusCode: 404,
  );

  @override
  Future<void> loadOverview() async => throw _missing('tổng quan');

  @override
  Future<void> loadSchedule() async => throw _missing('thời khóa biểu');

  @override
  Future<void> loadClasses() async => throw _missing('lớp phụ trách');

  @override
  Future<void> loadStudents() async => throw _missing('danh sách học sinh');

  @override
  Future<void> saveAttendance() async => throw _missing('lưu điểm danh');
}
