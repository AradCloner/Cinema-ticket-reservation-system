package net.daneshkar.cinematicketreservationsystem;

import net.daneshkar.cinematicketreservationsystem.dto.ticket.BookTicketRequestDto;
import net.daneshkar.cinematicketreservationsystem.entity.Movie;
import net.daneshkar.cinematicketreservationsystem.repository.MovieRepository;
import net.daneshkar.cinematicketreservationsystem.repository.TicketRepository;
import net.daneshkar.cinematicketreservationsystem.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TicketIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired MovieRepository movies;
    @Autowired TicketRepository tickets;
    @Autowired TicketService service;
    @Autowired ObjectMapper objectMapper;

    @Test
    void realUserLoginSessionAndCsrfCanBookAndCancel() throws Exception {
        var login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"password\":\"user123\"}"))
                .andExpect(status().isOk()).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        var csrfResponse = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn();
        var token = objectMapper.readTree(csrfResponse.getResponse().getContentAsString()).get("data");
        Long movieId = movies.findAll().getFirst().getId();
        var booking = mvc.perform(post("/api/tickets/book").session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .contentType(MediaType.APPLICATION_JSON).content(body(movieId, 1)))
                .andExpect(status().isCreated()).andReturn();
        long ticketId = objectMapper.readTree(booking.getResponse().getContentAsString())
                .get("data").get("id").asLong();
        mvc.perform(delete("/api/tickets/{id}", ticketId).session(session)
                        .header(token.get("headerName").asText(), token.get("token").asText()))
                .andExpect(status().isNoContent());
        assertThat(tickets.existsById(ticketId)).isFalse();
    }

    @Test
    void bookListCancelAndRebookLastSeat() throws Exception {
        Movie movie = movies.findAll().getFirst();
        int seat = movie.getCinemaHall().getCapacity();
        mvc.perform(get("/api/movies/{id}/available-seats", movie.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(seat));
        mvc.perform(post("/api/tickets/book").with(user("user1")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(movie.getId(), seat)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.seatNumber").value(seat))
                .andExpect(jsonPath("$.data.price").value(150000))
                .andExpect(jsonPath("$.data.purchasedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.buyer").doesNotExist());
        Long id = tickets.findAll().getFirst().getId();
        mvc.perform(get("/api/tickets/my-tickets").with(user("user1")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(id));
        mvc.perform(get("/api/tickets/my-tickets").with(user("another-user")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
        mvc.perform(get("/api/movies/{id}/available-seats", movie.getId()))
                .andExpect(jsonPath("$.data", not(hasItem(seat))));
        mvc.perform(delete("/api/tickets/{id}", id).with(user("user1")).with(csrf()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(get("/api/movies/{id}/available-seats", movie.getId()))
                .andExpect(jsonPath("$.data", hasItem(seat)));
        mvc.perform(post("/api/tickets/book").with(user("user1")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(movie.getId(), seat)))
                .andExpect(status().isCreated());
        assertThat(tickets.count()).isEqualTo(1);
    }

    @Test
    void duplicateSeatReturns409AndDoesNotAddTicket() throws Exception {
        Long movieId = movies.findAll().getFirst().getId();
        service.book("user1", new BookTicketRequestDto(movieId, 12));
        mvc.perform(post("/api/tickets/book").with(user("user1")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(movieId, 12)))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.path").value("/api/tickets/book"));
        assertThat(tickets.count()).isEqualTo(1);
    }

    @Test
    void invalidSeatsAndMissingFieldsReturn400WithoutSaving() throws Exception {
        Movie movie = movies.findAll().getFirst();
        for (int seat : new int[]{-1, 0, movie.getCinemaHall().getCapacity() + 1}) {
            mvc.perform(post("/api/tickets/book").with(user("user1")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(body(movie.getId(), seat)))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/tickets/book").with(user("user1")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(tickets.count()).isZero();
    }

    @Test
    void unknownMovieAndTicketReturn404() throws Exception {
        mvc.perform(get("/api/movies/{id}/available-seats", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/tickets/book").with(user("user1")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body(Long.MAX_VALUE, 1)))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/tickets/{id}", Long.MAX_VALUE).with(user("user1")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void cannotCancelAnotherBuyersTicket() throws Exception {
        Long movieId = movies.findAll().getFirst().getId();
        var ticket = service.book("user1", new BookTicketRequestDto(movieId, 1));
        mvc.perform(delete("/api/tickets/{id}", ticket.id()).with(user("other-user")).with(csrf()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.error").value("FORBIDDEN"));
        assertThat(tickets.existsById(ticket.id())).isTrue();
    }

    @Test
    void ticketEndpointsEnforceLoginRoleAndCsrf() throws Exception {
        String body = body(movies.findAll().getFirst().getId(), 1);
        mvc.perform(get("/api/tickets/my-tickets")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tickets/book").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/tickets/book").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/tickets/book").with(user("user1"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/tickets/stats").with(user("user1")))
                .andExpect(status().isForbidden());
        assertThat(tickets.count()).isZero();
    }

    @Test
    void statisticsIncludeOnlyActiveTicketsAndAreAdminOnly() throws Exception {
        mvc.perform(get("/api/admin/tickets/stats").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.totalTickets").value(0))
                .andExpect(jsonPath("$.data.totalRevenue").value(0));
        Long movieId = movies.findAll().getFirst().getId();
        var first = service.book("user1", new BookTicketRequestDto(movieId, 1));
        service.book("user1", new BookTicketRequestDto(movieId, 2));
        mvc.perform(get("/api/admin/tickets/stats").with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.data.totalTickets").value(2))
                .andExpect(jsonPath("$.data.totalRevenue").value(300000));
        service.cancel("user1", first.id());
        mvc.perform(get("/api/admin/tickets/stats").with(user("admin").roles("ADMIN")))
                .andExpect(jsonPath("$.data.totalTickets").value(1))
                .andExpect(jsonPath("$.data.totalRevenue").value(150000));
    }

    @Test
    void activeTicketPreventsMovieDeletionOrHallChange() throws Exception {
        Movie movie = movies.findAll().getFirst();
        service.book("user1", new BookTicketRequestDto(movie.getId(), 1));
        mvc.perform(delete("/api/admin/movies/{id}", movie.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/admin/movies/{id}", movie.getId())
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"title":"Updated", "genre":"Drama", "duration":100,
                                 "releaseDate":"2026-10-29", "cinemaHallId":9223372036854775807}
                                """))
                .andExpect(status().isConflict());
        assertThat(movies.existsById(movie.getId())).isTrue();
    }

    private String body(Long movieId, int seat) {
        return "{\"movieId\":" + movieId + ",\"seatNumber\":" + seat + "}";
    }
}
