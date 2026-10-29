package net.daneshkar.cinematicketreservationsystem.dto.auth;

public record CsrfResponseDto(String headerName, String parameterName, String token) {
}
