package net.daneshkar.cinematicketreservationsystem.dto;

public record ApiResponse<T>(
        String message,
        T data
) {
}
