package com.example.demo.service;



import com.example.demo.UserRepository.UserRepository;
import com.example.demo.domain.User;
import com.example.demo.domain.UserRole;
import com.example.demo.dto.UserRequestDto;
import com.example.demo.dto.UserResponseDto;
import com.example.demo.exceptions.BadRequestException;
import com.example.demo.exceptions.ConflictException;
import com.example.demo.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }


    public UserResponseDto createUser(UserRequestDto userDto) {
        // Validar duplicado de email
        if (userRepository.findAll().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(userDto.getEmail()))) {
            throw new ConflictException("El correo electrónico ya está registrado");
        }

        // Validar relación role-branch
        validateBranchRole(userDto);

        // Mapear DTO a entidad
        User user = modelMapper.map(userDto, User.class);


        user.setRole(UserRole.valueOf(String.valueOf(userDto.getRole())));

        // Guardar usuario
        User saved = userRepository.save(user);

        // Retornar DTO de respuesta
        return modelMapper.map(saved, UserResponseDto.class);
    }

    public List<UserResponseDto> getAllUsers() {
        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            throw new ResourceNotFoundException("No hay usuarios registrados");
        }
        return users.stream()
                .map(u -> modelMapper.map(u, UserResponseDto.class))
                .collect(Collectors.toList());
    }

    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado"));
        return modelMapper.map(user, UserResponseDto.class);
    }


    public UserResponseDto updateUser(Long id, UserRequestDto userDto) {
        User existing = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado"));

        // Validar email duplicado si cambió
        if (!existing.getEmail().equalsIgnoreCase(userDto.getEmail()) &&
                userRepository.findAll().stream()
                        .anyMatch(u -> u.getEmail().equalsIgnoreCase(userDto.getEmail()))) {
            throw new ConflictException("El correo electrónico ya está en uso");
        }

        // Validar relación role–branch
        validateBranchRole(userDto);

        // Mapear cambios
        modelMapper.map(userDto, existing);
        existing.setRole(UserRole.valueOf(String.valueOf(userDto.getRole())));

        // Guardar actualización
        User updated = userRepository.save(existing);
        return modelMapper.map(updated, UserResponseDto.class);
    }

    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado"));
        userRepository.delete(user);
    }

    public UserResponseDto getUserByEmail(String email) {
        User user = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró usuario con correo: " + email));
        return modelMapper.map(user, UserResponseDto.class);
    }

    public UserResponseDto promoteToAdmin(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario con ID " + id + " no encontrado"));

        if (user.getRole() == UserRole.CENTRAL) {
            throw new BadRequestException("El usuario ya es administrador");
        }

        user.setRole(UserRole.CENTRAL);
        User updated = userRepository.save(user);
        return modelMapper.map(updated, UserResponseDto.class);
    }

    // ---------- Validación personalizada ----------
    private void validateBranchRole(UserRequestDto dto) {
        UserRole role = dto.getRole();
        String branch = dto.getBranch();

        if (role == null) {
            throw new BadRequestException("El rol no puede ser nulo");
        }

        if (role == UserRole.BRANCH && (branch == null || branch.isBlank())) {
            throw new BadRequestException("Branch es obligatorio cuando el rol es BRANCH");
        }

        if (role == UserRole.CENTRAL && branch != null) {
            throw new BadRequestException("Branch debe ser null cuando el rol es CENTRAL");
        }
    }
}
