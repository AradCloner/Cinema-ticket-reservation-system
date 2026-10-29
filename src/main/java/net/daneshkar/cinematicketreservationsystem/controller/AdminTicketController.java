package net.daneshkar.cinematicketreservationsystem.controller;

import lombok.RequiredArgsConstructor;
import net.daneshkar.cinematicketreservationsystem.dto.ApiResponse;
import net.daneshkar.cinematicketreservationsystem.dto.ticket.TicketStatsDto;
import net.daneshkar.cinematicketreservationsystem.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/tickets")
@RequiredArgsConstructor
public class AdminTicketController {
    private final TicketService ticketService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TicketStatsDto>> stats() {
        return ResponseEntity.ok(new ApiResponse<>("آمار بلیت‌های فعال", ticketService.stats()));
    }
}
