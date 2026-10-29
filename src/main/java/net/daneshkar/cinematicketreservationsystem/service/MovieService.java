package net.daneshkar.cinematicketreservationsystem.service;

import lombok.RequiredArgsConstructor;
import net.daneshkar.cinematicketreservationsystem.dto.movie.MovieRequestDto;
import net.daneshkar.cinematicketreservationsystem.dto.movie.MovieResponseDto;
import net.daneshkar.cinematicketreservationsystem.entity.CinemaHall;
import net.daneshkar.cinematicketreservationsystem.entity.Movie;
import net.daneshkar.cinematicketreservationsystem.exception.ResourceNotFoundException;
import net.daneshkar.cinematicketreservationsystem.repository.CinemaHallRepository;
import net.daneshkar.cinematicketreservationsystem.repository.MovieRepository;
import net.daneshkar.cinematicketreservationsystem.repository.TicketRepository;
import net.daneshkar.cinematicketreservationsystem.exception.DuplicateResourceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieService {
    private final MovieRepository movieRepository;
    private final CinemaHallRepository cinemaHallRepository;
    private final TicketRepository ticketRepository;

    public List<MovieResponseDto> getMovies() {
        return findMovies(_ -> true);
    }

    public List<MovieResponseDto> searchMovies(String title) {
        String query = title.strip().toLowerCase(Locale.ROOT);
        Predicate<Movie> matchesTitle = movie -> movie.getTitle().toLowerCase(Locale.ROOT).contains(query);
        return findMovies(matchesTitle);
    }

    public MovieResponseDto getMovie(Long id) {
        return toResponse(findMovie(id));
    }

    @Transactional
    public MovieResponseDto createMovie(MovieRequestDto request) {
        Movie movie = new Movie();
        applyRequest(movie, request);
        return toResponse(movieRepository.save(movie));
    }

    @Transactional
    public MovieResponseDto updateMovie(Long id, MovieRequestDto request) {
        Movie movie = findMovie(id);
        applyRequest(movie, request);
        return toResponse(movieRepository.save(movie));
    }

    @Transactional
    public void deleteMovie(Long id) {
        Movie movie = findMovie(id);
        if (ticketRepository.existsByMovieId(id)) {
            throw new DuplicateResourceException("فیلم دارای بلیت فعال را نمی‌توان حذف کرد");
        }
        movieRepository.delete(movie);
    }

    private List<MovieResponseDto> findMovies(Predicate<Movie> filter) {
        Comparator<Movie> byTitle = (first, second) ->
                String.CASE_INSENSITIVE_ORDER.compare(first.getTitle(), second.getTitle());
        return movieRepository.findAll().stream()
                .filter(filter)
                .sorted(byTitle.thenComparing(Movie::getId))
                .map(this::toResponse)
                .toList();
    }

    private Movie findMovie(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("فیلم پیدا نشد"));
    }

    private void applyRequest(Movie movie, MovieRequestDto request) {
        if (movie.getId() != null && !movie.getCinemaHall().getId().equals(request.cinemaHallId())
                && ticketRepository.existsByMovieId(movie.getId())) {
            throw new DuplicateResourceException("سالن فیلم دارای بلیت فعال را نمی‌توان تغییر داد");
        }
        CinemaHall hall = cinemaHallRepository.findById(request.cinemaHallId())
                .orElseThrow(() -> new ResourceNotFoundException("سالن سینما پیدا نشد"));
        movie.setTitle(request.title().strip());
        movie.setGenre(request.genre().strip());
        movie.setDuration(request.duration());
        movie.setReleaseDate(request.releaseDate());
        movie.setDescription(request.description());
        movie.setCinemaHall(hall);
    }

    private MovieResponseDto toResponse(Movie movie) {
        return new MovieResponseDto(movie.getId(), movie.getTitle(), movie.getGenre(),
                movie.getDuration(), movie.getReleaseDate(), movie.getDescription(),
                movie.getCinemaHall().getId(), movie.getCinemaHall().getName());
    }
}
