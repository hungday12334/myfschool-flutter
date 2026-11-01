package com.example.myfschool.service;

import com.example.myfschool.dto.response.TuitionReportResponse;

public interface TuitionService {

    TuitionReportResponse getTuitionReport(Long parentId);
}
