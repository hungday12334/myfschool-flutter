class ParentChild {
  const ParentChild({
    required this.id,
    required this.fullName,
    required this.studentCode,
    this.avatar,
    this.school,
    this.studentClass,
    this.enrollmentStatus,
  });

  final int id;
  final String fullName;
  final String studentCode;
  final String? avatar;
  final SchoolInfo? school;
  final ClassInfo? studentClass;
  final String? enrollmentStatus;

  factory ParentChild.fromJson(Map<String, dynamic> json) => ParentChild(
    id: _requiredId(json['id']),
    fullName: _string(json['fullName']) ?? 'Học sinh',
    studentCode: _string(json['studentCode']) ?? '',
    avatar: _string(json['avatar']),
    school: _map(json['school']) == null
        ? null
        : SchoolInfo.fromJson(_map(json['school'])!),
    studentClass: _map(json['class']) == null
        ? null
        : ClassInfo.fromJson(_map(json['class'])!),
    enrollmentStatus:
        _string(json['enrollmentStatus']) ?? _string(json['status']),
  );
}

class SchoolInfo {
  const SchoolInfo({this.id, this.code, this.name, this.address, this.phone});
  final int? id;
  final String? code;
  final String? name;
  final String? address;
  final String? phone;

  factory SchoolInfo.fromJson(Map<String, dynamic> json) => SchoolInfo(
    id: _int(json['id']),
    code: _string(json['code']),
    name: _string(json['name']),
    address: _string(json['address']),
    phone: _string(json['phone']),
  );
}

class ClassInfo {
  const ClassInfo({
    this.id,
    this.name,
    this.gradeLevel,
    this.academicYear,
    this.homeroomTeacher,
  });
  final int? id;
  final String? name;
  final int? gradeLevel;
  final AcademicYearInfo? academicYear;
  final TeacherInfo? homeroomTeacher;

  factory ClassInfo.fromJson(Map<String, dynamic> json) => ClassInfo(
    id: _int(json['id']),
    name: _string(json['name']),
    gradeLevel: _int(json['gradeLevel']),
    academicYear: _map(json['academicYear']) == null
        ? null
        : AcademicYearInfo.fromJson(_map(json['academicYear'])!),
    homeroomTeacher: _map(json['homeroomTeacher']) == null
        ? null
        : TeacherInfo.fromJson(_map(json['homeroomTeacher'])!),
  );
}

class AcademicYearInfo {
  const AcademicYearInfo({this.id, this.name});
  final int? id;
  final String? name;
  factory AcademicYearInfo.fromJson(Map<String, dynamic> json) =>
      AcademicYearInfo(id: _int(json['id']), name: _string(json['name']));
}

class TeacherInfo {
  const TeacherInfo({this.id, this.fullName, this.phone, this.email});
  final int? id;
  final String? fullName;
  final String? phone;
  final String? email;
  factory TeacherInfo.fromJson(Map<String, dynamic> json) => TeacherInfo(
    id: _int(json['id']),
    fullName: _string(json['fullName']),
    phone: _string(json['phone']),
    email: _string(json['email']),
  );
}

class StudentProfile {
  const StudentProfile({
    required this.child,
    this.dateOfBirth,
    this.gender,
    this.status,
  });
  final ParentChild child;
  final DateTime? dateOfBirth;
  final String? gender;
  final String? status;
  factory StudentProfile.fromJson(Map<String, dynamic> json) => StudentProfile(
    child: ParentChild.fromJson(json),
    dateOfBirth: _date(json['dateOfBirth']),
    gender: _string(json['gender']),
    status: _string(json['status']),
  );
}

class ParentDashboard {
  const ParentDashboard({
    this.parentName,
    this.parentPhone,
    this.parentEmail,
    required this.children,
    this.selectedStudent,
    required this.recentAnnouncements,
    required this.attendanceSummary,
    required this.academicSummary,
    required this.pendingLeaveRequests,
  });
  final String? parentName;
  final String? parentPhone;
  final String? parentEmail;
  final List<ParentChild> children;
  final StudentProfile? selectedStudent;
  final List<ParentAnnouncement> recentAnnouncements;
  final AttendanceSummary attendanceSummary;
  final List<AcademicResult> academicSummary;
  final int pendingLeaveRequests;

  factory ParentDashboard.fromJson(Map<String, dynamic> json) =>
      ParentDashboard(
        parentName: _string(_map(json['parent'])?['fullName']),
        parentPhone: _string(_map(json['parent'])?['phone']),
        parentEmail: _string(_map(json['parent'])?['email']),
        children: _list(json['children']).map(ParentChild.fromJson).toList(),
        selectedStudent: _map(json['selectedStudent']) == null
            ? null
            : StudentProfile.fromJson(_map(json['selectedStudent'])!),
        recentAnnouncements: _list(json['recentAnnouncements'])
            .map(ParentAnnouncement.fromJson)
            .toList(),
        attendanceSummary: AttendanceSummary.fromJson(
          _map(json['attendanceSummary']) ?? const {},
        ),
        academicSummary: _list(json['academicSummary'])
            .map(AcademicResult.fromJson)
            .toList(),
        pendingLeaveRequests: _int(json['pendingLeaveRequests']) ?? 0,
      );
}

class TimetableEntry {
  const TimetableEntry({
    this.weekday,
    this.period,
    this.shift,
    this.subject,
    this.teacher,
    this.startTime,
    this.endTime,
    this.classroom,
  });
  final int? weekday;
  final int? period;
  final String? shift;
  final SubjectInfo? subject;
  final TeacherInfo? teacher;
  final String? startTime;
  final String? endTime;
  final String? classroom;
  factory TimetableEntry.fromJson(Map<String, dynamic> json) => TimetableEntry(
    weekday: _int(json['weekday']),
    period: _int(json['period']),
    shift: _string(json['shift']),
    subject: _map(json['subject']) == null
        ? null
        : SubjectInfo.fromJson(_map(json['subject'])!),
    teacher: _map(json['teacher']) == null
        ? null
        : TeacherInfo.fromJson(_map(json['teacher'])!),
    startTime: _string(json['startTime']),
    endTime: _string(json['endTime']),
    classroom: _string(json['classroom']),
  );
}

class SubjectInfo {
  const SubjectInfo({this.id, this.code, this.name});
  final int? id;
  final String? code;
  final String? name;
  factory SubjectInfo.fromJson(Map<String, dynamic> json) => SubjectInfo(
    id: _int(json['id']),
    code: _string(json['code']),
    name: _string(json['name']),
  );
}

class AcademicResult {
  const AcademicResult({required this.subject, required this.grades});
  final SubjectInfo subject;
  final List<StudentGrade> grades;
  factory AcademicResult.fromJson(Map<String, dynamic> json) => AcademicResult(
    subject: SubjectInfo.fromJson(_map(json['subject']) ?? const {}),
    grades: _list(json['grades']).map(StudentGrade.fromJson).toList(),
  );
}

class StudentGrade {
  const StudentGrade({
    this.gradeItemId,
    this.name,
    this.type,
    this.score,
    this.weight,
    this.assessmentDate,
    this.comment,
  });
  final int? gradeItemId;
  final String? name;
  final String? type;
  final double? score;
  final double? weight;
  final DateTime? assessmentDate;
  final String? comment;
  factory StudentGrade.fromJson(Map<String, dynamic> json) => StudentGrade(
    gradeItemId: _int(json['gradeItemId']),
    name: _string(json['name']),
    type: _string(json['type']),
    score: _double(json['score']),
    weight: _double(json['weight']),
    assessmentDate: _date(json['assessmentDate']),
    comment: _string(json['comment']),
  );
}

class StudentEvaluation {
  const StudentEvaluation({
    this.type,
    this.teacher,
    this.subject,
    this.content,
    this.academicResult,
    this.conductResult,
    this.date,
  });
  final String? type;
  final TeacherInfo? teacher;
  final SubjectInfo? subject;
  final String? content;
  final String? academicResult;
  final String? conductResult;
  final DateTime? date;
  factory StudentEvaluation.fromJson(Map<String, dynamic> json) =>
      StudentEvaluation(
        type: _string(json['type']),
        teacher: _map(json['teacher']) == null
            ? null
            : TeacherInfo.fromJson(_map(json['teacher'])!),
        subject: _map(json['subject']) == null
            ? null
            : SubjectInfo.fromJson(_map(json['subject'])!),
        content: _string(json['content']),
        academicResult: _string(json['academicResult']),
        conductResult: _string(json['conductResult']),
        date: _date(json['date']),
      );
}

class AttendanceSummary {
  const AttendanceSummary({
    this.totalSessions = 0,
    this.present = 0,
    this.absent = 0,
    this.excused = 0,
    this.late = 0,
  });
  final int totalSessions;
  final int present;
  final int absent;
  final int excused;
  final int late;
  factory AttendanceSummary.fromJson(Map<String, dynamic> json) =>
      AttendanceSummary(
        totalSessions: _int(json['totalSessions']) ?? 0,
        present: _int(json['present']) ?? 0,
        absent: _int(json['absent']) ?? 0,
        excused: _int(json['excused']) ?? 0,
        late: _int(json['late']) ?? 0,
      );
}

class AttendanceRecord {
  const AttendanceRecord({
    this.date,
    this.sessionType,
    this.shift,
    this.period,
    this.status,
    this.remarks,
  });
  final DateTime? date;
  final String? sessionType;
  final String? shift;
  final int? period;
  final String? status;
  final String? remarks;
  factory AttendanceRecord.fromJson(Map<String, dynamic> json) =>
      AttendanceRecord(
        date: _date(json['date']),
        sessionType: _string(json['sessionType']),
        shift: _string(json['shift']),
        period: _int(json['period']),
        status: _string(json['status']),
        remarks: _string(json['remarks']),
      );
}

class ParentAnnouncement {
  const ParentAnnouncement({
    required this.id,
    required this.title,
    this.preview,
    this.content,
    this.publishedAt,
    this.sender,
    this.read = false,
  });
  final int id;
  final String title;
  final String? preview;
  final String? content;
  final DateTime? publishedAt;
  final String? sender;
  final bool read;
  factory ParentAnnouncement.fromJson(Map<String, dynamic> json) =>
      ParentAnnouncement(
        id: _requiredId(json['id']),
        title: _string(json['title']) ?? 'Thông báo',
        preview: _string(json['preview']),
        content: _string(json['content']),
        publishedAt: _date(json['publishedAt']),
        sender: _string(json['sender']),
        read: json['read'] == true,
      );
}

class LeaveRequest {
  const LeaveRequest({
    required this.id,
    this.studentName,
    this.startDate,
    this.endDate,
    this.reason,
    this.status,
    this.createdAt,
    this.decisionAt,
    this.reviewer,
    this.rejectionReason,
    this.history = const [],
  });
  final int id;
  final String? studentName;
  final DateTime? startDate;
  final DateTime? endDate;
  final String? reason;
  final String? status;
  final DateTime? createdAt;
  final DateTime? decisionAt;
  final String? reviewer;
  final String? rejectionReason;
  final List<Map<String, dynamic>> history;
  factory LeaveRequest.fromJson(Map<String, dynamic> json) => LeaveRequest(
    id: _requiredId(json['id']),
    studentName: _string(_map(json['student'])?['fullName']),
    startDate: _date(json['startDate']),
    endDate: _date(json['endDate']),
    reason: _string(json['reason']),
    status: _string(json['status']),
    createdAt: _date(json['createdAt']),
    decisionAt: _date(json['decisionAt']),
    reviewer: _string(json['reviewer']),
    rejectionReason: _string(json['rejectionReason']),
    history: _list(json['history']),
  );
}

Map<String, dynamic>? _map(Object? value) {
  if (value is Map<String, dynamic>) return value;
  if (value is Map) return Map<String, dynamic>.from(value);
  return null;
}

List<Map<String, dynamic>> _list(Object? value) => value is List
    ? value.map(_map).whereType<Map<String, dynamic>>().toList()
    : <Map<String, dynamic>>[];
String? _string(Object? value) =>
    value is String && value.trim().isNotEmpty ? value.trim() : null;
int? _int(Object? value) => value is num ? value.toInt() : null;
double? _double(Object? value) => value is num ? value.toDouble() : null;
DateTime? _date(Object? value) =>
    value is String ? DateTime.tryParse(value) : null;

int _requiredId(Object? value) {
  final id = _int(value);
  if (id == null || id <= 0) {
    throw const FormatException('Dữ liệu máy chủ thiếu mã định danh hợp lệ.');
  }
  return id;
}
