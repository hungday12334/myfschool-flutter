class AttendanceStudent {
  const AttendanceStudent({
    required this.studentCode,
    required this.fullName,
    required this.className,
    required this.schoolName,
  });

  final String studentCode;
  final String fullName;
  final String className;
  final String schoolName;

  factory AttendanceStudent.fromJson(Map<String, dynamic> json) {
    return AttendanceStudent(
      studentCode: _requiredString(json, 'studentCode'),
      fullName: _requiredString(json, 'fullName'),
      className: _requiredString(json, 'className'),
      schoolName: _requiredString(json, 'schoolName'),
    );
  }
}

enum AttendanceStatus {
  present,
  late,
  excusedAbsence,
  unexcusedAbsence;

  factory AttendanceStatus.fromApi(String value) {
    return switch (value.toUpperCase()) {
      'PRESENT' => AttendanceStatus.present,
      'LATE' => AttendanceStatus.late,
      'EXCUSED_ABSENCE' => AttendanceStatus.excusedAbsence,
      'UNEXCUSED_ABSENCE' => AttendanceStatus.unexcusedAbsence,
      _ => throw FormatException('Invalid attendance status: $value'),
    };
  }
}

class AttendanceDay {
  const AttendanceDay({
    required this.id,
    required this.date,
    required this.status,
    this.checkInTime,
    this.note,
  });

  final int id;
  final DateTime date;
  final AttendanceStatus status;
  final String? checkInTime;
  final String? note;

  factory AttendanceDay.fromJson(Map<String, dynamic> json) {
    return AttendanceDay(
      id: _requiredInt(json, 'id'),
      date: _requiredDate(json, 'date'),
      status: AttendanceStatus.fromApi(_requiredString(json, 'status')),
      checkInTime: _optionalString(json['checkInTime']),
      note: _optionalString(json['note']),
    );
  }
}

class AttendanceReport {
  const AttendanceReport({
    required this.student,
    required this.month,
    required this.totalSchoolDays,
    required this.presentDays,
    required this.lateDays,
    required this.excusedAbsenceDays,
    required this.unexcusedAbsenceDays,
    required this.attendanceRate,
    required this.records,
  });

  final AttendanceStudent student;
  final DateTime month;
  final int totalSchoolDays;
  final int presentDays;
  final int lateDays;
  final int excusedAbsenceDays;
  final int unexcusedAbsenceDays;
  final double attendanceRate;
  final List<AttendanceDay> records;

  factory AttendanceReport.fromEnvelope(Map<String, dynamic> envelope) {
    final data = _requiredMap(envelope['data'], 'data');
    final rawRecords = data['records'];
    if (rawRecords is! List) {
      throw const FormatException('Missing or invalid field: records');
    }
    final month = _requiredMonth(data, 'month');
    final totalSchoolDays = _requiredNonNegativeInt(data, 'totalSchoolDays');
    final presentDays = _requiredNonNegativeInt(data, 'presentDays');
    final lateDays = _requiredNonNegativeInt(data, 'lateDays');
    final excused = _requiredNonNegativeInt(data, 'excusedAbsenceDays');
    final unexcused = _requiredNonNegativeInt(data, 'unexcusedAbsenceDays');
    if (presentDays + lateDays + excused + unexcused != totalSchoolDays) {
      throw const FormatException('Attendance totals are inconsistent.');
    }
    final rate = data['attendanceRate'];
    if (rate is! num || rate < 0 || rate > 100) {
      throw const FormatException('Missing or invalid field: attendanceRate');
    }
    return AttendanceReport(
      student: AttendanceStudent.fromJson(
        _requiredMap(data['student'], 'student'),
      ),
      month: month,
      totalSchoolDays: totalSchoolDays,
      presentDays: presentDays,
      lateDays: lateDays,
      excusedAbsenceDays: excused,
      unexcusedAbsenceDays: unexcused,
      attendanceRate: rate.toDouble(),
      records: rawRecords
          .map(
            (value) => AttendanceDay.fromJson(
              _requiredMap(value, 'attendance record'),
            ),
          )
          .toList(growable: false),
    );
  }
}

Map<String, dynamic> _requiredMap(Object? value, String fieldName) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) return Map<String, dynamic>.from(value);
  throw FormatException('Missing or invalid field: $fieldName');
}

String _requiredString(Map<String, dynamic> json, String fieldName) {
  final value = json[fieldName];
  if (value is String && value.trim().isNotEmpty) return value.trim();
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

int _requiredNonNegativeInt(Map<String, dynamic> json, String fieldName) {
  final value = _requiredInt(json, fieldName);
  if (value >= 0) return value;
  throw FormatException('Missing or invalid field: $fieldName');
}

DateTime _requiredDate(Map<String, dynamic> json, String fieldName) {
  final raw = _requiredString(json, fieldName);
  final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(raw);
  if (match == null) {
    throw FormatException('Invalid date: $fieldName');
  }
  final date = DateTime(
    int.parse(match.group(1)!),
    int.parse(match.group(2)!),
    int.parse(match.group(3)!),
  );
  if (_formatDate(date) != raw) {
    throw FormatException('Invalid date: $fieldName');
  }
  return date;
}

DateTime _requiredMonth(Map<String, dynamic> json, String fieldName) {
  final raw = _requiredString(json, fieldName);
  final match = RegExp(r'^(\d{4})-(\d{2})$').firstMatch(raw);
  if (match == null) {
    throw FormatException('Invalid month: $fieldName');
  }
  final year = int.parse(match.group(1)!);
  final month = int.parse(match.group(2)!);
  if (month < 1 || month > 12) {
    throw FormatException('Invalid month: $fieldName');
  }
  return DateTime(year, month);
}

String _formatDate(DateTime value) {
  return '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}
