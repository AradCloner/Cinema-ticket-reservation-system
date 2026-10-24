package net.daneshkar.cinematicketreservationsystem.dto.auth;

import lombok.Builder;
import net.daneshkar.cinematicketreservationsystem.entity.enums.Role;

@Builder
public record UserResponseDto(
        long userId,
        String username,
        String email,
        Role role
) {
}
