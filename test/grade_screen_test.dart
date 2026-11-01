import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myschoolse1947/features/grades/data/models/grade_report.dart';
import 'package:myschoolse1947/features/grades/data/services/grade_api_service.dart';
import 'package:myschoolse1947/features/grades/presentation/screens/grade_screen.dart';
import 'package:myschoolse1947/features/home/presentation/screens/parent_home_screen.dart';

void main() {
  testWidgets('parent home opens grade report and subject detail', (
    tester,
  ) async {
    final service = _FakeGradeService();
    await tester.pumpWidget(
      MaterialApp(home: ParentHomeScreen(gradeService: service)),
    );

    await tester.tap(find.text('Kết quả'));
    await tester.pumpAndSettle();

    expect(find.byType(GradeScreen), findsOneWidget);
    expect(find.text('Nguyễn Minh Anh'), findsOneWidget);
    expect(find.text('Toán'), findsOneWidget);
    expect(find.text('8.9'), findsOneWidget);

    await tester.tap(find.byKey(const Key('subject-MATH')));
    await tester.pumpAndSettle();

    expect(find.byType(GradeDetailScreen), findsOneWidget);
    expect(find.text('Điểm thành phần'), findsOneWidget);
    expect(find.text('Hệ số 3'), findsOneWidget);
    expect(find.byKey(const Key('grade-chart')), findsOneWidget);
    expect(service.lastSubjectCode, 'MATH');
  });

  testWidgets('changing period reloads report with semester 2', (tester) async {
    final service = _FakeGradeService();
    await tester.pumpWidget(MaterialApp(home: GradeScreen(service: service)));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('grade-period-2')));
    await tester.pumpAndSettle();

    expect(service.periods, <GradePeriod>[
      GradePeriod.semester1,
      GradePeriod.semester2,
    ]);
  });

  testWidgets('grade list and detail fit common mobile sizes', (tester) async {
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
      final service = _FakeGradeService();
      await tester.pumpWidget(
        MaterialApp(
          key: UniqueKey(),
          home: GradeScreen(service: service),
        ),
      );
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);

      await tester.tap(find.byKey(const Key('subject-MATH')));
      await tester.pumpAndSettle();
      expect(tester.takeException(), isNull);
    }
  });
}

const _student = StudentSummary(
  studentCode: 'HS001',
  fullName: 'Nguyễn Minh Anh',
  className: '10A1',
  schoolName: 'Trường THPT FPT',
);

const _subject = SubjectGradeSummary(
  subjectCode: 'MATH',
  subjectName: 'Toán',
  averageScore: 8.9,
);

const _components = <GradeComponent>[
  GradeComponent(
    id: 1,
    type: 'ORAL',
    label: 'Miệng',
    coefficient: 1,
    score: 8,
    semester: 1,
  ),
  GradeComponent(
    id: 2,
    type: 'FIFTEEN_MINUTES',
    label: '15 phút',
    coefficient: 1,
    score: 7.5,
    semester: 1,
  ),
  GradeComponent(
    id: 3,
    type: 'ONE_PERIOD',
    label: '1 tiết',
    coefficient: 2,
    score: 9,
    semester: 1,
  ),
  GradeComponent(
    id: 4,
    type: 'SEMESTER',
    label: 'Học kỳ',
    coefficient: 3,
    score: 9.5,
    semester: 1,
  ),
];

class _FakeGradeService implements GradeService {
  final List<GradePeriod> periods = <GradePeriod>[];
  String? lastSubjectCode;

  @override
  Future<GradeReport> fetchReport(GradePeriod period) async {
    periods.add(period);
    return GradeReport(
      student: _student,
      academicYear: '2026-2027',
      period: period.name,
      subjects: const <SubjectGradeSummary>[_subject],
    );
  }

  @override
  Future<SubjectGradeDetail> fetchSubject({
    required String subjectCode,
    required GradePeriod period,
  }) async {
    lastSubjectCode = subjectCode;
    return const SubjectGradeDetail(
      student: _student,
      academicYear: '2026-2027',
      period: 'SEMESTER_1',
      subjectCode: 'MATH',
      subjectName: 'Toán',
      averageScore: 8.9,
      assessments: _components,
    );
  }
}
