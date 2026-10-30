import 'package:flutter/material.dart';

import '../../../../core/constants/app_assets.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/storage/token_storage.dart';
import '../../data/models/login_request.dart';
import '../../data/models/login_response.dart';
import '../../data/services/auth_api_service.dart';
import '../../../home/presentation/screens/student_home_screen.dart';
import '../widgets/login_input_field.dart';
import 'forgot_password_screen.dart';

class LoginScreen extends StatefulWidget {
  const LoginScreen({this.authService, this.tokenStorage, super.key});

  final AuthService? authService;
  final TokenStorage? tokenStorage;

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final TextEditingController _phoneController = TextEditingController();
  final TextEditingController _passwordController = TextEditingController();

  late final AuthService _authService;
  late final TokenStorage _tokenStorage;
  late final bool _ownsAuthService;
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _ownsAuthService = widget.authService == null;
    _authService = widget.authService ?? AuthApiService();
    _tokenStorage = widget.tokenStorage ?? TokenStorage();
  }

  @override
  void dispose() {
    final authService = _authService;
    if (_ownsAuthService && authService is AuthApiService) {
      authService.close();
    }
    _phoneController.dispose();
    _passwordController.dispose();
    super.dispose();
  }

  Future<void> _handleLogin() async {
    if (_isLoading) {
      return;
    }

    final phone = _phoneController.text.trim();
    final password = _passwordController.text;
    FocusScope.of(context).unfocus();
    setState(() {
      _isLoading = true;
    });

    LoginResponse? loginResponse;
    try {
      final response = await _authService.login(
        LoginRequest(phoneNumber: phone, password: password),
      );
      await _tokenStorage.saveAccessToken(response.accessToken);
      loginResponse = response;
    } on ApiException catch (error) {
      if (mounted) {
        _showError(error.message);
      }
    } catch (_) {
      if (mounted) {
        _showError('Không thể đăng nhập lúc này. Vui lòng thử lại.');
      }
    } finally {
      if (mounted) {
        setState(() {
          _isLoading = false;
        });
      }
    }

    if (loginResponse != null && mounted) {
      Navigator.of(context).pushReplacement(
        MaterialPageRoute<void>(builder: (_) => const StudentHomeScreen()),
      );
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  Future<void> _openForgotPassword() async {
    final passwordWasReset = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) => ForgotPasswordScreen(authService: _authService),
      ),
    );

    if (passwordWasReset == true && mounted) {
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(
          const SnackBar(
            content: Text(
              'Đổi mật khẩu thành công. Bạn có thể đăng nhập ngay.',
            ),
          ),
        );
    }
  }

  @override
  Widget build(BuildContext context) {
    final mediaQuery = MediaQuery.of(context);
    final screenHeight = mediaQuery.size.height;
    final isNarrow = mediaQuery.size.width < 360;
    final horizontalPadding = isNarrow ? 16.0 : 28.0;

    return Scaffold(
      resizeToAvoidBottomInset: true,
      body: Stack(
        fit: StackFit.expand,
        children: [
          Image.asset(
            AppAssets.loginBackground,
            fit: BoxFit.cover,
            alignment: Alignment.topCenter,
          ),
          SafeArea(
            child: SingleChildScrollView(
              keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
              padding: EdgeInsets.fromLTRB(
                horizontalPadding,
                34,
                horizontalPadding,
                24 + mediaQuery.viewInsets.bottom,
              ),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight:
                      screenHeight -
                      mediaQuery.padding.top -
                      mediaQuery.padding.bottom -
                      58,
                ),
                child: Column(
                  children: [
                    SizedBox(height: screenHeight * 0.05),
                    const _BrandHeader(),
                    SizedBox(height: screenHeight * 0.12),
                    _LoginPanel(
                      phoneController: _phoneController,
                      passwordController: _passwordController,
                      onLogin: _handleLogin,
                      onForgotPassword: _openForgotPassword,
                      isLoading: _isLoading,
                    ),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _BrandHeader extends StatelessWidget {
  const _BrandHeader();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Image.asset(AppAssets.logo, width: 104, fit: BoxFit.contain),
            const SizedBox(width: 8),
            const Padding(
              padding: EdgeInsets.only(bottom: 7),
              child: Text(
                'Education',
                style: TextStyle(
                  color: Color(0xFF0F67B1),
                  fontSize: 19,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 26),
        Text.rich(
          const TextSpan(
            children: [
              TextSpan(text: 'MyF'),
              TextSpan(
                text: 'School',
                style: TextStyle(color: AppColors.deepOrange),
              ),
            ],
          ),
          textAlign: TextAlign.center,
          style: const TextStyle(
            color: AppColors.darkText,
            fontSize: 40,
            height: 1,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 18),
        const Text(
          'K\u1ebft n\u1ed1i nh\u00e0 tr\u01b0\u1eddng, gia \u0111\u00ecnh v\u00e0 h\u1ecdc sinh',
          textAlign: TextAlign.center,
          style: TextStyle(
            color: AppColors.mutedText,
            fontSize: 17,
            height: 1.25,
            fontWeight: FontWeight.w500,
          ),
        ),
      ],
    );
  }
}

class _LoginPanel extends StatefulWidget {
  const _LoginPanel({
    required this.phoneController,
    required this.passwordController,
    required this.onLogin,
    required this.onForgotPassword,
    required this.isLoading,
  });

  final TextEditingController phoneController;
  final TextEditingController passwordController;
  final VoidCallback onLogin;
  final VoidCallback onForgotPassword;
  final bool isLoading;

  @override
  State<_LoginPanel> createState() => _LoginPanelState();
}

class _LoginPanelState extends State<_LoginPanel> {
  bool _isPasswordVisible = false;

  @override
  Widget build(BuildContext context) {
    final isNarrow = MediaQuery.sizeOf(context).width < 360;

    return Container(
      width: double.infinity,
      constraints: const BoxConstraints(maxWidth: 420),
      padding: EdgeInsets.fromLTRB(
        isNarrow ? 16 : 22,
        28,
        isNarrow ? 16 : 22,
        28,
      ),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(14),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.08),
            blurRadius: 30,
            offset: const Offset(0, 16),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          LoginInputField(
            controller: widget.phoneController,
            hintText: 'S\u1ed1 \u0111i\u1ec7n tho\u1ea1i',
            prefixIcon: Icons.phone_outlined,
            keyboardType: TextInputType.phone,
          ),
          const SizedBox(height: 18),
          LoginInputField(
            controller: widget.passwordController,
            hintText: 'M\u1eadt kh\u1ea9u',
            prefixIcon: Icons.lock_outline,
            obscureText: !_isPasswordVisible,
            suffixIcon: IconButton(
              tooltip: _isPasswordVisible
                  ? '\u1ea8n m\u1eadt kh\u1ea9u'
                  : 'Hi\u1ec7n m\u1eadt kh\u1ea9u',
              onPressed: () {
                setState(() {
                  _isPasswordVisible = !_isPasswordVisible;
                });
              },
              icon: Icon(
                _isPasswordVisible
                    ? Icons.visibility_outlined
                    : Icons.visibility_off_outlined,
                color: AppColors.mutedText,
              ),
            ),
          ),
          const SizedBox(height: 12),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton(
              onPressed: widget.onForgotPassword,
              style: TextButton.styleFrom(
                foregroundColor: AppColors.deepOrange,
                padding: EdgeInsets.zero,
                minimumSize: const Size(0, 32),
                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
              ),
              child: const Text(
                'Qu\u00ean m\u1eadt kh\u1ea9u?',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w700),
              ),
            ),
          ),
          const SizedBox(height: 30),
          SizedBox(
            height: 56,
            child: ElevatedButton(
              onPressed: widget.isLoading ? null : widget.onLogin,
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.primaryOrange,
                foregroundColor: Colors.white,
                disabledBackgroundColor: AppColors.primaryOrange,
                disabledForegroundColor: Colors.white,
                elevation: 0,
                shadowColor: Colors.transparent,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: widget.isLoading
                  ? const SizedBox.square(
                      dimension: 24,
                      child: CircularProgressIndicator(
                        strokeWidth: 2.5,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      '\u0110\u0103ng nh\u1eadp',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}
