package com.parttimejob.util;

import com.parttimejob.entity.User;
import com.parttimejob.enums.Role;
import com.parttimejob.exception.UnauthorizedException;
import com.parttimejob.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public Optional<String> getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }

    public User getCurrentUser() {
        String email = getCurrentUserEmail()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
        
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Authenticated user not found in database"));
    }

    public boolean isCurrentUserAdmin() {
        try {
            User user = getCurrentUser();
            return user.getRole() == Role.ROLE_ADMIN;
        } catch (Exception e) {
            return false;
        }
    }
}
