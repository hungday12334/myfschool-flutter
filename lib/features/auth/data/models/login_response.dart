class LoginResponse {
  const LoginResponse({
    required this.accessToken,
    required this.tokenType,
    required this.expiresIn,
    required this.user,
  });

  final String accessToken;
  final String tokenType;
  final int expiresIn;
  final AuthenticatedUser user;

  factory LoginResponse.fromJson(Map<String, dynamic> json) {
    final data = _asStringKeyedMap(json['data'], fieldName: 'data');

    return LoginResponse(
      accessToken: _requiredString(data, 'accessToken'),
      tokenType: _requiredString(data, 'tokenType'),
      expiresIn: _requiredInt(data, 'expiresIn'),
      user: AuthenticatedUser.fromJson(
        _asStringKeyedMap(data['user'], fieldName: 'user'),
      ),
    );
  }
}

class AuthenticatedUser {
  const AuthenticatedUser({
    required this.id,
    required this.phoneNumber,
    required this.fullName,
    required this.role,
  });

  final int id;
  final String phoneNumber;
  final String fullName;
  final String role;

  factory AuthenticatedUser.fromJson(Map<String, dynamic> json) {
    return AuthenticatedUser(
      id: _requiredInt(json, 'id'),
      phoneNumber: _requiredString(json, 'phoneNumber'),
      fullName: _requiredString(json, 'fullName'),
      role: _requiredString(json, 'role'),
    );
  }
}

Map<String, dynamic> _asStringKeyedMap(
  Object? value, {
  required String fieldName,
}) {
  if (value is Map<String, dynamic>) {
    return value;
  }
  if (value is Map) {
    return Map<String, dynamic>.from(value);
  }
  throw FormatException('Missing or invalid field: $fieldName');
}

String _requiredString(Map<String, dynamic> json, String fieldName) {
  final value = json[fieldName];
  if (value is String && value.isNotEmpty) {
    return value;
  }
  throw FormatException('Missing or invalid field: $fieldName');
}

int _requiredInt(Map<String, dynamic> json, String fieldName) {
  final value = json[fieldName];
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.toInt();
  }
  throw FormatException('Missing or invalid field: $fieldName');
}
