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
    String? username,
    String? phone,
    this.email,
    List<String>? roles,
    String? phoneNumber,
    String? fullName,
    String? role,
    this.status,
    this.mustChangePassword = false,
  }) : username = username ?? phoneNumber ?? '',
       phone = phone ?? phoneNumber,
       roles = roles ?? const [],
       _legacyFullName = fullName,
       _legacyRole = role;

  final int id;
  final String username;
  final String? phone;
  final String? email;
  final List<String> roles;
  final String? status;
  final bool mustChangePassword;
  final String? _legacyFullName;
  final String? _legacyRole;

  String get phoneNumber => phone ?? username;
  String get fullName => _legacyFullName ?? username;
  String get role => roles.isEmpty
      ? _normalizedRole(_legacyRole ?? '')
      : _normalizedRole(roles.first);
  bool hasRole(String value) =>
      role == value.toUpperCase() ||
      roles.any((candidate) => _normalizedRole(candidate) == value.toUpperCase());

  factory AuthenticatedUser.fromJson(Map<String, dynamic> json) {
    return AuthenticatedUser(
      id: _requiredInt(json, 'id'),
      username: _firstString(json, const ['username', 'phoneNumber', 'email', 'fullName']),
      phone: json['phone'] as String?,
      email: json['email'] as String?,
      roles: json.containsKey('roles')
          ? _stringList(json['roles'])
          : <String>[_requiredString(json, 'role')],
      fullName: json['fullName'] as String?,
      status: json['status'] as String?,
      mustChangePassword: json['mustChangePassword'] == true,
    );
  }
}

String _normalizedRole(String role) =>
    role.toUpperCase().replaceFirst(RegExp(r'^ROLE_'), '');

List<String> _stringList(Object? value) {
  if (value is List) {
    return value.whereType<String>().toList(growable: false);
  }
  if (value is String) return [value]; // Older API response compatibility.
  throw const FormatException('Missing or invalid field: roles');
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

String _firstString(Map<String, dynamic> json, List<String> fields) {
  for (final field in fields) {
    final value = json[field];
    if (value is String && value.isNotEmpty) return value;
  }
  throw FormatException('Missing or invalid field: ${fields.first}');
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
