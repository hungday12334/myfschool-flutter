package com.example.myfschool.dto;

import com.example.myfschool.enums.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public final class AdminSchoolDtos {
    private AdminSchoolDtos() {}
    public record TeacherProfile(@NotBlank @Size(max=40) String teacherCode,
        @NotBlank @Size(max=150) String fullName, @Past LocalDate dateOfBirth, Gender gender,
        @Size(max=20) String phone, @Email @Size(max=255) String email, @NotNull TeacherStatus status) {}
    public record StudentProfile(@NotBlank @Size(max=40) String studentCode,
        @NotBlank @Size(max=150) String fullName, @Past LocalDate dateOfBirth, Gender gender,
        LocalDate admissionDate, @Size(max=500) String address, @NotNull StudentStatus status) {}
    public record ParentProfile(@Positive Long userId, @NotBlank @Size(max=150) String fullName,
        @Size(max=20) String phone, @Email @Size(max=255) String email, @Size(max=500) String address,
        @Size(max=100) String username, @Size(min=8,max=72) String password) {}
    public record ParentLink(@NotNull RelationshipType relationshipType, boolean primaryContact,
        boolean canViewAcademic, boolean canSubmitLeave) {}
    public record Year(@NotBlank @Size(max=20) String name,@NotNull LocalDate startDate,
        @NotNull LocalDate endDate,boolean current) {}
    public record Term(@NotNull @Positive Long academicYearId,@Min(1) @Max(3) int semesterNumber,
        @NotBlank @Size(max=60) String name,@NotNull LocalDate startDate,@NotNull LocalDate endDate) {}
    public record SubjectProfile(@NotBlank @Size(max=40) String subjectCode,
        @NotBlank @Size(max=150) String name,@Size(max=10000) String description,boolean active) {}
    public record ClassProfile(@NotNull @Positive Long academicYearId,
        @NotBlank @Size(max=50) String className,@Min(6) @Max(12) int gradeLevel,@NotNull SchoolClassStatus status) {}
    public record Enrollment(@NotNull @Positive Long studentId,@NotNull LocalDate enrolledAt) {}
    public record Move(@NotNull @Positive Long targetClassId,@NotNull LocalDate effectiveDate) {}
    public record EndEnrollment(@NotNull LocalDate leftAt,@NotNull EnrollmentStatus status) {}
    public record Homeroom(@Positive Long teacherId) {}
    public record Assignment(@NotNull @Positive Long classId,@NotNull @Positive Long teacherId,
        @NotNull @Positive Long subjectId,@NotNull @Positive Long semesterId,@NotNull ActiveStatus status) {}
}
