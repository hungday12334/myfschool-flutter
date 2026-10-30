package com.example.myfschool.service.impl;

import com.example.myfschool.dto.response.TimetableDayResponse;
import com.example.myfschool.dto.response.TimetableLessonResponse;
import com.example.myfschool.dto.response.WeeklyTimetableResponse;
import com.example.myfschool.entity.TimetableEntry;
import com.example.myfschool.repository.TimetableEntryRepository;
import com.example.myfschool.service.TimetableService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimetableServiceImpl implements TimetableService {

    private static final ZoneId VIETNAM_TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final TimetableEntryRepository timetableEntryRepository;

    @Override
    public WeeklyTimetableResponse getWeeklyTimetable(Long studentId, LocalDate requestedDate) {
        LocalDate dateInRequestedWeek = requestedDate != null
                ? requestedDate
                : LocalDate.now(VIETNAM_TIME_ZONE);
        LocalDate weekStart = dateInRequestedWeek.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = weekStart.plusDays(6);

        Map<LocalDate, List<TimetableEntry>> entriesByDate = timetableEntryRepository
                .findWeeklyEntries(
                        studentId,
                        weekStart,
                        weekEnd
                )
                .stream()
                .collect(Collectors.groupingBy(TimetableEntry::getLessonDate));

        List<TimetableDayResponse> days = IntStream.range(0, 7)
                .mapToObj(weekOffset -> toDayResponse(
                        weekStart.plusDays(weekOffset),
                        entriesByDate.getOrDefault(weekStart.plusDays(weekOffset), List.of())
                ))
                .toList();

        return new WeeklyTimetableResponse(weekStart, weekEnd, days);
    }

    private TimetableDayResponse toDayResponse(LocalDate date, List<TimetableEntry> entries) {
        List<TimetableLessonResponse> lessons = entries.stream()
                .sorted(Comparator.comparing(TimetableEntry::getStartTime)
                        .thenComparingInt(TimetableEntry::getPeriodNumber))
                .map(this::toLessonResponse)
                .toList();
        return new TimetableDayResponse(date, date.getDayOfWeek().name(), lessons);
    }

    private TimetableLessonResponse toLessonResponse(TimetableEntry entry) {
        return new TimetableLessonResponse(
                entry.getId(),
                entry.getPeriodNumber(),
                entry.getSubjectCode(),
                entry.getSubjectName(),
                entry.getTeacherName(),
                entry.getRoom(),
                TIME_FORMATTER.format(entry.getStartTime()),
                TIME_FORMATTER.format(entry.getEndTime())
        );
    }
}
