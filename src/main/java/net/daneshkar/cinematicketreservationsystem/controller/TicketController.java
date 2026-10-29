package net.daneshkar.cinematicketreservationsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.daneshkar.cinematicketreservationsystem.dto.ApiResponse;
import net.daneshkar.cinematicketreservationsystem.dto.ticket.BookTicketRequestDto;
import net.daneshkar.cinematicketreservationsystem.dto.ticket.TicketResponseDto;
import net.daneshkar.cinematicketreservationsystem.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<TicketResponseDto>> book(
            @Valid @RequestBody BookTicketRequestDto request, Authentication authentication) {
        return ResponseEntity.status(201).body(new ApiResponse<>("بلیت رزرو شد",
                ticketService.book(authentication.getName(), request)));
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<ApiResponse<List<TicketResponseDto>>> myTickets(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>("بلیت‌های من", ticketService.myTickets(authentication.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id, Authentication authentication) {
        ticketService.cancel(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
