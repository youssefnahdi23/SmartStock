package com.smartstock.identity.repository;

import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.domain.repository.UserRepository;
import com.smartstock.identity.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("repo.test.user")
                .email("repo.test@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .firstName("Repo")
                .lastName("Test")
                .active(true)
                .emailVerified(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();
        testUser = userRepository.save(testUser);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteById(testUser.getId());
    }

    @Test
    void findByUsernameAndNotDeleted_withExistingUsername_shouldReturnUser() {
        Optional<User> found = userRepository.findByUsernameAndNotDeleted("repo.test.user");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("repo.test@example.com");
    }

    @Test
    void findByUsernameAndNotDeleted_withDeletedUser_shouldReturnEmpty() {
        testUser.softDelete();
        userRepository.save(testUser);

        Optional<User> found = userRepository.findByUsernameAndNotDeleted("repo.test.user");
        assertThat(found).isEmpty();
    }

    @Test
    void findByEmailAndNotDeleted_shouldReturnUser() {
        Optional<User> found = userRepository.findByEmailAndNotDeleted("repo.test@example.com");
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("repo.test.user");
    }

    @Test
    void existsByUsernameAndNotDeleted_shouldReturnTrue() {
        boolean exists = userRepository.existsByUsernameAndNotDeleted("repo.test.user");
        assertThat(exists).isTrue();
    }

    @Test
    void existsByUsernameAndNotDeleted_withNonExistentUser_shouldReturnFalse() {
        boolean exists = userRepository.existsByUsernameAndNotDeleted("nonexistent.user");
        assertThat(exists).isFalse();
    }

    @Test
    void existsByEmailAndNotDeleted_shouldReturnTrue() {
        boolean exists = userRepository.existsByEmailAndNotDeleted("repo.test@example.com");
        assertThat(exists).isTrue();
    }

    @Test
    void findByIdAndNotDeleted_withValidId_shouldReturnUser() {
        Optional<User> found = userRepository.findByIdAndNotDeleted(testUser.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("repo.test.user");
    }

    @Test
    void findAllWithFilters_filterByActive_shouldReturnOnlyActiveUsers() {
        Page<User> page = userRepository.findAllWithFilters(null, true, null, PageRequest.of(0, 10));
        assertThat(page.getContent()).allMatch(User::isActive);
    }

    @Test
    void findAllWithFilters_searchByUsername_shouldReturnMatchingUsers() {
        Page<User> page = userRepository.findAllWithFilters(null, null, "repo.test", PageRequest.of(0, 10));
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).anyMatch(u -> u.getUsername().equals("repo.test.user"));
    }
}
