package net.daneshkar.cinematicketreservationsystem.dto.ticket;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketResponseDto(Long id, Long movieId, String movieTitle,
                                Long cinemaHallId, String cinemaHallName,
                                Integer seatNumber, LocalDateTime purchasedAt, BigDecimal price) {
}
