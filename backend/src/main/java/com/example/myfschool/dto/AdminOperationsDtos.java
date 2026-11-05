package com.example.myfschool.dto;

import com.example.myfschool.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public final class AdminOperationsDtos {
    private AdminOperationsDtos() {}
    public record Timetable(@NotNull @Positive Long teachingAssignmentId,@Min(1) @Max(7) int dayOfWeek,
        @Min(1) @Max(15) int periodNumber,@NotNull Shift shift,@Size(max=50) String room,
        @NotNull LocalDate effectiveFrom,LocalDate effectiveTo) {}
    public record SchoolInfo(@NotBlank @Size(max=255) String name,@NotNull SchoolLevel schoolLevel,
        @Size(max=500) String address,@Size(max=20) String phone,@Email @Size(max=255) String email) {}
    public record Target(@NotNull AnnouncementTargetType type,@Positive Long id) {}
    public record Notice(@NotBlank @Size(max=255) String title,@NotBlank @Size(max=100000) String content,
        LocalDateTime expiresAt,@NotEmpty @Size(max=200) List<@NotNull @Valid Target> targets) {}
}
