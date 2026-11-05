package com.example.myfschool.dto.teacher;
import com.example.myfschool.enums.*;
import java.time.LocalDate;
import java.util.List;
public final class TeacherContextDtos {
    private TeacherContextDtos() {}
    public record Ref(Long id,String name) {}
    public record Profile(Long id,String teacherCode,String fullName,String phone,String email,Ref school) {}
    public record Period(Long id,String name,LocalDate startDate,LocalDate endDate,Ref academicYear) {}
    public record Assignment(Long id,Ref subject,Ref schoolClass,Ref semester,Ref academicYear) {}
    public record ClassInfo(Long id,String className,Ref school,Ref academicYear,boolean homeroom,long studentCount,List<Assignment> assignments) {}
    public record StudentInfo(Long id,String studentCode,String fullName,LocalDate dateOfBirth,Gender gender,StudentStatus status,Ref school,List<Ref> classes) {}
    public record Lesson(Long id,int weekday,int period,Shift shift,String classroom,LocalDate effectiveFrom,LocalDate effectiveTo,Assignment assignment) {}
    public record ParentContact(Long id,String fullName,String phone,String email,RelationshipType relationship,boolean primaryContact) {}
    public record Activity(LocalDate from,LocalDate to,long assignedClasses,long assignedSubjects,long attendanceSessionsCreated,long attendanceSessionsCompleted,long gradeItemsCreated,long gradesEntered,long announcementsPublished,long leaveRequestsProcessed) {}
    public record AnnouncementSummary(Long id,String title,java.time.LocalDateTime publishedAt) {}
    public record Dashboard(Profile teacher,Ref academicYear,List<ClassInfo> classes,List<ClassInfo> homeroomClasses,List<Lesson> todayTimetable,long pendingLeaveRequests,List<AnnouncementSummary> recentAnnouncements,Activity activity) {}
}
