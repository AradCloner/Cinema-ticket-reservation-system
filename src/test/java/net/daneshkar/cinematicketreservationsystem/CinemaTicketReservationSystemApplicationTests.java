package net.daneshkar.cinematicketreservationsystem;

import net.daneshkar.cinematicketreservationsystem.config.DataLoader;
import net.daneshkar.cinematicketreservationsystem.dto.auth.LoginRequestDto;
import net.daneshkar.cinematicketreservationsystem.dto.movie.MovieRequestDto;
import net.daneshkar.cinematicketreservationsystem.entity.CinemaHall;
import net.daneshkar.cinematicketreservationsystem.entity.Movie;
import net.daneshkar.cinematicketreservationsystem.repository.CinemaHallRepository;
import net.daneshkar.cinematicketreservationsystem.repository.MovieRepository;
import net.daneshkar.cinematicketreservationsystem.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CinemaTicketReservationSystemApplicationTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private CinemaHallRepository cinemaHallRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private DataLoader dataLoader;

    @Test
    void publicMoviesAreSortedAndDoNotExposeEntities() throws Exception {
        mvc.perform(get("/api/movies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].title").value("Inception"))
                .andExpect(jsonPath("$.data[1].title").value("Interstellar"))
                .andExpect(jsonPath("$.data[0].cinemaHallName").value("سالن شماره 1"))
                .andExpect(jsonPath("$.data[0].cinemaHall").doesNotExist());
    }

    @Test
    void searchMatchesPartialTitleIgnoringCaseAndWhitespace() throws Exception {
        mvc.perform(get("/api/movies/search").param("title", "  CePt  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Inception"));
        mvc.perform(get("/api/movies/search").param("title", "no-such-title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void publicMovieDetailsAndMissingMovie() throws Exception {
        Movie movie = movieRepository.findAll().getFirst();
        mvc.perform(get("/api/movies/{id}", movie.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(movie.getId()));
        mvc.perform(get("/api/movies/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void malformedRequestsReturnStandardBadRequest() throws Exception {
        mvc.perform(get("/api/movies/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/movies/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/movies/not-a-number"));
        mvc.perform(post("/api/admin/movies").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }

    @Test
    void adminCanCreateUpdateAndDeleteMovie() throws Exception {
        String created = mvc.perform(post("/api/admin/movies")
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("New movie"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.data.title").value("New movie"))
                .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).path("data").path("id").asLong();

        CinemaHall anotherHall = cinemaHallRepository.save(CinemaHall.builder()
                .name("Second hall").capacity(30).build());
        MovieRequestDto update = new MovieRequestDto("Updated movie", "Drama", 90,
                LocalDate.of(2026, 1, 1), "Updated description", anotherHall.getId());
        mvc.perform(put("/api/admin/movies/{id}", id).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Updated movie"))
                .andExpect(jsonPath("$.data.cinemaHallId").value(anotherHall.getId()));

        mvc.perform(delete("/api/admin/movies/{id}", id)
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/movies/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void anonymousUserCannotCreateMovie() throws Exception {
        mvc.perform(post("/api/admin/movies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Denied movie"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void normalUserCannotModifyMovies() throws Exception {
        Movie movie = movieRepository.findAll().getFirst();
        String body = objectMapper.writeValueAsString(validRequest("Denied movie"));
        mvc.perform(post("/api/admin/movies").with(user("user1").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        mvc.perform(put("/api/admin/movies/{id}", movie.getId())
                        .with(user("user1").roles("USER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/admin/movies/{id}", movie.getId())
                        .with(user("user1").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
        assertThat(movieRepository.existsById(movie.getId())).isTrue();
    }

    @Test
    void adminStillNeedsCsrfToken() throws Exception {
        mvc.perform(post("/api/admin/movies").with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Missing token"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void invalidMovieDoesNotGetSaved() throws Exception {
        long before = movieRepository.count();
        MovieRequestDto invalid = new MovieRequestDto(" ", "", 0, null, "x".repeat(2001), null);
        mvc.perform(post("/api/admin/movies").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        assertThat(movieRepository.count()).isEqualTo(before);
    }

    @Test
    void missingHallDoesNotCreateMovie() throws Exception {
        long before = movieRepository.count();
        MovieRequestDto invalid = new MovieRequestDto("No hall", "Drama", 90,
                LocalDate.of(2026, 1, 1), null, Long.MAX_VALUE);
        mvc.perform(post("/api/admin/movies").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("سالن سینما پیدا نشد"));
        assertThat(movieRepository.count()).isEqualTo(before);
    }

    @Test
    void missingMovieCannotBeUpdatedOrDeleted() throws Exception {
        mvc.perform(put("/api/admin/movies/{id}", Long.MAX_VALUE)
                        .with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Missing"))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/admin/movies/{id}", Long.MAX_VALUE)
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void seederDoesNotDuplicateDataAndPasswordsAreEncoded() throws Exception {
        long movies = movieRepository.count();
        long halls = cinemaHallRepository.count();
        long users = userRepository.count();
        dataLoader.run();
        dataLoader.run();
        assertThat(movieRepository.count()).isEqualTo(movies);
        assertThat(cinemaHallRepository.count()).isEqualTo(halls);
        assertThat(userRepository.count()).isEqualTo(users);
        assertThat(passwordEncoder.matches("admin123",
                userRepository.findByUsername("admin").orElseThrow().getPassword())).isTrue();
        assertThat(cinemaHallRepository.findFirstByName("سالن شماره 1").orElseThrow().getCapacity()).isEqualTo(50);
    }

    @Test
    void adminSessionCanGetCsrfAndCreateMovieThenLogout() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("admin", "admin123"))))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
        String csrfBody = mvc.perform(get("/api/auth/csrf").session(session))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var token = objectMapper.readTree(csrfBody).path("data");
        mvc.perform(post("/api/admin/movies").session(session)
                        .header(token.path("headerName").asText(), token.path("token").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest("Session movie"))))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/auth/logout").session(session)).andExpect(status().isOk());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void badLoginReturnsStandardError() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDto("admin", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    private MovieRequestDto validRequest(String title) {
        Long hallId = cinemaHallRepository.findFirstByName("سالن شماره 1").orElseThrow().getId();
        return new MovieRequestDto(title, "Drama", 120, LocalDate.of(2026, 1, 1), "Description", hallId);
    }
}
