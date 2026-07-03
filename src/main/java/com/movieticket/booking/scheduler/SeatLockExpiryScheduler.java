package com.movieticket.booking.scheduler;

import com.movieticket.booking.service.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically releases seat locks whose hold window has expired. */
@Component
@RequiredArgsConstructor
public class SeatLockExpiryScheduler {

    private final SeatService seatService;

    @Scheduled(cron = "${app.seat-lock.cleanup-cron:0 */1 * * * *}")
    public void releaseExpiredLocks() {
        seatService.releaseExpiredLocks();
    }
}