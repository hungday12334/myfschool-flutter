import 'package:flutter/material.dart';

import '../../../../core/constants/app_assets.dart';
import '../../../../core/constants/app_colors.dart';
import '../../../timetable/data/services/timetable_api_service.dart';
import '../../../timetable/presentation/screens/timetable_screen.dart';

class StudentHomeScreen extends StatelessWidget {
  const StudentHomeScreen({this.timetableService, super.key});

  final TimetableService? timetableService;

  void _openTimetable(BuildContext context) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => TimetableScreen(service: timetableService),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFAFBFC),
      body: SafeArea(
        child: Stack(
          children: [
            const _TopBackground(),
            CustomScrollView(
              slivers: [
                const SliverToBoxAdapter(child: _HomeHeader()),
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(20, 10, 20, 92),
                  sliver: SliverList.list(
                    children: [
                      _FeatureGrid(
                        onTimetableTap: () => _openTimetable(context),
                      ),
                      const SizedBox(height: 20),
                      _ScheduleSection(
                        onViewAll: () => _openTimetable(context),
                      ),
                      const SizedBox(height: 20),
                      const _HomeworkSection(),
                    ],
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
      bottomNavigationBar: const _BottomNavigation(),
    );
  }
}

class _TopBackground extends StatelessWidget {
  const _TopBackground();

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.topCenter,
      child: SizedBox(
        height: 245,
        width: double.infinity,
        child: Image.asset(
          AppAssets.loginBackground,
          fit: BoxFit.cover,
          alignment: Alignment.topCenter,
        ),
      ),
    );
  }
}

class _HomeHeader extends StatelessWidget {
  const _HomeHeader();

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(24, 20, 24, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Text.rich(
                const TextSpan(
                  children: [
                    TextSpan(
                      text: 'MyFSchool',
                      style: TextStyle(color: AppColors.deepOrange),
                    ),
                  ],
                ),
                style: const TextStyle(
                  color: AppColors.darkText,
                  fontSize: 27,
                  fontWeight: FontWeight.w800,
                ),
              ),
              const Spacer(),
              Stack(
                clipBehavior: Clip.none,
                children: [
                  const Icon(
                    Icons.notifications_none_rounded,
                    color: AppColors.darkText,
                    size: 29,
                  ),
                  Positioned(
                    top: 1,
                    right: 2,
                    child: Container(
                      width: 9,
                      height: 9,
                      decoration: const BoxDecoration(
                        color: Color(0xFFFF3B30),
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 54),
          const Row(
            children: [
              CircleAvatar(
                radius: 35,
                backgroundColor: Color(0xFFDDEEFF),
                child: Icon(
                  Icons.person_rounded,
                  color: Color(0xFF1F5F92),
                  size: 48,
                ),
              ),
              SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Ch\u00e0o bu\u1ed5i s\u00e1ng,',
                      style: TextStyle(
                        color: AppColors.mutedText,
                        fontSize: 14,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    SizedBox(height: 3),
                    Text(
                      'Nguy\u1ec5n Minh Anh',
                      style: TextStyle(
                        color: AppColors.darkText,
                        fontSize: 19,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                    SizedBox(height: 4),
                    Row(
                      children: [
                        Flexible(
                          child: Text(
                            'L\u1edbp 10A1 - Tr\u01b0\u1eddng THPT FPT',
                            overflow: TextOverflow.ellipsis,
                            style: TextStyle(
                              color: AppColors.mutedText,
                              fontSize: 13,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ),
                        SizedBox(width: 4),
                        Icon(
                          Icons.keyboard_arrow_down_rounded,
                          color: AppColors.mutedText,
                          size: 20,
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _FeatureGrid extends StatelessWidget {
  const _FeatureGrid({required this.onTimetableTap});

  final VoidCallback onTimetableTap;

  @override
  Widget build(BuildContext context) {
    final items = <_FeatureItem>[
      const _FeatureItem(
        label: 'H\u1ecdc t\u1eadp',
        icon: Icons.menu_book_rounded,
        iconColor: Color(0xFFFF7A1A),
        backgroundColor: Color(0xFFFFF4E8),
      ),
      _FeatureItem(
        label: 'Th\u1eddi kh\u00f3a bi\u1ec3u',
        icon: Icons.calendar_month_rounded,
        iconColor: Color(0xFFFF3569),
        backgroundColor: Color(0xFFFFEEF3),
        onTap: onTimetableTap,
      ),
      const _FeatureItem(
        label: 'B\u00e0i t\u1eadp',
        icon: Icons.assignment_rounded,
        iconColor: Color(0xFF7847EA),
        backgroundColor: Color(0xFFF3EEFF),
      ),
      const _FeatureItem(
        label: 'K\u1ebft qu\u1ea3',
        icon: Icons.bar_chart_rounded,
        iconColor: Color(0xFF2F80ED),
        backgroundColor: Color(0xFFEAF4FF),
      ),
      const _FeatureItem(
        label: '\u0110i\u1ec3m danh',
        icon: Icons.check_box_rounded,
        iconColor: Color(0xFF18B982),
        backgroundColor: Color(0xFFEAFBF5),
      ),
      const _FeatureItem(
        label: 'Th\u00e0nh t\u00edch',
        icon: Icons.emoji_events_rounded,
        iconColor: Color(0xFFF5A400),
        backgroundColor: Color(0xFFFFF8E8),
      ),
      const _FeatureItem(
        label: '\u0110\u01a1n t\u1eeb',
        icon: Icons.article_rounded,
        iconColor: Color(0xFF7847EA),
        backgroundColor: Color(0xFFF4EEFF),
      ),
      const _FeatureItem(
        label: 'Xem th\u00eam',
        icon: Icons.grid_view_rounded,
        iconColor: Color(0xFF9AA3AD),
        backgroundColor: Color(0xFFF0F2F5),
      ),
    ];

    return Container(
      padding: const EdgeInsets.fromLTRB(14, 18, 14, 16),
      decoration: _cardDecoration(),
      child: GridView.count(
        crossAxisCount: 4,
        mainAxisSpacing: 22,
        crossAxisSpacing: 8,
        childAspectRatio: 0.82,
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        children: items,
      ),
    );
  }
}

class _FeatureItem extends StatelessWidget {
  const _FeatureItem({
    required this.label,
    required this.icon,
    required this.iconColor,
    required this.backgroundColor,
    this.onTap,
  });

  final String label;
  final IconData icon;
  final Color iconColor;
  final Color backgroundColor;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: onTap != null,
      label: label,
      child: Material(
        color: Colors.transparent,
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(14),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Container(
                width: 52,
                height: 52,
                decoration: BoxDecoration(
                  color: backgroundColor,
                  borderRadius: BorderRadius.circular(14),
                ),
                child: Icon(icon, color: iconColor, size: 30),
              ),
              const SizedBox(height: 9),
              Text(
                label,
                maxLines: 2,
                textAlign: TextAlign.center,
                overflow: TextOverflow.ellipsis,
                style: const TextStyle(
                  color: AppColors.darkText,
                  fontSize: 12,
                  height: 1.15,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _ScheduleSection extends StatelessWidget {
  const _ScheduleSection({required this.onViewAll});

  final VoidCallback onViewAll;

  @override
  Widget build(BuildContext context) {
    return _SectionCard(
      title: 'Th\u1eddi kh\u00f3a bi\u1ec3u h\u00f4m nay',
      onViewAll: onViewAll,
      child: const Column(
        children: [
          _ScheduleRow(
            title: 'To\u00e1n',
            time: '07:30 - 08:15',
            room: 'Ph\u00f2ng 101',
            icon: Icons.calculate_rounded,
            color: Color(0xFF3F8CFF),
          ),
          _ScheduleRow(
            title: 'Ti\u1ebfng Anh',
            time: '08:30 - 09:15',
            room: 'Ph\u00f2ng 102',
            icon: Icons.translate_rounded,
            color: Color(0xFF9468FF),
          ),
          _ScheduleRow(
            title: 'V\u1eadt l\u00fd',
            time: '09:30 - 10:15',
            room: 'Ph\u00f2ng 103',
            icon: Icons.science_rounded,
            color: Color(0xFFFFB020),
            showDivider: false,
          ),
        ],
      ),
    );
  }
}

class _ScheduleRow extends StatelessWidget {
  const _ScheduleRow({
    required this.title,
    required this.time,
    required this.room,
    required this.icon,
    required this.color,
    this.showDivider = true,
  });

  final String title;
  final String time;
  final String room;
  final IconData icon;
  final Color color;
  final bool showDivider;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(vertical: 11),
          child: Row(
            children: [
              Container(
                width: 36,
                height: 36,
                decoration: BoxDecoration(
                  color: color.withValues(alpha: 0.13),
                  borderRadius: BorderRadius.circular(9),
                ),
                child: Icon(icon, color: color, size: 22),
              ),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      title,
                      style: const TextStyle(
                        color: AppColors.darkText,
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Text(
                          time,
                          style: const TextStyle(
                            color: AppColors.mutedText,
                            fontSize: 13,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        const SizedBox(width: 16),
                        const Icon(
                          Icons.location_on_outlined,
                          color: AppColors.mutedText,
                          size: 15,
                        ),
                        const SizedBox(width: 4),
                        Flexible(
                          child: Text(
                            room,
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
                ),
              ),
              const Icon(
                Icons.chevron_right_rounded,
                color: Color(0xFFB4BAC2),
                size: 24,
              ),
            ],
          ),
        ),
        if (showDivider)
          const Divider(height: 1, color: Color(0xFFECEFF3), indent: 50),
      ],
    );
  }
}

class _HomeworkSection extends StatelessWidget {
  const _HomeworkSection();

  @override
  Widget build(BuildContext context) {
    return _SectionCard(
      title: 'B\u00e0i t\u1eadp c\u1ea7n l\u00e0m',
      child: Padding(
        padding: const EdgeInsets.only(top: 12),
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              decoration: BoxDecoration(
                color: const Color(0xFFFFF0E8),
                borderRadius: BorderRadius.circular(10),
              ),
              child: const Icon(
                Icons.description_rounded,
                color: AppColors.deepOrange,
                size: 24,
              ),
            ),
            const SizedBox(width: 14),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'B\u00e0i t\u1eadp To\u00e1n',
                    style: TextStyle(
                      color: AppColors.darkText,
                      fontSize: 15,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'H\u1ea1n n\u1ed9p: 10/09/2026',
                    style: TextStyle(
                      color: AppColors.mutedText,
                      fontSize: 13,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                ],
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 7),
              decoration: BoxDecoration(
                color: const Color(0xFFFFEEF2),
                borderRadius: BorderRadius.circular(9),
              ),
              child: const Text(
                'C\u00f2n 2 ng\u00e0y',
                style: TextStyle(
                  color: Color(0xFFFF4163),
                  fontSize: 13,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SectionCard extends StatelessWidget {
  const _SectionCard({
    required this.title,
    required this.child,
    this.onViewAll,
  });

  final String title;
  final Widget child;
  final VoidCallback? onViewAll;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 14),
      decoration: _cardDecoration(),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    color: AppColors.darkText,
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
              TextButton(
                onPressed: onViewAll ?? () {},
                style: TextButton.styleFrom(
                  foregroundColor: const Color(0xFF2F80ED),
                  padding: EdgeInsets.zero,
                  minimumSize: const Size(0, 30),
                  tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                ),
                child: const Text(
                  'Xem t\u1ea5t c\u1ea3',
                  style: TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                ),
              ),
            ],
          ),
          child,
        ],
      ),
    );
  }
}

class _BottomNavigation extends StatelessWidget {
  const _BottomNavigation();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 78,
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.08),
            blurRadius: 18,
            offset: const Offset(0, -4),
          ),
        ],
      ),
      child: const SafeArea(
        top: false,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceAround,
          children: [
            _BottomNavigationItem(
              label: 'Trang ch\u1ee7',
              icon: Icons.home_rounded,
              isActive: true,
            ),
            _BottomNavigationItem(
              label: 'H\u1ecdc t\u1eadp',
              icon: Icons.menu_book_outlined,
            ),
            _BottomNavigationItem(
              label: 'Th\u00f4ng b\u00e1o',
              icon: Icons.notifications_none_rounded,
            ),
            _BottomNavigationItem(
              label: 'T\u00e0i kho\u1ea3n',
              icon: Icons.person_outline_rounded,
            ),
          ],
        ),
      ),
    );
  }
}

class _BottomNavigationItem extends StatelessWidget {
  const _BottomNavigationItem({
    required this.label,
    required this.icon,
    this.isActive = false,
  });

  final String label;
  final IconData icon;
  final bool isActive;

  @override
  Widget build(BuildContext context) {
    final color = isActive ? AppColors.primaryOrange : AppColors.mutedText;

    return Expanded(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          Icon(icon, color: color, size: 25),
          const SizedBox(height: 4),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: TextStyle(
              color: color,
              fontSize: 11,
              fontWeight: isActive ? FontWeight.w700 : FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }
}

BoxDecoration _cardDecoration() {
  return BoxDecoration(
    color: Colors.white,
    borderRadius: BorderRadius.circular(16),
    boxShadow: [
      BoxShadow(
        color: Colors.black.withValues(alpha: 0.04),
        blurRadius: 18,
        offset: const Offset(0, 8),
      ),
    ],
  );
}
