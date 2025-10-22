package com.example.demo.unit.service;


//import com.oreo.insight.domain.dto.LoginRequest;
//import com.oreo.insight.domain.dto.RegisterRequest;
//import com.oreo.insight.domain.entity.User;
//import com.oreo.insight.domain.enums.UserRole;
//import com.oreo.insight.domain.repository.UserRepository;
//import com.oreo.insight.service.AuthService;
//import com.oreo.insight.service.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldRegisterCentralUserSuccessfully() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("oreo.admin");
        request.setEmail("admin@oreo.com");
        request.setPassword("Oreo1234");
        request.setRole(UserRole.ROLE_CENTRAL);

        when(userRepository.existsByUsername("oreo.admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@oreo.com")).thenReturn(false);
        when(passwordEncoder.encode("Oreo1234")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = authService.register(request);

        // Then
        assertThat(result.getUsername()).isEqualTo("oreo.admin");
        assertThat(result.getRole()).isEqualTo(UserRole.ROLE_CENTRAL);
        assertThat(result.getBranch()).isNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldRegisterBranchUserWithBranch() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("miraflores.user");
        request.setEmail("mira@oreo.com");
        request.setPassword("Oreo1234");
        request.setRole(UserRole.ROLE_BRANCH);
        request.setBranch("Miraflores");

        when(userRepository.existsByUsername("miraflores.user")).thenReturn(false);
        when(userRepository.existsByEmail("mira@oreo.com")).thenReturn(false);
        when(passwordEncoder.encode("Oreo1234")).thenReturn("encoded_password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        User result = authService.register(request);

        // Then
        assertThat(result.getUsername()).isEqualTo("miraflores.user");
        assertThat(result.getRole()).isEqualTo(UserRole.ROLE_BRANCH);
        assertThat(result.getBranch()).isEqualTo("Miraflores");
    }

    @Test
    void shouldThrowExceptionWhenUsernameAlreadyExists() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing.user");
        request.setEmail("test@oreo.com");
        request.setPassword("Oreo1234");
        request.setRole(UserRole.ROLE_CENTRAL);

        when(userRepository.existsByUsername("existing.user")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Username already exists");
    }

    @Test
    void shouldAuthenticateUserSuccessfully() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("oreo.admin");
        request.setPassword("Oreo1234");

        User user = User.builder()
                .username("oreo.admin")
                .email("admin@oreo.com")
                .role(UserRole.ROLE_CENTRAL)
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(tokenProvider.generateToken(authentication)).thenReturn("jwt_token");

        // When
        String token = authService.authenticate(request);

        // Then
        assertThat(token).isEqualTo("jwt_token");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void shouldThrowExceptionWhenAuthenticationFails() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("oreo.admin");
        request.setPassword("wrong_password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // When & Then
        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Bad credentials");
    }
}