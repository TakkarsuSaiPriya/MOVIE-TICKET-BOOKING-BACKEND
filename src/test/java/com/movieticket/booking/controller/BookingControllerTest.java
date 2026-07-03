package com.movieticket.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movieticket.booking.config.JwtAuthFilter;
import com.movieticket.booking.config.SecurityConfig;
import com.movieticket.booking.dto.request.BookingConfirmRequest;
import com.movieticket.booking.dto.response.BookingResponse;
import com.movieticket.booking.enums.AccessType;
import com.movieticket.booking.enums.BookingStatus;
import com.movieticket.booking.enums.RoleType;
import com.movieticket.booking.exception.ResourceNotFoundException;
import com.movieticket.booking.exception.SeatLockExpiredException;
import com.movieticket.booking.exception.UnauthorizedAccessException;
import com.movieticket.booking.model.Role;
import com.movieticket.booking.model.User;
import com.movieticket.booking.security.CustomUserDetails;
import com.movieticket.booking.security.JwtUtil;
import com.movieticket.booking.security.UserDetailsServiceImpl;
import com.movieticket.booking.service.BookingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
@org.springframework.context.annotation.Import({SecurityConfig.class, JwtAuthFilter.class})
class BookingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private BookingService bookingService;
    @MockBean private UserDetailsServiceImpl userDetailsService;
    @MockBean private JwtUtil jwtUtil; // required by JwtAuthFilter's constructor

    private UsernamePasswordAuthenticationToken customerAuth() {
        Set<Role> roles = new HashSet<>();
        roles.add(Role.builder().id(1L).name(RoleType.ROLE_CUSTOMER).accessType(AccessType.EXTERNAL).build());
        User user = User.builder().id(10L).username("john").password("pw").enabled(true).roles(roles).build();
        CustomUserDetails principal = new CustomUserDetails(user);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Test
    void confirm_returns200_onSuccess() throws Exception {
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("PAY1").build();
        when(bookingService.confirmBooking(any(), anyLong())).thenReturn(
                BookingResponse.builder().id(500L).status(BookingStatus.CONFIRMED).build());

        mockMvc.perform(post("/api/bookings/confirm")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(customerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void confirm_returns410_whenLockExpired() throws Exception {
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("PAY1").build();
        when(bookingService.confirmBooking(any(), anyLong())).thenThrow(new SeatLockExpiredException("expired"));

        mockMvc.perform(post("/api/bookings/confirm")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(customerAuth())))
                .andExpect(status().isGone());
    }

    @Test
    void confirm_returns404_whenBookingNotFound() throws Exception {
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(999L).paymentReference("PAY1").build();
        when(bookingService.confirmBooking(any(), anyLong())).thenThrow(new ResourceNotFoundException("Booking not found"));

        mockMvc.perform(post("/api/bookings/confirm")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(customerAuth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirm_returns403_whenNotOwnedByUser() throws Exception {
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("PAY1").build();
        when(bookingService.confirmBooking(any(), anyLong())).thenThrow(new UnauthorizedAccessException("not yours"));

        mockMvc.perform(post("/api/bookings/confirm")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(customerAuth())))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirm_returns401_whenUnauthenticated() throws Exception {
        BookingConfirmRequest request = BookingConfirmRequest.builder().bookingId(500L).paymentReference("PAY1").build();

        mockMvc.perform(post("/api/bookings/confirm")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirm_returns400_whenBookingIdMissing() throws Exception {
        BookingConfirmRequest request = BookingConfirmRequest.builder().paymentReference("PAY1").build();

        mockMvc.perform(post("/api/bookings/confirm")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request))
                        .with(authentication(customerAuth())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancel_returns200_onSuccess() throws Exception {
        when(bookingService.cancelBooking(anyLong(), anyLong())).thenReturn(
                BookingResponse.builder().id(500L).status(BookingStatus.CANCELLED).build());

        mockMvc.perform(post("/api/bookings/500/cancel").with(authentication(customerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void cancel_returns404_whenBookingNotFound() throws Exception {
        when(bookingService.cancelBooking(anyLong(), anyLong())).thenThrow(new ResourceNotFoundException("not found"));

        mockMvc.perform(post("/api/bookings/999/cancel").with(authentication(customerAuth())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/bookings/500/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void myBookings_returns200_withList() throws Exception {
        when(bookingService.getUserBookings(anyLong())).thenReturn(
                List.of(BookingResponse.builder().id(500L).build()));

        mockMvc.perform(get("/api/bookings/my").with(authentication(customerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(500));
    }

    @Test
    void myBookings_returns200_withEmptyList() throws Exception {
        when(bookingService.getUserBookings(anyLong())).thenReturn(List.of());

        mockMvc.perform(get("/api/bookings/my").with(authentication(customerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void myBookings_returns401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/bookings/my"))
                .andExpect(status().isUnauthorized());
    }
}