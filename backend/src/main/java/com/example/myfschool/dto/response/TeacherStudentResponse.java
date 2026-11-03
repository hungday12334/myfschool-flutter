package com.example.myfschool.dto.response;

public record TeacherStudentResponse(Long id, String studentCode, String fullName, String className,
                                     String schoolName, String avatarUrl) { }
