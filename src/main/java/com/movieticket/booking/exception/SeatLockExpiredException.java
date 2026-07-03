package com.movieticket.booking.exception;

public class SeatLockExpiredException extends RuntimeException {
    public SeatLockExpiredException(String message) {
        super(message);
    }
}