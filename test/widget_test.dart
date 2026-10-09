import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:myschoolse1947/main.dart';

void main() {
  testWidgets('Login screen smoke test', (WidgetTester tester) async {
    await tester.pumpWidget(const MyFSchoolApp());

    expect(find.text('MyFSchool'), findsOneWidget);
    expect(find.text('S\u1ed1 \u0111i\u1ec7n tho\u1ea1i'), findsOneWidget);
    expect(find.text('M\u1eadt kh\u1ea9u'), findsOneWidget);
    expect(find.text('\u0110\u0103ng nh\u1eadp'), findsOneWidget);
  });

  testWidgets('Hard-coded student account opens home screen', (
    WidgetTester tester,
  ) async {
    await tester.pumpWidget(const MyFSchoolApp());

    await tester.enterText(
      find.widgetWithText(TextField, 'S\u1ed1 \u0111i\u1ec7n tho\u1ea1i'),
      '0123456789',
    );
    await tester.enterText(
      find.widgetWithText(TextField, 'M\u1eadt kh\u1ea9u'),
      '123456',
    );
    await tester.tap(find.text('\u0110\u0103ng nh\u1eadp'));
    await tester.pumpAndSettle();

    expect(find.text('Nguy\u1ec5n Minh Anh'), findsOneWidget);
    expect(find.text('Trang ch\u1ee7'), findsOneWidget);
  });

  testWidgets('Login screen fits common mobile sizes', (
    WidgetTester tester,
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

      await tester.pumpWidget(const MyFSchoolApp());
      await tester.pump();

      expect(tester.takeException(), isNull);
    }
  });
}
