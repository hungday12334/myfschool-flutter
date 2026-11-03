import 'package:flutter/material.dart';

import '../../../../core/constants/app_colors.dart';
import '../../../../core/network/api_client.dart';
import '../../../auth/data/models/login_response.dart';
import '../../data/services/teacher_api_service.dart';

class TeacherHomeScreen extends StatefulWidget {
  const TeacherHomeScreen({required this.user, this.service, super.key});

  final AuthenticatedUser user;
  final TeacherService? service;

  @override
  State<TeacherHomeScreen> createState() => _TeacherHomeScreenState();
}

class _TeacherHomeScreenState extends State<TeacherHomeScreen> {
  late final TeacherService _service;
  int _selectedIndex = 0;
  bool _loading = false;
  String? _message;

  @override
  void initState() {
    super.initState();
    _service = widget.service ?? const TeacherApiService();
    _loadSelected();
  }

  Future<void> _loadSelected() async {
    setState(() {
      _loading = true;
      _message = null;
    });
    try {
      switch (_selectedIndex) {
        case 0:
          await _service.loadOverview();
        case 1:
          await _service.loadSchedule();
        case 2:
          await _service.loadClasses();
        case 3:
          await _service.loadStudents();
        case 4:
          await _service.loadStudents();
      }
    } on ApiException catch (error) {
      _message = error.message;
    } catch (_) {
      _message = 'Không thể tải dữ liệu. Vui lòng thử lại.';
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final titles = [
      'Tổng quan',
      'Thời khóa biểu',
      'Lớp của tôi',
      'Học sinh',
      'Điểm danh',
    ];
    return Scaffold(
      backgroundColor: const Color(0xFFFAFBFC),
      appBar: AppBar(
        title: const Text('MyFSchool'),
        actions: [
          IconButton(
            tooltip: 'Tải lại',
            onPressed: _loading ? null : _loadSelected,
            icon: const Icon(Icons.refresh_rounded),
          ),
        ],
      ),
      body: SafeArea(
        child: RefreshIndicator(
          onRefresh: _loadSelected,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(20, 12, 20, 28),
            children: [
              if (_selectedIndex == 0) ...[
                _WelcomeCard(name: widget.user.fullName),
                const SizedBox(height: 18),
                Text(
                  'Chức năng giáo viên',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                const SizedBox(height: 12),
                _QuickLinks(
                  onSelect: (index) => setState(() {
                    _selectedIndex = index;
                    _loadSelected();
                  }),
                ),
                const SizedBox(height: 18),
              ] else
                Text(
                  titles[_selectedIndex],
                  style: Theme.of(context).textTheme.headlineSmall,
                ),
              if (_loading)
                const Padding(
                  padding: EdgeInsets.all(36),
                  child: Center(child: CircularProgressIndicator()),
                )
              else if (_message != null)
                _UnavailableCard(message: _message!, onRetry: _loadSelected)
              else
                const _UnavailableCard(
                  message: 'Chưa có dữ liệu cho mục này.',
                  onRetry: null,
                ),
            ],
          ),
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _selectedIndex,
        onDestinationSelected: (index) {
          setState(() => _selectedIndex = index);
          _loadSelected();
        },
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.dashboard_outlined),
            label: 'Tổng quan',
          ),
          NavigationDestination(
            icon: Icon(Icons.calendar_month_outlined),
            label: 'Lịch dạy',
          ),
          NavigationDestination(
            icon: Icon(Icons.groups_outlined),
            label: 'Lớp',
          ),
          NavigationDestination(
            icon: Icon(Icons.school_outlined),
            label: 'Học sinh',
          ),
          NavigationDestination(
            icon: Icon(Icons.fact_check_outlined),
            label: 'Điểm danh',
          ),
        ],
      ),
    );
  }
}

class _WelcomeCard extends StatelessWidget {
  const _WelcomeCard({required this.name});
  final String name;

  @override
  Widget build(BuildContext context) => Container(
    padding: const EdgeInsets.all(20),
    decoration: BoxDecoration(
      gradient: const LinearGradient(
        colors: [AppColors.primaryOrange, AppColors.deepOrange],
      ),
      borderRadius: BorderRadius.circular(20),
    ),
    child: Row(
      children: [
        const CircleAvatar(
          radius: 27,
          backgroundColor: Colors.white,
          child: Icon(Icons.person, color: AppColors.deepOrange, size: 30),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Xin chào, giáo viên',
                style: TextStyle(color: Colors.white70),
              ),
              const SizedBox(height: 4),
              Text(
                name,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w700,
                  fontSize: 19,
                ),
              ),
            ],
          ),
        ),
      ],
    ),
  );
}

class _QuickLinks extends StatelessWidget {
  const _QuickLinks({required this.onSelect});
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) => GridView.count(
    crossAxisCount: 2,
    shrinkWrap: true,
    physics: const NeverScrollableScrollPhysics(),
    mainAxisSpacing: 10,
    crossAxisSpacing: 10,
    childAspectRatio: 2.5,
    children: const [
      _QuickLink(label: 'Lịch dạy', icon: Icons.calendar_month, index: 1),
      _QuickLink(label: 'Lớp phụ trách', icon: Icons.groups, index: 2),
      _QuickLink(label: 'Danh sách học sinh', icon: Icons.school, index: 3),
      _QuickLink(label: 'Điểm danh', icon: Icons.fact_check, index: 4),
    ].map((item) => _QuickLinkButton(item: item, onSelect: onSelect)).toList(),
  );
}

class _QuickLink {
  const _QuickLink({
    required this.label,
    required this.icon,
    required this.index,
  });
  final String label;
  final IconData icon;
  final int index;
}

class _QuickLinkButton extends StatelessWidget {
  const _QuickLinkButton({required this.item, required this.onSelect});
  final _QuickLink item;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) => Card(
    child: InkWell(
      borderRadius: BorderRadius.circular(12),
      onTap: () => onSelect(item.index),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12),
        child: Row(
          children: [
            Icon(item.icon, color: AppColors.deepOrange),
            const SizedBox(width: 9),
            Expanded(
              child: Text(
                item.label,
                style: const TextStyle(fontWeight: FontWeight.w600),
              ),
            ),
          ],
        ),
      ),
    ),
  );
}

class _UnavailableCard extends StatelessWidget {
  const _UnavailableCard({required this.message, required this.onRetry});
  final String message;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) => Card(
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          const Icon(
            Icons.info_outline_rounded,
            color: AppColors.deepOrange,
            size: 36,
          ),
          const SizedBox(height: 10),
          Text(message, textAlign: TextAlign.center),
          if (onRetry != null) ...[
            const SizedBox(height: 10),
            TextButton.icon(
              onPressed: onRetry,
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
            ),
          ],
        ],
      ),
    ),
  );
}
