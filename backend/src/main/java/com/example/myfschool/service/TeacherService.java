package com.example.myfschool.service;

import com.example.myfschool.dto.request.AttendanceMarkRequest;
import com.example.myfschool.dto.request.CreateTeacherClassAssignmentRequest;
import com.example.myfschool.dto.response.*;
import java.time.LocalDate;
import java.util.List;

public interface TeacherService {
    UserResponse getProfile(Long teacherId);
    List<TeacherScheduleLessonResponse> getSchedule(Long teacherId, LocalDate weekDate);
    List<TeacherClassResponse> getClasses(Long teacherId);
    List<TeacherStudentResponse> getStudents(Long teacherId, Long assignmentId, String search);
    TeacherStudentResponse getStudent(Long teacherId, Long assignmentId, Long studentId);
    List<TeacherAttendanceResponse> getAttendance(Long teacherId, Long assignmentId, LocalDate date);
    List<TeacherAttendanceResponse> saveAttendance(Long teacherId, Long assignmentId, LocalDate date,
                                                   List<AttendanceMarkRequest> marks);
    TeacherClassResponse assignClass(CreateTeacherClassAssignmentRequest request);
}
