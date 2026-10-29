package net.daneshkar.cinematicketreservationsystem.dto.ticket;

import java.math.BigDecimal;

public record TicketStatsDto(long totalTickets, BigDecimal totalRevenue) {
}
