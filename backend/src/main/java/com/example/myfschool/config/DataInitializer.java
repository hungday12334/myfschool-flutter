package com.example.myfschool.config;

import com.example.myfschool.entity.Parent;
import com.example.myfschool.entity.Role;
import com.example.myfschool.entity.School;
import com.example.myfschool.entity.User;
import com.example.myfschool.entity.UserRole;
import com.example.myfschool.enums.SchoolLevel;
import com.example.myfschool.enums.SchoolStatus;
import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.repository.ParentRepository;
import com.example.myfschool.repository.RoleRepository;
import com.example.myfschool.repository.SchoolRepository;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.repository.UserRoleRepository;
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
    CommandLineRunner seedDevelopmentAccount(
            SchoolRepository schools,
            RoleRepository roles,
            UserRepository users,
            UserRoleRepository userRoles,
            ParentRepository parents,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            School school = schools.findBySchoolCode("MFS-LOCAL").orElseGet(() -> {
                School newSchool = new School();
                newSchool.setSchoolCode("MFS-LOCAL");
                newSchool.setName("MyFSchool Local");
                newSchool.setSchoolLevel(SchoolLevel.COMBINED);
                newSchool.setStatus(SchoolStatus.ACTIVE);
                return schools.save(newSchool);
            });

            Role parentRole = roles.findByCode("PARENT").orElseGet(() -> {
                Role newRole = new Role();
                newRole.setCode("PARENT");
                newRole.setName("Parent");
                return roles.save(newRole);
            });

            User user = users.findByUsername("parent_demo").orElseGet(() -> {
                User newUser = new User();
                newUser.setSchool(school);
                newUser.setUsername("parent_demo");
                newUser.setPasswordHash(passwordEncoder.encode("Password@123"));
                newUser.setEmail("parent@myfschool.local");
                newUser.setStatus(UserStatus.ACTIVE);
                newUser.setMustChangePassword(false);
                return users.save(newUser);
            });

            if (userRoles.findByUser_IdOrderByIdAsc(user.getId()).stream()
                    .noneMatch(userRole -> "PARENT".equals(userRole.getRole().getCode()))) {
                UserRole assignment = new UserRole();
                assignment.setUser(user);
                assignment.setRole(parentRole);
                userRoles.save(assignment);
            }

            if (!parents.existsByUser_Id(user.getId())) {
                Parent parent = new Parent();
                parent.setSchool(school);
                parent.setUser(user);
                parent.setFullName("Local Demo Parent");
                parent.setEmail(user.getEmail());
                parents.save(parent);
            }
            log.info("Development login is available as parent_demo / Password@123");
        };
    }
}
