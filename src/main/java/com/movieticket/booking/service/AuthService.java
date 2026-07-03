package com.movieticket.booking.service;

import com.movieticket.booking.dto.request.LoginRequest;
import com.movieticket.booking.dto.request.RegisterRequest;
import com.movieticket.booking.dto.response.LoginResponse;

public interface AuthService {
    void register(RegisterRequest request);
    LoginResponse login(LoginRequest request);
}