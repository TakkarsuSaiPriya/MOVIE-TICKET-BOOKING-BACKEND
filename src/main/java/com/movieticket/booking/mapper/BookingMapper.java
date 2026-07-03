package com.movieticket.booking.mapper;

import com.movieticket.booking.dto.response.BookingResponse;
import com.movieticket.booking.model.Booking;
import com.movieticket.booking.model.Seat;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "showtimeId", expression = "java(booking.getShowtime().getId())")
    @Mapping(target = "seatNumbers", expression = "java(mapSeatNumbers(booking.getSeats()))")
    BookingResponse toResponse(Booking booking);

    default List<String> mapSeatNumbers(java.util.Set<Seat> seats) {
        return seats.stream().map(Seat::getSeatNumber).collect(Collectors.toList());
    }
}