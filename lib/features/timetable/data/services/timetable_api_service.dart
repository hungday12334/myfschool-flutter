import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/storage/token_storage.dart';
import '../models/timetable_week.dart';

abstract interface class TimetableService {
  Future<TimetableWeek> fetchWeek(DateTime weekStart);
}

class TimetableApiService implements TimetableService {
  TimetableApiService({ApiClient? apiClient, TokenStorage? tokenStorage})
    : _apiClient = apiClient ?? ApiClient(),
      _tokenStorage = tokenStorage ?? TokenStorage();

  final ApiClient _apiClient;
  final TokenStorage _tokenStorage;

  @override
  Future<TimetableWeek> fetchWeek(DateTime weekStart) async {
    final accessToken = (await _tokenStorage.readAccessToken())?.trim();
    if (accessToken == null || accessToken.isEmpty) {
      throw const ApiException(
        message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        statusCode: 401,
      );
    }

    final monday = _mondayOf(weekStart);
    final response = await _apiClient.getJson(
      ApiConfig.studentTimetablePath,
      queryParameters: <String, String>{'weekStart': _formatDate(monday)},
      bearerToken: accessToken,
    );

    try {
      return TimetableWeek.fromEnvelope(response);
    } on FormatException {
      throw const ApiException(message: 'Phản hồi từ máy chủ không hợp lệ.');
    }
  }

  void close() {
    _apiClient.close();
  }
}

DateTime _mondayOf(DateTime date) {
  final value = DateTime(date.year, date.month, date.day);
  return value.subtract(Duration(days: value.weekday - DateTime.monday));
}

String _formatDate(DateTime value) {
  return '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}
