package com.example.myfschool.service.impl;

import com.example.myfschool.dto.response.AttendanceRecordResponse;
import com.example.myfschool.dto.response.AttendanceReportResponse;
import com.example.myfschool.dto.response.StudentSummaryResponse;
import com.example.myfschool.entity.AttendanceRecord;
import com.example.myfschool.entity.StudentProfile;
import com.example.myfschool.enums.AttendanceStatus;
import com.example.myfschool.exception.BusinessException;
import com.example.myfschool.exception.ErrorCode;
import com.example.myfschool.repository.AttendanceRecordRepository;
import com.example.myfschool.repository.StudentProfileRepository;
import com.example.myfschool.service.AttendanceService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private static final ZoneId VIETNAM_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StudentProfileRepository studentProfileRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;

    @Override
    public AttendanceReportResponse getMonthlyReport(Long parentId, String month) {
        YearMonth selectedMonth = parseMonth(month);
        StudentProfile student = studentProfileRepository.findFirstByParentIdOrderByIdAsc(parentId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "No student is linked to this parent"
                ));
        List<AttendanceRecord> records = attendanceRecordRepository
                .findByStudentIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
                        student.getId(),
                        selectedMonth.atDay(1),
                        selectedMonth.atEndOfMonth()
                );

        int present = count(records, AttendanceStatus.PRESENT);
        int late = count(records, AttendanceStatus.LATE);
        int excused = count(records, AttendanceStatus.EXCUSED_ABSENCE);
        int unexcused = count(records, AttendanceStatus.UNEXCUSED_ABSENCE);
        BigDecimal rate = records.isEmpty()
                ? BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf((present + late) * 100L)
                        .divide(BigDecimal.valueOf(records.size()), 1, RoundingMode.HALF_UP);

        return new AttendanceReportResponse(
                toStudentResponse(student),
                selectedMonth.toString(),
                records.size(),
                present,
                late,
                excused,
                unexcused,
                rate,
                records.stream().map(this::toRecordResponse).toList()
        );
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now(VIETNAM_TIME_ZONE);
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException exception) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Month must use YYYY-MM format");
        }
    }

    private int count(List<AttendanceRecord> records, AttendanceStatus status) {
        return (int) records.stream().filter(record -> record.getStatus() == status).count();
    }

    private AttendanceRecordResponse toRecordResponse(AttendanceRecord record) {
        return new AttendanceRecordResponse(
                record.getId(),
                record.getAttendanceDate(),
                record.getStatus().name(),
                record.getCheckInTime() == null ? null : TIME_FORMATTER.format(record.getCheckInTime()),
                record.getNote()
        );
    }

    private StudentSummaryResponse toStudentResponse(StudentProfile student) {
        return new StudentSummaryResponse(
                student.getStudentCode(),
                student.getFullName(),
                student.getClassName(),
                student.getSchoolName()
        );
    }
}
