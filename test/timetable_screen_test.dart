import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/features/home/presentation/screens/parent_home_screen.dart';
import 'package:myschoolse1947/features/timetable/data/models/timetable_week.dart';
import 'package:myschoolse1947/features/timetable/data/services/timetable_api_service.dart';
import 'package:myschoolse1947/features/timetable/presentation/screens/timetable_screen.dart';

void main() {
  testWidgets('renders selected day lessons and derives the lunch break', (
    WidgetTester tester,
  ) async {
    final monday = DateTime(2026, 10, 5);
    final service = _FakeTimetableService(
      (weekStart, _) async => _buildWeek(
        weekStart,
        lessonsByOffset: <int, List<TimetableLesson>>{4: _sampleLessons},
      ),
    );

    await tester.pumpWidget(
      MaterialApp(
        home: TimetableScreen(
          service: service,
          initialDate: DateTime(2026, 10, 9),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(service.requestedWeeks, <DateTime>[monday]);
    expect(find.text('Thứ 6, ngày 09 tháng 10 năm 2026'), findsOneWidget);
    expect(find.text('Toán'), findsOneWidget);
    expect(find.text('Cô Nguyễn Thị Lan'), findsOneWidget);
    expect(find.text('Phòng 101'), findsWidgets);
    expect(find.text('Nghỉ trưa'), findsOneWidget);
    expect(find.text('11:15'), findsWidgets);
    expect(find.text('13:30'), findsWidgets);

    await tester.tap(find.text('05/10'));
    await tester.pump();
    expect(find.text('Không có lịch học trong ngày này.'), findsOneWidget);
  });

  testWidgets('switches to next week and keeps the selected weekday', (
    WidgetTester tester,
  ) async {
    final service = _FakeTimetableService((weekStart, _) async {
      final isNextWeek = weekStart == DateTime(2026, 10, 12);
      return _buildWeek(
        weekStart,
        lessonsByOffset: <int, List<TimetableLesson>>{
          4: <TimetableLesson>[
            _lesson(
              id: isNextWeek ? 20 : 10,
              subjectCode: isNextWeek ? 'CHEM' : 'MATH',
              subjectName: isNextWeek ? 'Hóa học' : 'Toán',
              startTime: '07:30',
              endTime: '08:15',
            ),
          ],
        },
      );
    });

    await tester.pumpWidget(
      MaterialApp(
        home: TimetableScreen(
          service: service,
          initialDate: DateTime(2026, 10, 9),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Toán'), findsOneWidget);

    await tester.tap(find.text('Tuần sau'));
    await tester.pumpAndSettle();

    expect(service.requestedWeeks.last, DateTime(2026, 10, 12));
    expect(find.text('Thứ 6, ngày 16 tháng 10 năm 2026'), findsOneWidget);
    expect(find.text('Hóa học'), findsOneWidget);
  });

  testWidgets('shows an error and retries the request', (
    WidgetTester tester,
  ) async {
    final service = _FakeTimetableService((weekStart, callNumber) async {
      if (callNumber == 1) {
        throw const ApiException(message: 'Mất kết nối đến máy chủ.');
      }
      return _buildWeek(
        weekStart,
        lessonsByOffset: <int, List<TimetableLesson>>{
          4: <TimetableLesson>[
            _lesson(
              id: 30,
              subjectCode: 'IT',
              subjectName: 'Tin học',
              startTime: '14:30',
              endTime: '15:15',
            ),
          ],
        },
      );
    });

    await tester.pumpWidget(
      MaterialApp(
        home: TimetableScreen(
          service: service,
          initialDate: DateTime(2026, 10, 9),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Mất kết nối đến máy chủ.'), findsOneWidget);
    await tester.tap(find.text('Thử lại'));
    await tester.pumpAndSettle();

    expect(service.requestedWeeks, hasLength(2));
    expect(find.text('Tin học'), findsOneWidget);
  });

  testWidgets('home timetable tile opens the timetable screen', (
    WidgetTester tester,
  ) async {
    final service = _FakeTimetableService(
      (weekStart, _) async => _buildWeek(weekStart),
    );
    await tester.pumpWidget(
      MaterialApp(home: ParentHomeScreen(timetableService: service)),
    );

    await tester.tap(find.text('Thời khóa biểu'));
    await tester.pumpAndSettle();

    expect(find.byType(TimetableScreen), findsOneWidget);
    expect(service.requestedWeeks, hasLength(1));
  });

  testWidgets('timetable layout fits a narrow phone', (
    WidgetTester tester,
  ) async {
    final view = tester.view;
    addTearDown(() {
      view.resetPhysicalSize();
      view.resetDevicePixelRatio();
    });
    view
      ..devicePixelRatio = 1
      ..physicalSize = const Size(320, 568);

    final service = _FakeTimetableService(
      (weekStart, _) async => _buildWeek(
        weekStart,
        lessonsByOffset: <int, List<TimetableLesson>>{4: _sampleLessons},
      ),
    );
    await tester.pumpWidget(
      MaterialApp(
        home: TimetableScreen(
          service: service,
          initialDate: DateTime(2026, 10, 9),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
  });
}

final List<TimetableLesson> _sampleLessons = <TimetableLesson>[
  _lesson(
    id: 1,
    periodNumber: 1,
    subjectCode: 'MATH',
    subjectName: 'Toán',
    teacherName: 'Cô Nguyễn Thị Lan',
    room: 'Phòng 101',
    startTime: '07:30',
    endTime: '08:15',
  ),
  _lesson(
    id: 2,
    periodNumber: 4,
    subjectCode: 'LIT',
    subjectName: 'Ngữ văn',
    teacherName: 'Cô Phạm Thị Mai',
    room: 'Phòng 101',
    startTime: '10:30',
    endTime: '11:15',
  ),
  _lesson(
    id: 3,
    periodNumber: 5,
    subjectCode: 'CHEM',
    subjectName: 'Hóa học',
    teacherName: 'Thầy Hoàng Anh Tuấn',
    room: 'Phòng 104',
    startTime: '13:30',
    endTime: '14:15',
  ),
];

TimetableLesson _lesson({
  required int id,
  int periodNumber = 1,
  required String subjectCode,
  required String subjectName,
  String teacherName = 'Giáo viên',
  String room = 'Phòng 101',
  required String startTime,
  required String endTime,
}) {
  return TimetableLesson(
    id: id,
    periodNumber: periodNumber,
    subjectCode: subjectCode,
    subjectName: subjectName,
    teacherName: teacherName,
    room: room,
    startTime: startTime,
    endTime: endTime,
  );
}

TimetableWeek _buildWeek(
  DateTime monday, {
  Map<int, List<TimetableLesson>> lessonsByOffset =
      const <int, List<TimetableLesson>>{},
}) {
  return TimetableWeek(
    weekStart: monday,
    weekEnd: monday.add(const Duration(days: 6)),
    days: List<TimetableDay>.generate(7, (index) {
      final date = monday.add(Duration(days: index));
      return TimetableDay(
        date: date,
        dayOfWeek: _dayOfWeek(date.weekday),
        lessons: lessonsByOffset[index] ?? const <TimetableLesson>[],
      );
    }),
  );
}

String _dayOfWeek(int weekday) => switch (weekday) {
  DateTime.monday => 'MONDAY',
  DateTime.tuesday => 'TUESDAY',
  DateTime.wednesday => 'WEDNESDAY',
  DateTime.thursday => 'THURSDAY',
  DateTime.friday => 'FRIDAY',
  DateTime.saturday => 'SATURDAY',
  _ => 'SUNDAY',
};

class _FakeTimetableService implements TimetableService {
  _FakeTimetableService(this._fetch);

  final Future<TimetableWeek> Function(DateTime weekStart, int callNumber)
  _fetch;
  final List<DateTime> requestedWeeks = <DateTime>[];

  @override
  Future<TimetableWeek> fetchWeek(DateTime weekStart) {
    requestedWeeks.add(weekStart);
    return _fetch(weekStart, requestedWeeks.length);
  }
}
