package net.daneshkar.cinematicketreservationsystem.service;

import net.daneshkar.cinematicketreservationsystem.dto.ticket.*;
import net.daneshkar.cinematicketreservationsystem.entity.Movie;
import net.daneshkar.cinematicketreservationsystem.entity.Ticket;
import net.daneshkar.cinematicketreservationsystem.exception.*;
import net.daneshkar.cinematicketreservationsystem.repository.MovieRepository;
import net.daneshkar.cinematicketreservationsystem.repository.TicketRepository;
import net.daneshkar.cinematicketreservationsystem.repository.UserRepository;
import net.daneshkar.cinematicketreservationsystem.validation.BookingRule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class TicketService {
    private final TicketRepository ticketRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final BigDecimal ticketPrice;
    private final List<BookingRule> bookingRules;

    public TicketService(TicketRepository ticketRepository, MovieRepository movieRepository,
                         UserRepository userRepository,
                         @Value("${cinema.ticket-price:150000}") BigDecimal ticketPrice) {
        if (ticketPrice.signum() <= 0 || ticketPrice.scale() > 2 || ticketPrice.precision() - ticketPrice.scale() > 12) {
            throw new IllegalArgumentException("cinema.ticket-price must be positive and fit decimal(14,2)");
        }
        this.ticketRepository = ticketRepository;
        this.movieRepository = movieRepository;
        this.userRepository = userRepository;
        this.ticketPrice = ticketPrice;
        this.bookingRules = List.of(
                (movie, seat) -> {
                    if (seat < 1 || seat > movie.getCinemaHall().getCapacity()) {
                        throw new InvalidBookingException("شماره صندلی باید بین ۱ و ظرفیت سالن باشد");
                    }
                },
                (movie, seat) -> {
                    if (ticketRepository.existsByMovieIdAndSeatNumber(movie.getId(), seat)) {
                        throw new DuplicateResourceException("صندلی شماره " + seat + " قبلاً فروخته شده است");
                    }
                }
        );
    }

    @Transactional
    public TicketResponseDto book(String username, BookTicketRequestDto request) {
        Movie movie = findForReservation(request.movieId());
        bookingRules.forEach(rule -> rule.validate(movie, request.seatNumber()));
        movie.setReservationRevision(movie.getReservationRevision() + 1);
        Ticket ticket = new Ticket();
        ticket.setBuyer(userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("کاربر پیدا نشد")));
        ticket.setMovie(movie);
        ticket.setSeatNumber(request.seatNumber());
        ticket.setPurchasedAt(LocalDateTime.now());
        ticket.setPrice(ticketPrice); // The client cannot choose the price.
        return toResponse(ticketRepository.saveAndFlush(ticket));
    }

    public List<TicketResponseDto> myTickets(String username) {
        return ticketRepository.findByBuyerUsernameOrderByPurchasedAtDescIdDesc(username).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public void cancel(String username, Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("بلیت پیدا نشد"));
        if (!ticket.getBuyer().getUsername().equals(username)) {
            throw new AccessDeniedException("فقط می‌توانید بلیت خودتان را لغو کنید");
        }
        Movie movie = findForReservation(ticket.getMovie().getId());
        movie.setReservationRevision(movie.getReservationRevision() + 1);
        ticketRepository.delete(ticket);
        ticketRepository.flush();
    }

    public List<Integer> availableSeats(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("فیلم پیدا نشد"));
        var reserved = new HashSet<>(ticketRepository.findReservedSeats(movieId));
        return IntStream.rangeClosed(1, movie.getCinemaHall().getCapacity())
                .filter(seat -> !reserved.contains(seat)).boxed().toList();
    }

    public TicketStatsDto stats() {
        return ticketRepository.getStats();
    }

    private Movie findForReservation(Long id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("فیلم پیدا نشد"));
    }

    private TicketResponseDto toResponse(Ticket ticket) {
        Movie movie = ticket.getMovie();
        return new TicketResponseDto(ticket.getId(), movie.getId(), movie.getTitle(),
                movie.getCinemaHall().getId(), movie.getCinemaHall().getName(),
                ticket.getSeatNumber(), ticket.getPurchasedAt(), ticket.getPrice());
    }
}
