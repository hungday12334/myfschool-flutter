import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/network/api_client.dart';
import '../../data/models/tuition_report.dart';
import '../../data/services/tuition_api_service.dart';

enum TuitionTab { due, paid }

class TuitionScreen extends StatefulWidget {
  const TuitionScreen({this.service, super.key});

  final TuitionService? service;

  @override
  State<TuitionScreen> createState() => _TuitionScreenState();
}

class _TuitionScreenState extends State<TuitionScreen> {
  late final TuitionService _service;
  late final bool _ownsService;
  TuitionReport? _report;
  String? _error;
  bool _isLoading = true;
  TuitionTab _tab = TuitionTab.due;

  @override
  void initState() {
    super.initState();
    _ownsService = widget.service == null;
    _service = widget.service ?? TuitionApiService();
    _load();
  }

  @override
  void dispose() {
    final service = _service;
    if (_ownsService && service is TuitionApiService) service.close();
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _isLoading = true;
      _error = null;
    });
    try {
      final report = await _service.fetchReport();
      if (!mounted) return;
      setState(() {
        _report = report;
        _isLoading = false;
      });
    } on ApiException catch (error) {
      _setError(error.message);
    } catch (_) {
      _setError('Không thể tải thông tin học phí. Vui lòng thử lại.');
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
      backgroundColor: const Color(0xFFFFFAF5),
      body: Stack(
        children: [
          const _TuitionBackground(),
          SafeArea(
            child: Column(
              children: [
                _TitleBar(onBack: () => Navigator.maybePop(context)),
                Expanded(
                  child: RefreshIndicator(
                    color: AppColors.primaryOrange,
                    onRefresh: _load,
                    child: ListView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: const EdgeInsets.fromLTRB(18, 6, 18, 28),
                      children: [
                        if (_report != null)
                          _StudentCard(student: _report!.student),
                        const SizedBox(height: 14),
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
        height: 420,
        child: Center(
          child: CircularProgressIndicator(color: AppColors.primaryOrange),
        ),
      );
    }
    if (_error != null) return _ErrorCard(message: _error!, onRetry: _load);
    final report = _report!;
    final items = _tab == TuitionTab.due ? report.dueItems : report.paidItems;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _BalanceCard(report: report),
        const SizedBox(height: 13),
        Row(
          children: [
            Expanded(
              child: _MoneySummary(
                label: 'Đã nộp',
                value: report.totalPaid,
                color: const Color(0xFF18B982),
                icon: Icons.check_circle_outline_rounded,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: _MoneySummary(
                label: 'Tổng học phí',
                value: report.totalAmount,
                color: const Color(0xFF2F80ED),
                icon: Icons.account_balance_wallet_outlined,
              ),
            ),
          ],
        ),
        const SizedBox(height: 20),
        _TabSelector(
          selected: _tab,
          dueCount: report.dueItems.length,
          paidCount: report.paidItems.length,
          onSelected: (value) => setState(() => _tab = value),
        ),
        const SizedBox(height: 15),
        Text(
          _tab == TuitionTab.due ? 'Các khoản cần nộp' : 'Các khoản đã nộp',
          style: const TextStyle(
            color: AppColors.darkText,
            fontSize: 19,
            fontWeight: FontWeight.w800,
          ),
        ),
        const SizedBox(height: 10),
        if (items.isEmpty)
          _EmptyCard(tab: _tab)
        else
          for (var index = 0; index < items.length; index++) ...[
            _TuitionItemCard(item: items[index]),
            if (index != items.length - 1) const SizedBox(height: 9),
          ],
      ],
    );
  }
}

class _TuitionBackground extends StatelessWidget {
  const _TuitionBackground();

  @override
  Widget build(BuildContext context) {
    return const Positioned.fill(
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [Color(0xFFFFF0E2), Color(0xFFFFFCF9), Color(0xFFF3F8FF)],
          ),
        ),
      ),
    );
  }
}

class _TitleBar extends StatelessWidget {
  const _TitleBar({required this.onBack});
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
          const Expanded(
            child: Text(
              'Học phí',
              textAlign: TextAlign.center,
              style: TextStyle(
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
  final TuitionStudent student;

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

class _BalanceCard extends StatelessWidget {
  const _BalanceCard({required this.report});
  final TuitionReport report;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        gradient: const LinearGradient(
          colors: [Color(0xFFFF7A1A), Color(0xFFFF9F43)],
        ),
        borderRadius: BorderRadius.circular(17),
        boxShadow: [
          BoxShadow(
            color: AppColors.primaryOrange.withValues(alpha: 0.25),
            blurRadius: 18,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(
                Icons.receipt_long_rounded,
                color: Colors.white,
                size: 25,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Năm học ${report.academicYear}',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 17),
          Text(
            'Còn cần nộp',
            style: TextStyle(
              color: Colors.white.withValues(alpha: 0.88),
              fontSize: 13,
            ),
          ),
          const SizedBox(height: 3),
          Text(
            _formatMoney(report.totalDue),
            key: const Key('tuition-total-due'),
            style: const TextStyle(
              color: Colors.white,
              fontSize: 29,
              fontWeight: FontWeight.w800,
            ),
          ),
        ],
      ),
    );
  }
}

class _MoneySummary extends StatelessWidget {
  const _MoneySummary({
    required this.label,
    required this.value,
    required this.color,
    required this.icon,
  });

  final String label;
  final int value;
  final Color color;
  final IconData icon;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(12),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icon, color: color, size: 19),
              const SizedBox(width: 6),
              Expanded(
                child: Text(
                  label,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: AppColors.mutedText,
                    fontSize: 12,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerLeft,
            child: Text(
              _formatMoney(value),
              style: TextStyle(
                color: color,
                fontSize: 16,
                fontWeight: FontWeight.w800,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _TabSelector extends StatelessWidget {
  const _TabSelector({
    required this.selected,
    required this.dueCount,
    required this.paidCount,
    required this.onSelected,
  });

  final TuitionTab selected;
  final int dueCount;
  final int paidCount;
  final ValueChanged<TuitionTab> onSelected;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 47,
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: const Color(0xFFF0F2F5),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          _tab(
            key: const Key('tuition-tab-due'),
            tab: TuitionTab.due,
            label: 'Cần nộp ($dueCount)',
          ),
          _tab(
            key: const Key('tuition-tab-paid'),
            tab: TuitionTab.paid,
            label: 'Đã nộp ($paidCount)',
          ),
        ],
      ),
    );
  }

  Widget _tab({
    required Key key,
    required TuitionTab tab,
    required String label,
  }) {
    final isSelected = selected == tab;
    return Expanded(
      child: InkWell(
        key: key,
        onTap: () => onSelected(tab),
        borderRadius: BorderRadius.circular(9),
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 160),
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: isSelected ? Colors.white : Colors.transparent,
            borderRadius: BorderRadius.circular(9),
            border: isSelected
                ? Border.all(color: AppColors.primaryOrange)
                : null,
          ),
          child: Text(
            label,
            style: TextStyle(
              color: isSelected ? AppColors.primaryOrange : AppColors.mutedText,
              fontSize: 13,
              fontWeight: isSelected ? FontWeight.w800 : FontWeight.w600,
            ),
          ),
        ),
      ),
    );
  }
}

class _TuitionItemCard extends StatelessWidget {
  const _TuitionItemCard({required this.item});
  final TuitionItem item;

  @override
  Widget build(BuildContext context) {
    final paid = item.status == TuitionItemStatus.paid;
    final partiallyPaid = item.status == TuitionItemStatus.partiallyPaid;
    final color = paid
        ? const Color(0xFF18B982)
        : partiallyPaid
        ? const Color(0xFFF5A400)
        : const Color(0xFFFF4163);
    final statusLabel = paid
        ? 'Đã nộp'
        : partiallyPaid
        ? 'Đã nộp một phần'
        : _isOverdue(item.dueDate)
        ? 'Quá hạn'
        : 'Chưa nộp';
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                width: 43,
                height: 43,
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.11),
                  borderRadius: BorderRadius.circular(11),
                ),
                child: Icon(
                  paid ? Icons.check_rounded : Icons.receipt_long_rounded,
                  color: color,
                  size: 24,
                ),
              ),
              const SizedBox(width: 11),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.title,
                      style: const TextStyle(
                        color: AppColors.darkText,
                        fontSize: 15,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      paid && item.paidDate != null
                          ? 'Ngày nộp: ${_formatDate(item.paidDate!)}'
                          : 'Hạn nộp: ${_formatDate(item.dueDate)}',
                      style: const TextStyle(
                        color: AppColors.mutedText,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 5),
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.11),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  statusLabel,
                  style: TextStyle(
                    color: color,
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 13),
          Row(
            children: [
              Expanded(
                child: Text(
                  paid ? 'Số tiền đã nộp' : 'Số tiền còn lại',
                  style: const TextStyle(
                    color: AppColors.mutedText,
                    fontSize: 12,
                  ),
                ),
              ),
              Text(
                _formatMoney(paid ? item.paidAmount : item.remainingAmount),
                style: TextStyle(
                  color: color,
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ],
          ),
          if (partiallyPaid) ...[
            const SizedBox(height: 10),
            ClipRRect(
              borderRadius: BorderRadius.circular(4),
              child: LinearProgressIndicator(
                minHeight: 7,
                value: item.amount == 0 ? 0 : item.paidAmount / item.amount,
                backgroundColor: const Color(0xFFF0F2F5),
                valueColor: AlwaysStoppedAnimation<Color>(color),
              ),
            ),
            const SizedBox(height: 5),
            Text(
              'Đã nộp ${_formatMoney(item.paidAmount)} / ${_formatMoney(item.amount)}',
              style: const TextStyle(color: AppColors.mutedText, fontSize: 11),
            ),
          ],
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
  const _EmptyCard({required this.tab});
  final TuitionTab tab;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(28),
      decoration: _cardDecoration(),
      child: Text(
        tab == TuitionTab.due
            ? 'Không còn khoản học phí nào cần nộp.'
            : 'Chưa có khoản học phí nào đã nộp.',
        textAlign: TextAlign.center,
      ),
    );
  }
}

String _formatMoney(int value) {
  final digits = value.toString();
  final buffer = StringBuffer();
  for (var index = 0; index < digits.length; index++) {
    if (index > 0 && (digits.length - index) % 3 == 0) buffer.write('.');
    buffer.write(digits[index]);
  }
  return '${buffer.toString()} đ';
}

String _formatDate(DateTime value) {
  return '${value.day.toString().padLeft(2, '0')}/'
      '${value.month.toString().padLeft(2, '0')}/${value.year}';
}

bool _isOverdue(DateTime dueDate) {
  final now = DateTime.now();
  return dueDate.isBefore(DateTime(now.year, now.month, now.day));
}

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
