package net.daneshkar.cinematicketreservationsystem.dto.movie;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MovieRequestDto(
        @NotBlank
        @Size(max = 255)
        String title,
        @NotBlank
        @Size(max = 255)
        String genre,
        @NotNull
        @Positive
        Integer duration,
        @NotNull
        LocalDate releaseDate,
        @Size(max = 2000)
        String description,
        @NotNull
        @Positive
        Long cinemaHallId
) {
}
