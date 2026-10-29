package net.daneshkar.cinematicketreservationsystem.dto.ticket;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookTicketRequestDto(@NotNull @Positive Long movieId,
                                   @NotNull @Positive Integer seatNumber) {
}
