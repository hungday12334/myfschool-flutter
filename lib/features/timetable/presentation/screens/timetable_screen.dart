import 'package:flutter/material.dart';

import '../../../../core/constants/app_assets.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../../core/network/api_client.dart';
import '../../data/models/timetable_week.dart';
import '../../data/services/timetable_api_service.dart';

class TimetableScreen extends StatefulWidget {
  const TimetableScreen({this.service, this.initialDate, super.key});

  final TimetableService? service;
  final DateTime? initialDate;

  @override
  State<TimetableScreen> createState() => _TimetableScreenState();
}

class _TimetableScreenState extends State<TimetableScreen> {
  late final TimetableService _service;
  late final bool _ownsService;
  late final DateTime _baseWeekStart;
  late DateTime _currentWeekStart;
  late int _selectedDayOffset;

  final Map<String, TimetableWeek> _weekCache = <String, TimetableWeek>{};
  TimetableWeek? _week;
  String? _errorMessage;
  bool _isLoading = true;
  int _selectedWeekIndex = 0;
  int _requestSequence = 0;

  @override
  void initState() {
    super.initState();
    final initialDate = _dateOnly(widget.initialDate ?? DateTime.now());
    _baseWeekStart = _mondayOf(initialDate);
    _currentWeekStart = _baseWeekStart;
    _selectedDayOffset = initialDate.weekday - DateTime.monday;
    _ownsService = widget.service == null;
    _service = widget.service ?? TimetableApiService();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _loadWeek();
    });
  }

  @override
  void dispose() {
    final service = _service;
    if (_ownsService && service is TimetableApiService) {
      service.close();
    }
    super.dispose();
  }

  DateTime get _selectedDate =>
      _currentWeekStart.add(Duration(days: _selectedDayOffset));

  Future<void> _loadWeek({bool forceRefresh = false}) async {
    final requestedWeek = _currentWeekStart;
    final cacheKey = _dateKey(requestedWeek);
    final sequence = ++_requestSequence;
    final cachedWeek = forceRefresh ? null : _weekCache[cacheKey];
    if (cachedWeek != null) {
      if (mounted) {
        setState(() {
          _week = cachedWeek;
          _errorMessage = null;
          _isLoading = false;
        });
      }
      return;
    }

    if (mounted) {
      setState(() {
        _week = null;
        _errorMessage = null;
        _isLoading = true;
      });
    }

    try {
      final week = await _service.fetchWeek(requestedWeek);
      if (!mounted || sequence != _requestSequence) {
        return;
      }
      _weekCache[cacheKey] = week;
      setState(() {
        _week = week;
        _isLoading = false;
      });
    } on ApiException catch (error) {
      _showLoadError(sequence, error.message);
    } catch (_) {
      _showLoadError(
        sequence,
        'Không thể tải thời khóa biểu lúc này. Vui lòng thử lại.',
      );
    }
  }

  void _showLoadError(int sequence, String message) {
    if (!mounted || sequence != _requestSequence) {
      return;
    }
    setState(() {
      _errorMessage = message;
      _isLoading = false;
    });
  }

  void _selectWeek(int index) {
    if (_selectedWeekIndex == index) {
      return;
    }
    setState(() {
      _selectedWeekIndex = index;
      _currentWeekStart = _baseWeekStart.add(Duration(days: index * 7));
      _week = _weekCache[_dateKey(_currentWeekStart)];
      _errorMessage = null;
    });
    _loadWeek();
  }

  void _selectDay(int offset) {
    if (_selectedDayOffset == offset) {
      return;
    }
    setState(() {
      _selectedDayOffset = offset;
    });
  }

  @override
  Widget build(BuildContext context) {
    final mediaQuery = MediaQuery.of(context);
    final horizontalPadding = mediaQuery.size.width < 360 ? 12.0 : 18.0;

    return Scaffold(
      body: Stack(
        fit: StackFit.expand,
        children: [
          Image.asset(
            AppAssets.loginBackground,
            fit: BoxFit.cover,
            alignment: Alignment.topCenter,
          ),
          SafeArea(
            child: Column(
              children: [
                const _TimetableHeader(),
                Expanded(
                  child: RefreshIndicator(
                    color: AppColors.primaryOrange,
                    onRefresh: () => _loadWeek(forceRefresh: true),
                    child: ListView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      padding: EdgeInsets.fromLTRB(
                        horizontalPadding,
                        8,
                        horizontalPadding,
                        28,
                      ),
                      children: [
                        _WeekSwitcher(
                          selectedIndex: _selectedWeekIndex,
                          onSelected: _selectWeek,
                        ),
                        const SizedBox(height: 12),
                        _DaySelector(
                          weekStart: _currentWeekStart,
                          selectedOffset: _selectedDayOffset,
                          onSelected: _selectDay,
                        ),
                        const SizedBox(height: 22),
                        Text(
                          _fullVietnameseDate(_selectedDate),
                          style: const TextStyle(
                            color: AppColors.darkText,
                            fontSize: 19,
                            height: 1.25,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(height: 14),
                        _buildScheduleContent(),
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

  Widget _buildScheduleContent() {
    if (_isLoading) {
      return const Padding(
        padding: EdgeInsets.symmetric(vertical: 72),
        child: Center(
          child: CircularProgressIndicator(color: AppColors.primaryOrange),
        ),
      );
    }

    final errorMessage = _errorMessage;
    if (errorMessage != null) {
      return _ErrorState(
        message: errorMessage,
        onRetry: () => _loadWeek(forceRefresh: true),
      );
    }

    final day = _week?.dayFor(_selectedDate);
    if (day == null || day.lessons.isEmpty) {
      return const _EmptyState();
    }

    final entries = _buildEntries(day.lessons);
    return Column(
      children: [
        for (var index = 0; index < entries.length; index++) ...[
          _ScheduleCard(entry: entries[index]),
          if (index != entries.length - 1) const SizedBox(height: 10),
        ],
      ],
    );
  }
}

class _TimetableHeader extends StatelessWidget {
  const _TimetableHeader();

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 64,
      child: Stack(
        alignment: Alignment.center,
        children: [
          const Text(
            'Thời khóa biểu',
            style: TextStyle(
              color: AppColors.darkText,
              fontSize: 24,
              fontWeight: FontWeight.w800,
            ),
          ),
          Positioned(
            left: 8,
            child: IconButton(
              tooltip: 'Quay lại',
              onPressed: () => Navigator.of(context).maybePop(),
              icon: const Icon(Icons.arrow_back_rounded, size: 30),
              color: AppColors.darkText,
            ),
          ),
        ],
      ),
    );
  }
}

class _WeekSwitcher extends StatelessWidget {
  const _WeekSwitcher({required this.selectedIndex, required this.onSelected});

  final int selectedIndex;
  final ValueChanged<int> onSelected;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 56,
      padding: const EdgeInsets.all(4),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.78),
        borderRadius: BorderRadius.circular(17),
        border: Border.all(color: Colors.white.withValues(alpha: 0.9)),
      ),
      child: Row(
        children: [
          _WeekOption(
            label: 'Tuần này',
            isSelected: selectedIndex == 0,
            onTap: () => onSelected(0),
          ),
          _WeekOption(
            label: 'Tuần sau',
            isSelected: selectedIndex == 1,
            onTap: () => onSelected(1),
          ),
        ],
      ),
    );
  }
}

class _WeekOption extends StatelessWidget {
  const _WeekOption({
    required this.label,
    required this.isSelected,
    required this.onTap,
  });

  final String label;
  final bool isSelected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: Material(
        color: isSelected ? Colors.white : Colors.transparent,
        borderRadius: BorderRadius.circular(14),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(14),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 180),
            alignment: Alignment.center,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(14),
              border: isSelected
                  ? Border.all(color: const Color(0xFFFFA478))
                  : null,
              boxShadow: isSelected
                  ? [
                      BoxShadow(
                        color: AppColors.primaryOrange.withValues(alpha: 0.08),
                        blurRadius: 10,
                      ),
                    ]
                  : null,
            ),
            child: Text(
              label,
              style: TextStyle(
                color: isSelected ? AppColors.deepOrange : AppColors.mutedText,
                fontSize: 16,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _DaySelector extends StatelessWidget {
  const _DaySelector({
    required this.weekStart,
    required this.selectedOffset,
    required this.onSelected,
  });

  final DateTime weekStart;
  final int selectedOffset;
  final ValueChanged<int> onSelected;

  static const _labels = <String>['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 12),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.86),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Row(
        children: List<Widget>.generate(7, (index) {
          final date = weekStart.add(Duration(days: index));
          final isSelected = selectedOffset == index;
          return Expanded(
            child: Semantics(
              button: true,
              selected: isSelected,
              label: '${_labels[index]} ${_shortDate(date)}',
              child: InkWell(
                onTap: () => onSelected(index),
                borderRadius: BorderRadius.circular(13),
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 2),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        _labels[index],
                        style: TextStyle(
                          color: isSelected
                              ? AppColors.deepOrange
                              : AppColors.mutedText,
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(height: 7),
                      AnimatedContainer(
                        duration: const Duration(milliseconds: 180),
                        padding: const EdgeInsets.symmetric(
                          horizontal: 6,
                          vertical: 7,
                        ),
                        decoration: BoxDecoration(
                          color: isSelected
                              ? AppColors.primaryOrange
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(14),
                        ),
                        child: Text(
                          _shortDate(date),
                          style: TextStyle(
                            color: isSelected
                                ? Colors.white
                                : AppColors.mutedText,
                            fontSize: 12,
                            fontWeight: isSelected
                                ? FontWeight.w800
                                : FontWeight.w500,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}

class _ScheduleCard extends StatelessWidget {
  const _ScheduleCard({required this.entry});

  final _ScheduleEntry entry;

  @override
  Widget build(BuildContext context) {
    final lesson = entry.lesson;
    final isBreak = lesson == null;
    final style = isBreak
        ? const _SubjectStyle(
            icon: Icons.restaurant_rounded,
            color: Color(0xFFFF762B),
            background: Color(0xFFFFE9D9),
          )
        : _styleFor(lesson);

    return Container(
      padding: const EdgeInsets.fromLTRB(13, 12, 12, 12),
      decoration: BoxDecoration(
        color: isBreak
            ? const Color(0xFFFFF7EF).withValues(alpha: 0.94)
            : Colors.white.withValues(alpha: 0.93),
        borderRadius: BorderRadius.circular(17),
        border: isBreak ? Border.all(color: const Color(0xFFFFD8BC)) : null,
        boxShadow: isBreak
            ? null
            : [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.035),
                  blurRadius: 12,
                  offset: const Offset(0, 5),
                ),
              ],
      ),
      child: Row(
        children: [
          SizedBox(
            width: 49,
            child: Column(
              children: [
                Text(
                  entry.startTime,
                  style: const TextStyle(
                    color: AppColors.darkText,
                    fontSize: 14,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const Text(
                  '|',
                  style: TextStyle(
                    color: Color(0xFFADB4BC),
                    fontSize: 13,
                    height: 1.1,
                  ),
                ),
                Text(
                  entry.endTime,
                  style: const TextStyle(
                    color: AppColors.mutedText,
                    fontSize: 14,
                    fontWeight: FontWeight.w500,
                  ),
                ),
              ],
            ),
          ),
          Container(width: 1, height: 66, color: const Color(0xFFE7EAEE)),
          const SizedBox(width: 10),
          Container(
            width: 48,
            height: 58,
            decoration: BoxDecoration(
              color: style.background,
              borderRadius: BorderRadius.circular(14),
            ),
            child: Icon(style.icon, color: style.color, size: 29),
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        isBreak ? 'Nghỉ trưa' : lesson.subjectName,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: AppColors.darkText,
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                    if (!isBreak) ...[
                      const SizedBox(width: 6),
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 5,
                        ),
                        decoration: BoxDecoration(
                          color: const Color(0xFFFFEFE7),
                          borderRadius: BorderRadius.circular(11),
                        ),
                        child: Text(
                          'Tiết ${lesson.periodNumber}',
                          style: const TextStyle(
                            color: AppColors.deepOrange,
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
                const SizedBox(height: 5),
                Text(
                  isBreak ? 'Thời gian nghỉ' : lesson.teacherName,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(
                    color: AppColors.mutedText,
                    fontSize: 13,
                    fontWeight: FontWeight.w500,
                  ),
                ),
                if (!isBreak) ...[
                  const SizedBox(height: 4),
                  Row(
                    children: [
                      const Icon(
                        Icons.location_on_outlined,
                        color: AppColors.mutedText,
                        size: 15,
                      ),
                      const SizedBox(width: 3),
                      Expanded(
                        child: Text(
                          lesson.room,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(
                            color: AppColors.mutedText,
                            fontSize: 13,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                    ],
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

class _EmptyState extends StatelessWidget {
  const _EmptyState();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 50),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.88),
        borderRadius: BorderRadius.circular(18),
      ),
      child: const Column(
        children: [
          Icon(
            Icons.event_available_rounded,
            color: AppColors.primaryOrange,
            size: 48,
          ),
          SizedBox(height: 14),
          Text(
            'Không có lịch học trong ngày này.',
            textAlign: TextAlign.center,
            style: TextStyle(
              color: AppColors.darkText,
              fontSize: 16,
              fontWeight: FontWeight.w700,
            ),
          ),
        ],
      ),
    );
  }
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});

  final String message;
  final VoidCallback onRetry;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 38),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.9),
        borderRadius: BorderRadius.circular(18),
      ),
      child: Column(
        children: [
          const Icon(
            Icons.cloud_off_rounded,
            color: AppColors.deepOrange,
            size: 46,
          ),
          const SizedBox(height: 12),
          Text(
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(
              color: AppColors.darkText,
              fontSize: 15,
              height: 1.35,
              fontWeight: FontWeight.w600,
            ),
          ),
          const SizedBox(height: 18),
          FilledButton.icon(
            onPressed: onRetry,
            icon: const Icon(Icons.refresh_rounded),
            label: const Text('Thử lại'),
          ),
        ],
      ),
    );
  }
}

class _ScheduleEntry {
  _ScheduleEntry.lesson(TimetableLesson value)
    : lesson = value,
      startTime = value.startTime,
      endTime = value.endTime;

  const _ScheduleEntry.breakTime({
    required this.startTime,
    required this.endTime,
  }) : lesson = null;

  final TimetableLesson? lesson;
  final String startTime;
  final String endTime;
}

class _SubjectStyle {
  const _SubjectStyle({
    required this.icon,
    required this.color,
    required this.background,
  });

  final IconData icon;
  final Color color;
  final Color background;
}

List<_ScheduleEntry> _buildEntries(List<TimetableLesson> lessons) {
  final entries = <_ScheduleEntry>[];
  for (var index = 0; index < lessons.length; index++) {
    final lesson = lessons[index];
    entries.add(_ScheduleEntry.lesson(lesson));
    if (index == lessons.length - 1) {
      continue;
    }

    final nextLesson = lessons[index + 1];
    final gapMinutes = nextLesson.startMinutes - lesson.endMinutes;
    final spansMidday =
        lesson.endMinutes <= 13 * 60 && nextLesson.startMinutes >= 12 * 60;
    if (gapMinutes >= 60 && spansMidday) {
      entries.add(
        _ScheduleEntry.breakTime(
          startTime: lesson.endTime,
          endTime: nextLesson.startTime,
        ),
      );
    }
  }
  return entries;
}

_SubjectStyle _styleFor(TimetableLesson lesson) {
  final key = '${lesson.subjectCode} ${lesson.subjectName}'.toUpperCase();
  if (key.contains('TOAN') || key.contains('MATH')) {
    return const _SubjectStyle(
      icon: Icons.calculate_rounded,
      color: Color(0xFF2F80ED),
      background: Color(0xFFE6F1FF),
    );
  }
  if (key.contains('ANH') || key.contains('ENG')) {
    return const _SubjectStyle(
      icon: Icons.translate_rounded,
      color: Color(0xFF7B4CE7),
      background: Color(0xFFF0E9FF),
    );
  }
  if (key.contains('LY') || key.contains('PHYS')) {
    return const _SubjectStyle(
      icon: Icons.hub_rounded,
      color: Color(0xFFF3A400),
      background: Color(0xFFFFF4D8),
    );
  }
  if (key.contains('VAN') || key.contains('LIT')) {
    return const _SubjectStyle(
      icon: Icons.menu_book_rounded,
      color: Color(0xFFEC4772),
      background: Color(0xFFFFE6ED),
    );
  }
  if (key.contains('HOA') || key.contains('CHEM')) {
    return const _SubjectStyle(
      icon: Icons.science_rounded,
      color: Color(0xFF19B778),
      background: Color(0xFFE0F8ED),
    );
  }
  if (key.contains('TIN') || key.contains(' IT')) {
    return const _SubjectStyle(
      icon: Icons.computer_rounded,
      color: Color(0xFF7449EB),
      background: Color(0xFFEFE9FF),
    );
  }
  return const _SubjectStyle(
    icon: Icons.school_rounded,
    color: Color(0xFF2F80ED),
    background: Color(0xFFEAF4FF),
  );
}

DateTime _dateOnly(DateTime value) =>
    DateTime(value.year, value.month, value.day);

DateTime _mondayOf(DateTime value) {
  final date = _dateOnly(value);
  return date.subtract(Duration(days: date.weekday - DateTime.monday));
}

String _dateKey(DateTime value) {
  return '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}

String _shortDate(DateTime value) {
  return '${value.day.toString().padLeft(2, '0')}/'
      '${value.month.toString().padLeft(2, '0')}';
}

String _fullVietnameseDate(DateTime value) {
  final weekday = switch (value.weekday) {
    DateTime.monday => 'Thứ 2',
    DateTime.tuesday => 'Thứ 3',
    DateTime.wednesday => 'Thứ 4',
    DateTime.thursday => 'Thứ 5',
    DateTime.friday => 'Thứ 6',
    DateTime.saturday => 'Thứ 7',
    _ => 'Chủ nhật',
  };
  return '$weekday, ngày ${value.day.toString().padLeft(2, '0')} '
      'tháng ${value.month.toString().padLeft(2, '0')} năm ${value.year}';
}
