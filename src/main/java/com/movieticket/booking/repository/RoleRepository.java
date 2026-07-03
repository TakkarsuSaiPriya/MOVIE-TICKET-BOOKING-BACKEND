package com.movieticket.booking.repository;

import com.movieticket.booking.enums.RoleType;
import com.movieticket.booking.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}