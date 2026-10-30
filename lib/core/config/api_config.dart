import 'package:flutter/foundation.dart';

abstract final class ApiConfig {
  static const String loginPath = 'api/v1/auth/login';
  static const String passwordResetRequestPath =
      'api/v1/auth/password-reset/request';
  static const String passwordResetVerifyPath =
      'api/v1/auth/password-reset/verify';
  static const String passwordResetConfirmPath =
      'api/v1/auth/password-reset/confirm';
  static const Duration requestTimeout = Duration(seconds: 12);

  static const String _configuredBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
  );

  static Uri get baseUri {
    final configuredUrl = _configuredBaseUrl.trim();
    final rawUrl = configuredUrl.isEmpty ? _platformDefaultUrl : configuredUrl;
    final uri = Uri.tryParse(rawUrl);

    if (uri == null ||
        !uri.hasScheme ||
        uri.host.isEmpty ||
        uri.userInfo.isNotEmpty ||
        uri.hasQuery ||
        uri.hasFragment ||
        (uri.scheme != 'http' && uri.scheme != 'https')) {
      throw StateError(
        'API_BASE_URL must be a valid HTTP(S) URL, for example '
        'https://api.example.com.',
      );
    }

    if (kReleaseMode && uri.scheme != 'https') {
      throw StateError('API_BASE_URL must use HTTPS in release builds.');
    }

    return uri.replace(
      path: uri.path.endsWith('/') ? uri.path : '${uri.path}/',
    );
  }

  static String get _platformDefaultUrl {
    if (kIsWeb) {
      return 'http://localhost:8080';
    }

    return switch (defaultTargetPlatform) {
      TargetPlatform.android => 'http://10.0.2.2:8080',
      _ => 'http://localhost:8080',
    };
  }
}
