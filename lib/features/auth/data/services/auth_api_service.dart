import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';
import '../models/login_request.dart';
import '../models/login_response.dart';

abstract interface class AuthService {
  Future<LoginResponse> login(LoginRequest request);

  Future<void> requestPasswordReset(String email);

  Future<String> verifyPasswordResetCode({
    required String email,
    required String code,
  });

  Future<void> confirmPasswordReset({
    required String resetToken,
    required String newPassword,
    required String confirmPassword,
  });
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

  @override
  Future<void> requestPasswordReset(String email) async {
    await _apiClient.postJson(
      ApiConfig.passwordResetRequestPath,
      body: <String, dynamic>{'email': email},
    );
  }

  @override
  Future<String> verifyPasswordResetCode({
    required String email,
    required String code,
  }) async {
    final response = await _apiClient.postJson(
      ApiConfig.passwordResetVerifyPath,
      body: <String, dynamic>{'email': email, 'code': code},
    );
    final data = response['data'];
    if (data is Map<String, dynamic>) {
      final resetToken = data['resetToken'];
      if (resetToken is String && resetToken.trim().isNotEmpty) {
        return resetToken.trim();
      }
    }

    throw const ApiException(message: 'Phản hồi từ máy chủ không hợp lệ.');
  }

  @override
  Future<void> confirmPasswordReset({
    required String resetToken,
    required String newPassword,
    required String confirmPassword,
  }) async {
    await _apiClient.postJson(
      ApiConfig.passwordResetConfirmPath,
      body: <String, dynamic>{
        'resetToken': resetToken,
        'newPassword': newPassword,
        'confirmPassword': confirmPassword,
      },
    );
  }

  void close() {
    _apiClient.close();
  }
}
