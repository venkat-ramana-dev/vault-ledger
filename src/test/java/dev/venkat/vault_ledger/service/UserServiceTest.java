package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private String username;
    private String password;

    @BeforeEach
    void setUp() {
        username = "venkat";
        password = "password123";
    }

    @Nested
    class CreateUserTests {

        @Test
        void shouldThrowExceptionWhenUsernameAlreadyExists() {

            // Arrange
            User existingUser = User.builder()
                    .username(username)
                    .build();

            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.of(existingUser));

            // Act
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> userService.createUser(username, password)
            );

            // Assert
            assertEquals(
                    "Username already exist.",
                    exception.getMessage()
            );

            verify(userRepository).findByUsername(username);
            verify(userRepository, never()).save(any(User.class));
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        void shouldCreateAndSaveUserWhenUsernameDoesNotExist() {

            // Arrange
            String encodedPassword = "encodedPassword";

            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.empty());

            when(passwordEncoder.encode(password))
                    .thenReturn(encodedPassword);

            // Act
            User result = userService.createUser(username, password);

            // Assert
            verify(userRepository).save(userCaptor.capture());

            User savedUser = userCaptor.getValue();

            assertEquals(username, savedUser.getUsername());

            assertEquals(
                    encodedPassword,
                    savedUser.getPassword()
            );

            assertEquals(
                    UserRole.USER,
                    savedUser.getUserRole()
            );

            assertNotNull(savedUser.getCreatedAt());
            assertSame(savedUser, result);

            verify(userRepository).findByUsername(username);
            verify(passwordEncoder).encode(password);
        }
    }

    @Nested
    class FindByUsernameTests {

        @Test
        void shouldReturnUserWhenUsernameExists() {

            // Arrange
            User user = User.builder()
                    .username(username)
                    .password("encodedPassword")
                    .userRole(UserRole.USER)
                    .createdAt(Instant.now())
                    .build();

            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.of(user));

            // Act
            User result = userService.findByUsername(username);

            // Assert
            assertSame(user, result);

            verify(userRepository).findByUsername(username);
            verifyNoMoreInteractions(userRepository);
        }

        @Test
        void shouldThrowExceptionWhenUsernameDoesNotExist() {

            // Arrange
            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.empty());

            // Act
            UsernameNotFoundException exception = assertThrows(
                    UsernameNotFoundException.class,
                    () -> userService.findByUsername(username)
            );

            // Assert
            assertEquals(
                    "Username not found.",
                    exception.getMessage()
            );

            verify(userRepository).findByUsername(username);
            verifyNoMoreInteractions(userRepository);
        }
    }
}