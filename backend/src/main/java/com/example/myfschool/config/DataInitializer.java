package com.example.myfschool.config;

import com.example.myfschool.entity.User;
import com.example.myfschool.entity.TimetableEntry;
import com.example.myfschool.enums.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.repository.TimetableEntryRepository;
import com.example.myfschool.repository.UserRepository;
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
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            String phoneNumber = "0987654321";
            User user = userRepository.findByPhoneNumber(phoneNumber).orElse(null);
            if (user == null) {
                user = User.builder()
                        .phoneNumber(phoneNumber)
                        .email("student@myfschool.local")
                        .password(passwordEncoder.encode("123456"))
                        .fullName("Test Student")
                        .role(UserRole.STUDENT)
                        .status(UserStatus.ACTIVE)
                        .build();
                user = userRepository.save(user);
                log.info("Created development-only test user {}", phoneNumber);
            }

            if (user.getRole() == UserRole.STUDENT) {
                seedTimetable(user, timetableEntryRepository);
            }
        };
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
}
