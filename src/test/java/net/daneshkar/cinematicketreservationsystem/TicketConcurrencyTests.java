package net.daneshkar.cinematicketreservationsystem;

import net.daneshkar.cinematicketreservationsystem.dto.ticket.BookTicketRequestDto;
import net.daneshkar.cinematicketreservationsystem.entity.*;
import net.daneshkar.cinematicketreservationsystem.entity.enums.Role;
import net.daneshkar.cinematicketreservationsystem.repository.*;
import net.daneshkar.cinematicketreservationsystem.service.TicketService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** No outer test transaction: both worker transactions must really commit or roll back. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketConcurrencyTests {
    @Autowired MockMvc mvc;
    @Autowired MovieRepository movies;
    @Autowired CinemaHallRepository halls;
    @Autowired TicketRepository tickets;
    @Autowired UserRepository users;
    @Autowired TicketService service;
    @Autowired PlatformTransactionManager transactionManager;
    private Long movieId;
    private Long secondUserId;
    private static final String SECOND_BUYER = "concurrency-buyer";

    @BeforeEach
    void createCommittedFixture() {
        secondUserId = users.saveAndFlush(User.builder().username(SECOND_BUYER)
                .email("concurrency-buyer@example.com").password("unused-in-mock-authentication")
                .role(Role.USER).build()).getId();
        movieId = movies.saveAndFlush(Movie.builder().title("Concurrency test movie").genre("Test")
                .duration(90).releaseDate(LocalDate.of(2026, 10, 29))
                .cinemaHall(halls.findFirstByName("سالن شماره 1").orElseThrow()).build()).getId();
    }

    @AfterEach
    void removeOnlyOwnFixture() {
        new TransactionTemplate(transactionManager).executeWithoutResult(_ -> {
            tickets.findAll().stream().filter(t -> t.getMovie().getId().equals(movieId))
                    .forEach(tickets::delete);
            tickets.flush();
            movies.deleteById(movieId);
            users.deleteById(secondUserId);
        });
    }

    @Test
    void twoBuyersCannotPurchaseSameSeatConcurrently() throws Exception {
        var start = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> first = () -> bookOverHttp(start, "user1");
            Callable<Integer> second = () -> bookOverHttp(start, SECOND_BUYER);
            var a = executor.submit(first);
            var b = executor.submit(second);
            List<Integer> statuses = List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
            assertThat(tickets.findReservedSeats(movieId)).containsExactly(12);
            System.out.println("CONCURRENCY SAME SEAT: two buyers, HTTP " + statuses
                    + ", committed tickets = 1 [PASS]");
        }
    }

    @Test
    void staleMovieVersionRollsBackEvenWhenSeatsDiffer() throws Exception {
        var bothReadSameVersion = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(() -> bookWithSameSnapshot(bothReadSameVersion, "user1", 1));
            var b = executor.submit(() -> bookWithSameSnapshot(bothReadSameVersion, SECOND_BUYER, 2));
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
            assertThat(tickets.findReservedSeats(movieId)).hasSize(1);
            System.out.println("OPTIMISTIC LOCK: same movie version, different seats,"
                    + " one commit + one rollback, committed tickets = 1 [PASS]");
        }
    }

    private int bookOverHttp(CyclicBarrier start, String username) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        return mvc.perform(post("/api/tickets/book").with(user(username)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"movieId\":" + movieId + ",\"seatNumber\":12}"))
                .andReturn().getResponse().getStatus();
    }

    private boolean bookWithSameSnapshot(CyclicBarrier barrier, String buyer, int seat) {
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(_ -> {
                // Ensure both persistence contexts hold the same original version.
                movies.findById(movieId).orElseThrow();
                try {
                    barrier.await(10, TimeUnit.SECONDS);
                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }
                service.book(buyer, new BookTicketRequestDto(movieId, seat));
            });
            return true;
        } catch (OptimisticLockingFailureException expected) {
            return false;
        }
    }
}
