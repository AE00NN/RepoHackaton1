package com.example.demo.unit.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
//import com.oreo.insight.controller.AuthController;
//import com.oreo.insight.domain.dto.LoginRequest;
//import com.oreo.insight.domain.dto.RegisterRequest;
//import com.oreo.insight.domain.entity.User;
//import com.oreo.insight.domain.enums.UserRole;
//import com.oreo.insight.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void shouldRegisterCentralUserSuccessfully() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("oreo.admin");
        request.setEmail("admin@oreo.com");
        request.setPassword("Oreo1234");
        request.setRole(UserRole.ROLE_CENTRAL);

        User mockUser = User.builder()
                .id("user_123")
                .username("oreo.admin")
                .email("admin@oreo.com")
                .role(UserRole.ROLE_CENTRAL)
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("user_123"))
                .andExpect(jsonPath("$.username").value("oreo.admin"))
                .andExpect(jsonPath("$.email").value("admin@oreo.com"))
                .andExpect(jsonPath("$.role").value("ROLE_CENTRAL"))
                .andExpect(jsonPath("$.branch").doesNotExist());
    }

    @Test
    void shouldRegisterBranchUserWithBranch() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("miraflores.user");
        request.setEmail("mira@oreo.com");
        request.setPassword("Oreo1234");
        request.setRole(UserRole.ROLE_BRANCH);
        request.setBranch("Miraflores");

        User mockUser = User.builder()
                .id("user_456")
                .username("miraflores.user")
                .email("mira@oreo.com")
                .role(UserRole.ROLE_BRANCH)
                .branch("Miraflores")
                .createdAt(LocalDateTime.now())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockUser);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("miraflores.user"))
                .andExpect(jsonPath("$.role").value("ROLE_BRANCH"))
                .andExpect(jsonPath("$.branch").value("Miraflores"));
    }

    @Test
    void shouldValidateRegisterRequest() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest();
        invalidRequest.setUsername("ab"); // Too short
        invalidRequest.setEmail("invalid-email");
        invalidRequest.setPassword("short"); // Too short
        // Missing role

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldValidateBranchForBranchRole() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("branch.user");
        request.setEmail("branch@oreo.com");
        request.setPassword("Oreo1234");
        request.setRole(UserRole.ROLE_BRANCH);
        // Missing branch - should be invalid

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("oreo.admin");
        request.setPassword("Oreo1234");

        when(authService.authenticate(any(LoginRequest.class))).thenReturn("jwt_token_123");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt_token_123"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").exists());
    }

    @Test
    void shouldHandleLoginFailure() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("oreo.admin");
        request.setPassword("wrong_password");

        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldValidateLoginRequest() throws Exception {
        LoginRequest invalidRequest = new LoginRequest();
        invalidRequest.setUsername(""); // Empty username
        invalidRequest.setPassword(""); // Empty password

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleDuplicateUsernameRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing.user");
        request.setEmail("test@oreo.com");
        request.setPassword("Oreo1234");
        request.setRole(UserRole.ROLE_CENTRAL);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleDuplicateEmailRegistration() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("new.user");
        request.setEmail("existing@oreo.com");
        request.setPassword("Oreo1234");
        request.setRole(UserRole.ROLE_CENTRAL);

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email already exists"));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}