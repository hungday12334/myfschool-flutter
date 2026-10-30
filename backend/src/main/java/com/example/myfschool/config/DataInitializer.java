package com.example.myfschool.config;

import com.example.myfschool.entity.User;
import com.example.myfschool.enums.UserRole;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Configuration
@Profile("dev")
public class DataInitializer {

    @Bean
    CommandLineRunner seedDevelopmentUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String phoneNumber = "0987654321";
            if (userRepository.existsByPhoneNumber(phoneNumber)) {
                return;
            }

            User user = User.builder()
                    .phoneNumber(phoneNumber)
                    .email("student@myfschool.local")
                    .password(passwordEncoder.encode("123456"))
                    .fullName("Test Student")
                    .role(UserRole.STUDENT)
                    .status(UserStatus.ACTIVE)
                    .build();
            userRepository.save(user);
            log.info("Created development-only test user {}", phoneNumber);
        };
    }
}
