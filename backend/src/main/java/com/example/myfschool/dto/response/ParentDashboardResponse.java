package com.example.myfschool.dto.response;

import java.util.List;
import java.util.Map;

public record ParentDashboardResponse(
        Map<String, Object> parent,
        List<Map<String, Object>> children,
        Map<String, Object> selectedStudent,
        List<Map<String, Object>> recentAnnouncements,
        Map<String, Object> attendanceSummary,
        List<Map<String, Object>> academicSummary,
        long pendingLeaveRequests
) {}
