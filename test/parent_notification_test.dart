import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:myschoolse1947/features/parent/data/services/parent_api_service.dart';
import 'package:myschoolse1947/features/parent/data/services/parent_notification_service.dart';

void main() {
  test('unconfigured messaging makes no device registration claim', () async {
    final api = _Api();
    final service = ParentNotificationService(api: api);
    expect(service.isConfigured, isFalse);
    await service.start();
    await service.dispose();
    expect(api.registered, isEmpty);
    expect(api.removed, isEmpty);
  });

  test(
    'deduplicates tokens, rotates registration and cleans up on logout',
    () async {
      final api = _Api();
      final source = _Source();
      final service = ParentNotificationService(api: api, source: source);
      await service.start();
      await service.start();
      source.refresh.add('first');
      source.refresh.add('second');
      source.refresh.add('second');
      await _drain();
      expect(api.registered, ['first', 'second']);
      expect(api.removed, ['first']);
      await service.stop();
      expect(api.removed, ['first', 'second']);
      expect(source.deleted, 1);
      source.refresh.add('after-logout');
      await _drain();
      expect(api.registered, ['first', 'second']);
      await service.dispose();
      expect(source.deleted, 1);
      await source.close();
    },
  );

  test('failed registration can be retried and logout revokes token on API failure', () async {
    final api = _Api()..failRegister = true;
    final source = _Source();
    final service = ParentNotificationService(api: api, source: source);
    final failures = <ParentNotificationFailure>[];
    final subscription = service.failures.listen(failures.add);
    await service.start();
    expect(api.registered, isEmpty);
    api.failRegister = false;
    source.refresh.add('first');
    await _drain();
    expect(api.registered, ['first']);
    api.failRemove = true;
    await service.stop();
    expect(source.deleted, 1);
    await _drain();
    expect(failures, contains(ParentNotificationFailure.registration));
    expect(failures, contains(ParentNotificationFailure.removal));
    await subscription.cancel();
    await service.dispose();
    await source.close();
  });

  test(
    'logout ignores a token returned by an earlier permission request',
    () async {
      final api = _Api();
      final pending = Completer<String?>();
      final source = _Source()..tokenFuture = pending.future;
      final service = ParentNotificationService(api: api, source: source);
      final started = service.start();
      await _drain();
      final stopped = service.stop();
      pending.complete('late-token');
      await started;
      await stopped;
      expect(api.registered, isEmpty);
      expect(source.deleted, 1);
      await service.dispose();
      await source.close();
    },
  );

  test(
    'accepts only validated targets and forwards foreground content',
    () async {
      final source = _Source();
      final service = ParentNotificationService(api: _Api(), source: source);
      final taps = <ParentNotificationTarget>[];
      final foreground = <ParentForegroundNotification>[];
      final tapSubscription = service.taps.listen(taps.add);
      final messageSubscription = service.foregroundMessages.listen(
        foreground.add,
      );
      await service.start();
      source.opened.add({'type': 'ANNOUNCEMENT', 'announcementId': '34'});
      source.opened.add({'type': 'ANNOUNCEMENT', 'announcementId': '-1'});
      source.opened.add({'type': 'OTHER', 'requestId': '34'});
      source.foreground.add({
        'notification': {'title': 'Thông báo mới', 'body': 'Nội dung'},
        'data': {'type': 'LEAVE_REQUEST', 'requestId': '12'},
      });
      await _drain();
      expect(taps, hasLength(1));
      expect(taps.single.id, 34);
      expect(taps.single.kind, ParentNotificationKind.announcement);
      expect(foreground.single.title, 'Thông báo mới');
      expect(
        foreground.single.target?.kind,
        ParentNotificationKind.leaveRequest,
      );
      expect(foreground.single.target?.id, 12);
      await tapSubscription.cancel();
      await messageSubscription.cancel();
      await service.dispose();
      await source.close();
    },
  );
}

Future<void> _drain() async {
  await Future<void>.delayed(Duration.zero);
  await Future<void>.delayed(Duration.zero);
}

class _Api implements ParentApi {
  final registered = <String>[];
  final removed = <String>[];
  bool failRegister = false;
  bool failRemove = false;
  @override
  Future<void> registerDevice({
    required String token,
    required String platform,
    String? deviceName,
  }) async {
    expect(platform, 'ANDROID');
    if (failRegister) throw StateError('offline');
    registered.add(token);
  }

  @override
  Future<void> removeDevice(String token) async {
    if (failRemove) throw StateError('offline');
    removed.add(token);
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Source implements ParentPushTokenSource {
  final refresh = StreamController<String>();
  final foreground = StreamController<Map<String, dynamic>>();
  final opened = StreamController<Map<String, dynamic>>();
  Future<String?>? tokenFuture;
  int deleted = 0;
  @override
  bool get supported => true;
  @override
  String get platform => 'ANDROID';
  @override
  String? get deviceName => 'Test device';
  @override
  Future<String?> requestPermissionAndToken() async => tokenFuture ?? 'first';
  @override
  Stream<String> get tokenRefresh => refresh.stream;
  @override
  Stream<Map<String, dynamic>> get foregroundMessages => foreground.stream;
  @override
  Stream<Map<String, dynamic>> get openedMessages => opened.stream;
  @override
  Future<Map<String, dynamic>?> initialMessage() async => null;
  @override
  Future<void> deleteToken() async => deleted++;
  Future<void> close() async {
    await refresh.close();
    await foreground.close();
    await opened.close();
  }
}
