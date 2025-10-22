package com.example.demo.security.dto;

import com.example.demo.domain.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import javax.management.relation.Role;
import java.time.LocalDate;

@Getter
@Setter
public class SignUpRequest {

    @NotBlank
    @Size(min = 3, max = 30)
    @Pattern(regexp = "^[a-zA-Z0-9_.]+$", message = "El username solo puede contener letras, números, '_' y '.'")
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8)
    private String password;


    @Enumerated(EnumType.STRING)
    @Pattern(regexp = "^(CENTRAL|BRANCH)$", message = "El rol debe ser CENTRAL o BRANCH")
    private String role;

    private String branch;
}
