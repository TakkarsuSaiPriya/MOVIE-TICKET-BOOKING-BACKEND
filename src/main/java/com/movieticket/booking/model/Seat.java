package com.movieticket.booking.model;

import com.movieticket.booking.enums.SeatStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * version column enables OPTIMISTIC locking as a second line of defence.
 * Primary concurrency control is via pessimistic row lock (SELECT ... FOR UPDATE)
 * executed in BookingServiceImpl within a single DB transaction.
 */
@Entity
@Table(
        name = "seats",
        uniqueConstraints = @UniqueConstraint(columnNames = {"showtime_id", "seat_number"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Column(nullable = false)
    private String seatNumber; // e.g. A1, A2

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SeatStatus status = SeatStatus.AVAILABLE;

    private Long lockedByUserId;

    private LocalDateTime lockExpiresAt;

    @Version
    private Long version; // optimistic lock fallback
}