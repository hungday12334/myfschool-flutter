import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myschoolse1947/features/attendance/data/models/attendance_report.dart';
import 'package:myschoolse1947/features/attendance/data/services/attendance_api_service.dart';
import 'package:myschoolse1947/features/attendance/presentation/screens/attendance_screen.dart';
import 'package:myschoolse1947/features/home/presentation/screens/parent_home_screen.dart';
import 'package:myschoolse1947/features/tuition/data/models/tuition_report.dart';
import 'package:myschoolse1947/features/tuition/data/services/tuition_api_service.dart';
import 'package:myschoolse1947/features/tuition/presentation/screens/tuition_screen.dart';

void main() {
  testWidgets('parent home opens attendance report and changes month', (
    tester,
  ) async {
    final service = _FakeAttendanceService();
    await tester.pumpWidget(
      MaterialApp(home: ParentHomeScreen(attendanceService: service)),
    );

    await tester.ensureVisible(find.text('Điểm danh'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Điểm danh'));
    await tester.pumpAndSettle();

    expect(find.byType(AttendanceScreen), findsOneWidget);
    expect(find.text('Tỷ lệ chuyên cần'), findsOneWidget);
    expect(find.text('50.0%'), findsOneWidget);
    expect(find.text('Nghỉ không phép'), findsWidgets);

    await tester.tap(find.byKey(const Key('attendance-previous-month')));
    await tester.pumpAndSettle();
    final now = DateTime.now();
    expect(service.months.last, DateTime(now.year, now.month - 1));
  });

  testWidgets('parent home opens tuition and switches paid/due tabs', (
    tester,
  ) async {
    final service = _FakeTuitionService();
    await tester.pumpWidget(
      MaterialApp(home: ParentHomeScreen(tuitionService: service)),
    );

    await tester.ensureVisible(find.text('Học phí'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Học phí'));
    await tester.pumpAndSettle();

    expect(find.byType(TuitionScreen), findsOneWidget);
    expect(find.text('Các khoản cần nộp'), findsOneWidget);
    expect(find.text('Phí bán trú'), findsOneWidget);
    expect(find.text('260.000 đ'), findsWidgets);

    await tester.tap(find.byKey(const Key('tuition-tab-paid')));
    await tester.pumpAndSettle();
    expect(find.text('Các khoản đã nộp'), findsOneWidget);
    expect(find.text('Học phí tháng 9'), findsOneWidget);
  });

  testWidgets('attendance and tuition screens fit common mobile widths', (
    tester,
  ) async {
    final view = tester.view;
    addTearDown(() {
      view.resetPhysicalSize();
      view.resetDevicePixelRatio();
    });

    for (final size in <Size>[
      const Size(320, 568),
      const Size(375, 667),
      const Size(390, 844),
    ]) {
      view
        ..devicePixelRatio = 1
        ..physicalSize = size;
      await tester.pumpWidget(
        MaterialApp(
          key: UniqueKey(),
          home: AttendanceScreen(
            service: _FakeAttendanceService(),
            initialMonth: DateTime(2026, 11),
          ),
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);

      await tester.pumpWidget(
        MaterialApp(
          key: UniqueKey(),
          home: TuitionScreen(service: _FakeTuitionService()),
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    }
  });
}

const _attendanceStudent = AttendanceStudent(
  studentCode: 'HS001',
  fullName: 'Nguyễn Minh Anh',
  className: '10A1',
  schoolName: 'Trường THPT FPT',
);

final _attendanceRecords = <AttendanceDay>[
  AttendanceDay(
    id: 1,
    date: _novemberThird,
    status: AttendanceStatus.present,
    checkInTime: '07:25',
  ),
  AttendanceDay(
    id: 2,
    date: _novemberFourth,
    status: AttendanceStatus.late,
    checkInTime: '07:48',
    note: 'Đến lớp sau giờ quy định',
  ),
  AttendanceDay(
    id: 3,
    date: _novemberFifth,
    status: AttendanceStatus.excusedAbsence,
    note: 'Nghỉ ốm có đơn',
  ),
  AttendanceDay(
    id: 4,
    date: _novemberSixth,
    status: AttendanceStatus.unexcusedAbsence,
  ),
];

final _novemberThird = DateTime(2026, 11, 3);
final _novemberFourth = DateTime(2026, 11, 4);
final _novemberFifth = DateTime(2026, 11, 5);
final _novemberSixth = DateTime(2026, 11, 6);

class _FakeAttendanceService implements AttendanceService {
  final List<DateTime> months = <DateTime>[];

  @override
  Future<AttendanceReport> fetchMonth(DateTime month) async {
    final normalized = DateTime(month.year, month.month);
    months.add(normalized);
    return AttendanceReport(
      student: _attendanceStudent,
      month: normalized,
      totalSchoolDays: 4,
      presentDays: 1,
      lateDays: 1,
      excusedAbsenceDays: 1,
      unexcusedAbsenceDays: 1,
      attendanceRate: 50,
      records: _attendanceRecords,
    );
  }
}

const _tuitionStudent = TuitionStudent(
  studentCode: 'HS001',
  fullName: 'Nguyễn Minh Anh',
  className: '10A1',
  schoolName: 'Trường THPT FPT',
);

final _paidItem = TuitionItem(
  id: 1,
  feeCode: 'PAID',
  title: 'Học phí tháng 9',
  academicYear: '2026-2027',
  dueDate: DateTime(2026, 9, 15),
  amount: 100000,
  paidAmount: 100000,
  remainingAmount: 0,
  paidDate: DateTime(2026, 9, 10),
  status: TuitionItemStatus.paid,
);

final _partialItem = TuitionItem(
  id: 2,
  feeCode: 'PARTIAL',
  title: 'Phí bán trú',
  academicYear: '2026-2027',
  dueDate: DateTime(2026, 11, 15),
  amount: 100000,
  paidAmount: 40000,
  remainingAmount: 60000,
  status: TuitionItemStatus.partiallyPaid,
);

final _dueItem = TuitionItem(
  id: 3,
  feeCode: 'DUE',
  title: 'Học phí học kỳ 2',
  academicYear: '2026-2027',
  dueDate: DateTime(2027, 1, 15),
  amount: 200000,
  paidAmount: 0,
  remainingAmount: 200000,
  status: TuitionItemStatus.due,
);

class _FakeTuitionService implements TuitionService {
  @override
  Future<TuitionReport> fetchReport() async {
    return TuitionReport(
      student: _tuitionStudent,
      academicYear: '2026-2027',
      totalAmount: 400000,
      totalPaid: 140000,
      totalDue: 260000,
      paidItems: <TuitionItem>[_paidItem],
      dueItems: <TuitionItem>[_partialItem, _dueItem],
    );
  }
}
