import 'dart:async';

import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/storage/token_storage.dart';
import '../../../attendance/data/services/attendance_api_service.dart';
import '../../../attendance/presentation/screens/attendance_screen.dart';
import '../../../auth/data/models/login_response.dart';
import '../../../auth/presentation/screens/login_screen.dart';
import '../../../grades/data/services/grade_api_service.dart';
import '../../../grades/presentation/screens/grade_screen.dart';
import '../../../parent/data/models/parent_models.dart';
import '../../../parent/data/services/parent_api_service.dart';
import '../../../parent/data/services/parent_notification_service.dart';
import '../../../parent/presentation/parent_context_controller.dart';
import '../../../timetable/data/services/timetable_api_service.dart';
import '../../../timetable/presentation/screens/timetable_screen.dart';
import '../../../tuition/data/services/tuition_api_service.dart';
import '../../../tuition/presentation/screens/tuition_screen.dart';

class ParentHomeScreen extends StatefulWidget {
  const ParentHomeScreen({
    this.user,
    this.tokenStorage,
    this.parentApi,
    this.pushTokenSource = const UnavailableParentPushTokenSource(),
    this.timetableService,
    this.gradeService,
    this.attendanceService,
    this.tuitionService,
    super.key,
  });

  final AuthenticatedUser? user;
  final TokenStorage? tokenStorage;
  final ParentApi? parentApi;
  final ParentPushTokenSource pushTokenSource;
  final TimetableService? timetableService;
  final GradeService? gradeService;
  final AttendanceService? attendanceService;
  final TuitionService? tuitionService;

  @override
  State<ParentHomeScreen> createState() => _ParentHomeScreenState();
}

class _ParentHomeScreenState extends State<ParentHomeScreen> {
  late final ParentApi _api =
      widget.parentApi ?? ParentApiService(tokenStorage: widget.tokenStorage);
  late final ParentContextController _context = ParentContextController(_api);
  late final bool _legacyOnly =
      widget.parentApi == null &&
      (widget.timetableService != null ||
          widget.gradeService != null ||
          widget.attendanceService != null ||
          widget.tuitionService != null);
  int _tab = 0;
  int _dashboardVersion = 0;
  final Set<int> _visitedTabs = <int>{0};
  late final ParentNotificationService _notifications =
      ParentNotificationService(api: _api, source: widget.pushTokenSource);
  final List<StreamSubscription<dynamic>> _subscriptions = [];
  bool _leaving = false;

  @override
  void initState() {
    super.initState();
    if (!_legacyOnly) {
      if (_api is ParentSessionEvents) {
        _subscriptions.add(
          (_api as ParentSessionEvents).sessionExpired.listen((_) {
            if (mounted) unawaited(_logout(expired: true));
          }),
        );
      }
      _subscriptions.add(_notifications.taps.listen(_openNotification));
      _subscriptions.add(
        _notifications.foregroundMessages.listen((message) {
          if (!mounted || _leaving) return;
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                message.title ?? message.body ?? 'Bạn có thông báo mới.',
              ),
              action: message.target == null
                  ? null
                  : SnackBarAction(
                      label: 'Xem',
                      onPressed: () => _openNotification(message.target!),
                    ),
            ),
          );
        }),
      );
      _subscriptions.add(
        _notifications.failures.listen((failure) {
          if (!mounted || _leaving) return;
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(
              content: Text(
                'Chưa thể cập nhật thông báo trên thiết bị. Bạn vẫn có thể xem mục Thông báo trong ứng dụng.',
              ),
            ),
          );
        }),
      );
      _context.load();
      unawaited(_notifications.start());
    }
  }

  @override
  void dispose() {
    for (final subscription in _subscriptions) {
      unawaited(subscription.cancel());
    }
    unawaited(_notifications.dispose());
    _context.dispose();
    if (widget.parentApi == null && _api is ParentApiService) {
      _api.close();
    }
    super.dispose();
  }

  void _selectTab(int index) => setState(() {
    if (index == 0 && _tab != 0) _dashboardVersion++;
    _tab = index;
    _visitedTabs.add(index);
  });

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _context,
      builder: (context, _) {
        return Scaffold(
          backgroundColor: const Color(0xFFF7F8FA),
          appBar: AppBar(
            title: Text(switch (_tab) {
              0 => 'Trang chủ',
              1 => 'Học tập',
              2 => 'Thông báo',
              3 => 'Đơn nghỉ học',
              _ => 'Con của tôi',
            }),
            backgroundColor: const Color(0xFFF7F8FA),
            actions: [
              PopupMenuButton<String>(
                tooltip: 'Tài khoản',
                onSelected: (value) async {
                  if (value == 'logout') await _logout();
                },
                itemBuilder: (_) => const [
                  PopupMenuItem(value: 'logout', child: Text('Đăng xuất')),
                ],
                child: const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 16),
                  child: Icon(Icons.account_circle_outlined),
                ),
              ),
            ],
          ),
          body: IndexedStack(
            index: _tab,
            children:
                [
                      _DashboardTab(
                        api: _api,
                        controller: _context,
                        user: widget.user,
                        legacy: _legacyOnly,
                        refreshVersion: _dashboardVersion,
                        onNavigate: _selectTab,
                        onTimetable: _openTimetable,
                        onGrades: _openGrades,
                        onAttendance: _openAttendance,
                        onTuition: _openTuition,
                      ),
                      _LearningTab(
                        api: _api,
                        controller: _context,
                        onOpen: _openResource,
                        onTimetable: _openTimetable,
                        onGrades: _openGrades,
                        onAttendance: _openAttendance,
                      ),
                      _AnnouncementsTab(api: _api),
                      _LeaveRequestsTab(api: _api, controller: _context),
                      _ChildrenTab(
                        controller: _context,
                        onProfile: (child) =>
                            _openResource(_Resource.profile, child),
                      ),
                    ]
                    .asMap()
                    .entries
                    .map(
                      (entry) => _visitedTabs.contains(entry.key)
                          ? entry.value
                          : const SizedBox.shrink(),
                    )
                    .toList(),
          ),
          bottomNavigationBar: NavigationBar(
            selectedIndex: _tab,
            onDestinationSelected: _selectTab,
            destinations: const [
              NavigationDestination(
                icon: Icon(Icons.home_outlined),
                selectedIcon: Icon(Icons.home),
                label: 'Trang chủ',
              ),
              NavigationDestination(
                icon: Icon(Icons.menu_book_outlined),
                selectedIcon: Icon(Icons.menu_book),
                label: 'Học tập',
              ),
              NavigationDestination(
                icon: Icon(Icons.notifications_outlined),
                selectedIcon: Icon(Icons.notifications),
                label: 'Thông báo',
              ),
              NavigationDestination(
                icon: Icon(Icons.event_note_outlined),
                selectedIcon: Icon(Icons.event_note),
                label: 'Nghỉ học',
              ),
              NavigationDestination(
                icon: Icon(Icons.people_outline),
                selectedIcon: Icon(Icons.people),
                label: 'Các con',
              ),
            ],
          ),
        );
      },
    );
  }

  void _openNotification(ParentNotificationTarget target) {
    if (!mounted || _leaving) return;
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => switch (target.kind) {
          ParentNotificationKind.announcement => _AnnouncementDetailScreen(
            api: _api,
            announcementId: target.id,
          ),
          ParentNotificationKind.leaveRequest => _LeaveDetailScreen(
            api: _api,
            requestId: target.id,
          ),
        },
      ),
    );
  }

  Future<void> _logout({bool expired = false}) async {
    if (_leaving) return;
    _leaving = true;
    await _notifications.stop();
    _context.reset();
    final storage = widget.tokenStorage ?? TokenStorage();
    await storage.clearAccessToken();
    if (!mounted) return;
    Navigator.of(context).pushAndRemoveUntil(
      MaterialPageRoute<void>(
        builder: (_) => LoginScreen(
          tokenStorage: storage,
          pushTokenSource: widget.pushTokenSource,
        ),
      ),
      (_) => false,
    );
    if (expired && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'),
        ),
      );
    }
  }

  void _openTimetable() {
    if (!_legacyOnly) {
      _openResource(_Resource.timetable, _context.selectedChild);
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => TimetableScreen(service: widget.timetableService),
      ),
    );
  }

  void _openGrades() {
    if (!_legacyOnly) {
      _openResource(_Resource.results, _context.selectedChild);
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => GradeScreen(service: widget.gradeService),
      ),
    );
  }

  void _openAttendance() {
    if (!_legacyOnly) {
      _openResource(_Resource.attendance, _context.selectedChild);
      return;
    }
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => AttendanceScreen(service: widget.attendanceService),
      ),
    );
  }

  void _openTuition() => Navigator.of(context).push(
    MaterialPageRoute<void>(
      builder: (_) => TuitionScreen(service: widget.tuitionService),
    ),
  );

  void _openResource(_Resource resource, ParentChild? child) {
    if (child == null) return;
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => AnimatedBuilder(
          animation: _context,
          builder: (_, _) {
            final selected = _context.selectedChild;
            if (selected == null) {
              return Scaffold(
                appBar: AppBar(),
                body: const _InlineEmpty('Chưa có học sinh được liên kết.'),
              );
            }
            return _ParentResourceScreen(
              key: ValueKey('${selected.id}-$resource'),
              api: _api,
              child: selected,
              resource: resource,
              controller: _context,
            );
          },
        ),
      ),
    );
  }
}

enum _Resource {
  profile,
  classInfo,
  timetable,
  results,
  evaluations,
  attendance,
  attendanceSummary,
}

class _DashboardTab extends StatefulWidget {
  const _DashboardTab({
    required this.api,
    required this.controller,
    required this.user,
    required this.legacy,
    required this.refreshVersion,
    required this.onNavigate,
    required this.onTimetable,
    required this.onGrades,
    required this.onAttendance,
    required this.onTuition,
  });
  final ParentApi api;
  final ParentContextController controller;
  final AuthenticatedUser? user;
  final bool legacy;
  final int refreshVersion;
  final ValueChanged<int> onNavigate;
  final VoidCallback onTimetable;
  final VoidCallback onGrades;
  final VoidCallback onAttendance;
  final VoidCallback onTuition;

  @override
  State<_DashboardTab> createState() => _DashboardTabState();
}

class _DashboardTabState extends State<_DashboardTab> {
  Future<ParentDashboard>? _future;
  int? _requestedStudent;
  @override
  void initState() {
    super.initState();
    if (widget.legacy) _load();
  }

  @override
  void didUpdateWidget(covariant _DashboardTab oldWidget) {
    super.didUpdateWidget(oldWidget);
    final id = widget.controller.selectedChild?.id;
    if (!widget.legacy &&
        (id != _requestedStudent ||
            oldWidget.refreshVersion != widget.refreshVersion)) {
      _load();
    }
  }

  void _load() {
    _requestedStudent = widget.controller.selectedChild?.id;
    if (!widget.legacy) {
      _future = _requestedStudent == null
          ? null
          : widget.api.dashboard(_requestedStudent);
    }
  }

  Future<void> _refresh() async {
    setState(_load);
    try {
      await _future;
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    final controller = widget.controller;
    if (controller.loading && controller.children.isEmpty && !widget.legacy) {
      return const Center(child: CircularProgressIndicator());
    }
    if (controller.error != null &&
        controller.children.isEmpty &&
        !widget.legacy) {
      return _ErrorState(message: controller.error!, onRetry: controller.load);
    }
    if (!widget.legacy && controller.children.isEmpty) {
      return RefreshIndicator(
        onRefresh: controller.load,
        child: ListView(
          children: const [
            SizedBox(height: 120),
            _EmptyState(
              icon: Icons.people_outline,
              title: 'Chưa có học sinh được liên kết',
              message: 'Nhà trường chưa liên kết học sinh với tài khoản phụ huynh này.',
            ),
          ],
        ),
      );
    }
    return RefreshIndicator(
      onRefresh: _refresh,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(18, 8, 18, 26),
        children: [
          Text(
            'Xin chào, ${widget.user?.fullName ?? 'Quý phụ huynh'}',
            style: const TextStyle(
              fontSize: 21,
              fontWeight: FontWeight.w800,
              color: AppColors.darkText,
            ),
          ),
          const SizedBox(height: 12),
          if (!widget.legacy) _ChildSelector(controller: controller),
          const SizedBox(height: 12),
          if (widget.legacy) ...[
            _QuickTile(
              title: 'Thời khóa biểu',
              icon: Icons.calendar_month,
              onTap: widget.onTimetable,
            ),
            _QuickTile(
              title: 'Kết quả',
              icon: Icons.bar_chart,
              onTap: widget.onGrades,
            ),
            _QuickTile(
              title: 'Điểm danh',
              icon: Icons.fact_check,
              onTap: widget.onAttendance,
            ),
            _QuickTile(
              title: 'Học phí',
              icon: Icons.account_balance_wallet,
              onTap: widget.onTuition,
            ),
          ] else if (_future != null)
            FutureBuilder<ParentDashboard>(
              key: ValueKey(_requestedStudent),
              future: _future,
              builder: (context, snapshot) {
                if (snapshot.connectionState == ConnectionState.waiting) {
                  return const _DashboardLoading();
                }
                if (snapshot.hasError) {
                  return _ErrorState(
                    message: _friendlyError(snapshot.error!),
                    onRetry: _refresh,
                  );
                }
                final dashboard = snapshot.data!;
                if (dashboard.selectedStudent == null) {
                  return const _EmptyState(
                    icon: Icons.child_care,
                    title: 'Chưa chọn học sinh',
                    message: 'Vui lòng chọn học sinh để xem thông tin.',
                  );
                }
                final student = dashboard.selectedStudent!;
                return Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (dashboard.parentName != null)
                      Padding(
                        padding: const EdgeInsets.only(bottom: 12),
                        child: Text(
                          'Phụ huynh: ${dashboard.parentName}',
                          style: const TextStyle(fontWeight: FontWeight.w600),
                        ),
                      ),
                    _StudentCard(child: student.child),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: _MetricCard(
                            label: 'Có mặt',
                            value: '${dashboard.attendanceSummary.present}',
                            icon: Icons.check_circle_outline,
                            tint: const Color(0xFFEAF8F1),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: _MetricCard(
                            label: 'Vắng',
                            value: '${dashboard.attendanceSummary.absent}',
                            icon: Icons.cancel_outlined,
                            tint: const Color(0xFFFFF0EE),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: _MetricCard(
                            label: 'Đơn chờ',
                            value: '${dashboard.pendingLeaveRequests}',
                            icon: Icons.pending_actions,
                            tint: const Color(0xFFFFF5E5),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 18),
                    _SectionHeading(title: 'Truy cập nhanh'),
                    _QuickGrid(
                      onLearning: () => widget.onNavigate(1),
                      onAttendance: widget.onAttendance,
                      onTimetable: widget.onTimetable,
                      onLeave: () => widget.onNavigate(3),
                    ),
                    const SizedBox(height: 18),
                    _SectionHeading(title: 'Tóm tắt học tập'),
                    if (dashboard.academicSummary.isEmpty)
                      const _InlineEmpty(
                        'Chưa có điểm được công bố hoặc dữ liệu học tập.',
                      ),
                    ...dashboard.academicSummary
                        .take(4)
                        .map(
                          (result) => _AcademicSubjectCard(
                            result: result,
                            onTap: () => widget.onNavigate(1),
                          ),
                        ),
                    const SizedBox(height: 18),
                    _SectionHeading(
                      title: 'Thông báo mới',
                      action: 'Xem tất cả',
                      onTap: () => widget.onNavigate(2),
                    ),
                    if (dashboard.recentAnnouncements.isEmpty)
                      const _InlineEmpty('Chưa có thông báo mới.'),
                    ...dashboard.recentAnnouncements.map(
                      (announcement) => _AnnouncementTile(
                        announcement: announcement,
                        onTap: () async {
                          await Navigator.of(context).push(
                            MaterialPageRoute<void>(
                              builder: (_) => _AnnouncementDetailScreen(
                                api: widget.api,
                                announcementId: announcement.id,
                              ),
                            ),
                          );
                          if (mounted) await _refresh();
                        },
                      ),
                    ),
                  ],
                );
              },
            ),
        ],
      ),
    );
  }
}

class _LearningTab extends StatelessWidget {
  const _LearningTab({
    required this.api,
    required this.controller,
    required this.onOpen,
    required this.onTimetable,
    required this.onGrades,
    required this.onAttendance,
  });
  final ParentApi api;
  final ParentContextController controller;
  final void Function(_Resource, ParentChild?) onOpen;
  final VoidCallback onTimetable;
  final VoidCallback onGrades;
  final VoidCallback onAttendance;
  @override
  Widget build(BuildContext context) {
    final child = controller.selectedChild;
    if (child == null) {
      return const _EmptyState(
        icon: Icons.school_outlined,
        title: 'Chưa có học sinh',
        message: 'Liên hệ nhà trường để liên kết học sinh với tài khoản.',
      );
    }
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _ChildSelector(controller: controller),
        const SizedBox(height: 12),
        _QuickTile(
          title: 'Hồ sơ học sinh',
          icon: Icons.badge_outlined,
          onTap: () => onOpen(_Resource.profile, child),
        ),
        _QuickTile(
          title: 'Lớp và giáo viên chủ nhiệm',
          icon: Icons.class_outlined,
          onTap: () => onOpen(_Resource.classInfo, child),
        ),
        _QuickTile(
          title: 'Thời khóa biểu',
          icon: Icons.calendar_month_outlined,
          onTap: onTimetable,
        ),
        _QuickTile(
          title: 'Kết quả học tập và điểm môn',
          icon: Icons.bar_chart_outlined,
          onTap: onGrades,
        ),
        _QuickTile(
          title: 'Nhận xét của giáo viên',
          icon: Icons.rate_review_outlined,
          onTap: () => onOpen(_Resource.evaluations, child),
        ),
        _QuickTile(
          title: 'Lịch sử điểm danh',
          icon: Icons.fact_check_outlined,
          onTap: onAttendance,
        ),
        _QuickTile(
          title: 'Tổng hợp điểm danh',
          icon: Icons.donut_small_outlined,
          onTap: () => onOpen(_Resource.attendanceSummary, child),
        ),
      ],
    );
  }
}

class _ChildrenTab extends StatelessWidget {
  const _ChildrenTab({required this.controller, required this.onProfile});
  final ParentContextController controller;
  final ValueChanged<ParentChild> onProfile;
  @override
  Widget build(BuildContext context) {
    if (controller.loading && controller.children.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    if (controller.error != null && controller.children.isEmpty) {
      return _ErrorState(message: controller.error!, onRetry: controller.load);
    }
    if (controller.children.isEmpty) {
      return const _EmptyState(
        icon: Icons.people_outline,
        title: 'Chưa có học sinh được liên kết',
        message: 'Danh sách sẽ hiển thị sau khi nhà trường liên kết học sinh.',
      );
    }
    return RefreshIndicator(
      onRefresh: controller.load,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text(
            'Đã liên kết ${controller.children.length} học sinh',
            style: const TextStyle(color: AppColors.mutedText),
          ),
          const SizedBox(height: 10),
          ...controller.children.map(
            (child) => Card(
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: const Color(0xFFFFF0E8),
                  child: Text(
                    child.fullName.isEmpty
                        ? '?'
                        : child.fullName.characters.first,
                    style: const TextStyle(
                      color: AppColors.deepOrange,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                title: Text(
                  child.fullName,
                  style: const TextStyle(fontWeight: FontWeight.w700),
                ),
                subtitle: Text(
                  '${child.studentCode}${child.studentClass?.name == null ? '' : ' · ${child.studentClass!.name}'}\n${child.school?.name ?? 'Chưa có thông tin trường'}',
                ),
                isThreeLine: true,
                trailing: controller.selectedChild?.id == child.id
                    ? const Icon(
                        Icons.check_circle,
                        color: AppColors.primaryOrange,
                      )
                    : const Icon(Icons.chevron_right),
                onTap: () async {
                  await controller.selectChild(child);
                  if (context.mounted) onProfile(child);
                },
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ChildSelector extends StatelessWidget {
  const _ChildSelector({required this.controller});
  final ParentContextController controller;
  @override
  Widget build(BuildContext context) {
    final child = controller.selectedChild;
    if (controller.children.isEmpty || child == null) {
      return const SizedBox.shrink();
    }
    return InputDecorator(
      decoration: InputDecoration(
        labelText: 'Đang xem thông tin của',
        prefixIcon: const Icon(Icons.person_outline),
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(14)),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<int>(
          isExpanded: true,
          value: child.id,
          items: controller.children
              .map(
                (item) => DropdownMenuItem(
                  value: item.id,
                  child: Text(
                    '${item.fullName} · ${item.studentCode}',
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
              )
              .toList(),
          onChanged: (id) {
            final matches = controller.children.where((item) => item.id == id);
            if (matches.isNotEmpty) controller.selectChild(matches.first);
          },
        ),
      ),
    );
  }
}

class _AnnouncementsTab extends StatefulWidget {
  const _AnnouncementsTab({required this.api});
  final ParentApi api;
  @override
  State<_AnnouncementsTab> createState() => _AnnouncementsTabState();
}

class _AnnouncementsTabState extends State<_AnnouncementsTab> {
  final _items = <ParentAnnouncement>[];
  bool _loading = true, _more = false;
  String? _error;
  int _page = 0, _totalPages = 1;
  int _generation = 0;
  @override
  void initState() {
    super.initState();
    _load(reset: true);
  }

  Future<void> _load({bool reset = false}) async {
    if (_loading && !reset) return;
    final generation = ++_generation;
    setState(() {
      _loading = true;
      _error = null;
      if (reset) {
        _page = 0;
        _items.clear();
      }
    });
    try {
      final result = await widget.api.announcements(page: _page);
      final content = (result['content'] as List)
          .whereType<Map<String, dynamic>>()
          .map(ParentAnnouncement.fromJson)
          .toList();
      if (!mounted || generation != _generation) return;
      setState(() {
        _items.addAll(content);
        _totalPages = (result['totalPages'] as num?)?.toInt() ?? 1;
        _page++;
      });
    } catch (error) {
      if (mounted && generation == _generation) {
        setState(() => _error = _friendlyError(error));
      }
    } finally {
      if (mounted && generation == _generation) {
        setState(() {
          _loading = false;
          _more = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    if (_error != null && _items.isEmpty) {
      return _ErrorState(message: _error!, onRetry: () => _load(reset: true));
    }
    return RefreshIndicator(
      onRefresh: () => _load(reset: true),
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (_items.isEmpty && !_loading)
            const _EmptyState(
              icon: Icons.notifications_none,
              title: 'Chưa có thông báo',
              message: 'Thông báo từ nhà trường sẽ hiển thị tại đây.',
            ),
          ..._items.map(
            (announcement) => _AnnouncementTile(
              announcement: announcement,
              onTap: () async {
                await Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => _AnnouncementDetailScreen(
                      api: widget.api,
                      announcementId: announcement.id,
                    ),
                  ),
                );
                if (mounted) _load(reset: true);
              },
            ),
          ),
          if (_loading)
            const Padding(
              padding: EdgeInsets.all(18),
              child: Center(child: CircularProgressIndicator()),
            ),
          if (!_loading && _page < _totalPages)
            Center(
              child: TextButton.icon(
                onPressed: _more
                    ? null
                    : () {
                        setState(() => _more = true);
                        _load();
                      },
                icon: const Icon(Icons.expand_more),
                label: const Text('Tải thêm'),
              ),
            ),
          if (_error != null)
            _ErrorInline(message: _error!, onRetry: () => _load()),
        ],
      ),
    );
  }
}

class _LeaveRequestsTab extends StatefulWidget {
  const _LeaveRequestsTab({required this.api, required this.controller});
  final ParentApi api;
  final ParentContextController controller;
  @override
  State<_LeaveRequestsTab> createState() => _LeaveRequestsTabState();
}

class _LeaveRequestsTabState extends State<_LeaveRequestsTab> {
  String? _status;
  int? _childId;
  int _nextPage = 0, _totalPages = 1, _generation = 0;
  bool _loading = false;
  String? _error;
  final _items = <LeaveRequest>[];

  @override
  void initState() {
    super.initState();
    widget.controller.addListener(_changed);
    _load();
  }

  @override
  void dispose() {
    _generation++;
    widget.controller.removeListener(_changed);
    super.dispose();
  }

  void _changed() {
    if (_childId != widget.controller.selectedChild?.id) _load();
  }

  Future<void> _load({bool more = false}) async {
    if (more && (_loading || _nextPage >= _totalPages)) return;
    final generation = ++_generation;
    _childId = widget.controller.selectedChild?.id;
    if (!more) {
      _nextPage = 0;
      _totalPages = 1;
      _items.clear();
    }
    setState(() {
      _loading = _childId != null;
      _error = null;
    });
    if (_childId == null) return;
    try {
      final result = await widget.api.leaveRequests(
        _childId!,
        status: _status,
        page: _nextPage,
      );
      if (!mounted || generation != _generation) return;
      setState(() {
        _items.addAll(
          (result['content'] as List? ?? const [])
              .whereType<Map<String, dynamic>>()
              .map(LeaveRequest.fromJson),
        );
        _totalPages = (result['totalPages'] as num?)?.toInt() ?? 1;
        _nextPage++;
      });
    } catch (error) {
      if (mounted && generation == _generation) {
        setState(() => _error = _friendlyError(error));
      }
    } finally {
      if (mounted && generation == _generation) {
        setState(() => _loading = false);
      }
    }
  }

  Future<void> _create() async {
    final child = widget.controller.selectedChild;
    if (child == null) return;
    final created = await Navigator.of(context).push<bool>(
      MaterialPageRoute<bool>(
        builder: (_) =>
            _CreateLeaveRequestScreen(api: widget.api, child: child),
      ),
    );
    if (created == true && mounted) await _load();
  }

  @override
  Widget build(BuildContext context) {
    if (widget.controller.selectedChild == null) {
      return const _EmptyState(
        icon: Icons.event_busy,
        title: 'Chưa có học sinh',
        message: 'Chọn hoặc liên kết học sinh để gửi đơn nghỉ học.',
      );
    }
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 4),
          child: Row(
            children: [
              Expanded(child: _ChildSelector(controller: widget.controller)),
              const SizedBox(width: 8),
              IconButton.filled(
                onPressed: _create,
                tooltip: 'Tạo đơn nghỉ học',
                icon: const Icon(Icons.add),
              ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: DropdownButtonFormField<String?>(
            initialValue: _status,
            decoration: const InputDecoration(labelText: 'Lọc trạng thái'),
            items: const [
              DropdownMenuItem(value: null, child: Text('Tất cả')),
              DropdownMenuItem(value: 'PENDING', child: Text('Chờ duyệt')),
              DropdownMenuItem(value: 'APPROVED', child: Text('Đã duyệt')),
              DropdownMenuItem(value: 'REJECTED', child: Text('Từ chối')),
              DropdownMenuItem(value: 'CANCELLED', child: Text('Đã hủy')),
            ],
            onChanged: (value) {
              _status = value;
              _load();
            },
          ),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: _load,
            child: ListView(
              physics: const AlwaysScrollableScrollPhysics(),
              padding: const EdgeInsets.all(16),
              children: [
                if (!_loading && _items.isEmpty && _error == null)
                  const _EmptyState(
                    icon: Icons.event_note,
                    title: 'Chưa có đơn nghỉ học',
                    message: 'Đơn đã gửi sẽ được hiển thị tại đây.',
                  ),
                ..._items.map(
                  (request) => Card(
                    child: ListTile(
                      title: Text(
                        '${_fmtDate(request.startDate)} – ${_fmtDate(request.endDate)}',
                      ),
                      subtitle: Text(
                        '${_statusLabel(request.status)} · ${request.reason ?? ''}\nNgày gửi: ${_fmtDate(request.createdAt)}',
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                      ),
                      trailing: const Icon(Icons.chevron_right),
                      onTap: () async {
                        await Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => _LeaveDetailScreen(
                              api: widget.api,
                              requestId: request.id,
                            ),
                          ),
                        );
                        if (mounted) await _load();
                      },
                    ),
                  ),
                ),
                if (_loading)
                  const Padding(
                    padding: EdgeInsets.all(18),
                    child: Center(child: CircularProgressIndicator()),
                  ),
                if (_error != null)
                  _ErrorState(
                    message: _error!,
                    onRetry: () => _load(more: _items.isNotEmpty),
                  ),
                if (!_loading && _error == null && _nextPage < _totalPages)
                  Center(
                    child: TextButton(
                      onPressed: () => _load(more: true),
                      child: const Text('Tải thêm'),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _ParentResourceScreen extends StatefulWidget {
  const _ParentResourceScreen({
    required this.api,
    required this.child,
    required this.resource,
    required this.controller,
    super.key,
  });
  final ParentApi api;
  final ParentChild child;
  final _Resource resource;
  final ParentContextController controller;

  @override
  State<_ParentResourceScreen> createState() => _ParentResourceScreenState();
}

class _ParentResourceScreenState extends State<_ParentResourceScreen> {
  late Future<Object> _future;
  @override
  void initState() {
    super.initState();
    _future = _fetch();
  }

  @override
  void didUpdateWidget(covariant _ParentResourceScreen oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.child.id != widget.child.id ||
        oldWidget.resource != widget.resource) {
      _future = _fetch();
    }
  }

  void _retry() => setState(() => _future = _fetch());
  Future<void> _refresh() async {
    _retry();
    try {
      await _future;
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(_title)),
    body: Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 8, 16, 8),
          child: _ChildSelector(controller: widget.controller),
        ),
        Expanded(
          child: FutureBuilder<Object>(
            key: ValueKey(_future),
            future: _future,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return const Center(child: CircularProgressIndicator());
              }
              if (snapshot.hasError) {
                return _ErrorState(
                  message: _friendlyError(snapshot.error!),
                  onRetry: _retry,
                );
              }
              final value = snapshot.data;
              if (value == null || value is List && value.isEmpty) {
                return RefreshIndicator(
                  onRefresh: _refresh,
                  child: ListView(
                    physics: const AlwaysScrollableScrollPhysics(),
                    children: const [
                      _EmptyState(
                        icon: Icons.inbox_outlined,
                        title: 'Chưa có dữ liệu',
                        message: 'Thông tin sẽ xuất hiện khi nhà trường cập nhật dữ liệu.',
                      ),
                    ],
                  ),
                );
              }
              final content = _ResourceContent(
                api: widget.api,
                child: widget.child,
                resource: widget.resource,
                value: value,
              );
              if (widget.resource == _Resource.attendance ||
                  widget.resource == _Resource.evaluations) {
                return content;
              }
              return RefreshIndicator(onRefresh: _refresh, child: content);
            },
          ),
        ),
      ],
    ),
  );

  String get _title => switch (widget.resource) {
    _Resource.profile => 'Hồ sơ học sinh',
    _Resource.classInfo => 'Lớp và giáo viên',
    _Resource.timetable => 'Thời khóa biểu',
    _Resource.results => 'Kết quả học tập',
    _Resource.evaluations => 'Nhận xét và đánh giá',
    _Resource.attendance => 'Lịch sử điểm danh',
    _Resource.attendanceSummary => 'Tổng hợp điểm danh',
  };
  Future<Object> _fetch() => switch (widget.resource) {
    _Resource.profile => widget.api.studentProfile(widget.child.id),
    _Resource.classInfo =>
      widget.api
          .classInformation(widget.child.id)
          .then((value) => value ?? <String, dynamic>{}),
    _Resource.timetable => widget.api.timetable(widget.child.id),
    _Resource.results => widget.api.academicResults(widget.child.id),
    _Resource.evaluations => widget.api.evaluations(widget.child.id),
    _Resource.attendance =>
      Future.wait([
        widget.api.attendance(widget.child.id),
        widget.api.attendanceSummary(widget.child.id),
      ]).then(
        (values) => <String, Object>{
          'history': values[0],
          'summary': values[1],
        },
      ),
    _Resource.attendanceSummary => widget.api.attendanceSummary(
      widget.child.id,
    ),
  };
}

class _ResourceContent extends StatelessWidget {
  const _ResourceContent({
    required this.api,
    required this.child,
    required this.resource,
    required this.value,
  });
  final ParentApi api;
  final ParentChild child;
  final _Resource resource;
  final Object value;
  @override
  Widget build(BuildContext context) {
    if (resource == _Resource.attendance && value is Map) {
      final data = Map<String, dynamic>.from(value as Map);
      return _AttendanceContent(
        api: api,
        studentId: child.id,
        firstPage: _asMap(data['history']) ?? const {},
        summary: data['summary'] as AttendanceSummary,
      );
    }
    if (resource == _Resource.evaluations && value is Map) {
      return _EvaluationsContent(
        api: api,
        studentId: child.id,
        firstPage: Map<String, dynamic>.from(value as Map),
      );
    }
    if (value is StudentProfile) {
      final profile = value as StudentProfile;
      final s = profile.child;
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _StudentCard(child: s),
          _InfoRow('Mã học sinh', s.studentCode),
          _InfoRow('Ngày sinh', _fmtDate(profile.dateOfBirth)),
          _InfoRow('Giới tính', _genderLabel(profile.gender)),
          _InfoRow(
            'Trạng thái',
            _statusLabel(profile.status ?? s.enrollmentStatus),
          ),
          _InfoRow('Trường', s.school?.name),
          _InfoRow('Lớp', s.studentClass?.name),
          _InfoRow('Năm học', s.studentClass?.academicYear?.name),
        ],
      );
    }
    if (value is Map<String, dynamic>) {
      final data = value as Map<String, dynamic>;
      final klass = _asMap(data['class']) ?? data;
      final school = _asMap(data['school']);
      final teacher = _asMap(klass['homeroomTeacher']);
      final year = _asMap(klass['academicYear']);
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _InfoRow('Lớp', _str(klass['name']) ?? 'Chưa xếp lớp'),
          _InfoRow('Năm học', _str(year?['name'])),
          _InfoRow('Trường', _str(school?['name']) ?? child.school?.name),
          _InfoRow('Giáo viên chủ nhiệm', _str(teacher?['fullName'])),
          _InfoRow('Điện thoại', _str(teacher?['phone'])),
          _InfoRow('Email', _str(teacher?['email'])),
        ],
      );
    }
    if (value is AttendanceSummary) {
      final a = value as AttendanceSummary;
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _MetricCard(
            label: 'Tổng số buổi',
            value: '${a.totalSessions}',
            icon: Icons.event_available,
            tint: const Color(0xFFEAF4FF),
          ),
          _MetricCard(
            label: 'Có mặt',
            value: '${a.present}',
            icon: Icons.check_circle_outline,
            tint: const Color(0xFFEAF8F1),
          ),
          _MetricCard(
            label: 'Vắng',
            value: '${a.absent}',
            icon: Icons.cancel_outlined,
            tint: const Color(0xFFFFF0EE),
          ),
          _MetricCard(
            label: 'Có phép',
            value: '${a.excused}',
            icon: Icons.assignment_turned_in_outlined,
            tint: const Color(0xFFFFF8E8),
          ),
          _MetricCard(
            label: 'Đi muộn',
            value: '${a.late}',
            icon: Icons.schedule,
            tint: const Color(0xFFFFF5E5),
          ),
        ],
      );
    }
    if (value is List<TimetableEntry>) {
      final entries = value as List<TimetableEntry>;
      return ListView(
        padding: const EdgeInsets.all(16),
        children: [
          for (var day = 1; day <= 7; day++) ...[
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(
                _weekday(day),
                style: TextStyle(
                  fontSize: 17,
                  fontWeight: FontWeight.bold,
                  color: day == DateTime.now().weekday
                      ? AppColors.deepOrange
                      : AppColors.darkText,
                ),
              ),
            ),
            ...entries
                .where((e) => e.weekday == day)
                .map(
                  (entry) => Card(
                    child: ListTile(
                      leading: CircleAvatar(
                        child: Text('${entry.period ?? '-'}'),
                      ),
                      title: Text(entry.subject?.name ?? 'Môn học'),
                      subtitle: Text(
                        '${entry.teacher?.fullName ?? 'Chưa có giáo viên'} · ${entry.shift ?? ''}${entry.classroom == null ? '' : ' · ${entry.classroom}'}',
                      ),
                      trailing: entry.startTime == null
                          ? null
                          : Text('${entry.startTime}\n${entry.endTime ?? ''}'),
                    ),
                  ),
                ),
          ],
        ],
      );
    }
    if (value is List<AcademicResult>) {
      final results = value as List<AcademicResult>;
      return ListView(
        padding: const EdgeInsets.all(16),
        children: results
            .map(
              (result) => _AcademicSubjectCard(
                result: result,
                onTap: () => Navigator.of(context).push(
                  MaterialPageRoute<void>(
                    builder: (_) => _SubjectGradesScreen(
                      api: api,
                      studentId: child.id,
                      subject: result.subject,
                      grades: result.grades,
                    ),
                  ),
                ),
              ),
            )
            .toList(),
      );
    }
    return const _InlineEmpty('Không có dữ liệu phù hợp.');
  }
}

class _AttendanceContent extends StatefulWidget {
  const _AttendanceContent({
    required this.api,
    required this.studentId,
    required this.firstPage,
    required this.summary,
  });
  final ParentApi api;
  final int studentId;
  final Map<String, dynamic> firstPage;
  final AttendanceSummary summary;

  @override
  State<_AttendanceContent> createState() => _AttendanceContentState();
}

class _AttendanceContentState extends State<_AttendanceContent> {
  late List<AttendanceRecord> _records;
  late int _page;
  late int _totalPages;
  bool _loadingMore = false;
  int _generation = 0;
  bool _retryMore = false;
  String? _error;
  DateTime? _from;
  DateTime? _to;

  @override
  void initState() {
    super.initState();
    _setPage(widget.firstPage);
  }

  void _setPage(Map<String, dynamic> value, {bool append = false}) {
    final parsed = (value['content'] as List? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(AttendanceRecord.fromJson)
        .toList();
    _records = append ? [..._records, ...parsed] : parsed;
    _page = (value['page'] as num?)?.toInt() ?? 0;
    _totalPages = (value['totalPages'] as num?)?.toInt() ?? 1;
  }

  Future<void> _filter() async {
    final request = ++_generation;
    setState(() {
      _loadingMore = true;
      _error = null;
      _retryMore = false;
      _records.clear();
    });
    try {
      final value = await widget.api.attendance(
        widget.studentId,
        from: _from,
        to: _to,
      );
      if (mounted && request == _generation) setState(() => _setPage(value));
    } catch (error) {
      if (mounted && request == _generation) {
        setState(() => _error = _friendlyError(error));
      }
    } finally {
      if (mounted && request == _generation) {
        setState(() => _loadingMore = false);
      }
    }
  }

  Future<void> _loadMore() async {
    if (_loadingMore || _page + 1 >= _totalPages) return;
    final request = _generation;
    setState(() {
      _loadingMore = true;
      _error = null;
      _retryMore = true;
    });
    try {
      final value = await widget.api.attendance(
        widget.studentId,
        from: _from,
        to: _to,
        page: _page + 1,
      );
      if (mounted && request == _generation) {
        setState(() => _setPage(value, append: true));
      }
    } catch (error) {
      if (mounted && request == _generation) {
        setState(() => _error = _friendlyError(error));
      }
    } finally {
      if (mounted && request == _generation) {
        setState(() => _loadingMore = false);
      }
    }
  }

  Future<void> _pickDate(bool isFrom) async {
    final now = DateTime.now();
    final selected = await showDatePicker(
      context: context,
      initialDate: (isFrom ? _from : _to) ?? now,
      firstDate: DateTime(now.year - 5),
      lastDate: DateTime(now.year + 1),
    );
    if (selected == null || !mounted) return;
    final from = isFrom ? selected : _from;
    final to = isFrom ? _to : selected;
    if (from != null && to != null && to.isBefore(from)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Ngày kết thúc phải từ ngày bắt đầu trở đi.'),
        ),
      );
      return;
    }
    setState(() {
      if (isFrom) {
        _from = selected;
      } else {
        _to = selected;
      }
    });
    await _filter();
  }

  @override
  Widget build(BuildContext context) => RefreshIndicator(
    onRefresh: _filter,
    child: ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(16),
      children: [
        Wrap(
          spacing: 8,
          runSpacing: 6,
          children: [
            _SmallAction(
              'Từ: ${_from == null ? '...' : _fmtDate(_from)}',
              Icons.calendar_today,
              () => _pickDate(true),
            ),
            _SmallAction(
              'Đến: ${_to == null ? '...' : _fmtDate(_to)}',
              Icons.calendar_today,
              () => _pickDate(false),
            ),
            if (_from != null || _to != null)
              _SmallAction('Xóa lọc', Icons.clear, () {
                setState(() {
                  _from = null;
                  _to = null;
                });
                _filter();
              }),
          ],
        ),
        const SizedBox(height: 10),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            _MetricCard(
              label: 'Tổng buổi (toàn bộ)',
              value: '${widget.summary.totalSessions}',
              icon: Icons.event_available,
              tint: const Color(0xFFEAF4FF),
            ),
            _MetricCard(
              label: 'Có mặt',
              value: '${widget.summary.present}',
              icon: Icons.check_circle_outline,
              tint: const Color(0xFFEAF8F1),
            ),
            _MetricCard(
              label: 'Vắng',
              value: '${widget.summary.absent}',
              icon: Icons.cancel_outlined,
              tint: const Color(0xFFFFF0EE),
            ),
            _MetricCard(
              label: 'Có phép',
              value: '${widget.summary.excused}',
              icon: Icons.assignment_turned_in_outlined,
              tint: const Color(0xFFFFF8E8),
            ),
            _MetricCard(
              label: 'Đi muộn',
              value: '${widget.summary.late}',
              icon: Icons.schedule,
              tint: const Color(0xFFFFF5E5),
            ),
          ],
        ),
        const SizedBox(height: 14),
        if (_records.isEmpty && !_loadingMore)
          const _InlineEmpty(
            'Không có lịch sử điểm danh trong khoảng thời gian này.',
          ),
        ..._records.map(
          (record) => Card(
            child: ListTile(
              leading: Icon(
                _statusIcon(record.status),
                color: _statusColor(record.status),
              ),
              title: Text(_fmtDate(record.date)),
              subtitle: Text(
                '${_statusLabel(record.status)}${record.period == null ? '' : ' · Tiết ${record.period}'}${record.remarks == null ? '' : '\n${record.remarks}'}',
              ),
            ),
          ),
        ),
        if (_error != null)
          _ErrorInline(
            message: _error!,
            onRetry: _retryMore ? _loadMore : _filter,
          ),
        if (_loadingMore)
          const Padding(
            padding: EdgeInsets.all(14),
            child: Center(child: CircularProgressIndicator()),
          ),
        if (!_loadingMore && _page + 1 < _totalPages)
          Center(
            child: TextButton(
              onPressed: _loadMore,
              child: const Text('Tải thêm'),
            ),
          ),
      ],
    ),
  );
}

class _EvaluationsContent extends StatefulWidget {
  const _EvaluationsContent({
    required this.api,
    required this.studentId,
    required this.firstPage,
  });
  final ParentApi api;
  final int studentId;
  final Map<String, dynamic> firstPage;
  @override
  State<_EvaluationsContent> createState() => _EvaluationsContentState();
}

class _EvaluationsContentState extends State<_EvaluationsContent> {
  late List<StudentEvaluation> _items;
  late int _page;
  late int _totalPages;
  bool _loading = false;
  String? _error;
  int _generation = 0;
  bool _retryReset = false;
  @override
  void initState() {
    super.initState();
    _setPage(widget.firstPage);
  }

  void _setPage(Map<String, dynamic> value, {bool append = false}) {
    final rows = (value['content'] as List? ?? const [])
        .whereType<Map<String, dynamic>>()
        .map(StudentEvaluation.fromJson)
        .toList();
    _items = append ? [..._items, ...rows] : rows;
    _page = (value['page'] as num?)?.toInt() ?? 0;
    _totalPages = (value['totalPages'] as num?)?.toInt() ?? 1;
  }

  Future<void> _more({bool reset = false}) async {
    if (!reset && (_loading || _page + 1 >= _totalPages)) return;
    final request = ++_generation;
    setState(() {
      _loading = true;
      _error = null;
      _retryReset = reset;
    });
    try {
      final value = await widget.api.evaluations(
        widget.studentId,
        page: reset ? 0 : _page + 1,
      );
      if (mounted && request == _generation) {
        setState(() => _setPage(value, append: !reset));
      }
    } catch (error) {
      if (mounted && request == _generation) {
        setState(() => _error = _friendlyError(error));
      }
    } finally {
      if (mounted && request == _generation) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) => RefreshIndicator(
    onRefresh: () => _more(reset: true),
    child: ListView(
      physics: const AlwaysScrollableScrollPhysics(),
      padding: const EdgeInsets.all(16),
      children: [
        if (_items.isEmpty)
          const _InlineEmpty('Chưa có nhận xét hoặc đánh giá.'),
        ..._items.map(
          (e) => Card(
            child: ListTile(
              title: Text(
                e.type == 'REMARK'
                    ? 'Nhận xét của giáo viên'
                    : 'Đánh giá học sinh',
              ),
              subtitle: Text(
                '${e.teacher?.fullName ?? ''}${e.subject?.name == null ? '' : ' · ${e.subject!.name}'}\n${e.content ?? 'Chưa có nhận xét'}${e.academicResult == null ? '' : '\nHọc tập: ${e.academicResult}'}${e.conductResult == null ? '' : '\nRèn luyện: ${e.conductResult}'}',
              ),
              trailing: Text(_fmtDate(e.date)),
            ),
          ),
        ),
        if (_error != null)
          _ErrorInline(
            message: _error!,
            onRetry: () => _more(reset: _retryReset),
          ),
        if (_loading)
          const Padding(
            padding: EdgeInsets.all(14),
            child: Center(child: CircularProgressIndicator()),
          ),
        if (!_loading && _page + 1 < _totalPages)
          Center(
            child: TextButton(onPressed: _more, child: const Text('Tải thêm')),
          ),
      ],
    ),
  );
}

class _SubjectGradesScreen extends StatefulWidget {
  const _SubjectGradesScreen({
    required this.api,
    required this.studentId,
    required this.subject,
    required this.grades,
  });
  final ParentApi api;
  final int studentId;
  final SubjectInfo subject;
  final List<StudentGrade> grades;
  @override
  State<_SubjectGradesScreen> createState() => _SubjectGradesScreenState();
}

class _SubjectGradesScreenState extends State<_SubjectGradesScreen> {
  late Future<List<StudentGrade>> _future = Future.value(widget.grades);

  Future<void> _refresh() async {
    // UC77 supplies the actual current-semester grades. The dedicated UC78
    // endpoint requires a semester ID that the current backend never returns.
    final future = widget.api.academicResults(widget.studentId).then((results) {
      for (final result in results) {
        if (result.subject.id == widget.subject.id) return result.grades;
      }
      return <StudentGrade>[];
    });
    setState(() => _future = future);
    try {
      await future;
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: Text(widget.subject.name ?? 'Điểm môn học')),
    body: FutureBuilder<List<StudentGrade>>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return _ErrorState(
            message: _friendlyError(snapshot.error!),
            onRetry: _refresh,
          );
        }
        final grades = snapshot.data ?? const <StudentGrade>[];
        return RefreshIndicator(
          onRefresh: _refresh,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(16),
            children: [
              if (grades.isEmpty)
                const _EmptyState(
                  icon: Icons.grade_outlined,
                  title: 'Chưa có điểm',
                  message: 'Điểm thành phần sẽ hiển thị sau khi được cập nhật.',
                ),
              ...grades.map(
                (grade) => Card(
                  child: ListTile(
                    title: Text(grade.name ?? grade.type ?? 'Điểm'),
                    subtitle: Text(
                      '${grade.type ?? ''}${grade.weight == null ? '' : ' · Hệ số ${grade.weight}'}${grade.assessmentDate == null ? '' : ' · ${_fmtDate(grade.assessmentDate)}'}${grade.comment == null ? '' : '\nNhận xét: ${grade.comment}'}',
                    ),
                    trailing: Text(
                      grade.score?.toStringAsFixed(2) ?? 'Chưa có điểm',
                      style: const TextStyle(fontWeight: FontWeight.bold),
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    ),
  );
}

class _AnnouncementDetailScreen extends StatefulWidget {
  const _AnnouncementDetailScreen({
    required this.api,
    required this.announcementId,
  });
  final ParentApi api;
  final int announcementId;
  @override
  State<_AnnouncementDetailScreen> createState() =>
      _AnnouncementDetailScreenState();
}

class _AnnouncementDetailScreenState extends State<_AnnouncementDetailScreen> {
  late Future<ParentAnnouncement> _future;
  bool _marking = false;
  @override
  void initState() {
    super.initState();
    _future = widget.api.announcement(widget.announcementId);
  }

  Future<void> _mark() async {
    if (_marking) return;
    setState(() => _marking = true);
    try {
      await widget.api.markAnnouncementRead(widget.announcementId);
      if (mounted) {
        setState(
          () => _future = widget.api.announcement(widget.announcementId),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(_friendlyError(e))));
      }
    } finally {
      if (mounted) setState(() => _marking = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Chi tiết thông báo')),
    body: FutureBuilder<ParentAnnouncement>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return _ErrorState(
            message: _friendlyError(snapshot.error!),
            onRetry: () => setState(
              () => _future = widget.api.announcement(widget.announcementId),
            ),
          );
        }
        final a = snapshot.data!;
        return ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Text(
              a.title,
              style: const TextStyle(fontSize: 22, fontWeight: FontWeight.w800),
            ),
            const SizedBox(height: 8),
            Text(
              '${a.sender ?? 'Nhà trường'} · ${_fmtDate(a.publishedAt)}',
              style: const TextStyle(color: AppColors.mutedText),
            ),
            Align(
              alignment: Alignment.centerLeft,
              child: Chip(
                avatar: Icon(
                  a.read
                      ? Icons.mark_email_read_outlined
                      : Icons.mark_email_unread_outlined,
                  size: 18,
                ),
                label: Text(a.read ? 'Đã đọc' : 'Chưa đọc'),
              ),
            ),
            const Divider(height: 28),
            SelectableText(
              a.content ?? a.preview ?? '',
              style: const TextStyle(fontSize: 16, height: 1.55),
            ),
            if (!a.read)
              Padding(
                padding: const EdgeInsets.only(top: 24),
                child: FilledButton.icon(
                  onPressed: _marking ? null : _mark,
                  icon: _marking
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.done),
                  label: const Text('Đánh dấu đã đọc'),
                ),
              ),
          ],
        );
      },
    ),
  );
}

class _CreateLeaveRequestScreen extends StatefulWidget {
  const _CreateLeaveRequestScreen({required this.api, required this.child});
  final ParentApi api;
  final ParentChild child;
  @override
  State<_CreateLeaveRequestScreen> createState() =>
      _CreateLeaveRequestScreenState();
}

class _CreateLeaveRequestScreenState extends State<_CreateLeaveRequestScreen> {
  final _formKey = GlobalKey<FormState>();
  final _reason = TextEditingController();
  DateTime? _start, _end;
  bool _saving = false;
  @override
  void dispose() {
    _reason.dispose();
    super.dispose();
  }

  Future<void> _pick(bool start) async {
    final now = DateTime.now();
    final picked = await showDatePicker(
      context: context,
      initialDate: (start ? _start : _end) ?? now,
      firstDate: DateTime(now.year - 1),
      lastDate: DateTime(now.year + 2),
      helpText: start ? 'Chọn ngày bắt đầu nghỉ' : 'Chọn ngày kết thúc nghỉ',
    );
    if (picked != null && mounted) {
      setState(() {
        if (start) {
          _start = picked;
          if (_end != null && _end!.isBefore(picked)) _end = null;
        } else {
          _end = picked;
        }
      });
    }
  }

  Future<void> _submit() async {
    if (_saving || !_formKey.currentState!.validate()) return;
    if (_start == null || _end == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Vui lòng chọn ngày bắt đầu và kết thúc.'),
        ),
      );
      return;
    }
    if (_end!.isBefore(_start!)) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Ngày kết thúc phải từ ngày bắt đầu trở đi.'),
        ),
      );
      return;
    }
    setState(() => _saving = true);
    try {
      final request = await widget.api.submitLeave(
        widget.child.id,
        startDate: _start!,
        endDate: _end!,
        reason: _reason.text,
      );
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Đã gửi đơn nghỉ học #${request.id}.')),
        );
        Navigator.of(context).pop(true);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(_friendlyError(e))));
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Tạo đơn nghỉ học')),
    body: Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(18),
        children: [
          _StudentCard(child: widget.child),
          const SizedBox(height: 16),
          _DatePickerTile(
            label: 'Ngày bắt đầu',
            value: _start == null ? 'Chọn ngày' : _fmtDate(_start),
            onTap: () => _pick(true),
          ),
          const SizedBox(height: 10),
          _DatePickerTile(
            label: 'Ngày kết thúc',
            value: _end == null ? 'Chọn ngày' : _fmtDate(_end),
            onTap: () => _pick(false),
          ),
          const SizedBox(height: 16),
          TextFormField(
            controller: _reason,
            minLines: 4,
            maxLines: 7,
            maxLength: 4000,
            decoration: const InputDecoration(
              labelText: 'Lý do nghỉ',
              alignLabelWithHint: true,
              border: OutlineInputBorder(),
            ),
            validator: (value) => value == null || value.trim().isEmpty
                ? 'Vui lòng nhập lý do nghỉ.'
                : null,
          ),
          const SizedBox(height: 18),
          FilledButton(
            onPressed: _saving ? null : _submit,
            child: _saving
                ? const SizedBox(
                    height: 20,
                    width: 20,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Text('Gửi đơn'),
          ),
        ],
      ),
    ),
  );
}

class _LeaveDetailScreen extends StatefulWidget {
  const _LeaveDetailScreen({required this.api, required this.requestId});
  final ParentApi api;
  final int requestId;
  @override
  State<_LeaveDetailScreen> createState() => _LeaveDetailScreenState();
}

class _LeaveDetailScreenState extends State<_LeaveDetailScreen> {
  late Future<LeaveRequest> _future;
  @override
  void initState() {
    super.initState();
    _future = widget.api.leaveRequest(widget.requestId);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('Chi tiết đơn nghỉ'),
      actions: [
        IconButton(
          tooltip: 'Cập nhật trạng thái',
          icon: const Icon(Icons.refresh),
          onPressed: () => setState(
            () => _future = widget.api.leaveRequest(widget.requestId),
          ),
        ),
      ],
    ),
    body: FutureBuilder<LeaveRequest>(
      future: _future,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return _ErrorState(
            message: _friendlyError(snapshot.error!),
            onRetry: () => setState(
              () => _future = widget.api.leaveRequest(widget.requestId),
            ),
          );
        }
        final r = snapshot.data!;
        return ListView(
          padding: const EdgeInsets.all(18),
          children: [
            _InfoRow('Học sinh', r.studentName),
            _InfoRow('Trạng thái', _statusLabel(r.status)),
            _InfoRow(
              'Thời gian nghỉ',
              '${_fmtDate(r.startDate)} – ${_fmtDate(r.endDate)}',
            ),
            _InfoRow('Ngày gửi', _fmtDate(r.createdAt)),
            _InfoRow('Lý do', r.reason),
            _InfoRow('Người duyệt', r.reviewer),
            _InfoRow('Ngày quyết định', _fmtDate(r.decisionAt)),
            if (r.rejectionReason != null)
              _InfoRow('Lý do từ chối', r.rejectionReason),
            const SizedBox(height: 12),
            const Text(
              'Lịch sử xử lý',
              style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold),
            ),
            ...r.history.map(
              (h) => ListTile(
                leading: const Icon(Icons.timeline),
                title: Text(
                  '${_statusLabel(_str(h['from']))} → ${_statusLabel(_str(h['to']))}',
                ),
                subtitle: Text(
                  '${_fmtDate(_dateValue(h['changedAt']))} ${_str(h['note']) ?? ''}',
                ),
              ),
            ),
          ],
        );
      },
    ),
  );
}

class _QuickGrid extends StatelessWidget {
  const _QuickGrid({
    required this.onLearning,
    required this.onAttendance,
    required this.onTimetable,
    required this.onLeave,
  });
  final VoidCallback onLearning, onAttendance, onTimetable, onLeave;
  @override
  Widget build(BuildContext context) => Wrap(
    spacing: 9,
    runSpacing: 9,
    children: [
      _SmallAction('Học tập', Icons.menu_book, onLearning),
      _SmallAction('Thời khóa biểu', Icons.calendar_month, onTimetable),
      _SmallAction('Điểm danh', Icons.fact_check, onAttendance),
      _SmallAction('Đơn nghỉ', Icons.event_note, onLeave),
    ],
  );
}

class _SmallAction extends StatelessWidget {
  const _SmallAction(this.label, this.icon, this.onTap);
  final String label;
  final IconData icon;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => ActionChip(
    avatar: Icon(icon, size: 18, color: AppColors.deepOrange),
    label: Text(label),
    onPressed: onTap,
  );
}

class _QuickTile extends StatelessWidget {
  const _QuickTile({
    required this.title,
    required this.icon,
    required this.onTap,
  });
  final String title;
  final IconData icon;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Card(
    child: ListTile(
      leading: Icon(icon, color: AppColors.deepOrange),
      title: Text(title),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    ),
  );
}

class _StudentCard extends StatelessWidget {
  const _StudentCard({required this.child});
  final ParentChild child;
  @override
  Widget build(BuildContext context) => Card(
    color: Colors.white,
    child: Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          CircleAvatar(
            backgroundColor: const Color(0xFFFFF0E8),
            child: Text(
              child.fullName.isEmpty ? '?' : child.fullName.characters.first,
              style: const TextStyle(
                color: AppColors.deepOrange,
                fontWeight: FontWeight.bold,
              ),
            ),
          ),
          const SizedBox(width: 13),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  child.fullName,
                  style: const TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  '${child.studentCode}${child.studentClass?.name == null ? '' : ' · ${child.studentClass!.name}'}',
                  style: const TextStyle(color: AppColors.mutedText),
                ),
                Text(
                  child.school?.name ?? 'Chưa có thông tin trường',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: const TextStyle(color: AppColors.mutedText),
                ),
              ],
            ),
          ),
        ],
      ),
    ),
  );
}

class _MetricCard extends StatelessWidget {
  const _MetricCard({
    required this.label,
    required this.value,
    required this.icon,
    required this.tint,
  });
  final String label, value;
  final IconData icon;
  final Color tint;
  @override
  Widget build(BuildContext context) => Card(
    color: tint,
    elevation: 0,
    child: Padding(
      padding: const EdgeInsets.all(12),
      child: Column(
        children: [
          Icon(icon, color: AppColors.deepOrange),
          const SizedBox(height: 4),
          Text(
            value,
            style: const TextStyle(fontSize: 19, fontWeight: FontWeight.w800),
          ),
          Text(
            label,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 11, color: AppColors.mutedText),
          ),
        ],
      ),
    ),
  );
}

class _AcademicSubjectCard extends StatelessWidget {
  const _AcademicSubjectCard({required this.result, required this.onTap});
  final AcademicResult result;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Card(
    child: ListTile(
      title: Text(result.subject.name ?? 'Môn học'),
      subtitle: Text(
        result.grades.isEmpty
            ? 'Chưa có điểm thành phần'
            : '${result.grades.length} điểm thành phần',
      ),
      trailing: const Icon(Icons.chevron_right),
      onTap: onTap,
    ),
  );
}

class _AnnouncementTile extends StatelessWidget {
  const _AnnouncementTile({required this.announcement, required this.onTap});
  final ParentAnnouncement announcement;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => Card(
    child: ListTile(
      leading: Icon(
        announcement.read
            ? Icons.mark_email_read_outlined
            : Icons.mark_email_unread_outlined,
        color: announcement.read
            ? AppColors.mutedText
            : AppColors.primaryOrange,
      ),
      title: Text(
        announcement.title,
        maxLines: 1,
        overflow: TextOverflow.ellipsis,
        style: TextStyle(
          fontWeight: announcement.read ? FontWeight.w500 : FontWeight.w800,
        ),
      ),
      subtitle: Text(
        '${announcement.preview ?? ''}\n${announcement.sender ?? 'Nhà trường'} · ${_fmtDate(announcement.publishedAt)}',
        maxLines: 2,
        overflow: TextOverflow.ellipsis,
      ),
      isThreeLine: true,
      onTap: onTap,
    ),
  );
}

class _SectionHeading extends StatelessWidget {
  const _SectionHeading({required this.title, this.action, this.onTap});
  final String title;
  final String? action;
  final VoidCallback? onTap;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.only(bottom: 8),
    child: Row(
      children: [
        Expanded(
          child: Text(
            title,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
          ),
        ),
        if (action != null) TextButton(onPressed: onTap, child: Text(action!)),
      ],
    ),
  );
}

class _InfoRow extends StatelessWidget {
  const _InfoRow(this.label, this.value);
  final String label;
  final String? value;
  @override
  Widget build(BuildContext context) => Card(
    child: ListTile(
      title: Text(
        label,
        style: const TextStyle(fontSize: 12, color: AppColors.mutedText),
      ),
      subtitle: Text(
        value?.isNotEmpty == true ? value! : 'Chưa có thông tin',
        style: const TextStyle(fontSize: 16, color: AppColors.darkText),
      ),
    ),
  );
}

class _DatePickerTile extends StatelessWidget {
  const _DatePickerTile({
    required this.label,
    required this.value,
    required this.onTap,
  });
  final String label, value;
  final VoidCallback onTap;
  @override
  Widget build(BuildContext context) => ListTile(
    onTap: onTap,
    shape: RoundedRectangleBorder(
      side: const BorderSide(color: AppColors.inputBorder),
      borderRadius: BorderRadius.circular(12),
    ),
    title: Text(label),
    subtitle: Text(
      value,
      style: TextStyle(
        color: value == 'Chọn ngày' ? AppColors.mutedText : AppColors.darkText,
      ),
    ),
    trailing: const Icon(Icons.calendar_today_outlined),
  );
}

class _EmptyState extends StatelessWidget {
  const _EmptyState({
    required this.icon,
    required this.title,
    required this.message,
  });
  final IconData icon;
  final String title, message;
  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(28),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 48, color: AppColors.mutedText),
          const SizedBox(height: 12),
          Text(
            title,
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 6),
          Text(
            message,
            textAlign: TextAlign.center,
            style: const TextStyle(color: AppColors.mutedText),
          ),
        ],
      ),
    ),
  );
}

class _InlineEmpty extends StatelessWidget {
  const _InlineEmpty(this.message);
  final String message;
  @override
  Widget build(BuildContext context) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 14),
    child: Text(message, style: const TextStyle(color: AppColors.mutedText)),
  );
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;
  @override
  Widget build(BuildContext context) => Center(
    child: Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(
            Icons.cloud_off_outlined,
            size: 42,
            color: AppColors.mutedText,
          ),
          const SizedBox(height: 10),
          Text(message, textAlign: TextAlign.center),
          const SizedBox(height: 10),
          FilledButton.tonal(onPressed: onRetry, child: const Text('Thử lại')),
        ],
      ),
    ),
  );
}

class _ErrorInline extends StatelessWidget {
  const _ErrorInline({required this.message, required this.onRetry});
  final String message;
  final VoidCallback onRetry;
  @override
  Widget build(BuildContext context) => ListTile(
    title: Text(message),
    trailing: TextButton(onPressed: onRetry, child: const Text('Thử lại')),
  );
}

class _DashboardLoading extends StatelessWidget {
  const _DashboardLoading();
  @override
  Widget build(BuildContext context) => const Column(
    children: [
      SizedBox(height: 10),
      LinearProgressIndicator(),
      SizedBox(height: 18),
      Card(child: SizedBox(height: 105)),
      Card(child: SizedBox(height: 82)),
      Card(child: SizedBox(height: 140)),
    ],
  );
}

Map<String, dynamic>? _asMap(Object? value) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) return Map<String, dynamic>.from(value);
  return null;
}

String? _str(Object? value) =>
    value is String && value.trim().isNotEmpty ? value.trim() : null;
DateTime? _dateValue(Object? value) =>
    value is String ? DateTime.tryParse(value) : null;
String _fmtDate(DateTime? value) => value == null
    ? 'Chưa cập nhật'
    : '${value.day.toString().padLeft(2, '0')}/${value.month.toString().padLeft(2, '0')}/${value.year}';
String _friendlyError(Object error) {
  if (error is ApiException) return error.message;
  return 'Không thể tải dữ liệu. Vui lòng thử lại.';
}

String _weekday(int day) => switch (day) {
  1 => 'Thứ Hai',
  2 => 'Thứ Ba',
  3 => 'Thứ Tư',
  4 => 'Thứ Năm',
  5 => 'Thứ Sáu',
  6 => 'Thứ Bảy',
  _ => 'Chủ Nhật',
};
String _statusLabel(String? status) => switch (status?.toUpperCase()) {
  'PRESENT' => 'Có mặt',
  'ABSENT' => 'Vắng',
  'EXCUSED' => 'Có phép',
  'LATE' => 'Đi muộn',
  'PENDING' => 'Chờ duyệt',
  'APPROVED' => 'Đã duyệt',
  'REJECTED' => 'Từ chối',
  'CANCELLED' => 'Đã hủy',
  'ACTIVE' => 'Đang học',
  'TRANSFERRED' => 'Đã chuyển trường',
  'GRADUATED' => 'Đã tốt nghiệp',
  'COMPLETED' => 'Đã hoàn thành',
  'WITHDRAWN' => 'Đã thôi học',
  _ => status ?? 'Không rõ',
};
IconData _statusIcon(String? status) => switch (status?.toUpperCase()) {
  'PRESENT' => Icons.check_circle,
  'ABSENT' => Icons.cancel,
  'EXCUSED' => Icons.assignment_turned_in,
  'LATE' => Icons.schedule,
  _ => Icons.help_outline,
};
Color _statusColor(String? status) => switch (status?.toUpperCase()) {
  'PRESENT' => const Color(0xFF169B62),
  'ABSENT' => const Color(0xFFD74747),
  'EXCUSED' => const Color(0xFF3978C5),
  'LATE' => const Color(0xFFD68A00),
  _ => AppColors.mutedText,
};
String _genderLabel(String? value) => switch (value?.toUpperCase()) {
  'MALE' => 'Nam',
  'FEMALE' => 'Nữ',
  'OTHER' => 'Khác',
  _ => 'Chưa có thông tin',
};
