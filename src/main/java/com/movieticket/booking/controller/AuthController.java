package com.movieticket.booking.controller;

import com.movieticket.booking.dto.request.LoginRequest;
import com.movieticket.booking.dto.request.RegisterRequest;
import com.movieticket.booking.dto.response.ApiResponse;
import com.movieticket.booking.dto.response.LoginResponse;
import com.movieticket.booking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully with role: " +
                        (request.getRole() != null ? request.getRole() : "ROLE_CUSTOMER"), null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // Endpoint to get all available roles for frontend dropdown
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, String>>>> getRoles() {
        java.util.List<java.util.Map<String, String>> roles = java.util.List.of(
                java.util.Map.of(
                        "value", "ROLE_CUSTOMER",
                        "label", "Customer",
                        "description", "Browse movies, select seats, book and cancel own tickets",
                        "accessType", "EXTERNAL"
                ),
                java.util.Map.of(
                        "value", "ROLE_STAFF",
                        "label", "Staff",
                        "description", "All customer permissions + manage movies and showtimes via admin panel",
                        "accessType", "INTERNAL"
                ),
                java.util.Map.of(
                        "value", "ROLE_ADMIN",
                        "label", "Admin",
                        "description", "Full system access including audit logs and user management",
                        "accessType", "INTERNAL"
                )
        );
        return ResponseEntity.ok(ApiResponse.success("Roles fetched", roles));
    }
}