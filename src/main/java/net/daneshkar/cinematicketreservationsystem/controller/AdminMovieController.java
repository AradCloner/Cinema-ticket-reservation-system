package net.daneshkar.cinematicketreservationsystem.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.daneshkar.cinematicketreservationsystem.dto.ApiResponse;
import net.daneshkar.cinematicketreservationsystem.dto.movie.MovieRequestDto;
import net.daneshkar.cinematicketreservationsystem.dto.movie.MovieResponseDto;
import net.daneshkar.cinematicketreservationsystem.service.MovieService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class AdminMovieController {
    private final MovieService movieService;

    @PostMapping
    public ResponseEntity<ApiResponse<MovieResponseDto>> createMovie(@Valid @RequestBody MovieRequestDto request) {
        MovieResponseDto movie = movieService.createMovie(request);
        return ResponseEntity.created(URI.create("/api/movies/" + movie.id()))
                .body(new ApiResponse<>("فیلم ایجاد شد", movie));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MovieResponseDto>> updateMovie(
            @PathVariable Long id, @Valid @RequestBody MovieRequestDto request) {
        return ResponseEntity.ok(new ApiResponse<>("فیلم ویرایش شد", movieService.updateMovie(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
