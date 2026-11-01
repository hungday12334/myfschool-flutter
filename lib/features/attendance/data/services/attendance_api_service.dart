import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/storage/token_storage.dart';
import '../models/attendance_report.dart';

abstract interface class AttendanceService {
  Future<AttendanceReport> fetchMonth(DateTime month);
}

class AttendanceApiService implements AttendanceService {
  AttendanceApiService({ApiClient? apiClient, TokenStorage? tokenStorage})
    : _apiClient = apiClient ?? ApiClient(),
      _tokenStorage = tokenStorage ?? TokenStorage();

  final ApiClient _apiClient;
  final TokenStorage _tokenStorage;

  @override
  Future<AttendanceReport> fetchMonth(DateTime month) async {
    final accessToken = (await _tokenStorage.readAccessToken())?.trim();
    if (accessToken == null || accessToken.isEmpty) {
      throw const ApiException(
        message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        statusCode: 401,
      );
    }
    final normalized = DateTime(month.year, month.month);
    final response = await _apiClient.getJson(
      ApiConfig.parentAttendancePath,
      queryParameters: <String, String>{
        'month':
            '${normalized.year.toString().padLeft(4, '0')}-'
            '${normalized.month.toString().padLeft(2, '0')}',
      },
      bearerToken: accessToken,
    );
    try {
      return AttendanceReport.fromEnvelope(response);
    } on FormatException {
      throw const ApiException(message: 'Phản hồi từ máy chủ không hợp lệ.');
    }
  }

  void close() => _apiClient.close();
}
