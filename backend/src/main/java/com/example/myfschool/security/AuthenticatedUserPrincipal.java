package com.example.myfschool.security;

import com.example.myfschool.enums.UserStatus;
import com.example.myfschool.enums.SchoolStatus;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;

public final class AuthenticatedUserPrincipal
        extends org.springframework.security.core.userdetails.User {

    private final Long userId;

    public AuthenticatedUserPrincipal(com.example.myfschool.entity.User user,
            Collection<? extends GrantedAuthority> authorities) {
        super(
                user.getUsername(),
                user.getPasswordHash(),
                user.getStatus() == UserStatus.ACTIVE
                        && user.getSchool().getStatus() == SchoolStatus.ACTIVE,
                true,
                true,
                user.getStatus() != UserStatus.LOCKED,
                authorities
        );
        this.userId = user.getId();
    }

    public Long getUserId() {
        return userId;
    }
}
