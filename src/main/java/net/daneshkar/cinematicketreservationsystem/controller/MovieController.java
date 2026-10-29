package net.daneshkar.cinematicketreservationsystem.controller;

import lombok.RequiredArgsConstructor;
import net.daneshkar.cinematicketreservationsystem.dto.ApiResponse;
import net.daneshkar.cinematicketreservationsystem.dto.movie.MovieResponseDto;
import net.daneshkar.cinematicketreservationsystem.service.MovieService;
import net.daneshkar.cinematicketreservationsystem.service.TicketService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;
    private final TicketService ticketService;

    @GetMapping("/{id}/available-seats")
    public ResponseEntity<ApiResponse<List<Integer>>> availableSeats(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("صندلی‌های آزاد", ticketService.availableSeats(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MovieResponseDto>>> getMovies() {
        return ResponseEntity.ok(new ApiResponse<>("فهرست فیلم‌ها", movieService.getMovies()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponseDto>> getMovie(@PathVariable Long id) {
        return ResponseEntity.ok(new ApiResponse<>("اطلاعات فیلم", movieService.getMovie(id)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MovieResponseDto>>> searchMovies(@RequestParam String title) {
        return ResponseEntity.ok(new ApiResponse<>("نتایج جستجوی فیلم‌ها", movieService.searchMovies(title)));
    }
}
