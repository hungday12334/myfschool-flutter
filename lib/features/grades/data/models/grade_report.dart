enum GradePeriod {
  semester1('1', 'Học kỳ 1'),
  semester2('2', 'Học kỳ 2'),
  fullYear('all', 'Cả năm');

  const GradePeriod(this.queryValue, this.label);

  final String queryValue;
  final String label;
}

class StudentSummary {
  const StudentSummary({
    required this.studentCode,
    required this.fullName,
    required this.className,
    required this.schoolName,
  });

  final String studentCode;
  final String fullName;
  final String className;
  final String schoolName;

  factory StudentSummary.fromJson(Map<String, dynamic> json) {
    return StudentSummary(
      studentCode: _requiredString(json, 'studentCode'),
      fullName: _requiredString(json, 'fullName'),
      className: _requiredString(json, 'className'),
      schoolName: _requiredString(json, 'schoolName'),
    );
  }
}

class SubjectGradeSummary {
  const SubjectGradeSummary({
    required this.subjectCode,
    required this.subjectName,
    required this.averageScore,
  });

  final String subjectCode;
  final String subjectName;
  final double averageScore;

  factory SubjectGradeSummary.fromJson(Map<String, dynamic> json) {
    return SubjectGradeSummary(
      subjectCode: _requiredString(json, 'subjectCode'),
      subjectName: _requiredString(json, 'subjectName'),
      averageScore: _requiredScore(json, 'averageScore'),
    );
  }
}

class GradeReport {
  const GradeReport({
    required this.student,
    required this.academicYear,
    required this.period,
    required this.subjects,
  });

  final StudentSummary student;
  final String academicYear;
  final String period;
  final List<SubjectGradeSummary> subjects;

  factory GradeReport.fromEnvelope(Map<String, dynamic> envelope) {
    final data = _requiredMap(envelope['data'], 'data');
    final rawSubjects = data['subjects'];
    if (rawSubjects is! List) {
      throw const FormatException('Missing or invalid field: subjects');
    }
    return GradeReport(
      student: StudentSummary.fromJson(
        _requiredMap(data['student'], 'student'),
      ),
      academicYear: _requiredString(data, 'academicYear', allowEmpty: true),
      period: _requiredString(data, 'period'),
      subjects: rawSubjects
          .map(
            (value) =>
                SubjectGradeSummary.fromJson(_requiredMap(value, 'subject')),
          )
          .toList(growable: false),
    );
  }
}

class GradeComponent {
  const GradeComponent({
    required this.id,
    required this.type,
    required this.label,
    required this.coefficient,
    required this.score,
    required this.semester,
  });

  final int id;
  final String type;
  final String label;
  final int coefficient;
  final double score;
  final int semester;

  factory GradeComponent.fromJson(Map<String, dynamic> json) {
    return GradeComponent(
      id: _requiredInt(json, 'id'),
      type: _requiredString(json, 'type'),
      label: _requiredString(json, 'label'),
      coefficient: _requiredPositiveInt(json, 'coefficient'),
      score: _requiredScore(json, 'score'),
      semester: _requiredPositiveInt(json, 'semester'),
    );
  }
}

class SubjectGradeDetail {
  const SubjectGradeDetail({
    required this.student,
    required this.academicYear,
    required this.period,
    required this.subjectCode,
    required this.subjectName,
    required this.averageScore,
    required this.assessments,
  });

  final StudentSummary student;
  final String academicYear;
  final String period;
  final String subjectCode;
  final String subjectName;
  final double averageScore;
  final List<GradeComponent> assessments;

  factory SubjectGradeDetail.fromEnvelope(Map<String, dynamic> envelope) {
    final data = _requiredMap(envelope['data'], 'data');
    final rawAssessments = data['assessments'];
    if (rawAssessments is! List) {
      throw const FormatException('Missing or invalid field: assessments');
    }
    return SubjectGradeDetail(
      student: StudentSummary.fromJson(
        _requiredMap(data['student'], 'student'),
      ),
      academicYear: _requiredString(data, 'academicYear', allowEmpty: true),
      period: _requiredString(data, 'period'),
      subjectCode: _requiredString(data, 'subjectCode'),
      subjectName: _requiredString(data, 'subjectName'),
      averageScore: _requiredScore(data, 'averageScore'),
      assessments: rawAssessments
          .map(
            (value) =>
                GradeComponent.fromJson(_requiredMap(value, 'assessment')),
          )
          .toList(growable: false),
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

int _requiredInt(Map<String, dynamic> json, String fieldName) {
  final value = json[fieldName];
  if (value is int || value is num && value == value.toInt()) {
    return (value as num).toInt();
  }
  throw FormatException('Missing or invalid field: $fieldName');
}

int _requiredPositiveInt(Map<String, dynamic> json, String fieldName) {
  final value = _requiredInt(json, fieldName);
  if (value > 0) {
    return value;
  }
  throw FormatException('Missing or invalid field: $fieldName');
}

double _requiredScore(Map<String, dynamic> json, String fieldName) {
  final value = json[fieldName];
  if (value is num && value >= 0 && value <= 10) {
    return value.toDouble();
  }
  throw FormatException('Missing or invalid field: $fieldName');
}
