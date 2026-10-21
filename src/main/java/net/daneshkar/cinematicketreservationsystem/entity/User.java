package net.daneshkar.cinematicketreservationsystem.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import net.daneshkar.cinematicketreservationsystem.entity.enums.Role;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(
        name = "user_tb",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_username",
                        columnNames = "username"
                ),
                @UniqueConstraint(
                        name = "uk_user_email",
                        columnNames = "email"
                )
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

}