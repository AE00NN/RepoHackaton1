package com.example.demo.security;


import com.example.demo.UserRepository.UserRepository;
import com.example.demo.domain.User;
import com.example.demo.domain.UserRole;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.ConflictException;
import com.example.demo.security.dto.SignUpRequest;
import com.example.demo.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;


    public TokenResponse signUp(SignUpRequest request) {
        // Validar duplicados
        if (userRepository.findAll().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(request.getEmail()))) {
            throw new ConflictException("El correo electrónico ya está registrado");
        }

        validateBranchRole(request);

        // Crear nuevo usuario
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.valueOf(request.getRole().toUpperCase()));
        user.setBranch(request.getBranch());

        userRepository.save(user);


        // Generar token JWT
        String token = jwtService.generateToken(user);
        return new TokenResponse(token);
    }


    public TokenResponse signIn(String username, String password){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        username,
                        password
                )
        );
        var account = userRepository.findByEmail(username).orElseThrow();
        var token = jwtService.generateToken(account);
        return new TokenResponse(token);
    }

    private void validateBranchRole(SignUpRequest request) {
        String role = request.getRole();
        String branch = request.getBranch();

        if (role == null) {
            throw new BadRequestException("El rol no puede ser nulo");
        }

        if (role.equalsIgnoreCase("BRANCH") && (branch == null || branch.isBlank())) {
            throw new BadRequestException("Branch es obligatorio cuando el rol es BRANCH");
        }

        if (role.equalsIgnoreCase("CENTRAL") && branch != null) {
            throw new BadRequestException("Branch debe ser null cuando el rol es CENTRAL");
        }

        if (!role.equalsIgnoreCase("BRANCH") && !role.equalsIgnoreCase("CENTRAL")) {
            throw new BadRequestException("El rol debe ser 'BRANCH' o 'CENTRAL'");
        }
    }

}



