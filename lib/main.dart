import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import 'core/storage/token_storage.dart';
import 'core/theme/app_theme.dart';
import 'features/auth/data/services/auth_api_service.dart';
import 'features/auth/presentation/screens/login_screen.dart';
import 'features/parent/data/services/parent_notification_service.dart';

void main() {
  runApp(const MyFSchoolApp());
}

class MyFSchoolApp extends StatelessWidget {
  const MyFSchoolApp({
    this.authService,
    this.tokenStorage,
    this.parentPushTokenSource = const UnavailableParentPushTokenSource(),
    super.key,
  });

  final AuthService? authService;
  final TokenStorage? tokenStorage;
  final ParentPushTokenSource parentPushTokenSource;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MyFSchool',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light,
      locale: const Locale('vi'),
      supportedLocales: const [Locale('vi'), Locale('en')],
      localizationsDelegates: GlobalMaterialLocalizations.delegates,
      home: LoginScreen(
        authService: authService,
        tokenStorage: tokenStorage,
        pushTokenSource: parentPushTokenSource,
      ),
    );
  }
}
