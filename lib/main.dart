import 'package:flutter/material.dart';

import 'core/storage/token_storage.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/data/services/auth_api_service.dart';
import 'features/auth/presentation/screens/login_screen.dart';

void main() {
  runApp(const MyFSchoolApp());
}

class MyFSchoolApp extends StatelessWidget {
  const MyFSchoolApp({this.authService, this.tokenStorage, super.key});

  final AuthService? authService;
  final TokenStorage? tokenStorage;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MyFSchool',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      home: LoginScreen(authService: authService, tokenStorage: tokenStorage),
    );
  }
}
