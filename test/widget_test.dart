import 'package:flutter_test/flutter_test.dart';

import 'package:need/main.dart';

void main() {
  testWidgets('Need dashboard renders', (WidgetTester tester) async {
    await tester.pumpWidget(const NeedApp());
    await tester.pump();

    expect(find.text('Need'), findsOneWidget);
    expect(find.text('Search installed apps'), findsOneWidget);
  });
}
