enum LoginSource {
  flutter,
  react,
}
class LoginRequest {
  const LoginRequest({
    String? username,
    String? phoneNumber,
    required this.password,
    this.source = LoginSource.flutter,
  })  : username = username ?? phoneNumber ?? '',
        phoneNumber = phoneNumber ?? username ?? '';

  final String username;
  final String phoneNumber;
  final String password;
  final LoginSource source;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{
      'username': username,
      'password': password,
      'source': source.name.toUpperCase(),
    };
  }
}