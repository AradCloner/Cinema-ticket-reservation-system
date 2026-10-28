package net.daneshkar.cinematicketreservationsystem.service;

import lombok.RequiredArgsConstructor;
import net.daneshkar.cinematicketreservationsystem.dto.auth.LoginRequestDto;
import net.daneshkar.cinematicketreservationsystem.dto.auth.RegisterRequestDto;
import net.daneshkar.cinematicketreservationsystem.dto.auth.UserResponseDto;
import net.daneshkar.cinematicketreservationsystem.entity.User;
import net.daneshkar.cinematicketreservationsystem.entity.enums.Role;
import net.daneshkar.cinematicketreservationsystem.exception.DuplicateResourceException;
import net.daneshkar.cinematicketreservationsystem.exception.ResourceNotFoundException;
import net.daneshkar.cinematicketreservationsystem.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponseDto register(RegisterRequestDto registerRequestDto) {
        if (userRepository.existsByEmail(registerRequestDto.email())) {
            throw new DuplicateResourceException("Email already exists");
        }

        if (userRepository.existsByUsername(registerRequestDto.username())) {
            throw new DuplicateResourceException("Username already exists");
        }

        User user = userRepository.save(
                User.builder()
                        .username(registerRequestDto.username())
                        .email(registerRequestDto.email())
                        .password(passwordEncoder.encode(registerRequestDto.password()))
                        .role(Role.USER)
                        .build()
        );

        return UserResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    public UserResponseDto getCurrentUser(String username) {
        Optional<User> byUsername = userRepository.findByUsername(username);

        return byUsername.map(value -> UserResponseDto.builder()
                .userId(value.getId())
                .username(value.getUsername())
                .email(value.getEmail())
                .role(value.getRole())
                .build()).orElseThrow(()-> new ResourceNotFoundException("کاربر پیدا نشد"));
    }
}
