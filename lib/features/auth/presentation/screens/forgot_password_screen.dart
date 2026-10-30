import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../../../core/constants/app_assets.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/network/api_client.dart';
import '../../data/services/auth_api_service.dart';

enum _ResetStep { email, code, password }

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({required this.authService, super.key});

  final AuthService authService;

  @override
  State<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  static const int _resendCooldownSeconds = 60;

  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _emailController = TextEditingController();
  final TextEditingController _codeController = TextEditingController();
  final TextEditingController _newPasswordController = TextEditingController();
  final TextEditingController _confirmPasswordController =
      TextEditingController();

  _ResetStep _step = _ResetStep.email;
  Timer? _resendTimer;
  String? _resetToken;
  int _resendSeconds = 0;
  bool _isLoading = false;
  bool _isNewPasswordVisible = false;
  bool _isConfirmPasswordVisible = false;

  @override
  void dispose() {
    _resendTimer?.cancel();
    _emailController.dispose();
    _codeController.dispose();
    _newPasswordController.dispose();
    _confirmPasswordController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (_isLoading || !(_formKey.currentState?.validate() ?? false)) {
      return;
    }

    FocusScope.of(context).unfocus();
    await switch (_step) {
      _ResetStep.email => _requestCode(),
      _ResetStep.code => _verifyCode(),
      _ResetStep.password => _confirmPasswordReset(),
    };
  }

  Future<void> _requestCode() async {
    _setLoading(true);
    try {
      await widget.authService.requestPasswordReset(
        _emailController.text.trim(),
      );
      if (!mounted) {
        return;
      }

      setState(() {
        _step = _ResetStep.code;
        _resendSeconds = _resendCooldownSeconds;
      });
      _startResendTimer();
    } on ApiException catch (error) {
      _showError(error.message);
    } catch (_) {
      _showError('Không thể gửi mã xác thực lúc này. Vui lòng thử lại.');
    } finally {
      _setLoading(false);
    }
  }

  Future<void> _resendCode() async {
    if (_isLoading || _resendSeconds > 0) {
      return;
    }

    _setLoading(true);
    try {
      await widget.authService.requestPasswordReset(
        _emailController.text.trim(),
      );
      if (!mounted) {
        return;
      }

      setState(() {
        _resendSeconds = _resendCooldownSeconds;
      });
      _startResendTimer();
      _showMessage('Mã xác thực mới đã được gửi đến email của bạn.');
    } on ApiException catch (error) {
      _showError(error.message);
    } catch (_) {
      _showError('Không thể gửi lại mã xác thực. Vui lòng thử lại.');
    } finally {
      _setLoading(false);
    }
  }

  Future<void> _verifyCode() async {
    _setLoading(true);
    try {
      final resetToken = await widget.authService.verifyPasswordResetCode(
        email: _emailController.text.trim(),
        code: _codeController.text.trim(),
      );
      if (!mounted) {
        return;
      }

      setState(() {
        _resetToken = resetToken;
        _step = _ResetStep.password;
      });
    } on ApiException catch (error) {
      _showError(error.message);
    } catch (_) {
      _showError('Không thể xác thực mã lúc này. Vui lòng thử lại.');
    } finally {
      _setLoading(false);
    }
  }

  Future<void> _confirmPasswordReset() async {
    final resetToken = _resetToken;
    if (resetToken == null) {
      _showError('Phiên đặt lại mật khẩu không hợp lệ. Vui lòng thử lại.');
      return;
    }

    _setLoading(true);
    try {
      await widget.authService.confirmPasswordReset(
        resetToken: resetToken,
        newPassword: _newPasswordController.text,
        confirmPassword: _confirmPasswordController.text,
      );
      if (mounted) {
        Navigator.of(context).pop(true);
      }
    } on ApiException catch (error) {
      _showError(error.message);
    } catch (_) {
      _showError('Không thể đổi mật khẩu lúc này. Vui lòng thử lại.');
    } finally {
      _setLoading(false);
    }
  }

  void _startResendTimer() {
    _resendTimer?.cancel();
    _resendTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (!mounted) {
        timer.cancel();
        return;
      }

      if (_resendSeconds <= 1) {
        timer.cancel();
        setState(() {
          _resendSeconds = 0;
        });
        return;
      }

      setState(() {
        _resendSeconds -= 1;
      });
    });
  }

  void _setLoading(bool value) {
    if (mounted) {
      setState(() {
        _isLoading = value;
      });
    }
  }

  void _showError(String message) {
    if (mounted) {
      _showMessage(message);
    }
  }

  void _showMessage(String message) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(message)));
  }

  String? _validateEmail(String? value) {
    final email = value?.trim() ?? '';
    if (email.isEmpty) {
      return 'Vui lòng nhập email.';
    }
    if (!RegExp(r'^[^\s@]+@[^\s@]+\.[^\s@]+$').hasMatch(email)) {
      return 'Email không đúng định dạng.';
    }
    return null;
  }

  String? _validateCode(String? value) {
    if (!RegExp(r'^\d{6}$').hasMatch(value?.trim() ?? '')) {
      return 'Mã xác thực phải gồm 6 chữ số.';
    }
    return null;
  }

  String? _validatePassword(String? value) {
    final password = value ?? '';
    if (password.length < 8) {
      return 'Mật khẩu phải có ít nhất 8 ký tự.';
    }
    if (password.length > 72) {
      return 'Mật khẩu không được vượt quá 72 ký tự.';
    }
    return null;
  }

  String? _validateConfirmation(String? value) {
    if (value != _newPasswordController.text) {
      return 'Mật khẩu xác nhận không khớp.';
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    final mediaQuery = MediaQuery.of(context);
    final isNarrow = mediaQuery.size.width < 360;

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
                isNarrow ? 16 : 24,
                8,
                isNarrow ? 16 : 24,
                24 + mediaQuery.viewInsets.bottom,
              ),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight:
                      mediaQuery.size.height -
                      mediaQuery.padding.top -
                      mediaQuery.padding.bottom -
                      32,
                ),
                child: Column(
                  children: [
                    Align(
                      alignment: Alignment.centerLeft,
                      child: IconButton(
                        tooltip: 'Quay lại',
                        onPressed: _isLoading
                            ? null
                            : () => Navigator.of(context).pop(),
                        icon: const Icon(Icons.arrow_back),
                        color: AppColors.darkText,
                      ),
                    ),
                    const SizedBox(height: 10),
                    const _ResetBrandHeader(),
                    SizedBox(height: mediaQuery.size.height * 0.06),
                    _buildPanel(isNarrow),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPanel(bool isNarrow) {
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
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              _title,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.darkText,
                fontSize: 26,
                fontWeight: FontWeight.w800,
              ),
            ),
            const SizedBox(height: 12),
            Text(
              _description,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.mutedText,
                fontSize: 15,
                height: 1.45,
                fontWeight: FontWeight.w500,
              ),
            ),
            const SizedBox(height: 26),
            KeyedSubtree(
              key: ValueKey<_ResetStep>(_step),
              child: _buildStepFields(),
            ),
            const SizedBox(height: 26),
            SizedBox(
              height: 56,
              child: ElevatedButton(
                onPressed: _isLoading ? null : _submit,
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.primaryOrange,
                  foregroundColor: Colors.white,
                  disabledBackgroundColor: AppColors.primaryOrange,
                  disabledForegroundColor: Colors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                child: _isLoading
                    ? const SizedBox.square(
                        dimension: 24,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.5,
                          color: Colors.white,
                        ),
                      )
                    : Text(
                        _buttonLabel,
                        style: const TextStyle(
                          fontSize: 18,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStepFields() {
    return switch (_step) {
      _ResetStep.email => TextFormField(
        controller: _emailController,
        autofocus: true,
        keyboardType: TextInputType.emailAddress,
        textInputAction: TextInputAction.done,
        autocorrect: false,
        enableSuggestions: false,
        autofillHints: const <String>[AutofillHints.email],
        validator: _validateEmail,
        onFieldSubmitted: (_) => _submit(),
        decoration: const InputDecoration(
          labelText: 'Email',
          hintText: 'Nhập email đã đăng ký',
          prefixIcon: Icon(Icons.email_outlined, color: AppColors.mutedText),
        ),
      ),
      _ResetStep.code => Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(
            _emailController.text.trim(),
            textAlign: TextAlign.center,
            style: const TextStyle(
              color: AppColors.deepOrange,
              fontSize: 16,
              fontWeight: FontWeight.w700,
            ),
          ),
          const SizedBox(height: 18),
          TextFormField(
            controller: _codeController,
            autofocus: true,
            keyboardType: TextInputType.number,
            textInputAction: TextInputAction.done,
            maxLength: 6,
            inputFormatters: <TextInputFormatter>[
              FilteringTextInputFormatter.digitsOnly,
              LengthLimitingTextInputFormatter(6),
            ],
            validator: _validateCode,
            onFieldSubmitted: (_) => _submit(),
            decoration: const InputDecoration(
              labelText: 'Mã xác thực',
              hintText: 'Nhập mã gồm 6 chữ số',
              counterText: '',
              prefixIcon: Icon(
                Icons.verified_user_outlined,
                color: AppColors.mutedText,
              ),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Flexible(
                child: Text(
                  'Không nhận được mã?',
                  style: TextStyle(color: AppColors.mutedText),
                ),
              ),
              TextButton(
                onPressed: !_isLoading && _resendSeconds == 0
                    ? _resendCode
                    : null,
                child: Text(
                  _resendSeconds > 0
                      ? 'Gửi lại mã (${_resendSeconds}s)'
                      : 'Gửi lại mã',
                ),
              ),
            ],
          ),
        ],
      ),
      _ResetStep.password => Column(
        children: [
          TextFormField(
            controller: _newPasswordController,
            autofocus: true,
            obscureText: !_isNewPasswordVisible,
            textInputAction: TextInputAction.next,
            validator: _validatePassword,
            decoration: InputDecoration(
              labelText: 'Mật khẩu mới',
              prefixIcon: const Icon(
                Icons.lock_outline,
                color: AppColors.mutedText,
              ),
              suffixIcon: IconButton(
                tooltip: _isNewPasswordVisible
                    ? 'Ẩn mật khẩu'
                    : 'Hiện mật khẩu',
                onPressed: () {
                  setState(() {
                    _isNewPasswordVisible = !_isNewPasswordVisible;
                  });
                },
                icon: Icon(
                  _isNewPasswordVisible
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                  color: AppColors.mutedText,
                ),
              ),
            ),
          ),
          const SizedBox(height: 18),
          TextFormField(
            controller: _confirmPasswordController,
            obscureText: !_isConfirmPasswordVisible,
            textInputAction: TextInputAction.done,
            validator: _validateConfirmation,
            onFieldSubmitted: (_) => _submit(),
            decoration: InputDecoration(
              labelText: 'Xác nhận mật khẩu',
              prefixIcon: const Icon(
                Icons.lock_outline,
                color: AppColors.mutedText,
              ),
              suffixIcon: IconButton(
                tooltip: _isConfirmPasswordVisible
                    ? 'Ẩn mật khẩu'
                    : 'Hiện mật khẩu',
                onPressed: () {
                  setState(() {
                    _isConfirmPasswordVisible = !_isConfirmPasswordVisible;
                  });
                },
                icon: Icon(
                  _isConfirmPasswordVisible
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                  color: AppColors.mutedText,
                ),
              ),
            ),
          ),
        ],
      ),
    };
  }

  String get _title => switch (_step) {
    _ResetStep.email => 'Quên mật khẩu?',
    _ResetStep.code => 'Nhập mã xác thực',
    _ResetStep.password => 'Tạo mật khẩu mới',
  };

  String get _description => switch (_step) {
    _ResetStep.email =>
      'Nhập email đã đăng ký để nhận mã xác thực gồm 6 chữ số.',
    _ResetStep.code => 'Mã xác thực đã được gửi đến email của bạn.',
    _ResetStep.password => 'Nhập mật khẩu mới để hoàn tất đặt lại mật khẩu.',
  };

  String get _buttonLabel => switch (_step) {
    _ResetStep.email => 'Gửi mã xác thực',
    _ResetStep.code => 'Xác thực mã',
    _ResetStep.password => 'Đổi mật khẩu',
  };
}

class _ResetBrandHeader extends StatelessWidget {
  const _ResetBrandHeader();

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            Image.asset(AppAssets.logo, width: 92, fit: BoxFit.contain),
            const SizedBox(width: 7),
            const Padding(
              padding: EdgeInsets.only(bottom: 6),
              child: Text(
                'Education',
                style: TextStyle(
                  color: Color(0xFF0F67B1),
                  fontSize: 17,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ),
        const SizedBox(height: 20),
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
            fontSize: 34,
            height: 1,
            fontWeight: FontWeight.w800,
          ),
        ),
      ],
    );
  }
}
