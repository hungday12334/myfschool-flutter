package com.example.myfschool.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParentDeviceDeleteRequest(@NotBlank @Size(max = 512) String token) {}
