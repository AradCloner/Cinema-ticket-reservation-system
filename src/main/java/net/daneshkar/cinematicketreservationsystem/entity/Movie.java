package net.daneshkar.cinematicketreservationsystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "movies")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Movie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // Existing movie rows also need an initial version when ddl-auto updates the schema.
    @Version
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long version;

    // Changing this field makes booking/cancellation participate in optimistic locking.
    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long reservationRevision;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private String genre;
    @Column(nullable = false)
    private Integer duration;
    @Column(nullable = false)
    private LocalDate releaseDate;
    @Column(length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_hall_id", nullable = false)
    private CinemaHall cinemaHall;

}
