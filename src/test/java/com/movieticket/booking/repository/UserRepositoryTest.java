package com.movieticket.booking.repository;

import com.movieticket.booking.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.save(User.builder()
                .username("john")
                .email("john@example.com")
                .password("encoded")
                .enabled(true)
                .build());
    }

    @Test
    void findByUsername_returnsUser_whenExists() {
        assertThat(userRepository.findByUsername("john")).isPresent();
    }

    @Test
    void findByUsername_returnsEmpty_whenNotExists() {
        assertThat(userRepository.findByUsername("ghost")).isEmpty();
    }

    @Test
    void existsByUsername_trueForExisting() {
        assertThat(userRepository.existsByUsername("john")).isTrue();
    }

    @Test
    void existsByEmail_trueForExisting() {
        assertThat(userRepository.existsByEmail("john@example.com")).isTrue();
    }

    @Test
    void existsByUsername_falseForNonExisting() {
        assertThat(userRepository.existsByUsername("nope")).isFalse();
    }

    @Test
    void existsByEmail_falseForNonExisting() {
        assertThat(userRepository.existsByEmail("nope@x.com")).isFalse();
    }

    @Test
    void save_persistsCreatedAtTimestamp() {
        User saved = userRepository.save(User.builder().username("jane").email("jane@x.com").password("pw").build());
        assertThat(saved.getCreatedAt()).isNotNull();
    }
}