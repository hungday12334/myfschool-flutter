import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myschoolse1947/core/storage/token_storage.dart';
import 'package:myschoolse1947/features/auth/data/models/login_request.dart';
import 'package:myschoolse1947/features/auth/data/models/login_response.dart';
import 'package:myschoolse1947/features/auth/data/services/auth_api_service.dart';
import 'package:myschoolse1947/features/home/presentation/screens/parent_home_screen.dart';
import 'package:myschoolse1947/features/teacher/presentation/screens/teacher_home_screen.dart';
import 'package:myschoolse1947/main.dart';

void main() {
  testWidgets(
    'teacher login still stores the token and opens the teacher home',
    (tester) async {
      final tokens = _TeacherTokens();
      await tester.pumpWidget(
        MyFSchoolApp(authService: _TeacherAuth(), tokenStorage: tokens),
      );
      await tester.pumpAndSettle();
      await tester.enterText(find.byType(TextField).first, 'teacher');
      await tester.enterText(find.byType(TextField).last, 'secret123');
      await tester.tap(find.text('Đăng nhập'));
      await tester.pump();
      await tester.pump(const Duration(seconds: 1));
      expect(tokens.token, 'teacher-jwt');
      expect(find.byType(TeacherHomeScreen), findsOneWidget);
      expect(find.byType(ParentHomeScreen), findsNothing);
    },
  );
}

class _TeacherAuth implements AuthService {
  @override
  Future<LoginResponse> login(LoginRequest request) async => LoginResponse(
    accessToken: 'teacher-jwt',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: const AuthenticatedUser(
      id: 8,
      username: 'teacher',
      roles: ['TEACHER'],
      fullName: 'Giáo viên',
    ),
  );
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _TeacherTokens extends TokenStorage {
  String? token;
  @override
  Future<void> saveAccessToken(String accessToken) async {
    token = accessToken;
  }

  @override
  Future<String?> readAccessToken() async => token;
}
