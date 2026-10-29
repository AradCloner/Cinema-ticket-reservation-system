package net.daneshkar.cinematicketreservationsystem.config;

import lombok.RequiredArgsConstructor;
import net.daneshkar.cinematicketreservationsystem.entity.CinemaHall;
import net.daneshkar.cinematicketreservationsystem.entity.Movie;
import net.daneshkar.cinematicketreservationsystem.entity.User;
import net.daneshkar.cinematicketreservationsystem.entity.enums.Role;
import net.daneshkar.cinematicketreservationsystem.repository.CinemaHallRepository;
import net.daneshkar.cinematicketreservationsystem.repository.MovieRepository;
import net.daneshkar.cinematicketreservationsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private final UserRepository userRepository;
    private final CinemaHallRepository cinemaHallRepository;
    private final MovieRepository movieRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        seedUser("admin", "admin@example.com", "admin123", Role.ADMIN);
        seedUser("user1", "user1@example.com", "user123", Role.USER);

        CinemaHall hall = cinemaHallRepository.findFirstByName("سالن شماره 1")
                .orElseGet(() -> cinemaHallRepository.save(CinemaHall.builder()
                        .name("سالن شماره 1").capacity(50).build()));

        seedMovie("Inception", "Sci-Fi", 148, LocalDate.of(2010, 7, 16),
                "A team enters dreams to plant an idea.", hall);
        seedMovie("Interstellar", "Sci-Fi", 169, LocalDate.of(2014, 11, 7),
                "Explorers travel through a wormhole to find a new home.", hall);
        seedMovie("The Dark Knight", "Action", 152, LocalDate.of(2008, 7, 18),
                "Batman faces the Joker in Gotham City.", hall);
    }

    private void seedUser(String username, String email, String password, Role role) {
        if (!userRepository.existsByUsername(username) && !userRepository.existsByEmail(email)) {
            userRepository.save(User.builder().username(username).email(email)
                    .password(passwordEncoder.encode(password)).role(role).build());
        }
    }

    private void seedMovie(String title, String genre, int duration, LocalDate releaseDate,
                           String description, CinemaHall hall) {
        if (!movieRepository.existsByTitleIgnoreCase(title)) {
            movieRepository.save(Movie.builder().title(title).genre(genre).duration(duration)
                    .releaseDate(releaseDate).description(description).cinemaHall(hall).build());
        }
    }
}
