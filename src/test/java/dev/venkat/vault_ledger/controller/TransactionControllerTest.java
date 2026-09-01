package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AmountDto;
import dev.venkat.vault_ledger.dto.TransactionDto;
import dev.venkat.vault_ledger.dto.TransferRequestDto;
import dev.venkat.vault_ledger.dto.TransferTransactionDto;
import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.service.JwtService;
import dev.venkat.vault_ledger.service.MyUserDetailsService;
import dev.venkat.vault_ledger.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Unit Tests: TransactionController")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private MyUserDetailsService myUserDetailsService;

    private String accountNumber;
    private String fromAccountNumber;
    private String toAccountNumber;

    @BeforeEach
    void setUp() {

        accountNumber = "ACC1234567890";
        fromAccountNumber = "ACC1234567890";
        toAccountNumber = "ACC9876543210";
    }

    @Nested
    @DisplayName("POST /accounts/{accountNumber}/deposit")
    class DepositTests {

        @Test
        @DisplayName("Should return transaction details for valid deposit")
        void deposit_WhenRequestIsValid_ReturnsSuccessResponse()
                throws Exception {

            // Arrange
            AmountDto amountDto =
                    new AmountDto(new BigDecimal("1000"));

            TransactionDto response =
                    TransactionDto.builder()
                            .transactionType(TransactionType.DEPOSIT)
                            .entryDirection(EntryDirection.CREDIT)
                            .amount(new BigDecimal("1000"))
                            .createdAt(Instant.parse(
                                    "2026-09-01T10:00:00Z"
                            ))
                            .build();

            when(transactionService.deposit(
                    eq(accountNumber),
                    any(AmountDto.class)
            )).thenReturn(response);

            // Act & Assert
            mockMvc.perform(
                            post(
                                    "/accounts/{accountNumber}/deposit",
                                    accountNumber
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    amountDto
                                            )
                                    )
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON
                    ))
                    .andExpect(jsonPath("$.transactionType")
                            .value("DEPOSIT"))
                    .andExpect(jsonPath("$.entryDirection")
                            .value("CREDIT"))
                    .andExpect(jsonPath("$.amount")
                            .value(1000))
                    .andExpect(jsonPath("$.createdAt")
                            .value("2026-09-01T10:00:00Z"));

            verify(transactionService).deposit(
                    eq(accountNumber),
                    any(AmountDto.class)
            );

            verifyNoMoreInteractions(transactionService);
        }

        @Test
        @DisplayName("Should not call service when deposit request is invalid")
        void deposit_WhenRequestIsInvalid_DoesNotCallService()
                throws Exception {

            // Arrange
            // AmountDto validation should reject an invalid amount.
            AmountDto invalidAmount =
                    new AmountDto(BigDecimal.ZERO);

            // Act & Assert
            mockMvc.perform(
                            post(
                                    "/accounts/{accountNumber}/deposit",
                                    accountNumber
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    invalidAmount
                                            )
                                    )
                    )
                    .andExpect(status().isBadRequest());

            // Critical:
            // @Valid rejects the request before TransactionService
            // is called.
            verifyNoInteractions(transactionService);
        }
    }

    @Nested
    @DisplayName("POST /accounts/{accountNumber}/withdraw")
    class WithdrawTests {

        @Test
        @DisplayName("Should return transaction details for valid withdrawal")
        void withdraw_WhenRequestIsValid_ReturnsSuccessResponse()
                throws Exception {

            // Arrange
            AmountDto amountDto =
                    new AmountDto(new BigDecimal("500"));

            TransactionDto response =
                    TransactionDto.builder()
                            .transactionType(TransactionType.WITHDRAWAL)
                            .entryDirection(EntryDirection.DEBIT)
                            .amount(new BigDecimal("500"))
                            .createdAt(Instant.parse(
                                    "2026-09-01T10:00:00Z"
                            ))
                            .build();

            when(transactionService.withdraw(
                    eq(accountNumber),
                    any(AmountDto.class)
            )).thenReturn(response);

            // Act & Assert
            mockMvc.perform(
                            post(
                                    "/accounts/{accountNumber}/withdraw",
                                    accountNumber
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    amountDto
                                            )
                                    )
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON
                    ))
                    .andExpect(jsonPath("$.transactionType")
                            .value("WITHDRAWAL"))
                    .andExpect(jsonPath("$.entryDirection")
                            .value("DEBIT"))
                    .andExpect(jsonPath("$.amount")
                            .value(500))
                    .andExpect(jsonPath("$.createdAt")
                            .value("2026-09-01T10:00:00Z"));

            verify(transactionService).withdraw(
                    eq(accountNumber),
                    any(AmountDto.class)
            );

            verifyNoMoreInteractions(transactionService);
        }

        @Test
        @DisplayName("Should not call service when withdrawal request is invalid")
        void withdraw_WhenRequestIsInvalid_DoesNotCallService()
                throws Exception {

            // Arrange
            AmountDto invalidAmount =
                    new AmountDto(BigDecimal.ZERO);

            // Act & Assert
            mockMvc.perform(
                            post(
                                    "/accounts/{accountNumber}/withdraw",
                                    accountNumber
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    invalidAmount
                                            )
                                    )
                    )
                    .andExpect(status().isBadRequest());

            // Critical:
            // Validation happens before TransactionService is invoked.
            verifyNoInteractions(transactionService);
        }
    }

    @Nested
    @DisplayName("POST /accounts/{fromAccountNumber}/transfer")
    class TransferTests {

        @Test
        @DisplayName("Should return transfer details for valid request")
        void transfer_WhenRequestIsValid_ReturnsSuccessResponse()
                throws Exception {

            // Arrange
            TransferRequestDto request =
                    new TransferRequestDto(
                            toAccountNumber,
                            new BigDecimal("750")
                    );

            TransferTransactionDto response =
                    TransferTransactionDto.builder()
                            .fromAccountNumber(fromAccountNumber)
                            .fromAccountHolderName("Venkat Ramana")
                            .transactionType(TransactionType.TRANSFER)
                            .toAccountNumber(toAccountNumber)
                            .toAccountHolderName("Test User")
                            .amount(new BigDecimal("750"))
                            .createdAt(Instant.parse(
                                    "2026-09-01T10:00:00Z"
                            ))
                            .build();

            when(transactionService.transfer(
                    eq(fromAccountNumber),
                    any(TransferRequestDto.class)
            )).thenReturn(response);

            // Act & Assert
            mockMvc.perform(
                            post(
                                    "/accounts/{fromAccountNumber}/transfer",
                                    fromAccountNumber
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    request
                                            )
                                    )
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            MediaType.APPLICATION_JSON
                    ))
                    .andExpect(jsonPath("$.fromAccountNumber")
                            .value(fromAccountNumber))
                    .andExpect(jsonPath("$.fromAccountHolderName")
                            .value("Venkat Ramana"))
                    .andExpect(jsonPath("$.transactionType")
                            .value("TRANSFER"))
                    .andExpect(jsonPath("$.toAccountNumber")
                            .value(toAccountNumber))
                    .andExpect(jsonPath("$.toAccountHolderName")
                            .value("Test User"))
                    .andExpect(jsonPath("$.amount")
                            .value(750))
                    .andExpect(jsonPath("$.createdAt")
                            .value("2026-09-01T10:00:00Z"));

            verify(transactionService).transfer(
                    eq(fromAccountNumber),
                    any(TransferRequestDto.class)
            );

            verifyNoMoreInteractions(transactionService);
        }

        @Test
        @DisplayName("Should not call service when transfer request is invalid")
        void transfer_WhenRequestIsInvalid_DoesNotCallService()
                throws Exception {

            // Arrange
            TransferRequestDto invalidRequest =
                    new TransferRequestDto(
                            "",
                            BigDecimal.ZERO
                    );

            // Act & Assert
            mockMvc.perform(
                            post(
                                    "/accounts/{fromAccountNumber}/transfer",
                                    fromAccountNumber
                            )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            objectMapper.writeValueAsString(
                                                    invalidRequest
                                            )
                                    )
                    )
                    .andExpect(status().isBadRequest());

            // Critical:
            // Invalid request data must never reach the service layer.
            verifyNoInteractions(transactionService);
        }
    }
}