package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.dto.*;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.exception.InvalidCredentialsException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AccountService accountService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<CreateAccountRequestDto> createAccountRequestCaptor;

    private String username;
    private String password;
    private String token;

    @BeforeEach
    void setUp() {
        username = "venkat";
        password = "password123";
        token = "jwt-token";

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }


    @Nested
    class RegisterTests {

        @Test
        void shouldRegisterUserAndReturnAuthResponse() {

            // Arrange
            AuthRequestDto request = new AuthRequestDto(
                    username,
                    password,
                    "Venkat Ramana",
                    new BigDecimal("1000.00")
            );

            User savedUser = User.builder()
                    .username(username)
                    .password("encodedPassword")
                    .userRole(UserRole.USER)
                    .createdAt(Instant.now())
                    .build();

            AccountDto accountDto = AccountDto.builder()
                    .accountNumber("ACC1234567890")
                    .accountHolderName("Venkat Ramana")
                    .balance(new BigDecimal("1000.00"))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            when(userService.createUser(username, password))
                    .thenReturn(savedUser);

            when(jwtService.generateToken(username))
                    .thenReturn(token);

            when(accountService.createAccount(
                    eq(savedUser),
                    any(CreateAccountRequestDto.class)))
                    .thenReturn(accountDto);

            // Act
            AuthResponseDto result = authService.register(request);

            // Assert
            assertEquals(token, result.token());
            assertEquals(username, result.username());
            assertEquals(UserRole.USER, result.role());
            assertEquals(accountDto.balance(), result.balance());
            assertEquals(accountDto.accountStatus(), result.accountStatus());
            assertEquals(accountDto.accountNumber(), result.accountNumber());
            assertEquals(accountDto.accountHolderName(), result.accountHolderName());

            verify(userService).createUser(username, password);
            verify(jwtService).generateToken(username);

            verify(accountService).createAccount(
                    eq(savedUser),
                    createAccountRequestCaptor.capture()
            );

            CreateAccountRequestDto accountRequest =
                    createAccountRequestCaptor.getValue();

            assertEquals(
                    request.accountHolderName(),
                    accountRequest.accountHolderName()
            );

            assertEquals(
                    request.initialDeposit(),
                    accountRequest.initialDeposit()
            );
        }


        @Test
        void shouldStopRegistrationWhenUserCreationFails() {

            // Arrange
            AuthRequestDto request = new AuthRequestDto(
                    username,
                    password,
                    "Venkat Ramana",
                    new BigDecimal("1000.00")
            );

            IllegalArgumentException exception =
                    new IllegalArgumentException("Username already exist.");

            when(userService.createUser(username, password))
                    .thenThrow(exception);

            // Act
            IllegalArgumentException thrownException = assertThrows(
                    IllegalArgumentException.class,
                    () -> authService.register(request)
            );

            // Assert
            assertSame(exception, thrownException);

            verify(jwtService, never()).generateToken(anyString());

            verify(accountService, never())
                    .createAccount(
                            any(User.class),
                            any(CreateAccountRequestDto.class)
                    );

            verify(userService).createUser(username, password);
        }
    }


    @Nested
    class LoginTests {

        @Test
        void shouldThrowInvalidCredentialsWhenAuthenticationIsNotAuthenticated() {

            // Arrange
            LoginRequestDto request = new LoginRequestDto(
                    username,
                    password
            );

            when(authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(authentication.isAuthenticated())
                    .thenReturn(false);

            // Act
            InvalidCredentialsException exception = assertThrows(
                    InvalidCredentialsException.class,
                    () -> authService.login(request)
            );

            // Assert
            assertEquals(
                    "Incorrect username or password",
                    exception.getMessage()
            );

            // AuthService must stop the login flow when
            // authentication is not successful.
            verify(userService, never()).findByUsername(anyString());
            verify(jwtService, never()).generateToken(anyString());
            verify(accountService, never()).getAccountDetails(any(User.class));

            assertNull(
                    SecurityContextHolder.getContext().getAuthentication()
            );
        }


        @Test
        void shouldLoginUserAndReturnAccountDetails() {

            // Arrange
            LoginRequestDto request = new LoginRequestDto(
                    username,
                    password
            );

            User user = User.builder()
                    .username(username)
                    .password("encodedPassword")
                    .userRole(UserRole.USER)
                    .createdAt(Instant.now())
                    .build();

            AccountDto accountDto = AccountDto.builder()
                    .accountNumber("ACC1234567890")
                    .accountHolderName("Venkat Ramana")
                    .balance(new BigDecimal("1500.00"))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            when(authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(authentication.isAuthenticated())
                    .thenReturn(true);

            when(userService.findByUsername(username))
                    .thenReturn(user);

            when(jwtService.generateToken(username))
                    .thenReturn(token);

            when(accountService.getAccountDetails(user))
                    .thenReturn(accountDto);

            // Act
            AuthResponseDto result = authService.login(request);

            // Assert
            assertEquals(token, result.token());
            assertEquals(username, result.username());
            assertEquals(UserRole.USER, result.role());
            assertEquals(accountDto.balance(), result.balance());
            assertEquals(accountDto.accountStatus(), result.accountStatus());
            assertEquals(accountDto.accountNumber(), result.accountNumber());
            assertEquals(accountDto.accountHolderName(), result.accountHolderName());

            assertSame(
                    authentication,
                    SecurityContextHolder.getContext().getAuthentication()
            );

            verify(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            verify(userService).findByUsername(username);
            verify(jwtService).generateToken(username);
            verify(accountService).getAccountDetails(user);
        }


        @Test
        void shouldLoginAdminWithoutFetchingAccountDetails() {

            // Arrange
            LoginRequestDto request = new LoginRequestDto(
                    username,
                    password
            );

            User admin = User.builder()
                    .username(username)
                    .password("encodedPassword")
                    .userRole(UserRole.ADMIN)
                    .createdAt(Instant.now())
                    .build();

            when(authenticationManager.authenticate(
                    any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);

            when(authentication.isAuthenticated())
                    .thenReturn(true);

            when(userService.findByUsername(username))
                    .thenReturn(admin);

            when(jwtService.generateToken(username))
                    .thenReturn(token);

            // Act
            AuthResponseDto result = authService.login(request);

            // Assert
            assertEquals(token, result.token());
            assertEquals(username, result.username());
            assertEquals(UserRole.ADMIN, result.role());

            assertNull(result.accountNumber());
            assertNull(result.balance());
            assertNull(result.accountHolderName());
            assertNull(result.accountStatus());

            assertSame(
                    authentication,
                    SecurityContextHolder.getContext().getAuthentication()
            );

            verify(authenticationManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            verify(userService).findByUsername(username);
            verify(jwtService).generateToken(username);

            // Admin login follows a different branch and
            // must not attempt to retrieve account details.
            verify(accountService, never())
                    .getAccountDetails(any(User.class));
        }
    }
}