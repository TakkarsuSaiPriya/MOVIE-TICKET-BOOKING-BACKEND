package com.movieticket.booking.model;

import com.movieticket.booking.enums.AccessType;
import com.movieticket.booking.enums.RoleType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false)
    private RoleType name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessType accessType; // INTERNAL or EXTERNAL
}