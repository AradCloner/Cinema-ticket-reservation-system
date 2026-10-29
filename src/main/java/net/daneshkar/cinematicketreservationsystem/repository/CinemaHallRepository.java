package net.daneshkar.cinematicketreservationsystem.repository;

import net.daneshkar.cinematicketreservationsystem.entity.CinemaHall;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CinemaHallRepository extends JpaRepository<CinemaHall, Long> {
    Optional<CinemaHall> findFirstByName(String name);
}
