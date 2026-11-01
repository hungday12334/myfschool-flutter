package com.example.myfschool.service;

import com.example.myfschool.dto.response.AttendanceReportResponse;

public interface AttendanceService {

    AttendanceReportResponse getMonthlyReport(Long parentId, String month);
}
