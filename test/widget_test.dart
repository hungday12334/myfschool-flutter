import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/core/storage/token_storage.dart';
import 'package:myschoolse1947/features/auth/data/models/login_request.dart';
import 'package:myschoolse1947/features/auth/data/models/login_response.dart';
import 'package:myschoolse1947/features/auth/data/services/auth_api_service.dart';
import 'package:myschoolse1947/features/home/presentation/screens/student_home_screen.dart';
import 'package:myschoolse1947/main.dart';

void main() {
  testWidgets('Login screen smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(
      MyFSchoolApp(
        authService: _FakeAuthService.successful(),
        tokenStorage: _FakeTokenStorage(),
      ),
    );

    expect(find.text('MyFSchool'), findsOneWidget);
    expect(find.text('S\u1ed1 \u0111i\u1ec7n tho\u1ea1i'), findsOneWidget);
    expect(find.text('M\u1eadt kh\u1ea9u'), findsOneWidget);
    expect(find.text('\u0110\u0103ng nh\u1eadp'), findsOneWidget);
    expect(
      find.text('B\u1ea1n ch\u01b0a c\u00f3 t\u00e0i kho\u1ea3n?'),
      findsNothing,
    );
    expect(find.text('ho\u1eb7c'), findsNothing);
  });

  testWidgets('Forgot password completes the email reset flow', (
    WidgetTester tester,
  ) async {
    final authService = _FakeAuthService.successful();
    await tester.pumpWidget(
      MyFSchoolApp(authService: authService, tokenStorage: _FakeTokenStorage()),
    );

    await tester.tap(find.text('Qu\u00ean m\u1eadt kh\u1ea9u?'));
    await tester.pumpAndSettle();

    expect(
      find.textContaining(
        'Nh\u1eadp email \u0111\u00e3 \u0111\u0103ng k\u00fd',
      ),
      findsWidgets,
    );
    await tester.enterText(
      find.widgetWithText(TextFormField, 'Email'),
      'student@example.com',
    );
    await tester.tap(find.text('G\u1eedi m\u00e3 x\u00e1c th\u1ef1c'));
    await tester.pump();
    await tester.pump();

    expect(authService.resetRequestCallCount, 1);
    expect(authService.lastResetEmail, 'student@example.com');
    expect(find.text('Nh\u1eadp m\u00e3 x\u00e1c th\u1ef1c'), findsOneWidget);

    await tester.enterText(
      find.widgetWithText(TextFormField, 'M\u00e3 x\u00e1c th\u1ef1c'),
      '123456',
    );
    await tester.ensureVisible(find.text('X\u00e1c th\u1ef1c m\u00e3'));
    await tester.pump();
    await tester.tap(find.text('X\u00e1c th\u1ef1c m\u00e3'));
    await tester.pump();
    await tester.pump();

    expect(authService.lastVerificationCode, '123456');
    expect(find.text('T\u1ea1o m\u1eadt kh\u1ea9u m\u1edbi'), findsOneWidget);

    await tester.enterText(
      find.widgetWithText(TextFormField, 'M\u1eadt kh\u1ea9u m\u1edbi'),
      'new-password',
    );
    await tester.enterText(
      find.widgetWithText(
        TextFormField,
        'X\u00e1c nh\u1eadn m\u1eadt kh\u1ea9u',
      ),
      'new-password',
    );
    await tester.ensureVisible(find.text('\u0110\u1ed5i m\u1eadt kh\u1ea9u'));
    await tester.pump();
    await tester.tap(find.text('\u0110\u1ed5i m\u1eadt kh\u1ea9u'));
    await tester.pumpAndSettle();

    expect(authService.lastResetToken, 'reset-token');
    expect(authService.lastNewPassword, 'new-password');
    expect(
      find.text(
        '\u0110\u1ed5i m\u1eadt kh\u1ea9u th\u00e0nh c\u00f4ng. B\u1ea1n c\u00f3 th\u1ec3 \u0111\u0103ng nh\u1eadp ngay.',
      ),
      findsOneWidget,
    );
  });

  testWidgets('Successful API login stores token and opens existing home', (
    WidgetTester tester,
  ) async {
    final authService = _FakeAuthService.successful();
    final tokenStorage = _FakeTokenStorage();
    await tester.pumpWidget(
      MyFSchoolApp(authService: authService, tokenStorage: tokenStorage),
    );

    await _enterCredentials(tester, phone: ' 0987654321 ');
    await tester.tap(find.text('\u0110\u0103ng nh\u1eadp'));
    await tester.pumpAndSettle();

    expect(authService.callCount, 1);
    expect(authService.lastRequest?.phoneNumber, '0987654321');
    expect(authService.lastRequest?.password, '123456');
    expect(tokenStorage.savedToken, 'signed-jwt');
    expect(find.byType(StudentHomeScreen), findsOneWidget);
    expect(find.text('Trang ch\u1ee7'), findsOneWidget);
  });

  testWidgets('Backend login error is displayed without navigating', (
    WidgetTester tester,
  ) async {
    final authService = _FakeAuthService(
      (_) => Future<LoginResponse>.error(
        const ApiException(message: 'Invalid phone number or password'),
      ),
    );
    final tokenStorage = _FakeTokenStorage();
    await tester.pumpWidget(
      MyFSchoolApp(authService: authService, tokenStorage: tokenStorage),
    );

    await _enterCredentials(tester);
    await tester.tap(find.text('\u0110\u0103ng nh\u1eadp'));
    await tester.pumpAndSettle();

    expect(find.text('Invalid phone number or password'), findsOneWidget);
    expect(find.byType(StudentHomeScreen), findsNothing);
    expect(tokenStorage.savedToken, isNull);
  });

  testWidgets('Login is disabled while one request is in progress', (
    WidgetTester tester,
  ) async {
    final pendingLogin = Completer<LoginResponse>();
    final authService = _FakeAuthService((_) => pendingLogin.future);
    await tester.pumpWidget(
      MyFSchoolApp(authService: authService, tokenStorage: _FakeTokenStorage()),
    );

    await _enterCredentials(tester);
    await tester.tap(find.text('\u0110\u0103ng nh\u1eadp'));
    await tester.pump();

    expect(authService.callCount, 1);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
    final button = tester.widget<ElevatedButton>(find.byType(ElevatedButton));
    expect(button.onPressed, isNull);

    await tester.tap(find.byType(ElevatedButton));
    await tester.pump();
    expect(authService.callCount, 1);

    pendingLogin.complete(_successResponse);
    await tester.pumpAndSettle();
    expect(find.byType(StudentHomeScreen), findsOneWidget);
  });

  testWidgets('Secure storage failure does not navigate to home', (
    WidgetTester tester,
  ) async {
    final tokenStorage = _FakeTokenStorage(writeError: Exception('keychain'));
    await tester.pumpWidget(
      MyFSchoolApp(
        authService: _FakeAuthService.successful(),
        tokenStorage: tokenStorage,
      ),
    );

    await _enterCredentials(tester);
    await tester.tap(find.text('\u0110\u0103ng nh\u1eadp'));
    await tester.pumpAndSettle();

    expect(tokenStorage.saveCallCount, 1);
    expect(find.byType(StudentHomeScreen), findsNothing);
    expect(find.text('\u0110\u0103ng nh\u1eadp'), findsOneWidget);
  });

  testWidgets('Login screen fits common mobile sizes', (
    WidgetTester tester,
  ) async {
    final view = tester.view;
    addTearDown(() {
      view.resetPhysicalSize();
      view.resetDevicePixelRatio();
    });

    for (final size in <Size>[
      const Size(320, 568),
      const Size(375, 667),
      const Size(390, 844),
    ]) {
      view
        ..devicePixelRatio = 1
        ..physicalSize = size;

      await tester.pumpWidget(
        MyFSchoolApp(
          authService: _FakeAuthService.successful(),
          tokenStorage: _FakeTokenStorage(),
        ),
      );
      await tester.pump();

      expect(tester.takeException(), isNull);
    }
  });
}

Future<void> _enterCredentials(
  WidgetTester tester, {
  String phone = '0987654321',
}) async {
  await tester.enterText(
    find.widgetWithText(TextField, 'S\u1ed1 \u0111i\u1ec7n tho\u1ea1i'),
    phone,
  );
  await tester.enterText(
    find.widgetWithText(TextField, 'M\u1eadt kh\u1ea9u'),
    '123456',
  );
}

const LoginResponse _successResponse = LoginResponse(
  accessToken: 'signed-jwt',
  tokenType: 'Bearer',
  expiresIn: 3600,
  user: AuthenticatedUser(
    id: 1,
    phoneNumber: '0987654321',
    fullName: 'Test Student',
    role: 'STUDENT',
  ),
);

class _FakeAuthService implements AuthService {
  _FakeAuthService(this._login);

  _FakeAuthService.successful() : _login = ((_) async => _successResponse);

  final Future<LoginResponse> Function(LoginRequest request) _login;
  int callCount = 0;
  LoginRequest? lastRequest;
  int resetRequestCallCount = 0;
  String? lastResetEmail;
  String? lastVerificationCode;
  String? lastResetToken;
  String? lastNewPassword;
  String? lastConfirmedPassword;

  @override
  Future<LoginResponse> login(LoginRequest request) {
    callCount += 1;
    lastRequest = request;
    return _login(request);
  }

  @override
  Future<void> requestPasswordReset(String email) async {
    resetRequestCallCount += 1;
    lastResetEmail = email;
  }

  @override
  Future<String> verifyPasswordResetCode({
    required String email,
    required String code,
  }) async {
    lastResetEmail = email;
    lastVerificationCode = code;
    return 'reset-token';
  }

  @override
  Future<void> confirmPasswordReset({
    required String resetToken,
    required String newPassword,
    required String confirmPassword,
  }) async {
    lastResetToken = resetToken;
    lastNewPassword = newPassword;
    lastConfirmedPassword = confirmPassword;
  }
}

class _FakeTokenStorage extends TokenStorage {
  _FakeTokenStorage({this.writeError});

  final Object? writeError;
  int saveCallCount = 0;
  String? savedToken;

  @override
  Future<void> saveAccessToken(String accessToken) async {
    saveCallCount += 1;
    if (writeError != null) {
      throw writeError!;
    }
    savedToken = accessToken;
  }
}
