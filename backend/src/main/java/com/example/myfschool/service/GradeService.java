package com.example.myfschool.service;

import com.example.myfschool.dto.response.GradeReportResponse;
import com.example.myfschool.dto.response.SubjectGradeDetailResponse;

public interface GradeService {

    GradeReportResponse getGradeReport(Long parentId, String period);

    SubjectGradeDetailResponse getSubjectGrade(Long parentId, String subjectCode, String period);
}
