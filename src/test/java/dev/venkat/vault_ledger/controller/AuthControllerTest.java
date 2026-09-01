package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AuthRequestDto;
import dev.venkat.vault_ledger.dto.AuthResponseDto;
import dev.venkat.vault_ledger.dto.LoginRequestDto;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.venkat.vault_ledger.service.JwtService;
import dev.venkat.vault_ledger.service.MyUserDetailsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Unit Tests: AuthController")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private MyUserDetailsService myUserDetailsService;

    private AuthRequestDto authRequest;
    private LoginRequestDto loginRequest;
    private AuthResponseDto authResponse;

    @BeforeEach
    void setUp() {

        authRequest = new AuthRequestDto(
                "venkat ramana",
                "password123",
                "Venkat Ramana",
                new BigDecimal("1000")
        );

        loginRequest = new LoginRequestDto(
                "venkat ramana",
                "password123"
        );

        authResponse = AuthResponseDto.builder()
                .token("test-jwt-token")
                .username("venkat ramana")
                .role(UserRole.USER)
                .balance(new BigDecimal("1000"))
                .accountStatus(AccountStatus.ACTIVE)
                .accountNumber("ACC1234567890")
                .accountHolderName("Venkat Ramana")
                .build();
    }

    @Nested
    @DisplayName("POST /auth/register")
    class RegisterTests {

        @Test
        @DisplayName("Should return 200 OK and registration response")
        void register_WhenRequestIsValid_ReturnsSuccessResponse()
                throws Exception {

            // Arrange
            when(authService.register(any(AuthRequestDto.class)))
                    .thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(
                            post("/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(authRequest))
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON
                    ))
                    .andExpect(jsonPath("$.token")
                            .value("test-jwt-token"))
                    .andExpect(jsonPath("$.username")
                            .value("venkat ramana"))
                    .andExpect(jsonPath("$.role")
                            .value("USER"))
                    .andExpect(jsonPath("$.balance")
                            .value(1000))
                    .andExpect(jsonPath("$.accountStatus")
                            .value("ACTIVE"))
                    .andExpect(jsonPath("$.accountNumber")
                            .value("ACC1234567890"))
                    .andExpect(jsonPath("$.accountHolderName")
                            .value("Venkat Ramana"));

            verify(authService).register(authRequest);
            verifyNoMoreInteractions(authService);
        }

        @Test
        @DisplayName("Should not call service when request validation fails")
        void register_WhenRequestIsInvalid_DoesNotCallService()
                throws Exception {

            // Arrange
            AuthRequestDto invalidRequest = new AuthRequestDto(
                    "",
                    "",
                    "",
                    new BigDecimal("-100")
            );

            // Act & Assert
            mockMvc.perform(
                            post("/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            invalidRequest
                                    ))
                    )
                    .andExpect(status().isBadRequest());

            // Critical:
            // Validation happens before the controller method executes,
            // so AuthService must never be called.
            verifyNoInteractions(authService);
        }
    }

    @Nested
    @DisplayName("POST /auth/login")
    class LoginTests {

        @Test
        @DisplayName("Should return 200 OK and login response")
        void login_WhenRequestIsValid_ReturnsSuccessResponse()
                throws Exception {

            // Arrange
            when(authService.login(any(LoginRequestDto.class)))
                    .thenReturn(authResponse);

            // Act & Assert
            mockMvc.perform(
                            post("/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(loginRequest))
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON
                    ))
                    .andExpect(jsonPath("$.token")
                            .value("test-jwt-token"))
                    .andExpect(jsonPath("$.username")
                            .value("venkat ramana"))
                    .andExpect(jsonPath("$.role")
                            .value("USER"))
                    .andExpect(jsonPath("$.balance")
                            .value(1000))
                    .andExpect(jsonPath("$.accountStatus")
                            .value("ACTIVE"))
                    .andExpect(jsonPath("$.accountNumber")
                            .value("ACC1234567890"))
                    .andExpect(jsonPath("$.accountHolderName")
                            .value("Venkat Ramana"));

            verify(authService).login(loginRequest);
            verifyNoMoreInteractions(authService);
        }

        @Test
        @DisplayName("Should not call service when request validation fails")
        void login_WhenRequestIsInvalid_DoesNotCallService()
                throws Exception {

            // Arrange
            LoginRequestDto invalidRequest = new LoginRequestDto(
                    "",
                    ""
            );

            // Act & Assert
            mockMvc.perform(
                            post("/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(
                                            invalidRequest
                                    ))
                    )
                    .andExpect(status().isBadRequest());

            // Critical:
            // Invalid requests must be rejected by Bean Validation
            // before reaching AuthService.
            verifyNoInteractions(authService);
        }
    }
}