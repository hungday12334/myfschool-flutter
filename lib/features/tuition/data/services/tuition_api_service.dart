import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/storage/token_storage.dart';
import '../models/tuition_report.dart';

abstract interface class TuitionService {
  Future<TuitionReport> fetchReport();
}

class TuitionApiService implements TuitionService {
  TuitionApiService({ApiClient? apiClient, TokenStorage? tokenStorage})
    : _apiClient = apiClient ?? ApiClient(),
      _tokenStorage = tokenStorage ?? TokenStorage();

  final ApiClient _apiClient;
  final TokenStorage _tokenStorage;

  @override
  Future<TuitionReport> fetchReport() async {
    final accessToken = (await _tokenStorage.readAccessToken())?.trim();
    if (accessToken == null || accessToken.isEmpty) {
      throw const ApiException(
        message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        statusCode: 401,
      );
    }
    final response = await _apiClient.getJson(
      ApiConfig.parentTuitionPath,
      bearerToken: accessToken,
    );
    try {
      return TuitionReport.fromEnvelope(response);
    } on FormatException {
      throw const ApiException(message: 'Phản hồi từ máy chủ không hợp lệ.');
    }
  }

  void close() => _apiClient.close();
}
