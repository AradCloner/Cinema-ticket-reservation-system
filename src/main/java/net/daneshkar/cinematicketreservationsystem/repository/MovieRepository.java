package net.daneshkar.cinematicketreservationsystem.repository;

import net.daneshkar.cinematicketreservationsystem.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
