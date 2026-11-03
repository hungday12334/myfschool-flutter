package com.example.myfschool.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateTeacherClassAssignmentRequest(
        @NotNull Long teacherId,
        @NotBlank @Size(max = 60) String className,
        @NotBlank @Size(max = 220) String schoolName
) { }
