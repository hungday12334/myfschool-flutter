import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../config/api_config.dart';

class ApiClient {
  ApiClient({http.Client? httpClient, Uri? baseUri})
    : _httpClient = httpClient ?? http.Client(),
      _baseUriOverride = baseUri;

  final http.Client _httpClient;
  final Uri? _baseUriOverride;

  Future<Map<String, dynamic>> getJson(
    String path, {
    Map<String, String>? queryParameters,
    String? bearerToken,
  }) async {
    final uri = _resolveUri(path, queryParameters: queryParameters);
    final token = bearerToken?.trim();

    late final http.Response response;
    try {
      response = await _httpClient
          .get(
            uri,
            headers: <String, String>{
              'Accept': 'application/json',
              if (token != null && token.isNotEmpty)
                'Authorization': 'Bearer $token',
            },
          )
          .timeout(ApiConfig.requestTimeout);
    } on TimeoutException {
      throw const ApiException(
        message: 'Máy chủ phản hồi quá chậm. Vui lòng thử lại.',
      );
    } on http.ClientException {
      throw const ApiException(
        message:
            'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.',
      );
    }

    return _handleResponse(response);
  }

  Future<Map<String, dynamic>> postJson(
    String path, {
    required Map<String, dynamic> body,
  }) async {
    final uri = _resolveUri(path);

    late final http.Response response;
    try {
      response = await _httpClient
          .post(
            uri,
            headers: const <String, String>{
              'Accept': 'application/json',
              'Content-Type': 'application/json',
            },
            body: jsonEncode(body),
          )
          .timeout(ApiConfig.requestTimeout);
    } on TimeoutException {
      throw const ApiException(
        message: 'Máy chủ phản hồi quá chậm. Vui lòng thử lại.',
      );
    } on http.ClientException {
      throw const ApiException(
        message:
            'Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng.',
      );
    }

    return _handleResponse(response);
  }

  Uri _resolveUri(String path, {Map<String, String>? queryParameters}) {
    final normalizedPath = path.startsWith('/') ? path.substring(1) : path;
    final uri = (_baseUriOverride ?? ApiConfig.baseUri).resolve(normalizedPath);
    if (queryParameters == null || queryParameters.isEmpty) {
      return uri;
    }
    return uri.replace(queryParameters: queryParameters);
  }

  Map<String, dynamic> _handleResponse(http.Response response) {
    final decodedBody = _decodeBody(response.body);
    if (response.statusCode >= 200 && response.statusCode < 300) {
      if (decodedBody is Map<String, dynamic>) {
        return decodedBody;
      }
      throw const ApiException(message: 'Phản hồi từ máy chủ không hợp lệ.');
    }

    throw ApiException(
      message: _errorMessage(decodedBody, response.statusCode),
      statusCode: response.statusCode,
      code: _errorCode(decodedBody),
    );
  }

  void close() {
    _httpClient.close();
  }

  Object? _decodeBody(String body) {
    if (body.trim().isEmpty) {
      return null;
    }

    try {
      return jsonDecode(body);
    } on FormatException {
      return null;
    }
  }

  String _errorMessage(Object? decodedBody, int statusCode) {
    if (decodedBody case {'message': final String message}
        when message.trim().isNotEmpty) {
      return message.trim();
    }

    return switch (statusCode) {
      401 => 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
      403 => 'Bạn không có quyền thực hiện thao tác này.',
      404 => 'Không tìm thấy dữ liệu yêu cầu.',
      >= 500 => 'Máy chủ đang gặp sự cố. Vui lòng thử lại sau.',
      _ => 'Yêu cầu không thành công. Vui lòng thử lại.',
    };
  }

  Object? _errorCode(Object? decodedBody) {
    if (decodedBody case {'code': final Object code}) {
      return code;
    }
    return null;
  }
}

class ApiException implements Exception {
  const ApiException({required this.message, this.statusCode, this.code});

  final String message;
  final int? statusCode;
  final Object? code;
}
