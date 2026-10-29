package net.daneshkar.cinematicketreservationsystem.dto.movie;

import java.time.LocalDate;

public record MovieResponseDto(
        Long id,
        String title,
        String genre,
        Integer duration,
        LocalDate releaseDate,
        String description,
        Long cinemaHallId,
        String cinemaHallName
) {
}
