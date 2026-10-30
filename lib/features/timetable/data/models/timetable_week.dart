class TimetableWeek {
  const TimetableWeek({
    required this.weekStart,
    required this.weekEnd,
    required this.days,
  });

  final DateTime weekStart;
  final DateTime weekEnd;
  final List<TimetableDay> days;

  factory TimetableWeek.fromEnvelope(Map<String, dynamic> envelope) {
    final data = _requiredMap(envelope['data'], 'data');
    final weekStart = _requiredDate(data, 'weekStart');
    final weekEnd = _requiredDate(data, 'weekEnd');
    final rawDays = data['days'];
    if (rawDays is! List || rawDays.length != 7) {
      throw const FormatException('Field days must contain exactly 7 items.');
    }

    final days = rawDays
        .map((value) => TimetableDay.fromJson(_requiredMap(value, 'day')))
        .toList(growable: false);
    final expectedWeekEnd = weekStart.add(const Duration(days: 6));
    final hasValidRange =
        weekStart.weekday == DateTime.monday &&
        _dateKey(weekEnd) == _dateKey(expectedWeekEnd);
    final hasConsecutiveDays = List<bool>.generate(7, (index) {
      final expectedDate = weekStart.add(Duration(days: index));
      final day = days[index];
      return _dateKey(day.date) == _dateKey(expectedDate) &&
          day.dayOfWeek.toUpperCase() == _dayOfWeek(expectedDate.weekday);
    }).every((matches) => matches);
    if (!hasValidRange || !hasConsecutiveDays) {
      throw const FormatException('Invalid timetable week range.');
    }

    return TimetableWeek(weekStart: weekStart, weekEnd: weekEnd, days: days);
  }

  TimetableDay? dayFor(DateTime date) {
    final key = _dateKey(date);
    for (final day in days) {
      if (_dateKey(day.date) == key) {
        return day;
      }
    }
    return null;
  }
}

class TimetableDay {
  const TimetableDay({
    required this.date,
    required this.dayOfWeek,
    required this.lessons,
  });

  final DateTime date;
  final String dayOfWeek;
  final List<TimetableLesson> lessons;

  factory TimetableDay.fromJson(Map<String, dynamic> json) {
    final rawLessons = json['lessons'];
    if (rawLessons is! List) {
      throw const FormatException('Missing or invalid field: lessons');
    }
    final lessons =
        rawLessons
            .map(
              (value) =>
                  TimetableLesson.fromJson(_requiredMap(value, 'lesson')),
            )
            .toList(growable: false)
          ..sort(
            (left, right) => left.startMinutes.compareTo(right.startMinutes),
          );

    return TimetableDay(
      date: _requiredDate(json, 'date'),
      dayOfWeek: _requiredString(json, 'dayOfWeek'),
      lessons: lessons,
    );
  }
}

class TimetableLesson {
  const TimetableLesson({
    required this.id,
    required this.periodNumber,
    required this.subjectCode,
    required this.subjectName,
    required this.teacherName,
    required this.room,
    required this.startTime,
    required this.endTime,
  });

  final int id;
  final int periodNumber;
  final String subjectCode;
  final String subjectName;
  final String teacherName;
  final String room;
  final String startTime;
  final String endTime;

  int get startMinutes => _timeToMinutes(startTime);
  int get endMinutes => _timeToMinutes(endTime);

  factory TimetableLesson.fromJson(Map<String, dynamic> json) {
    final startTime = _requiredTime(json, 'startTime');
    final endTime = _requiredTime(json, 'endTime');
    if (_timeToMinutes(endTime) <= _timeToMinutes(startTime)) {
      throw const FormatException('Lesson endTime must be after startTime.');
    }

    return TimetableLesson(
      id: _requiredInt(json, 'id'),
      periodNumber: _requiredInt(json, 'periodNumber'),
      subjectCode: _requiredString(json, 'subjectCode'),
      subjectName: _requiredString(json, 'subjectName'),
      teacherName: _requiredString(json, 'teacherName'),
      room: _requiredString(json, 'room'),
      startTime: startTime,
      endTime: endTime,
    );
  }
}

Map<String, dynamic> _requiredMap(Object? value, String fieldName) {
  if (value is Map<String, dynamic>) {
    return value;
  }
  if (value is Map) {
    return Map<String, dynamic>.from(value);
  }
  throw FormatException('Missing or invalid field: $fieldName');
}

String _requiredString(Map<String, dynamic> json, String fieldName) {
  final value = json[fieldName];
  if (value is String && value.trim().isNotEmpty) {
    return value.trim();
  }
  throw FormatException('Missing or invalid field: $fieldName');
}

int _requiredInt(Map<String, dynamic> json, String fieldName) {
  final value = json[fieldName];
  if (value is int) {
    return value;
  }
  if (value is num && value == value.toInt()) {
    return value.toInt();
  }
  throw FormatException('Missing or invalid field: $fieldName');
}

DateTime _requiredDate(Map<String, dynamic> json, String fieldName) {
  final raw = _requiredString(json, fieldName);
  final match = RegExp(r'^(\d{4})-(\d{2})-(\d{2})$').firstMatch(raw);
  if (match == null) {
    throw FormatException('Missing or invalid field: $fieldName');
  }
  final date = DateTime(
    int.parse(match.group(1)!),
    int.parse(match.group(2)!),
    int.parse(match.group(3)!),
  );
  if (_formatDate(date) != raw) {
    throw FormatException('Missing or invalid field: $fieldName');
  }
  return date;
}

String _requiredTime(Map<String, dynamic> json, String fieldName) {
  final raw = _requiredString(json, fieldName);
  if (!RegExp(r'^(?:[01]\d|2[0-3]):[0-5]\d$').hasMatch(raw)) {
    throw FormatException('Missing or invalid field: $fieldName');
  }
  return raw;
}

int _timeToMinutes(String value) {
  final parts = value.split(':');
  return int.parse(parts[0]) * 60 + int.parse(parts[1]);
}

String _formatDate(DateTime value) {
  return '${value.year.toString().padLeft(4, '0')}-'
      '${value.month.toString().padLeft(2, '0')}-'
      '${value.day.toString().padLeft(2, '0')}';
}

String _dateKey(DateTime value) => _formatDate(value);

String _dayOfWeek(int weekday) => switch (weekday) {
  DateTime.monday => 'MONDAY',
  DateTime.tuesday => 'TUESDAY',
  DateTime.wednesday => 'WEDNESDAY',
  DateTime.thursday => 'THURSDAY',
  DateTime.friday => 'FRIDAY',
  DateTime.saturday => 'SATURDAY',
  _ => 'SUNDAY',
};
