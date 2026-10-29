package net.daneshkar.cinematicketreservationsystem.repository;

import net.daneshkar.cinematicketreservationsystem.dto.ticket.TicketStatsDto;
import net.daneshkar.cinematicketreservationsystem.entity.Ticket;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    boolean existsByMovieIdAndSeatNumber(Long movieId, Integer seatNumber);

    boolean existsByMovieId(Long movieId);

    @EntityGraph(attributePaths = {"movie", "movie.cinemaHall"})
    List<Ticket> findByBuyerUsernameOrderByPurchasedAtDescIdDesc(String username);

    @Query("select t.seatNumber from Ticket t where t.movie.id = :movieId")
    List<Integer> findReservedSeats(Long movieId);

    // Count and sum are calculated in one database statement for a consistent snapshot.
    @Query("select new net.daneshkar.cinematicketreservationsystem.dto.ticket.TicketStatsDto(" +
            "count(t), coalesce(sum(t.price), 0)) from Ticket t")
    TicketStatsDto getStats();
}
