import 'dart:async';

import 'parent_api_service.dart';

/// Implement with the configured messaging SDK. No tokens are persisted by the
/// Parent feature, and an unavailable provider never pretends to deliver push.
abstract interface class ParentPushTokenSource {
  bool get supported;
  String get platform;
  String? get deviceName;
  Future<String?> requestPermissionAndToken();
  Stream<String> get tokenRefresh;
  Stream<Map<String, dynamic>> get foregroundMessages;
  Stream<Map<String, dynamic>> get openedMessages;
  Future<Map<String, dynamic>?> initialMessage();
  Future<void> deleteToken();
}

class UnavailableParentPushTokenSource implements ParentPushTokenSource {
  const UnavailableParentPushTokenSource();

  @override
  bool get supported => false;
  @override
  String get platform => '';
  @override
  String? get deviceName => null;
  @override
  Future<String?> requestPermissionAndToken() async => null;
  @override
  Stream<String> get tokenRefresh => const Stream.empty();
  @override
  Stream<Map<String, dynamic>> get foregroundMessages => const Stream.empty();
  @override
  Stream<Map<String, dynamic>> get openedMessages => const Stream.empty();
  @override
  Future<Map<String, dynamic>?> initialMessage() async => null;
  @override
  Future<void> deleteToken() async {}
}

enum ParentNotificationKind { announcement, leaveRequest }

class ParentNotificationTarget {
  const ParentNotificationTarget(this.kind, this.id);
  final ParentNotificationKind kind;
  final int id;

  /// This is the app's explicit payload contract for a future push provider.
  /// Opening the destination still fetches the resource through the secure API.
  static ParentNotificationTarget? fromPayload(Map<String, dynamic> payload) {
    final data = payload['data'];
    final fields = data is Map ? data : payload;
    final type = fields['type'];
    final kind = switch (type) {
      'ANNOUNCEMENT' => ParentNotificationKind.announcement,
      'LEAVE_REQUEST' => ParentNotificationKind.leaveRequest,
      _ => null,
    };
    if (kind == null) return null;
    final rawId =
        fields[kind == ParentNotificationKind.announcement
            ? 'announcementId'
            : 'requestId'];
    final id = rawId is int
        ? rawId
        : rawId is String
        ? int.tryParse(rawId)
        : null;
    if (id == null || id <= 0) return null;
    return ParentNotificationTarget(kind, id);
  }
}

class ParentForegroundNotification {
  const ParentForegroundNotification({this.title, this.body, this.target});
  final String? title;
  final String? body;
  final ParentNotificationTarget? target;

  factory ParentForegroundNotification.fromPayload(
    Map<String, dynamic> payload,
  ) {
    final notification = payload['notification'];
    final fields = notification is Map ? notification : payload;
    return ParentForegroundNotification(
      title: fields['title'] is String ? fields['title'] as String : null,
      body: fields['body'] is String ? fields['body'] as String : null,
      target: ParentNotificationTarget.fromPayload(payload),
    );
  }
}

enum ParentNotificationFailure { registration, removal, provider }

/// One instance belongs to one authenticated Parent session. All device writes
/// are serialized, so refresh/logout cannot leave a late registration behind.
class ParentNotificationService {
  ParentNotificationService({
    required this.api,
    this.source = const UnavailableParentPushTokenSource(),
  });

  final ParentApi api;
  final ParentPushTokenSource source;
  final _taps = StreamController<ParentNotificationTarget>.broadcast();
  final _foreground =
      StreamController<ParentForegroundNotification>.broadcast();
  final _failures = StreamController<ParentNotificationFailure>.broadcast();
  final List<StreamSubscription<dynamic>> _subscriptions = [];
  final Set<String> _registeredTokens = {};
  Future<void> _pending = Future.value();
  String? _currentToken;
  bool _active = false;
  int _generation = 0;
  bool _providerStarted = false;
  bool _disposed = false;

  bool get isConfigured => source.supported;
  Stream<ParentNotificationTarget> get taps => _taps.stream;
  Stream<ParentForegroundNotification> get foregroundMessages =>
      _foreground.stream;
  Stream<ParentNotificationFailure> get failures => _failures.stream;

  Future<void> _enqueue(Future<void> Function() action) {
    final next = _pending.then((_) => action());
    _pending = next.catchError((Object _) {});
    return next;
  }

  void _failure(ParentNotificationFailure failure) {
    if (!_failures.isClosed) _failures.add(failure);
  }

  Future<void> start() async {
    if (_disposed || _active || !isConfigured) return;
    if (!const {'ANDROID', 'IOS', 'WEB'}.contains(source.platform)) {
      _failure(ParentNotificationFailure.provider);
      return;
    }
    _active = true;
    final generation = ++_generation;
    _subscriptions.add(
      source.tokenRefresh.listen(
        (token) => unawaited(_enqueue(() => _register(token, generation))),
        onError: (Object _) => _failure(ParentNotificationFailure.provider),
      ),
    );
    _subscriptions.add(
      source.openedMessages.listen(
        (payload) => _open(payload, generation),
        onError: (Object _) => _failure(ParentNotificationFailure.provider),
      ),
    );
    _subscriptions.add(
      source.foregroundMessages.listen((payload) {
        if (_active && generation == _generation && !_foreground.isClosed) {
          _foreground.add(ParentForegroundNotification.fromPayload(payload));
        }
      }, onError: (Object _) => _failure(ParentNotificationFailure.provider)),
    );
    await _enqueue(() async {
      if (!_active || generation != _generation) return;
      _providerStarted = true;
      try {
        final token = await source.requestPermissionAndToken();
        if (token != null) await _register(token, generation);
        final initial = await source.initialMessage();
        if (initial != null) _open(initial, generation);
      } catch (_) {
        _failure(ParentNotificationFailure.provider);
      }
    });
  }

  void _open(Map<String, dynamic> payload, int generation) {
    final target = ParentNotificationTarget.fromPayload(payload);
    if (_active &&
        generation == _generation &&
        target != null &&
        !_taps.isClosed) {
      _taps.add(target);
    }
  }

  Future<void> _register(String rawToken, int generation) async {
    if (!_active || _disposed || generation != _generation) return;
    final token = rawToken.trim();
    if (token.isEmpty || token.length > 512 || token == _currentToken) return;
    final deviceName = source.deviceName;
    try {
      await api.registerDevice(
        token: token,
        platform: source.platform,
        deviceName: deviceName?.substring(
          0,
          deviceName.length > 150 ? 150 : deviceName.length,
        ),
      );
      _registeredTokens.add(token);
      final previous = _currentToken;
      _currentToken = token;
      if (previous != null && previous != token) {
        await _remove(previous);
      }
    } catch (_) {
      _failure(ParentNotificationFailure.registration);
    }
  }

  Future<void> _remove(String token) async {
    try {
      await api.removeDevice(token);
      _registeredTokens.remove(token);
    } catch (_) {
      _failure(ParentNotificationFailure.removal);
    }
  }

  /// Call before clearing the JWT. Revoking the provider token is attempted even
  /// when the unregister API is unavailable during logout.
  Future<void> stop() async {
    _active = false;
    _generation++;
    final subscriptions = List<StreamSubscription<dynamic>>.of(_subscriptions);
    _subscriptions.clear();
    for (final subscription in subscriptions) {
      await subscription.cancel();
    }
    await _enqueue(() async {
      for (final token in _registeredTokens.toList()) {
        await _remove(token);
      }
      _registeredTokens.clear();
      _currentToken = null;
      if (_providerStarted) {
        _providerStarted = false;
        try {
          await source.deleteToken();
        } catch (_) {
          _failure(ParentNotificationFailure.provider);
        }
      }
    });
  }

  Future<void> dispose() async {
    if (_disposed) return;
    _disposed = true;
    await stop();
    await _taps.close();
    await _foreground.close();
    await _failures.close();
  }
}
