import 'package:flutter/foundation.dart';
import 'package:myschoolse1947/core/network/api_client.dart';

import '../data/models/parent_models.dart';
import '../data/services/parent_api_service.dart';

class ParentContextController extends ChangeNotifier {
  ParentContextController(this.api);
  final ParentApi api;

  List<ParentChild> children = const [];
  ParentChild? selectedChild;
  StudentProfile? selectedContext;
  bool loading = false;
  String? error;
  int _generation = 0;
  bool _disposed = false;

  Future<void> load() async {
    if (_disposed) return;
    final request = ++_generation;
    loading = true;
    error = null;
    notifyListeners();
    try {
      final result = await api.children();
      if (request != _generation) return;
      children = result;
      final stillLinked = selectedChild == null
          ? null
          : result.where((child) => child.id == selectedChild!.id).firstOrNull;
      selectedChild = stillLinked ?? (result.isEmpty ? null : result.first);
      selectedContext = null;
      if (selectedChild != null) await _loadContext(selectedChild!.id, request);
    } catch (e) {
      if (request == _generation) error = _message(e);
    } finally {
      if (request == _generation) {
        loading = false;
        notifyListeners();
      }
    }
  }

  Future<void> selectChild(ParentChild child) async {
    if (_disposed) return;
    final authorized = children
        .where((value) => value.id == child.id)
        .firstOrNull;
    if (authorized == null) return;
    if (selectedChild?.id == child.id) return;
    final request = ++_generation;
    selectedChild = authorized;
    selectedContext = null;
    loading = true;
    error = null;
    notifyListeners();
    try {
      await _loadContext(child.id, request);
    } catch (e) {
      if (request == _generation) error = _message(e);
    } finally {
      if (request == _generation) {
        loading = false;
        notifyListeners();
      }
    }
  }

  Future<void> _loadContext(int studentId, int request) async {
    final context = await api.childContext(studentId);
    if (request != _generation || selectedChild?.id != studentId) return;
    selectedContext = context;
  }

  void reset() {
    if (_disposed) return;
    _generation++;
    children = const [];
    selectedChild = null;
    selectedContext = null;
    error = null;
    loading = false;
    notifyListeners();
  }

  String _message(Object error) => error is ApiException
      ? error.message
      : 'Không thể tải danh sách học sinh. Vui lòng thử lại.';

  @override
  void dispose() {
    _disposed = true;
    _generation++;
    super.dispose();
  }
}

extension _FirstOrNull<T> on Iterable<T> {
  T? get firstOrNull {
    final iterator = this.iterator;
    return iterator.moveNext() ? iterator.current : null;
  }
}
