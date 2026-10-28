package net.daneshkar.cinematicketreservationsystem.entity;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "cinema_halls")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CinemaHall {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer capacity;
}
