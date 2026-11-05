package com.example.myfschool.dto.teacher;

import com.example.myfschool.enums.AnnouncementStatus;
import com.example.myfschool.enums.LeaveRequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class TeacherCommunicationDtos {
    private TeacherCommunicationDtos() {}

    public record CreateAnnouncement(@NotNull @Positive Long classId,
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = 100_000) String content, LocalDateTime expiresAt) {}

    public record LeaveDecision(@NotNull LeaveRequestStatus decision,
            @Size(max = 5000) String reason) {}

    public record ClassReference(Long id, String name) {}
    public record StudentReference(Long id, String studentCode, String fullName) {}
    public record Actor(Long id, String username) {}
    public record AnnouncementResponse(Long id, String title, String content,
            AnnouncementStatus status, ClassReference schoolClass, Actor author,
            LocalDateTime createdAt, LocalDateTime publishedAt, LocalDateTime expiresAt) {}

    public record PageResponse<T>(List<T> content, int page, int size,
            long totalElements, int totalPages) {
        public static <T> PageResponse<T> of(List<T> content, int page, int size, long total) {
            return new PageResponse<>(content, page, size, total, (int) ((total + size - 1) / size));
        }
    }
    public record RecipientRead(Long parentId, String fullName, boolean read,
            LocalDateTime readAt) {}
    public record AnnouncementReads(Long announcementId, long recipientCount, long readCount,
            PageResponse<RecipientRead> recipients) {}
    public record LeaveResponse(Long id, StudentReference student, ClassReference schoolClass,
            String parentName, LocalDate startDate, LocalDate endDate, String reason,
            LeaveRequestStatus status, LocalDateTime createdAt, Actor reviewer,
            LocalDateTime decisionAt, String reviewNote) {}
    public record HistoryResponse(Long id, LeaveRequestStatus from, LeaveRequestStatus to,
            Actor actor, LocalDateTime changedAt, String note) {}
}
