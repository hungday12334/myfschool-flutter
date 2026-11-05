package com.example.myfschool.service;

import com.example.myfschool.dto.teacher.TeacherContextDtos.*;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;

public interface TeacherService {
    Profile profile(Long userId);
    Dashboard dashboard(Long userId,Long academicYearId);
    List<ClassInfo> classes(Long userId,Long academicYearId);
    Page<StudentInfo> students(Long userId,Long classId,String keyword,int page,int size);
    StudentInfo student(Long userId,Long studentId);
    List<Lesson> timetable(Long userId,Long academicYearId,Long semesterId,LocalDate date);
    List<Assignment> subjects(Long userId,Long academicYearId,Long semesterId);
    List<Period> periods(Long userId);
    List<ClassInfo> homeroom(Long userId,Long academicYearId);
    List<ParentContact> parents(Long userId,Long studentId);
    Activity activities(Long userId,LocalDate from,LocalDate to);
}
