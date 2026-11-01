class TuitionStudent {
  const TuitionStudent({
    required this.studentCode,
    required this.fullName,
    required this.className,
    required this.schoolName,
  });

  final String studentCode;
  final String fullName;
  final String className;
  final String schoolName;

  factory TuitionStudent.fromJson(Map<String, dynamic> json) {
    return TuitionStudent(
      studentCode: _requiredString(json, 'studentCode'),
      fullName: _requiredString(json, 'fullName'),
      className: _requiredString(json, 'className'),
      schoolName: _requiredString(json, 'schoolName'),
    );
  }
}

enum TuitionItemStatus {
  paid,
  partiallyPaid,
  due;

  factory TuitionItemStatus.fromApi(String value) {
    return switch (value.toUpperCase()) {
      'PAID' => TuitionItemStatus.paid,
      'PARTIALLY_PAID' => TuitionItemStatus.partiallyPaid,
      'DUE' => TuitionItemStatus.due,
      _ => throw FormatException('Invalid tuition status: $value'),
    };
  }
}

class TuitionItem {
  const TuitionItem({
    required this.id,
    required this.feeCode,
    required this.title,
    required this.academicYear,
    required this.dueDate,
    required this.amount,
    required this.paidAmount,
    required this.remainingAmount,
    required this.status,
    this.description,
    this.paidDate,
  });

  final int id;
  final String feeCode;
  final String title;
  final String? description;
  final String academicYear;
  final DateTime dueDate;
  final int amount;
  final int paidAmount;
  final int remainingAmount;
  final DateTime? paidDate;
  final TuitionItemStatus status;

  factory TuitionItem.fromJson(Map<String, dynamic> json) {
    final amount = _requiredMoney(json, 'amount');
    final paidAmount = _requiredMoney(json, 'paidAmount');
    final remainingAmount = _requiredMoney(json, 'remainingAmount');
    if (paidAmount + remainingAmount != amount) {
      throw const FormatException('Tuition amounts are inconsistent.');
    }
    return TuitionItem(
      id: _requiredInt(json, 'id'),
      feeCode: _requiredString(json, 'feeCode'),
      title: _requiredString(json, 'title'),
      description: _optionalString(json['description']),
      academicYear: _requiredString(json, 'academicYear'),
      dueDate: _requiredDate(json, 'dueDate'),
      amount: amount,
      paidAmount: paidAmount,
      remainingAmount: remainingAmount,
      paidDate: _optionalDate(json['paidDate']),
      status: TuitionItemStatus.fromApi(_requiredString(json, 'status')),
    );
  }
}

class TuitionReport {
  const TuitionReport({
    required this.student,
    required this.academicYear,
    required this.totalAmount,
    required this.totalPaid,
    required this.totalDue,
    required this.paidItems,
    required this.dueItems,
  });

  final TuitionStudent student;
  final String academicYear;
  final int totalAmount;
  final int totalPaid;
  final int totalDue;
  final List<TuitionItem> paidItems;
  final List<TuitionItem> dueItems;

  factory TuitionReport.fromEnvelope(Map<String, dynamic> envelope) {
    final data = _requiredMap(envelope['data'], 'data');
    final paidItems = _requiredList(data, 'paidItems')
        .map((value) => TuitionItem.fromJson(_requiredMap(value, 'paid item')))
        .toList(growable: false);
    final dueItems = _requiredList(data, 'dueItems')
        .map((value) => TuitionItem.fromJson(_requiredMap(value, 'due item')))
        .toList(growable: false);
    final totalAmount = _requiredMoney(data, 'totalAmount');
    final totalPaid = _requiredMoney(data, 'totalPaid');
    final totalDue = _requiredMoney(data, 'totalDue');
    if (totalPaid + totalDue != totalAmount) {
      throw const FormatException('Tuition report totals are inconsistent.');
    }
    return TuitionReport(
      student: TuitionStudent.fromJson(
        _requiredMap(data['student'], 'student'),
      ),
      academicYear: _requiredString(data, 'academicYear', allowEmpty: true),
      totalAmount: totalAmount,
      totalPaid: totalPaid,
      totalDue: totalDue,
      paidItems: paidItems,
      dueItems: dueItems,
    );
  }
}

Map<String, dynamic> _requiredMap(Object? value, String fieldName) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) return Map<String, dynamic>.from(value);
  throw FormatException('Missing or invalid field: $fieldName');
}

List<Object?> _requiredList(Map<String, dynamic> json, String fieldName) {
  final value = json[fieldName];
  if (value is List) return value;
  throw FormatException('Missing or invalid field: $fieldName');
}

String _requiredString(
  Map<String, dynamic> json,
  String fieldName, {
  bool allowEmpty = false,
}) {
  final value = json[fieldName];
  if (value is String && (allowEmpty || value.trim().isNotEmpty)) {
    return value.trim();
  }
  throw FormatException('Missing or invalid field: $fieldName');
}

String? _optionalString(Object? value) {
  if (value == null) return null;
  if (value is String) {
    final normalized = value.trim();
    return normalized.isEmpty ? null : normalized;
  }
  throw const FormatException('Invalid optional string.');
}

int _requiredInt(Map<String, dynamic> json, String fieldName) {
  final value = json[fieldName];
  if (value is num && value == value.toInt()) return value.toInt();
  throw FormatException('Missing or invalid field: $fieldName');
}

int _requiredMoney(Map<String, dynamic> json, String fieldName) {
  final value = _requiredInt(json, fieldName);
  if (value >= 0) return value;
  throw FormatException('Missing or invalid field: $fieldName');
}

DateTime _requiredDate(Map<String, dynamic> json, String fieldName) {
  return _parseDate(_requiredString(json, fieldName), fieldName);
}

DateTime? _optionalDate(Object? value) {
  if (value == null) return null;
  if (value is String) return _parseDate(value, 'optional date');
  throw const FormatException('Invalid optional date.');
}

DateTime _parseDate(String raw, String fieldName) {
  final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(raw);
  if (match == null) throw FormatException('Invalid date: $fieldName');
  final date = DateTime(
    int.parse(match.group(1)!),
    int.parse(match.group(2)!),
    int.parse(match.group(3)!),
  );
  final formatted =
      '${date.year.toString().padLeft(4, '0')}-'
      '${date.month.toString().padLeft(2, '0')}-'
      '${date.day.toString().padLeft(2, '0')}';
  if (formatted != raw) throw FormatException('Invalid date: $fieldName');
  return date;
}
