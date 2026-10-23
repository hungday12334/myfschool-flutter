import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class TokenStorage {
  TokenStorage({FlutterSecureStorage? secureStorage})
    : _secureStorage =
          secureStorage ??
          const FlutterSecureStorage(
            // Browser storage is exposed to same-origin JavaScript and cannot
            // match a native keychain. Limit persistence to the current tab
            // session; production web builds must also use HTTPS, HSTS and CSP.
            webOptions: WebOptions(useSessionStorage: true),
          );

  static const String _accessTokenKey = 'auth_access_token';

  final FlutterSecureStorage _secureStorage;

  Future<void> saveAccessToken(String accessToken) async {
    if (accessToken.isEmpty) {
      throw ArgumentError.value(
        accessToken,
        'accessToken',
        'Access token cannot be empty.',
      );
    }

    await _secureStorage.write(key: _accessTokenKey, value: accessToken);
  }

  Future<String?> readAccessToken() {
    return _secureStorage.read(key: _accessTokenKey);
  }

  Future<void> clearAccessToken() {
    return _secureStorage.delete(key: _accessTokenKey);
  }
}
