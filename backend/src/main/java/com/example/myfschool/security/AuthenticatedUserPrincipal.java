package com.example.myfschool.security;

import com.example.myfschool.enums.UserStatus;
import org.springframework.security.core.authority.AuthorityUtils;

public final class AuthenticatedUserPrincipal
        extends org.springframework.security.core.userdetails.User {

    private final Long userId;

    public AuthenticatedUserPrincipal(com.example.myfschool.entity.User user) {
        super(
                user.getPhoneNumber(),
                user.getPassword(),
                user.getStatus() != UserStatus.INACTIVE,
                true,
                true,
                user.getStatus() != UserStatus.LOCKED,
                AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().name())
        );
        this.userId = user.getId();
    }

    public Long getUserId() {
        return userId;
    }
}
