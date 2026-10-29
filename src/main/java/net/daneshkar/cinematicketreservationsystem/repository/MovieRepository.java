package net.daneshkar.cinematicketreservationsystem.repository;

import net.daneshkar.cinematicketreservationsystem.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    @Override
    @EntityGraph(attributePaths = "cinemaHall")
    List<Movie> findAll();

    boolean existsByTitleIgnoreCase(String title);
}
