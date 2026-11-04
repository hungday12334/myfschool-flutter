import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';
import 'package:myschoolse1947/core/network/api_client.dart';
import 'package:myschoolse1947/core/storage/token_storage.dart';
import 'package:myschoolse1947/features/auth/data/models/login_response.dart';
import 'package:myschoolse1947/features/home/presentation/screens/parent_home_screen.dart';
import 'package:myschoolse1947/features/parent/data/models/parent_models.dart';
import 'package:myschoolse1947/features/parent/data/services/parent_api_service.dart';

void main() {
  testWidgets('one child is selected and dashboard renders API data', (
    tester,
  ) async {
    final api = _FakeParentApi(linkedChildren: [_child(1, 'An')]);
    await tester.pumpWidget(
      MaterialApp(
        home: ParentHomeScreen(
          parentApi: api,
          user: const AuthenticatedUser(
            id: 7,
            username: 'parent',
            roles: ['PARENT'],
          ),
        ),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('An'), findsWidgets);
    expect(find.text('Đã liên kết 1 học sinh'), findsNothing);
    expect(find.text('Có mặt'), findsOneWidget);
    expect(api.dashboardRequests, [1]);
  });

  testWidgets(
    'parent can switch children and the dashboard reloads for the new child',
    (tester) async {
      final api = _FakeParentApi(
        linkedChildren: [_child(1, 'An'), _child(2, 'Bình')],
      );
      await tester.pumpWidget(
        MaterialApp(home: ParentHomeScreen(parentApi: api)),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byType(DropdownButton<int>));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Bình · S2').last);
      await tester.pumpAndSettle();

      expect(api.contextRequests, containsAll([1, 2]));
      expect(api.dashboardRequests.last, 2);
      expect(find.text('Bình'), findsWidgets);
    },
  );

  testWidgets('parent with no linked children sees an empty state', (
    tester,
  ) async {
    await tester.pumpWidget(
      MaterialApp(
        home: ParentHomeScreen(
          parentApi: _FakeParentApi(linkedChildren: const []),
        ),
      ),
    );
    await tester.pumpAndSettle();
    expect(find.text('Chưa có học sinh được liên kết'), findsOneWidget);
  });

  test('parent API uses backend route and parses the data wrapper', () async {
    final client = MockClient((request) async {
      expect(request.url.path, '/api/parent/children');
      expect(request.headers['authorization'], 'Bearer jwt');
      return http.Response.bytes(
        utf8.encode(
          jsonEncode({
            'code': 200,
            'message': 'OK',
            'data': [
              {
                'id': 3,
                'fullName': 'Chi',
                'studentCode': 'S3',
                'school': {'name': 'Trường A'},
                'class': {'name': '9A'},
              },
            ],
          }),
        ),
        200,
        headers: {'content-type': 'application/json; charset=utf-8'},
      );
    });
    final api = ParentApiService(
      apiClient: ApiClient(
        httpClient: client,
        baseUri: Uri.parse('https://example.test/'),
      ),
      tokenStorage: _FakeTokenStorage(),
    );
    addTearDown(api.close);
    final children = await api.children();
    expect(children.single.fullName, 'Chi');
    expect(children.single.studentClass?.name, '9A');
  });

  test('missing optional model fields parse safely', () {
    final child = ParentChild.fromJson({
      'id': 4,
      'fullName': 'Dũng',
      'studentCode': 'S4',
    });
    expect(child.school, isNull);
    expect(child.studentClass, isNull);
    expect(child.avatar, isNull);
  });
}

ParentChild _child(int id, String name) => ParentChild(
  id: id,
  fullName: name,
  studentCode: 'S$id',
  school: const SchoolInfo(name: 'Trường A'),
);

class _FakeParentApi implements ParentApi {
  _FakeParentApi({required this.linkedChildren});
  final List<ParentChild> linkedChildren;
  final List<int?> dashboardRequests = [];
  final List<int> contextRequests = [];
  @override
  Future<List<ParentChild>> children() async => linkedChildren;
  @override
  Future<StudentProfile> childContext(int studentId) async {
    contextRequests.add(studentId);
    final child = linkedChildren.singleWhere((item) => item.id == studentId);
    return StudentProfile(child: child);
  }

  @override
  Future<ParentDashboard> dashboard(int? studentId) async {
    dashboardRequests.add(studentId);
    final child = studentId == null
        ? null
        : linkedChildren.singleWhere((item) => item.id == studentId);
    return ParentDashboard(
      parentName: 'Phụ huynh',
      children: linkedChildren,
      selectedStudent: child == null ? null : StudentProfile(child: child),
      recentAnnouncements: const [],
      attendanceSummary: const AttendanceSummary(totalSessions: 1, present: 1),
      academicSummary: const [],
      pendingLeaveRequests: 0,
    );
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeTokenStorage extends TokenStorage {
  @override
  Future<String?> readAccessToken() async => 'jwt';
  @override
  Future<void> clearAccessToken() async {}
}
