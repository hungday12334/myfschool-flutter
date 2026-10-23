class LoginRequest {
  const LoginRequest({required this.phoneNumber, required this.password});

  final String phoneNumber;
  final String password;

  Map<String, dynamic> toJson() {
    return <String, dynamic>{'phoneNumber': phoneNumber, 'password': password};
  }
}
