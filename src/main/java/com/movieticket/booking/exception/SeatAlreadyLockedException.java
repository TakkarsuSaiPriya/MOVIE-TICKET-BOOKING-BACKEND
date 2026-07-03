package com.movieticket.booking.exception;

/** Thrown when a user tries to lock/book a seat that is already LOCKED or BOOKED by someone else. */
public class SeatAlreadyLockedException extends RuntimeException {
    public SeatAlreadyLockedException(String message) {
        super(message);
    }
}