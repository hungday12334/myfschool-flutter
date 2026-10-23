import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';
import '../models/login_request.dart';
import '../models/login_response.dart';

abstract interface class AuthService {
  Future<LoginResponse> login(LoginRequest request);
}

class AuthApiService implements AuthService {
  AuthApiService({ApiClient? apiClient})
    : _apiClient = apiClient ?? ApiClient();

  final ApiClient _apiClient;

  @override
  Future<LoginResponse> login(LoginRequest request) async {
    final response = await _apiClient.postJson(
      ApiConfig.loginPath,
      body: request.toJson(),
    );

    try {
      return LoginResponse.fromJson(response);
    } on FormatException {
      throw const ApiException(message: 'Phản hồi từ máy chủ không hợp lệ.');
    }
  }

  void close() {
    _apiClient.close();
  }
}
