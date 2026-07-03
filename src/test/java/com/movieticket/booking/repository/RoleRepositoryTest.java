package com.movieticket.booking.repository;

import com.movieticket.booking.enums.AccessType;
import com.movieticket.booking.enums.RoleType;
import com.movieticket.booking.model.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void findByName_returnsRole_whenExists() {
        roleRepository.save(Role.builder().name(RoleType.ROLE_ADMIN).accessType(AccessType.INTERNAL).build());

        var role = roleRepository.findByName(RoleType.ROLE_ADMIN);

        assertThat(role).isPresent();
        assertThat(role.get().getAccessType()).isEqualTo(AccessType.INTERNAL);
    }

    @Test
    void findByName_returnsEmpty_whenNotExists() {
        assertThat(roleRepository.findByName(RoleType.ROLE_STAFF)).isEmpty();
    }
}