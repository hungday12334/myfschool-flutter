import '../../../../core/config/api_config.dart';
import '../../../../core/network/api_client.dart';
import '../../../../core/storage/token_storage.dart';
import '../models/grade_report.dart';

abstract interface class GradeService {
  Future<GradeReport> fetchReport(GradePeriod period);

  Future<SubjectGradeDetail> fetchSubject({
    required String subjectCode,
    required GradePeriod period,
  });
}

class GradeApiService implements GradeService {
  GradeApiService({ApiClient? apiClient, TokenStorage? tokenStorage})
    : _apiClient = apiClient ?? ApiClient(),
      _tokenStorage = tokenStorage ?? TokenStorage();

  final ApiClient _apiClient;
  final TokenStorage _tokenStorage;

  @override
  Future<GradeReport> fetchReport(GradePeriod period) async {
    final response = await _getGrades(
      ApiConfig.parentGradesPath,
      period: period,
    );
    try {
      return GradeReport.fromEnvelope(response);
    } on FormatException {
      throw const ApiException(message: 'Phản hồi từ máy chủ không hợp lệ.');
    }
  }

  @override
  Future<SubjectGradeDetail> fetchSubject({
    required String subjectCode,
    required GradePeriod period,
  }) async {
    final code = subjectCode.trim();
    if (code.isEmpty) {
      throw const ApiException(message: 'Mã môn học không hợp lệ.');
    }
    final response = await _getGrades(
      '${ApiConfig.parentGradesPath}/${Uri.encodeComponent(code)}',
      period: period,
    );
    try {
      return SubjectGradeDetail.fromEnvelope(response);
    } on FormatException {
      throw const ApiException(message: 'Phản hồi từ máy chủ không hợp lệ.');
    }
  }

  Future<Map<String, dynamic>> _getGrades(
    String path, {
    required GradePeriod period,
  }) async {
    final accessToken = (await _tokenStorage.readAccessToken())?.trim();
    if (accessToken == null || accessToken.isEmpty) {
      throw const ApiException(
        message: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.',
        statusCode: 401,
      );
    }
    return _apiClient.getJson(
      path,
      queryParameters: <String, String>{'period': period.queryValue},
      bearerToken: accessToken,
    );
  }

  void close() {
    _apiClient.close();
  }
}
