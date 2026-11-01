package com.example.myfschool.config;

import com.example.myfschool.entity.User;
import com.example.myfschool.entity.TimetableEntry;
import com.example.myfschool.entity.GradeAssessment;
import com.example.myfschool.entity.StudentProfile;
import com.example.myfschool.enums.AssessmentType;
import com.example.myfschool.enums.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.repository.TimetableEntryRepository;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.repository.GradeAssessmentRepository;
import com.example.myfschool.repository.StudentProfileRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    CommandLineRunner seedDevelopmentData(
            UserRepository userRepository,
            TimetableEntryRepository timetableEntryRepository,
            StudentProfileRepository studentProfileRepository,
            GradeAssessmentRepository gradeAssessmentRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            String phoneNumber = "0987654321";
            User user = userRepository.findByPhoneNumber(phoneNumber).orElse(null);
            if (user == null) {
                user = User.builder()
                        .phoneNumber(phoneNumber)
                        .email("parent@myfschool.local")
                        .password(passwordEncoder.encode("123456"))
                        .fullName("Test Parent")
                        .role(UserRole.PARENT)
                        .status(UserStatus.ACTIVE)
                        .build();
                user = userRepository.save(user);
                log.info("Created development-only test user {}", phoneNumber);
            }

            if (user.getRole() != UserRole.PARENT) {
                user.changeRole(UserRole.PARENT);
                user = userRepository.save(user);
            }
            seedTimetable(user, timetableEntryRepository);
            StudentProfile student = seedStudentProfile(user, studentProfileRepository);
            seedGrades(student, gradeAssessmentRepository);
        };
    }

    private StudentProfile seedStudentProfile(
            User parent,
            StudentProfileRepository studentProfileRepository
    ) {
        return studentProfileRepository.findByParentIdAndStudentCode(parent.getId(), "HS10A1001")
                .orElseGet(() -> studentProfileRepository.save(StudentProfile.builder()
                        .parent(parent)
                        .studentCode("HS10A1001")
                        .fullName("Nguyễn Minh Anh")
                        .className("10A1")
                        .schoolName("Trường THPT FPT")
                        .build()));
    }

    private void seedGrades(
            StudentProfile student,
            GradeAssessmentRepository gradeAssessmentRepository
    ) {
        if (gradeAssessmentRepository.existsByStudentId(student.getId())) {
            return;
        }

        List<SubjectGradeTemplate> subjects = List.of(
                new SubjectGradeTemplate("MATH", "Toán", 8.0, 7.5, 9.0, 9.5),
                new SubjectGradeTemplate("LITERATURE", "Ngữ văn", 8.0, 8.0, 8.0, 8.3),
                new SubjectGradeTemplate("ENGLISH", "Tiếng Anh", 8.5, 8.0, 9.0, 9.0),
                new SubjectGradeTemplate("PHYSICS", "Vật lý", 8.0, 7.5, 8.5, 8.3),
                new SubjectGradeTemplate("CHEMISTRY", "Hóa học", 9.0, 8.5, 9.5, 9.3),
                new SubjectGradeTemplate("BIOLOGY", "Sinh học", 8.0, 7.5, 8.0, 8.5),
                new SubjectGradeTemplate("HISTORY", "Lịch sử", 7.5, 7.0, 8.0, 8.0),
                new SubjectGradeTemplate("GEOGRAPHY", "Địa lý", 8.5, 8.0, 8.5, 9.0),
                new SubjectGradeTemplate("CIVICS", "GD KT&PL", 8.0, 7.5, 8.0, 8.6),
                new SubjectGradeTemplate("INFORMATICS", "Tin học", 9.0, 8.5, 9.5, 9.3)
        );
        List<GradeAssessment> grades = new ArrayList<>();
        for (int semester = 1; semester <= 2; semester++) {
            double adjustment = semester == 1 ? 0 : 0.1;
            for (SubjectGradeTemplate subject : subjects) {
                grades.add(grade(student, semester, subject, AssessmentType.ORAL,
                        "Miệng", 1, subject.oral() + adjustment));
                grades.add(grade(student, semester, subject, AssessmentType.FIFTEEN_MINUTES,
                        "15 phút", 1, subject.fifteenMinutes() + adjustment));
                grades.add(grade(student, semester, subject, AssessmentType.ONE_PERIOD,
                        "1 tiết", 2, subject.onePeriod() + adjustment));
                grades.add(grade(student, semester, subject, AssessmentType.SEMESTER,
                        "Học kỳ", 3, Math.min(10, subject.semester() + adjustment)));
            }
        }
        gradeAssessmentRepository.saveAll(grades);
        log.info("Created {} development-only grade assessments", grades.size());
    }

    private GradeAssessment grade(
            StudentProfile student,
            int semester,
            SubjectGradeTemplate subject,
            AssessmentType type,
            String label,
            int coefficient,
            double score
    ) {
        return GradeAssessment.builder()
                .student(student)
                .academicYear("2026-2027")
                .semester(semester)
                .subjectCode(subject.code())
                .subjectName(subject.name())
                .assessmentType(type)
                .assessmentLabel(label)
                .coefficient(coefficient)
                .score(BigDecimal.valueOf(score))
                .build();
    }

    private void seedTimetable(User student, TimetableEntryRepository timetableEntryRepository) {
        LocalDate currentWeekStart = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        List<SubjectTemplate> subjects = List.of(
                new SubjectTemplate("MATH", "Toán", "Cô Nguyễn Thị Lan", "Phòng 101"),
                new SubjectTemplate("ENGLISH", "Tiếng Anh", "Thầy Trần Văn Minh", "Phòng 102"),
                new SubjectTemplate("PHYSICS", "Vật lý", "Cô Lê Thu Hương", "Phòng 103"),
                new SubjectTemplate("LITERATURE", "Ngữ văn", "Cô Phạm Thị Mai", "Phòng 101"),
                new SubjectTemplate("CHEMISTRY", "Hóa học", "Thầy Hoàng Anh Tuấn", "Phòng 104"),
                new SubjectTemplate("INFORMATICS", "Tin học", "Cô Đỗ Thị Nga", "Phòng 201")
        );
        List<PeriodTemplate> periods = List.of(
                new PeriodTemplate(1, LocalTime.of(7, 30), LocalTime.of(8, 15)),
                new PeriodTemplate(2, LocalTime.of(8, 30), LocalTime.of(9, 15)),
                new PeriodTemplate(3, LocalTime.of(9, 30), LocalTime.of(10, 15)),
                new PeriodTemplate(4, LocalTime.of(10, 30), LocalTime.of(11, 15)),
                new PeriodTemplate(5, LocalTime.of(13, 30), LocalTime.of(14, 15)),
                new PeriodTemplate(6, LocalTime.of(14, 30), LocalTime.of(15, 15))
        );

        List<TimetableEntry> entries = new ArrayList<>();
        for (int weekOffset = 0; weekOffset < 2; weekOffset++) {
            LocalDate weekStart = currentWeekStart.plusWeeks(weekOffset);
            for (int dayOffset = 0; dayOffset < 5; dayOffset++) {
                LocalDate lessonDate = weekStart.plusDays(dayOffset);
                for (int periodIndex = 0; periodIndex < periods.size(); periodIndex++) {
                    PeriodTemplate period = periods.get(periodIndex);
                    if (timetableEntryRepository.existsByStudentIdAndLessonDateAndPeriodNumber(
                            student.getId(),
                            lessonDate,
                            period.number()
                    )) {
                        continue;
                    }
                    SubjectTemplate subject = subjects.get((periodIndex + dayOffset) % subjects.size());
                    entries.add(TimetableEntry.builder()
                            .student(student)
                            .lessonDate(lessonDate)
                            .periodNumber(period.number())
                            .subjectCode(subject.code())
                            .subjectName(subject.name())
                            .teacherName(subject.teacherName())
                            .room(subject.room())
                            .startTime(period.startTime())
                            .endTime(period.endTime())
                            .build());
                }
            }
        }
        if (!entries.isEmpty()) {
            timetableEntryRepository.saveAll(entries);
            log.info("Created {} development-only timetable entries", entries.size());
        }
    }

    private record SubjectTemplate(String code, String name, String teacherName, String room) {
    }

    private record PeriodTemplate(int number, LocalTime startTime, LocalTime endTime) {
    }

    private record SubjectGradeTemplate(
            String code,
            String name,
            double oral,
            double fifteenMinutes,
            double onePeriod,
            double semester
    ) {
    }
}
