package net.daneshkar.cinematicketreservationsystem.repository;

import net.daneshkar.cinematicketreservationsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
