import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/features/auth/data/models/login_request.dart';
import 'package:myschoolse1947/features/auth/data/services/auth_api_service.dart';

void main() {
  const request = LoginRequest(phoneNumber: '0987654321', password: '123456');

  test('login posts credentials and parses the response envelope', () async {
    final httpClient = MockClient((http.Request sentRequest) async {
      expect(sentRequest.method, 'POST');
      expect(
        sentRequest.url,
        Uri.parse('http://localhost:8080/api/v1/auth/login'),
      );
      expect(sentRequest.headers['content-type'], 'application/json');
      expect(jsonDecode(sentRequest.body), <String, dynamic>{
        'phoneNumber': '0987654321',
        'password': '123456',
      });

      return http.Response(
        jsonEncode(<String, dynamic>{
          'code': 200,
          'message': 'Login successful',
          'data': <String, dynamic>{
            'accessToken': 'signed-jwt',
            'tokenType': 'Bearer',
            'expiresIn': 3600,
            'user': <String, dynamic>{
              'id': 1,
              'phoneNumber': '0987654321',
              'fullName': 'Test Parent',
              'role': 'PARENT',
            },
          },
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final apiClient = ApiClient(
      httpClient: httpClient,
      baseUri: Uri.parse('http://localhost:8080/'),
    );
    final service = AuthApiService(apiClient: apiClient);
    addTearDown(service.close);

    final response = await service.login(request);

    expect(response.accessToken, 'signed-jwt');
    expect(response.expiresIn, 3600);
    expect(response.user.fullName, 'Test Parent');
    expect(response.user.role, 'PARENT');
  });

  test('login surfaces the backend error message and status', () async {
    final httpClient = MockClient(
      (_) async => http.Response(
        jsonEncode(<String, dynamic>{
          'code': 40101,
          'message': 'Invalid phone number or password',
          'data': null,
        }),
        401,
        headers: const <String, String>{'content-type': 'application/json'},
      ),
    );
    final apiClient = ApiClient(
      httpClient: httpClient,
      baseUri: Uri.parse('http://localhost:8080/'),
    );
    final service = AuthApiService(apiClient: apiClient);
    addTearDown(service.close);

    await expectLater(
      service.login(request),
      throwsA(
        isA<ApiException>()
            .having(
              (ApiException error) => error.message,
              'message',
              'Invalid phone number or password',
            )
            .having((ApiException error) => error.statusCode, 'statusCode', 401)
            .having((ApiException error) => error.code, 'code', 40101),
      ),
    );
  });

  test('login rejects a malformed successful response', () async {
    final httpClient = MockClient(
      (_) async => http.Response(
        jsonEncode(<String, dynamic>{'code': 200, 'data': null}),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      ),
    );
    final apiClient = ApiClient(
      httpClient: httpClient,
      baseUri: Uri.parse('http://localhost:8080/'),
    );
    final service = AuthApiService(apiClient: apiClient);
    addTearDown(service.close);

    await expectLater(
      service.login(request),
      throwsA(
        isA<ApiException>().having(
          (ApiException error) => error.message,
          'message',
          'Phản hồi từ máy chủ không hợp lệ.',
        ),
      ),
    );
  });

  test('password reset request posts the email', () async {
    final httpClient = MockClient((http.Request sentRequest) async {
      expect(sentRequest.method, 'POST');
      expect(
        sentRequest.url,
        Uri.parse('http://localhost:8080/api/v1/auth/password-reset/request'),
      );
      expect(jsonDecode(sentRequest.body), <String, dynamic>{
        'email': 'student@example.com',
      });

      return http.Response(
        jsonEncode(<String, dynamic>{
          'code': 200,
          'message': 'Code sent',
          'data': null,
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final service = AuthApiService(
      apiClient: ApiClient(
        httpClient: httpClient,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
    );
    addTearDown(service.close);

    await service.requestPasswordReset('student@example.com');
  });

  test('password reset verification returns the reset token', () async {
    final httpClient = MockClient((http.Request sentRequest) async {
      expect(
        sentRequest.url,
        Uri.parse('http://localhost:8080/api/v1/auth/password-reset/verify'),
      );
      expect(jsonDecode(sentRequest.body), <String, dynamic>{
        'email': 'student@example.com',
        'code': '123456',
      });

      return http.Response(
        jsonEncode(<String, dynamic>{
          'code': 200,
          'message': 'Code verified',
          'data': <String, dynamic>{'resetToken': ' reset-token '},
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final service = AuthApiService(
      apiClient: ApiClient(
        httpClient: httpClient,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
    );
    addTearDown(service.close);

    final resetToken = await service.verifyPasswordResetCode(
      email: 'student@example.com',
      code: '123456',
    );

    expect(resetToken, 'reset-token');
  });

  test('password reset confirmation posts the new passwords', () async {
    final httpClient = MockClient((http.Request sentRequest) async {
      expect(
        sentRequest.url,
        Uri.parse('http://localhost:8080/api/v1/auth/password-reset/confirm'),
      );
      expect(jsonDecode(sentRequest.body), <String, dynamic>{
        'resetToken': 'reset-token',
        'newPassword': 'new-password',
        'confirmPassword': 'new-password',
      });

      return http.Response(
        jsonEncode(<String, dynamic>{
          'code': 200,
          'message': 'Password reset',
          'data': null,
        }),
        200,
        headers: const <String, String>{'content-type': 'application/json'},
      );
    });
    final service = AuthApiService(
      apiClient: ApiClient(
        httpClient: httpClient,
        baseUri: Uri.parse('http://localhost:8080/'),
      ),
    );
    addTearDown(service.close);

    await service.confirmPasswordReset(
      resetToken: 'reset-token',
      newPassword: 'new-password',
      confirmPassword: 'new-password',
    );
  });
}
