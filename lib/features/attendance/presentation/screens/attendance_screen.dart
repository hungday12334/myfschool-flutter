import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/network/api_client.dart';
import '../../data/models/attendance_report.dart';
import '../../data/services/attendance_api_service.dart';

class AttendanceScreen extends StatefulWidget {
  const AttendanceScreen({this.service, this.initialMonth, super.key});

  final AttendanceService? service;
  final DateTime? initialMonth;

  @override
  State<AttendanceScreen> createState() => _AttendanceScreenState();
}

class _AttendanceScreenState extends State<AttendanceScreen> {
  late final AttendanceService _service;
  late final bool _ownsService;
  late DateTime _month;
  AttendanceReport? _report;
  String? _error;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _ownsService = widget.service == null;
    _service = widget.service ?? AttendanceApiService();
    final initial = widget.initialMonth ?? DateTime.now();
    _month = DateTime(initial.year, initial.month);
    _load();
  }

  @override
  void dispose() {
    final service = _service;
    if (_ownsService && service is AttendanceApiService) service.close();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final report = await _service.fetchMonth(_month);
      if (!mounted) return;
      setState(() {
        _report = report;
        _isLoading = false;
      });
    } on ApiException catch (error) {
      _showError(error.message);
    } catch (_) {
      _showError('Không thể tải báo cáo điểm danh. Vui lòng thử lại.');
    }
  }

  void _showError(String message) {
    if (!mounted) return;
    setState(() {
      _error = message;
      _isLoading = false;
    });
  }

  void _changeMonth(int offset) {
    if (_isLoading) return;
    final next = DateTime(_month.year, _month.month + offset);
    final current = DateTime.now();
    if (next.isAfter(DateTime(current.year, current.month))) return;
    _month = next;
    _load();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF8FBF9),
      body: Stack(
        children: [
          const _AttendanceBackground(),
          SafeArea(
            child: Column(
              children: [
                _TitleBar(
                  title: 'Báo cáo điểm danh',
                  onBack: () => Navigator.maybePop(context),
                ),
                Expanded(
                  child: RefreshIndicator(
                    color: const Color(0xFF18B982),
                    onRefresh: _load,
                    child: ListView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.fromLTRB(18, 6, 18, 28),
                      children: [
                        if (_report != null)
                          _StudentCard(student: _report!.student),
                        const SizedBox(height: 14),
                        _MonthPicker(
                          month: _month,
                          canGoNext: !_isCurrentMonth(_month),
                          onPrevious: () => _changeMonth(-1),
                          onNext: () => _changeMonth(1),
                        ),
                        const SizedBox(height: 16),
                        _buildContent(),
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
      return const SizedBox(
        height: 360,
        child: Center(
          child: CircularProgressIndicator(color: Color(0xFF18B982)),
        ),
      );
    }
    if (_error != null) return _ErrorCard(message: _error!, onRetry: _load);
    final report = _report!;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _AttendanceRateCard(report: report),
        const SizedBox(height: 14),
        GridView.count(
          crossAxisCount: 2,
          mainAxisSpacing: 10,
          crossAxisSpacing: 10,
          childAspectRatio: 1.75,
          shrinkWrap: true,
          physics: const NeverScrollableScrollPhysics(),
          children: [
            _SummaryCard(
              label: 'Có mặt',
              value: report.presentDays,
              icon: Icons.check_circle_rounded,
              color: const Color(0xFF18B982),
            ),
            _SummaryCard(
              label: 'Đi muộn',
              value: report.lateDays,
              icon: Icons.schedule_rounded,
              color: const Color(0xFFF5A400),
            ),
            _SummaryCard(
              label: 'Nghỉ có phép',
              value: report.excusedAbsenceDays,
              icon: Icons.event_available_rounded,
              color: const Color(0xFF2F80ED),
            ),
            _SummaryCard(
              label: 'Nghỉ không phép',
              value: report.unexcusedAbsenceDays,
              icon: Icons.cancel_rounded,
              color: const Color(0xFFFF4163),
            ),
          ],
        ),
        const SizedBox(height: 22),
        const Text(
          'Chi tiết điểm danh',
          style: TextStyle(
            color: AppColors.darkText,
            fontSize: 19,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 10),
        if (report.records.isEmpty)
          const _EmptyCard()
        else
          for (var index = 0; index < report.records.length; index++) ...[
            _AttendanceTile(day: report.records[index]),
            if (index != report.records.length - 1) const SizedBox(height: 8),
          ],
      ],
    );
  }
}

class _AttendanceBackground extends StatelessWidget {
  const _AttendanceBackground();

  @override
  Widget build(BuildContext context) {
    return const Positioned.fill(
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFFEAFBF5), Color(0xFFFBFEFC), Color(0xFFFFF8F0)],
          ),
        ),
      ),
    );
  }
}

class _TitleBar extends StatelessWidget {
  const _TitleBar({required this.title, required this.onBack});

  final String title;
  final VoidCallback onBack;

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 62,
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

class _StudentCard extends StatelessWidget {
  const _StudentCard({required this.student});

  final AttendanceStudent student;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(13),
      decoration: _cardDecoration(),
      child: Row(
        children: [
          const CircleAvatar(
            radius: 27,
            backgroundColor: Color(0xFFDDEEFF),
            child: Icon(
              Icons.person_rounded,
              color: Color(0xFF1F5F92),
              size: 37,
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  student.fullName,
                  style: const TextStyle(
                    color: AppColors.darkText,
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  'Lớp ${student.className} - ${student.schoolName}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: AppColors.mutedText,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _MonthPicker extends StatelessWidget {
  const _MonthPicker({
    required this.month,
    required this.canGoNext,
    required this.onPrevious,
    required this.onNext,
  });

  final DateTime month;
  final bool canGoNext;
  final VoidCallback onPrevious;
  final VoidCallback onNext;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 48,
      decoration: _cardDecoration(),
      child: Row(
        children: [
          IconButton(
            key: const Key('attendance-previous-month'),
            onPressed: onPrevious,
            icon: const Icon(Icons.chevron_left_rounded),
          ),
          Expanded(
            child: Text(
              'Tháng ${month.month.toString().padLeft(2, '0')}/${month.year}',
              textAlign: TextAlign.center,
              style: const TextStyle(
                color: AppColors.darkText,
                fontSize: 16,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
          IconButton(
            key: const Key('attendance-next-month'),
            onPressed: canGoNext ? onNext : null,
            icon: const Icon(Icons.chevron_right_rounded),
          ),
        ],
      ),
    );
  }
}

class _AttendanceRateCard extends StatelessWidget {
  const _AttendanceRateCard({required this.report});

  final AttendanceReport report;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color(0xFF18B982),
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: const Color(0xFF18B982).withValues(alpha: 0.22),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Tỷ lệ chuyên cần',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
              Text(
                '${report.attendanceRate.toStringAsFixed(1)}%',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 25,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              minHeight: 8,
              value: report.attendanceRate / 100,
              backgroundColor: Colors.white.withValues(alpha: 0.28),
              valueColor: const AlwaysStoppedAnimation<Color>(Colors.white),
            ),
          ),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerLeft,
            child: Text(
              '${report.totalSchoolDays} ngày học đã ghi nhận',
              style: TextStyle(
                color: Colors.white.withValues(alpha: 0.88),
                fontSize: 12,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _SummaryCard extends StatelessWidget {
  const _SummaryCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.color,
  });

  final String label;
  final int value;
  final IconData icon;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 11),
      decoration: _cardDecoration(),
      child: Row(
        children: [
          Container(
            width: 38,
            height: 38,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: color, size: 22),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '$value',
                  style: TextStyle(
                    color: color,
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: AppColors.mutedText,
                    fontSize: 11,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _AttendanceTile extends StatelessWidget {
  const _AttendanceTile({required this.day});

  final AttendanceDay day;

  @override
  Widget build(BuildContext context) {
    final style = _attendanceStyle(day.status);
    return Container(
      padding: const EdgeInsets.all(13),
      decoration: _cardDecoration(),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 47,
            padding: const EdgeInsets.symmetric(vertical: 7),
            decoration: BoxDecoration(
              color: style.color.withValues(alpha: 0.11),
              borderRadius: BorderRadius.circular(11),
            ),
            child: Column(
              children: [
                Text(
                  day.date.day.toString().padLeft(2, '0'),
                  style: TextStyle(
                    color: style.color,
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                Text(
                  _weekday(day.date.weekday),
                  style: TextStyle(
                    color: style.color,
                    fontSize: 10,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Icon(style.icon, color: style.color, size: 18),
                    const SizedBox(width: 6),
                    Expanded(
                      child: Text(
                        style.label,
                        style: const TextStyle(
                          color: AppColors.darkText,
                          fontSize: 14,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                    if (day.checkInTime != null)
                      Text(
                        day.checkInTime!,
                        style: TextStyle(
                          color: style.color,
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                  ],
                ),
                if (day.note != null) ...[
                  const SizedBox(height: 5),
                  Text(
                    day.note!,
                    style: const TextStyle(
                      color: AppColors.mutedText,
                      fontSize: 12,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
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
      padding: const EdgeInsets.all(24),
      decoration: _cardDecoration(),
      child: Column(
        children: [
          const Icon(
            Icons.cloud_off_rounded,
            size: 40,
            color: AppColors.mutedText,
          ),
          const SizedBox(height: 10),
          Text(message, textAlign: TextAlign.center),
          TextButton(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    );
  }
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(28),
      decoration: _cardDecoration(),
      child: const Center(
        child: Text('Chưa có dữ liệu điểm danh trong tháng này.'),
      ),
    );
  }
}

class _AttendanceStyle {
  const _AttendanceStyle(this.label, this.icon, this.color);
  final String label;
  final IconData icon;
  final Color color;
}

_AttendanceStyle _attendanceStyle(AttendanceStatus status) {
  return switch (status) {
    AttendanceStatus.present => const _AttendanceStyle(
      'Có mặt',
      Icons.check_circle_rounded,
      Color(0xFF18B982),
    ),
    AttendanceStatus.late => const _AttendanceStyle(
      'Đi muộn',
      Icons.schedule_rounded,
      Color(0xFFF5A400),
    ),
    AttendanceStatus.excusedAbsence => const _AttendanceStyle(
      'Nghỉ có phép',
      Icons.event_available_rounded,
      Color(0xFF2F80ED),
    ),
    AttendanceStatus.unexcusedAbsence => const _AttendanceStyle(
      'Nghỉ không phép',
      Icons.cancel_rounded,
      Color(0xFFFF4163),
    ),
  };
}

bool _isCurrentMonth(DateTime month) {
  final now = DateTime.now();
  return month.year == now.year && month.month == now.month;
}

String _weekday(int weekday) => switch (weekday) {
  DateTime.monday => 'T2',
  DateTime.tuesday => 'T3',
  DateTime.wednesday => 'T4',
  DateTime.thursday => 'T5',
  DateTime.friday => 'T6',
  DateTime.saturday => 'T7',
  _ => 'CN',
};

BoxDecoration _cardDecoration() {
  return BoxDecoration(
    color: Colors.white.withValues(alpha: 0.95),
    borderRadius: BorderRadius.circular(15),
    boxShadow: [
      BoxShadow(
        color: Colors.black.withValues(alpha: 0.035),
        blurRadius: 14,
        offset: const Offset(0, 6),
      ),
    ],
  );
}
