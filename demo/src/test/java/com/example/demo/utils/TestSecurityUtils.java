package com.example.demo.utils;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.Collections;
import java.util.List;

public class TestSecurityUtils {

    public static void setupSecurityContext(String username, String role, String branch) {
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(role)
        );

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                authorities
        );

        // Set branch as a detail in the authentication
        authentication.setDetails(new UserDetails(branch));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    public static void setupCentralUser() {
        setupSecurityContext("central.user", "ROLE_CENTRAL", null);
    }

    public static void setupBranchUser(String branch) {
        setupSecurityContext("branch.user", "ROLE_BRANCH", branch);
    }

    public static class UserDetails {
        private final String branch;

        public UserDetails(String branch) {
            this.branch = branch;
        }

        public String getBranch() {
            return branch;
        }
    }

    // Security Context Factory for annotation-based testing
    public static class WithMockCustomUserSecurityContextFactory
            implements WithSecurityContextFactory<WithMockCustomUser> {

        @Override
        public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
            SecurityContext context = SecurityContextHolder.createEmptyContext();

            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                    new SimpleGrantedAuthority(customUser.role())
            );

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    customUser.username(),
                    "password",
                    authorities
            );

            // Set branch as detail
            auth.setDetails(new UserDetails(customUser.branch()));
            context.setAuthentication(auth);
            return context;
        }
    }
}

// Custom annotation for security testing
@interface WithMockCustomUser {
    String username() default "testuser";
    String role() default "ROLE_USER";
    String branch() default "";
}
