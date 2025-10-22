package com.example.demo.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    @Size(min = 2, max = 100)
    private String username;

    @Enumerated(EnumType.STRING)
    private UserRole role;


    @Column(nullable = false, unique = true)
    @Email(message = "Debe ingresar un correo válido")
    @NotBlank
    private String email;


    @NotBlank
    @Size(min = 8,message = "La contraseña debe tener al menos 8 caracteres")
    private String password;


    private String branch;



    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + this.role.name()));
    }

    @CreationTimestamp
    private LocalDateTime createdAt;


    @Override
    public String getUsername() {
        return this.email;
    }


    public User(String email, String password, String username) {
        this.email = email;
        this.password = password;
        this.username = username;
    }
}
