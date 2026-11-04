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
  late final TeacherService _service = widget.service ?? TeacherApiService();
  int _tab = 0;
  bool _loading = false, _saving = false;
  String? _error;
  Map<String, dynamic>? _profile;
  List<TeacherClass> _classes = [];
  List<TeacherLesson> _lessons = [];
  List<TeacherStudent> _students = [];
  List<TeacherAttendance> _attendance = [];
  int? _classId;
  DateTime _date = DateTime.now();
  final _search = TextEditingController();
  final _notes = <int, TextEditingController>{};

  @override
  void initState() {
    super.initState();
    _load();
  }

  @override
  void dispose() {
    _search.dispose();
    for (final c in _notes.values) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _load() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      if (_tab == 0) {
        _profile = await _service.loadOverview();
        _classes = await _service.loadClasses();
        _lessons = await _service.loadSchedule(_monday(_date));
      } else if (_tab == 1) {
        _lessons = await _service.loadSchedule(_monday(_date));
      } else {
        _classes = await _service.loadClasses();
        _classId ??= _classes.isEmpty ? null : _classes.first.assignmentId;
        if (_classId != null) {
          if (_tab == 2) {
            _students = await _service.loadStudents(
              _classId!,
              search: _search.text,
            );
          }
          if (_tab == 3) {
            _attendance = await _service.loadAttendance(_classId!, _date);
          }
        }
      }
    } on ApiException catch (e) {
      _error = e.message;
    } catch (_) {
      _error = 'Không thể tải dữ liệu. Vui lòng thử lại.';
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  DateTime _monday(DateTime d) =>
      DateTime(d.year, d.month, d.day).subtract(Duration(days: d.weekday - 1));
  Future<void> _selectDate() async {
    final picked = await showDatePicker(
      context: context,
      initialDate: _date,
      firstDate: DateTime(2020),
      lastDate: DateTime(2035),
    );
    if (picked != null) {
      setState(() => _date = picked);
      await _load();
    }
  }

  Future<void> _saveAttendance() async {
    if (_classId == null) return;
    setState(() => _saving = true);
    try {
      for (final row in _attendance) {
        row.note = _notes[row.studentId]?.text.trim();
      }
      _attendance = await _service.saveAttendance(
        _classId!,
        _date,
        _attendance,
      );
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('Đã lưu điểm danh.')));
      }
    } on ApiException catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(e.message)));
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Không thể lưu điểm danh. Vui lòng thử lại.'),
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    const titles = ['Tổng quan', 'Lịch dạy', 'Lớp của tôi', 'Điểm danh'];
    return Scaffold(
      backgroundColor: const Color(0xFFF7F8FA),
      appBar: AppBar(
        title: const Text('MyFSchool'),
        actions: [
          IconButton(
            onPressed: _loading ? null : _load,
            icon: const Icon(Icons.refresh),
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          padding: const EdgeInsets.all(18),
          children: [
            if (_tab == 0)
              _welcome(_profile?['fullName'] as String? ?? widget.user.fullName)
            else
              Text(
                titles[_tab],
                style: Theme.of(context).textTheme.headlineSmall,
              ),
            if (_tab == 0) ...[
              _section('Truy cập nhanh'),
              _quickLinks(),
              _section('Lịch tuần này'),
            ],
            if (_tab == 1 || _tab == 3) _datePicker(),
            if (_tab == 2 || _tab == 3) _classPicker(),
            if (_loading)
              const Padding(
                padding: EdgeInsets.all(40),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (_error != null)
              _errorCard(_error!)
            else if (_tab == 0 || _tab == 1)
              _scheduleList()
            else if (_tab == 2)
              _studentList()
            else
              _attendanceList(),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _tab,
        onDestinationSelected: (i) {
          setState(() {
            _tab = i;
          });
          _load();
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
            icon: Icon(Icons.fact_check_outlined),
            label: 'Điểm danh',
          ),
        ],
      ),
      floatingActionButton: _tab == 3 && _attendance.isNotEmpty
          ? FloatingActionButton.extended(
              onPressed: _saving ? null : _saveAttendance,
              icon: _saving
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.save_outlined),
              label: const Text('Lưu điểm danh'),
            )
          : null,
    );
  }

  Widget _welcome(String name) => Container(
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
          backgroundColor: Colors.white,
          child: Icon(Icons.person, color: AppColors.deepOrange),
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
              Text(
                name,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
      ],
    ),
  );
  Widget _section(String text) => Padding(
    padding: const EdgeInsets.only(top: 20, bottom: 10),
    child: Text(
      text,
      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
    ),
  );
  Widget _quickLinks() => Wrap(
    spacing: 9,
    runSpacing: 9,
    children: [
      for (final item in [
        (1, 'Lịch dạy', Icons.calendar_month),
        (2, 'Lớp của tôi', Icons.groups),
        (3, 'Điểm danh', Icons.fact_check),
      ])
        ActionChip(
          avatar: Icon(item.$3, size: 18),
          label: Text(item.$2),
          onPressed: () {
            setState(() => _tab = item.$1);
            _load();
          },
        ),
    ],
  );
  Widget _datePicker() => Align(
    alignment: Alignment.centerLeft,
    child: Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: OutlinedButton.icon(
        onPressed: _selectDate,
        icon: const Icon(Icons.event),
        label: Text('${_date.day}/${_date.month}/${_date.year}'),
      ),
    ),
  );
  Widget _classPicker() => Padding(
    padding: const EdgeInsets.symmetric(vertical: 12),
    child: _classes.isEmpty
        ? const Text('Bạn chưa được phân công lớp.')
        : DropdownButtonFormField<int>(
            initialValue: _classes.any((c) => c.assignmentId == _classId)
                ? _classId
                : _classes.first.assignmentId,
            decoration: const InputDecoration(
              labelText: 'Lớp phụ trách',
              border: OutlineInputBorder(),
            ),
            items: [
              for (final c in _classes)
                DropdownMenuItem(
                  value: c.assignmentId,
                  child: Text('${c.className} • ${c.schoolName}'),
                ),
            ],
            onChanged: (v) {
              setState(() => _classId = v);
              _load();
            },
          ),
  );
  Widget _scheduleList() => _lessons.isEmpty
      ? _empty('Tuần này chưa có tiết dạy.')
      : Column(
          children: [
            for (final l in _lessons)
              Card(
                child: ListTile(
                  leading: CircleAvatar(child: Text('${l.periodNumber}')),
                  title: Text(l.subjectName),
                  subtitle: Text(
                    '${l.date.day}/${l.date.month} • ${l.startTime} - ${l.endTime} • ${l.room}',
                  ),
                  trailing: Text(l.subjectCode),
                ),
              ),
          ],
        );
  Widget _studentList() => Column(
    children: [
      TextField(
        controller: _search,
        decoration: InputDecoration(
          prefixIcon: const Icon(Icons.search),
          hintText: 'Tìm theo tên hoặc mã học sinh',
          suffixIcon: IconButton(
            icon: const Icon(Icons.search),
            onPressed: _load,
          ),
        ),
        onSubmitted: (_) => _load(),
      ),
      const SizedBox(height: 10),
      if (_students.isEmpty)
        _empty('Không tìm thấy học sinh.')
      else
        for (final s in _students)
          Card(
            child: ListTile(
              leading: const CircleAvatar(child: Icon(Icons.person_outline)),
              title: Text(s.fullName),
              subtitle: Text('${s.studentCode} • ${s.className}'),
            ),
          ),
    ],
  );
  Widget _attendanceList() => _attendance.isEmpty
      ? _empty('Lớp chưa có học sinh.')
      : Column(
          children: [
            for (final row in _attendance)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    children: [
                      Row(
                        children: [
                          const CircleAvatar(child: Icon(Icons.person_outline)),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  row.fullName,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                                Text(row.studentCode),
                              ],
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 8),
                      DropdownButtonFormField<String>(
                        initialValue: row.status,
                        decoration: const InputDecoration(
                          labelText: 'Trạng thái',
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                        items: const [
                          DropdownMenuItem(
                            value: 'PRESENT',
                            child: Text('Có mặt'),
                          ),
                          DropdownMenuItem(
                            value: 'LATE',
                            child: Text('Đi muộn'),
                          ),
                          DropdownMenuItem(
                            value: 'EXCUSED_ABSENCE',
                            child: Text('Vắng có phép'),
                          ),
                          DropdownMenuItem(
                            value: 'UNEXCUSED_ABSENCE',
                            child: Text('Vắng không phép'),
                          ),
                        ],
                        onChanged: (v) => setState(() => row.status = v),
                      ),
                      const SizedBox(height: 8),
                      TextField(
                        controller: _notes.putIfAbsent(
                          row.studentId,
                          () => TextEditingController(text: row.note ?? ''),
                        ),
                        decoration: const InputDecoration(
                          labelText: 'Ghi chú',
                          border: OutlineInputBorder(),
                          isDense: true,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        );
  Widget _empty(String message) => Card(
    child: Padding(
      padding: const EdgeInsets.all(22),
      child: Center(child: Text(message, textAlign: TextAlign.center)),
    ),
  );
  Widget _errorCard(String message) => Card(
    child: Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          const Icon(Icons.cloud_off_outlined, size: 36),
          const SizedBox(height: 8),
          Text(message, textAlign: TextAlign.center),
          TextButton.icon(
            onPressed: _load,
            icon: const Icon(Icons.refresh),
            label: const Text('Thử lại'),
          ),
        ],
      ),
    ),
  );
}
