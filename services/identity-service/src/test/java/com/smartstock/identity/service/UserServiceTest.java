package com.smartstock.identity.service;

import com.smartstock.identity.api.dto.request.RegisterRequest;
import com.smartstock.identity.api.dto.request.UpdateUserRequest;
import com.smartstock.identity.api.dto.response.UserResponse;
import com.smartstock.identity.domain.model.User;
import com.smartstock.identity.domain.repository.RoleRepository;
import com.smartstock.identity.domain.repository.UserRepository;
import com.smartstock.identity.exception.UserAlreadyExistsException;
import com.smartstock.identity.exception.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;

    @InjectMocks
    private UserService userService;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userService, "passwordEncoder", encoder);
    }

    @Test
    void register_withValidRequest_shouldReturnUserResponse() {
        when(userRepository.existsByUsernameAndNotDeleted("newuser")).thenReturn(false);
        when(userRepository.existsByEmailAndNotDeleted("new@test.com")).thenReturn(false);

        User savedUser = buildUser("user-001", "newuser", "new@test.com");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@test.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("New");
        request.setLastName("User");

        UserResponse response = userService.register(request);

        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getEmail()).isEqualTo("new@test.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_withExistingUsername_shouldThrow() {
        when(userRepository.existsByUsernameAndNotDeleted("existing")).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("existing");
        request.setEmail("new@test.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("A");
        request.setLastName("B");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void register_withExistingEmail_shouldThrow() {
        when(userRepository.existsByUsernameAndNotDeleted("newuser")).thenReturn(false);
        when(userRepository.existsByEmailAndNotDeleted("existing@test.com")).thenReturn(true);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@test.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("A");
        request.setLastName("B");

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void findById_withValidId_shouldReturnUser() {
        User user = buildUser("user-001", "testuser", "test@example.com");
        when(userRepository.findByIdAndNotDeleted("user-001")).thenReturn(Optional.of(user));

        UserResponse response = userService.findById("user-001");

        assertThat(response.getId()).isEqualTo("user-001");
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    void findById_withNonExistentId_shouldThrow() {
        when(userRepository.findByIdAndNotDeleted("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById("nonexistent"))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void update_withNewFirstName_shouldUpdateUser() {
        User user = buildUser("user-001", "testuser", "test@example.com");
        when(userRepository.findByIdAndNotDeleted("user-001")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("UpdatedFirst");

        UserResponse response = userService.update("user-001", request);
        assertThat(response).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deactivate_shouldSetUserInactive() {
        User user = buildUser("user-001", "testuser", "test@example.com");
        user.setActive(true);
        when(userRepository.findByIdAndNotDeleted("user-001")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.deactivate("user-001");
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void reactivate_shouldSetUserActive() {
        User user = buildUser("user-001", "testuser", "test@example.com");
        user.setActive(false);
        when(userRepository.findByIdAndNotDeleted("user-001")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.reactivate("user-001");
        assertThat(response.isActive()).isTrue();
    }

    // --------------------------------------------------------

    private User buildUser(String id, String username, String email) {
        return User.builder()
                .id(id)
                .username(username)
                .email(email)
                .passwordHash(encoder.encode("Password123!"))
                .firstName("Test")
                .lastName("User")
                .active(true)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();
    }
}
