package com.example.myfschool.dto;

import jakarta.validation.constraints.*;
import java.util.Set;
import com.example.myfschool.enums.UserStatus;

public final class AdminAccountDtos {
    private AdminAccountDtos() {}
    public record CreateTeacher(@NotBlank @Size(max=100) String username,
        @NotBlank @Size(min=8,max=72) String password,
        @NotBlank @Size(max=40) String teacherCode, @NotBlank @Size(max=150) String fullName,
        @Email @Size(max=255) String email, @Size(max=20) String phone) {}
    public record UpdateAccount(@Email @Size(max=255) String email, @Size(max=20) String phone,
        @Size(max=150) String fullName) {}
    public record ChangeStatus(@NotNull UserStatus status) {}
    public record ResetPassword(@NotBlank @Size(min=8,max=72) String password) {}
    public record ChangeRoles(@NotEmpty Set<@NotBlank String> roles) {}
    public record Account(Long id,String username,String email,String phone,UserStatus status,
        boolean mustChangePassword,Set<String> roles) {}
}
