package net.daneshkar.cinematicketreservationsystem.validation;

import net.daneshkar.cinematicketreservationsystem.entity.Movie;

/** A reservation rule either accepts the seat or throws a business exception. */
@FunctionalInterface
public interface BookingRule {
    void validate(Movie movie, int seatNumber);
}
