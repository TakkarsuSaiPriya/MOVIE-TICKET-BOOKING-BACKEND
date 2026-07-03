package com.movieticket.booking.service.impl;

import com.movieticket.booking.dto.request.LoginRequest;
import com.movieticket.booking.dto.request.RegisterRequest;
import com.movieticket.booking.dto.response.LoginResponse;
import com.movieticket.booking.enums.AccessType;
import com.movieticket.booking.enums.RoleType;
import com.movieticket.booking.exception.DuplicateResourceException;
import com.movieticket.booking.exception.InvalidCredentialsException;
import com.movieticket.booking.model.Role;
import com.movieticket.booking.model.User;
import com.movieticket.booking.repository.RoleRepository;
import com.movieticket.booking.repository.UserRepository;
import com.movieticket.booking.security.CustomUserDetails;
import com.movieticket.booking.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements com.movieticket.booking.service.AuthService {

    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil               jwtUtil;

    @Override
    public void register(RegisterRequest request) {

        // Duplicate checks
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        // Resolve role — default to ROLE_CUSTOMER if missing or invalid
        // Must be final for use inside lambda below
        final RoleType resolvedRoleType = resolveRoleType(request.getRole());

        // INTERNAL access for ADMIN / STAFF, EXTERNAL for CUSTOMER
        final AccessType resolvedAccessType = (resolvedRoleType == RoleType.ROLE_CUSTOMER)
                ? AccessType.EXTERNAL
                : AccessType.INTERNAL;

        // Find existing role or create it
        Role assignedRole = roleRepository.findByName(resolvedRoleType)
                .orElseGet(() -> roleRepository.save(
                        Role.builder()
                                .name(resolvedRoleType)
                                .accessType(resolvedAccessType)
                                .build()));

        Set<Role> roles = new HashSet<>();
        roles.add(assignedRole);

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("Registered new user '{}' with role '{}'", request.getUsername(), resolvedRoleType);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid username or password");
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        UserDetails userDetails = new CustomUserDetails(user);
        String token = jwtUtil.generateToken(userDetails);

        Set<String> roleNames = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        log.info("User '{}' logged in with roles '{}'", request.getUsername(), roleNames);

        return LoginResponse.builder()
                .token(token)
                .username(user.getUsername())
                .roles(roleNames)
                .expiresInMs(jwtUtil.getExpirationMs())
                .build();
    }

    /**
     * Resolves the role string from the request into a RoleType enum.
     * Defaults to ROLE_CUSTOMER if the value is null, blank, or unrecognized.
     */
    private RoleType resolveRoleType(String roleString) {
        if (roleString == null || roleString.isBlank()) {
            return RoleType.ROLE_CUSTOMER;
        }
        try {
            return RoleType.valueOf(roleString.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown role '{}' requested during registration, defaulting to ROLE_CUSTOMER", roleString);
            return RoleType.ROLE_CUSTOMER;
        }
    }
}