package com.example.myfschool.security;

import com.example.myfschool.entity.User;
import com.example.myfschool.repository.UserRepository;
import com.example.myfschool.repository.UserRoleRepository;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        var authorities = userRoleRepository.findByUser_IdOrderByIdAsc(user.getId()).stream()
                .map(userRole -> new SimpleGrantedAuthority("ROLE_" + userRole.getRole().getCode()))
                .collect(Collectors.toSet());
        return new AuthenticatedUserPrincipal(user, authorities);
    }
}
