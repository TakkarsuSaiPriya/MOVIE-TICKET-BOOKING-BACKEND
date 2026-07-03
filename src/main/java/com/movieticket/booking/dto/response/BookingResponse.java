package com.movieticket.booking.dto.response;

import com.movieticket.booking.enums.BookingStatus;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingResponse {
    private Long id;
    private Long showtimeId;
    private List<String> seatNumbers;
    private BookingStatus status;
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}