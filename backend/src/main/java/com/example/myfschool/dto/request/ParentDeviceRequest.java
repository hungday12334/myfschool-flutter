package com.example.myfschool.dto.request;

import com.example.myfschool.enums.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParentDeviceRequest(
        @NotBlank @Size(max = 512) String token,
        @NotNull DevicePlatform platform,
        @Size(max = 150) String deviceName
) {}
