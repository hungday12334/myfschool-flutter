import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/network/api_client.dart';
import '../../data/models/grade_report.dart';
import '../../data/services/grade_api_service.dart';

class GradeScreen extends StatefulWidget {
  const GradeScreen({this.service, super.key});

  final GradeService? service;

  @override
  State<GradeScreen> createState() => _GradeScreenState();
}

class _GradeScreenState extends State<GradeScreen> {
  late final GradeService _service;
  late final bool _ownsService;
  GradePeriod _period = GradePeriod.semester1;
  GradeReport? _report;
  String? _error;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _ownsService = widget.service == null;
    _service = widget.service ?? GradeApiService();
    _load();
  }

  @override
  void dispose() {
    final service = _service;
    if (_ownsService && service is GradeApiService) {
      service.close();
    }
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final report = await _service.fetchReport(_period);
      if (mounted) {
        setState(() {
          _report = report;
          _isLoading = false;
        });
      }
    } on ApiException catch (error) {
      _setError(error.message);
    } catch (_) {
      _setError('Không thể tải kết quả học tập. Vui lòng thử lại.');
    }
  }

  void _setError(String message) {
    if (!mounted) return;
    setState(() {
      _error = message;
      _isLoading = false;
    });
  }

  void _selectPeriod(GradePeriod period) {
    if (period == _period || _isLoading) return;
    _period = period;
    _load();
  }

  void _openSubject(SubjectGradeSummary subject) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => GradeDetailScreen(
          service: _service,
          period: _period,
          subject: subject,
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF9F3),
      body: Stack(
        children: [
          const _WarmBackground(),
          SafeArea(
            child: Column(
              children: [
                _ScreenTitle(
                  title: 'Kết quả học tập',
                  onBack: () => Navigator.maybePop(context),
                ),
                Expanded(
                  child: RefreshIndicator(
                    color: AppColors.primaryOrange,
                    onRefresh: _load,
                    child: CustomScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      slivers: [
                        SliverPadding(
                          padding: const EdgeInsets.fromLTRB(18, 8, 18, 28),
                          sliver: SliverList.list(
                            children: [
                              if (_report != null)
                                _StudentHeader(student: _report!.student),
                              const SizedBox(height: 14),
                              _PeriodSelector(
                                selected: _period,
                                onSelected: _selectPeriod,
                              ),
                              const SizedBox(height: 22),
                              const Text(
                                'Danh sách môn học',
                                style: TextStyle(
                                  color: AppColors.darkText,
                                  fontSize: 19,
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              const SizedBox(height: 10),
                              _buildContent(),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContent() {
    if (_isLoading) {
      return const Padding(
        padding: EdgeInsets.only(top: 70),
        child: Center(
          child: CircularProgressIndicator(color: AppColors.primaryOrange),
        ),
      );
    }
    if (_error != null) {
      return _ErrorCard(message: _error!, onRetry: _load);
    }
    final subjects = _report?.subjects ?? const <SubjectGradeSummary>[];
    if (subjects.isEmpty) {
      return const _EmptyCard(message: 'Chưa có điểm cho giai đoạn này.');
    }
    return Column(
      children: [
        for (var index = 0; index < subjects.length; index++) ...[
          _SubjectTile(
            subject: subjects[index],
            onTap: () => _openSubject(subjects[index]),
          ),
          if (index != subjects.length - 1) const SizedBox(height: 5),
        ],
      ],
    );
  }
}

class GradeDetailScreen extends StatefulWidget {
  const GradeDetailScreen({
    required this.service,
    required this.period,
    required this.subject,
    super.key,
  });

  final GradeService service;
  final GradePeriod period;
  final SubjectGradeSummary subject;

  @override
  State<GradeDetailScreen> createState() => _GradeDetailScreenState();
}

class _GradeDetailScreenState extends State<GradeDetailScreen> {
  SubjectGradeDetail? _detail;
  String? _error;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final detail = await widget.service.fetchSubject(
        subjectCode: widget.subject.subjectCode,
        period: widget.period,
      );
      if (mounted) {
        setState(() {
          _detail = detail;
          _isLoading = false;
        });
      }
    } on ApiException catch (error) {
      _setError(error.message);
    } catch (_) {
      _setError('Không thể tải chi tiết điểm. Vui lòng thử lại.');
    }
  }

  void _setError(String message) {
    if (!mounted) return;
    setState(() {
      _error = message;
      _isLoading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFFF9F3),
      body: Stack(
        children: [
          const _WarmBackground(),
          SafeArea(
            child: Column(
              children: [
                _ScreenTitle(
                  title: 'Chi tiết điểm môn ${widget.subject.subjectName}',
                  onBack: () => Navigator.maybePop(context),
                ),
                Expanded(
                  child: RefreshIndicator(
                    color: AppColors.primaryOrange,
                    onRefresh: _load,
                    child: SingleChildScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.fromLTRB(18, 8, 18, 28),
                      child: _buildContent(),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildContent() {
    if (_isLoading) {
      return const SizedBox(
        height: 420,
        child: Center(
          child: CircularProgressIndicator(color: AppColors.primaryOrange),
        ),
      );
    }
    if (_error != null) {
      return _ErrorCard(message: _error!, onRetry: _load);
    }
    final detail = _detail!;
    final style = _subjectStyle(detail.subjectCode);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          padding: const EdgeInsets.all(14),
          decoration: _whiteCardDecoration(radius: 16),
          child: Row(
            children: [
              _SubjectIcon(style: style, size: 62),
              const SizedBox(width: 15),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      detail.subjectName,
                      style: const TextStyle(
                        color: AppColors.darkText,
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 5),
                    Text(
                      'Lớp ${detail.student.className} - ${widget.period.label}',
                      style: const TextStyle(
                        color: AppColors.mutedText,
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                width: 96,
                padding: const EdgeInsets.symmetric(vertical: 8),
                decoration: BoxDecoration(
                  color: const Color(0xFFEAF4FF),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Column(
                  children: [
                    const Text(
                      'Điểm trung bình',
                      maxLines: 2,
                      textAlign: TextAlign.center,
                      style: TextStyle(color: Color(0xFF2F80ED), fontSize: 12),
                    ),
                    Text(
                      detail.averageScore.toStringAsFixed(1),
                      style: const TextStyle(
                        color: Color(0xFF2374D8),
                        fontSize: 27,
                        height: 1.15,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 22),
        const _SectionTitle('Điểm thành phần'),
        const SizedBox(height: 9),
        Container(
          padding: const EdgeInsets.symmetric(horizontal: 15),
          decoration: _whiteCardDecoration(),
          child: Column(
            children: [
              for (
                var index = 0;
                index < detail.assessments.length;
                index++
              ) ...[
                _AssessmentRow(component: detail.assessments[index]),
                if (index != detail.assessments.length - 1)
                  const Divider(height: 1, color: Color(0xFFEDF0F3)),
              ],
            ],
          ),
        ),
        const SizedBox(height: 22),
        const _SectionTitle('Biểu đồ điểm'),
        const SizedBox(height: 9),
        Container(
          height: 225,
          padding: const EdgeInsets.fromLTRB(8, 14, 8, 6),
          decoration: _whiteCardDecoration(),
          child: CustomPaint(
            key: const Key('grade-chart'),
            painter: _GradeChartPainter(detail.assessments),
            child: const SizedBox.expand(),
          ),
        ),
        const SizedBox(height: 16),
        Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: const Color(0xFFFFEEDF),
            borderRadius: BorderRadius.circular(14),
          ),
          child: const Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              CircleAvatar(
                radius: 14,
                backgroundColor: AppColors.primaryOrange,
                child: Icon(Icons.info_outline, color: Colors.white, size: 18),
              ),
              SizedBox(width: 11),
              Expanded(
                child: Text.rich(
                  TextSpan(
                    children: [
                      TextSpan(
                        text: 'Lưu ý\n',
                        style: TextStyle(
                          color: AppColors.darkText,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      TextSpan(
                        text:
                            'Điểm số hiển thị mang tính chất tham khảo, '
                            'vui lòng liên hệ nhà trường để biết thêm thông tin chi tiết.',
                        style: TextStyle(
                          color: AppColors.mutedText,
                          height: 1.35,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _WarmBackground extends StatelessWidget {
  const _WarmBackground();

  @override
  Widget build(BuildContext context) {
    return Positioned.fill(
      child: DecoratedBox(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFFFFF4E8), Color(0xFFFFFBF7), Color(0xFFFFF7EE)],
          ),
        ),
      ),
    );
  }
}

class _ScreenTitle extends StatelessWidget {
  const _ScreenTitle({required this.title, required this.onBack});

  final String title;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 64,
      child: Row(
        children: [
          IconButton(
            tooltip: 'Quay lại',
            onPressed: onBack,
            icon: const Icon(Icons.arrow_back_rounded, size: 28),
          ),
          Expanded(
            child: Text(
              title,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.darkText,
                fontSize: 21,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          const SizedBox(width: 48),
        ],
      ),
    );
  }
}

class _StudentHeader extends StatelessWidget {
  const _StudentHeader({required this.student});

  final StudentSummary student;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        const CircleAvatar(
          radius: 34,
          backgroundColor: Color(0xFFDDEEFF),
          child: Icon(Icons.person_rounded, color: Color(0xFF1F5F92), size: 46),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                student.fullName,
                style: const TextStyle(
                  color: AppColors.darkText,
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const SizedBox(height: 5),
              Text(
                'Lớp ${student.className} - ${student.schoolName}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: AppColors.mutedText,
                  fontSize: 13,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
        const Icon(
          Icons.keyboard_arrow_down_rounded,
          color: AppColors.mutedText,
        ),
      ],
    );
  }
}

class _PeriodSelector extends StatelessWidget {
  const _PeriodSelector({required this.selected, required this.onSelected});

  final GradePeriod selected;
  final ValueChanged<GradePeriod> onSelected;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 46,
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: const Color(0xFFF2F3F5),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.white, width: 2),
      ),
      child: Row(
        children: [
          for (final period in GradePeriod.values)
            Expanded(
              child: InkWell(
                key: Key('grade-period-${period.queryValue}'),
                onTap: () => onSelected(period),
                borderRadius: BorderRadius.circular(9),
                child: AnimatedContainer(
                  duration: const Duration(milliseconds: 160),
                  alignment: Alignment.center,
                  decoration: BoxDecoration(
                    color: selected == period
                        ? Colors.white
                        : Colors.transparent,
                    borderRadius: BorderRadius.circular(9),
                    border: selected == period
                        ? Border.all(color: AppColors.primaryOrange)
                        : null,
                  ),
                  child: Text(
                    period.label,
                    style: TextStyle(
                      color: selected == period
                          ? AppColors.primaryOrange
                          : AppColors.mutedText,
                      fontSize: 14,
                      fontWeight: selected == period
                          ? FontWeight.w800
                          : FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _SubjectTile extends StatelessWidget {
  const _SubjectTile({required this.subject, required this.onTap});

  final SubjectGradeSummary subject;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final style = _subjectStyle(subject.subjectCode);
    return Material(
      color: Colors.white.withValues(alpha: 0.92),
      borderRadius: BorderRadius.circular(14),
      child: InkWell(
        key: Key('subject-${subject.subjectCode}'),
        onTap: onTap,
        borderRadius: BorderRadius.circular(14),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 9),
          child: Row(
            children: [
              _SubjectIcon(style: style),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      subject.subjectName,
                      style: const TextStyle(
                        color: AppColors.darkText,
                        fontSize: 15,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 3),
                    const Text(
                      'Điểm trung bình',
                      style: TextStyle(
                        color: AppColors.mutedText,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                width: 58,
                padding: const EdgeInsets.symmetric(vertical: 7),
                alignment: Alignment.center,
                decoration: BoxDecoration(
                  color: style.background,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Text(
                  subject.averageScore.toStringAsFixed(1),
                  style: TextStyle(
                    color: style.foreground,
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              const SizedBox(width: 7),
              const Icon(
                Icons.chevron_right_rounded,
                color: AppColors.mutedText,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _SubjectIcon extends StatelessWidget {
  const _SubjectIcon({required this.style, this.size = 46});

  final _SubjectStyle style;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: style.background,
        borderRadius: BorderRadius.circular(size * 0.27),
      ),
      child: Icon(style.icon, color: style.foreground, size: size * 0.58),
    );
  }
}

class _AssessmentRow extends StatelessWidget {
  const _AssessmentRow({required this.component});

  final GradeComponent component;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  component.label,
                  style: const TextStyle(
                    color: AppColors.darkText,
                    fontSize: 15,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  'Hệ số ${component.coefficient}',
                  style: const TextStyle(
                    color: AppColors.mutedText,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
          Text(
            component.score.toStringAsFixed(1),
            style: const TextStyle(
              color: AppColors.darkText,
              fontSize: 16,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _SectionTitle extends StatelessWidget {
  const _SectionTitle(this.text);

  final String text;

  @override
  Widget build(BuildContext context) {
    return Text(
      text,
      style: const TextStyle(
        color: AppColors.darkText,
        fontSize: 19,
        fontWeight: FontWeight.w800,
      ),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  const _ErrorCard({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(22),
      decoration: _whiteCardDecoration(),
      child: Column(
        children: [
          const Icon(
            Icons.cloud_off_rounded,
            color: AppColors.mutedText,
            size: 40,
          ),
          const SizedBox(height: 10),
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 10),
          TextButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({required this.message});

  final String message;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(28),
      decoration: _whiteCardDecoration(),
      child: Text(message, textAlign: TextAlign.center),
    );
  }
}

class _GradeChartPainter extends CustomPainter {
  _GradeChartPainter(this.components);

  final List<GradeComponent> components;

  @override
  void paint(Canvas canvas, Size size) {
    if (components.isEmpty) return;
    const left = 30.0;
    const right = 10.0;
    const top = 18.0;
    const bottom = 38.0;
    final chartWidth = size.width - left - right;
    final chartHeight = size.height - top - bottom;
    final gridPaint = Paint()
      ..color = const Color(0xFFE7EAED)
      ..strokeWidth = 1;
    for (var score = 0; score <= 10; score += 2) {
      final y = top + chartHeight * (1 - score / 10);
      canvas.drawLine(
        Offset(left, y),
        Offset(size.width - right, y),
        gridPaint,
      );
      _drawText(
        canvas,
        '$score',
        Offset(3, y - 7),
        const Color(0xFF6F7A86),
        10,
      );
    }

    final points = <Offset>[];
    for (var index = 0; index < components.length; index++) {
      final x = components.length == 1
          ? left + chartWidth / 2
          : left + chartWidth * index / (components.length - 1);
      final y = top + chartHeight * (1 - components[index].score / 10);
      points.add(Offset(x, y));
    }
    final area = Path()..moveTo(points.first.dx, top + chartHeight);
    for (final point in points) {
      area.lineTo(point.dx, point.dy);
    }
    area
      ..lineTo(points.last.dx, top + chartHeight)
      ..close();
    canvas.drawPath(
      area,
      Paint()..color = AppColors.primaryOrange.withValues(alpha: 0.10),
    );
    final line = Path()..moveTo(points.first.dx, points.first.dy);
    for (final point in points.skip(1)) {
      line.lineTo(point.dx, point.dy);
    }
    canvas.drawPath(
      line,
      Paint()
        ..color = AppColors.primaryOrange
        ..strokeWidth = 2
        ..style = PaintingStyle.stroke,
    );
    for (var index = 0; index < points.length; index++) {
      final point = points[index];
      canvas.drawCircle(point, 4.5, Paint()..color = AppColors.primaryOrange);
      final score = components[index].score.toStringAsFixed(1);
      _drawCenteredText(
        canvas,
        score,
        Offset(point.dx, math.max(0, point.dy - 19)),
        AppColors.darkText,
        10,
        FontWeight.w700,
      );
      var label = components[index].label;
      if (components.length > 4) {
        label =
            '${components[index].semester == 1 ? 'HK1' : 'HK2'} ${index % 4 + 1}';
      }
      _drawCenteredText(
        canvas,
        label,
        Offset(point.dx, top + chartHeight + 17),
        const Color(0xFF6F7A86),
        components.length > 5 ? 8 : 10,
        FontWeight.w500,
      );
    }
  }

  void _drawText(
    Canvas canvas,
    String text,
    Offset offset,
    Color color,
    double size,
  ) {
    final painter = TextPainter(
      text: TextSpan(
        text: text,
        style: TextStyle(color: color, fontSize: size),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    painter.paint(canvas, offset);
  }

  void _drawCenteredText(
    Canvas canvas,
    String text,
    Offset center,
    Color color,
    double size,
    FontWeight weight,
  ) {
    final painter = TextPainter(
      text: TextSpan(
        text: text,
        style: TextStyle(color: color, fontSize: size, fontWeight: weight),
      ),
      textDirection: TextDirection.ltr,
      maxLines: 1,
    )..layout(maxWidth: 70);
    painter.paint(
      canvas,
      Offset(center.dx - painter.width / 2, center.dy - painter.height / 2),
    );
  }

  @override
  bool shouldRepaint(covariant _GradeChartPainter oldDelegate) {
    return oldDelegate.components != components;
  }
}

class _SubjectStyle {
  const _SubjectStyle(this.icon, this.foreground, this.background);

  final IconData icon;
  final Color foreground;
  final Color background;
}

_SubjectStyle _subjectStyle(String code) {
  return switch (code.toUpperCase()) {
    'MATH' => const _SubjectStyle(
      Icons.calculate_rounded,
      Color(0xFF2F80ED),
      Color(0xFFEAF4FF),
    ),
    'LITERATURE' => const _SubjectStyle(
      Icons.menu_book_rounded,
      Color(0xFFFF3569),
      Color(0xFFFFEEF3),
    ),
    'ENGLISH' => const _SubjectStyle(
      Icons.translate_rounded,
      Color(0xFF7847EA),
      Color(0xFFF3EEFF),
    ),
    'PHYSICS' => const _SubjectStyle(
      Icons.science_outlined,
      Color(0xFFF5A400),
      Color(0xFFFFF8E8),
    ),
    'CHEMISTRY' => const _SubjectStyle(
      Icons.science_rounded,
      Color(0xFF18B982),
      Color(0xFFEAFBF5),
    ),
    'BIOLOGY' => const _SubjectStyle(
      Icons.eco_rounded,
      Color(0xFF38A944),
      Color(0xFFECF9EA),
    ),
    'HISTORY' => const _SubjectStyle(
      Icons.account_balance_rounded,
      Color(0xFFE46B13),
      Color(0xFFFFF0E3),
    ),
    'GEOGRAPHY' => const _SubjectStyle(
      Icons.public_rounded,
      Color(0xFF15A7AD),
      Color(0xFFE6F9FA),
    ),
    'CIVICS' => const _SubjectStyle(
      Icons.balance_rounded,
      Color(0xFFFF3569),
      Color(0xFFFFEEF3),
    ),
    'INFORMATICS' => const _SubjectStyle(
      Icons.computer_rounded,
      Color(0xFF7847EA),
      Color(0xFFF3EEFF),
    ),
    _ => const _SubjectStyle(
      Icons.school_rounded,
      Color(0xFF2F80ED),
      Color(0xFFEAF4FF),
    ),
  };
}

BoxDecoration _whiteCardDecoration({double radius = 14}) {
  return BoxDecoration(
    color: Colors.white.withValues(alpha: 0.94),
    borderRadius: BorderRadius.circular(radius),
    boxShadow: [
      BoxShadow(
        color: Colors.black.withValues(alpha: 0.035),
        blurRadius: 16,
        offset: const Offset(0, 6),
      ),
    ],
  );
}
