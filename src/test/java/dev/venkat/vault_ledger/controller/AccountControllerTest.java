package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.TransactionHistoryDto;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.service.AccountService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Unit Tests: AccountController")
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountService accountService;

    @MockitoBean
    private TransactionService transactionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private MyUserDetailsService myUserDetailsService;

    private String accountNumber;
    private AccountDto accountDto;

    @BeforeEach
    void setUp() {

        accountNumber = "ACC1234567890";

        accountDto = new AccountDto(
                accountNumber,
                "Venkat Ramana",
                AccountStatus.ACTIVE,
                new BigDecimal("1000")
        );
    }

    @Nested
    @DisplayName("GET /accounts/{accountNumber}")
    class GetAccountDetailsTests {

        @Test
        @DisplayName("Should return account details")
        void getAccountDetails_WhenAccountExists_ReturnsAccountDetails()
                throws Exception {

            // Arrange
            when(accountService.getAccountDetails(accountNumber))
                    .thenReturn(accountDto);

            // Act & Assert
            mockMvc.perform(
                            get("/accounts/{accountNumber}", accountNumber)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            "application/json"
                    ))
                    .andExpect(jsonPath("$.accountNumber")
                            .value(accountNumber))
                    .andExpect(jsonPath("$.accountHolderName")
                            .value("Venkat Ramana"))
                    .andExpect(jsonPath("$.accountStatus")
                            .value("ACTIVE"))
                    .andExpect(jsonPath("$.balance")
                            .value(1000));

            // Controller must delegate using the exact account number.
            verify(accountService).getAccountDetails(accountNumber);
            verifyNoMoreInteractions(accountService);
        }
    }

    @Nested
    @DisplayName("GET /accounts/all")
    class GetAllAccountsTests {

        @Test
        @DisplayName("Should return all accounts")
        void getAllAccounts_ReturnsAccounts()
                throws Exception {

            // Arrange
            AccountDto secondAccount = new AccountDto(
                    "ACC9876543210",
                    "Test User",
                    AccountStatus.ACTIVE,
                    new BigDecimal("2500")
            );

            List<AccountDto> accounts =
                    List.of(accountDto, secondAccount);

            when(accountService.getAllAccounts())
                    .thenReturn(accounts);

            // Act & Assert
            mockMvc.perform(
                            get("/accounts/all")
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            "application/json"
                    ))
                    .andExpect(jsonPath("$.length()")
                            .value(2))
                    .andExpect(jsonPath("$[0].accountNumber")
                            .value(accountNumber))
                    .andExpect(jsonPath("$[0].balance")
                            .value(1000))
                    .andExpect(jsonPath("$[1].accountNumber")
                            .value("ACC9876543210"))
                    .andExpect(jsonPath("$[1].balance")
                            .value(2500));

            verify(accountService).getAllAccounts();
            verifyNoMoreInteractions(accountService);
        }

        @Test
        @DisplayName("Should return empty list when no accounts exist")
        void getAllAccounts_WhenNoAccounts_ReturnsEmptyList()
                throws Exception {

            // Arrange
            when(accountService.getAllAccounts())
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(
                            get("/accounts/all")
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()")
                            .value(0));

            verify(accountService).getAllAccounts();
            verifyNoMoreInteractions(accountService);
        }
    }

    @Nested
    @DisplayName("PATCH /accounts/{accountNumber}/close")
    class CloseAccountTests {

        @Test
        @DisplayName("Should close account and return service response")
        void closeAccount_ReturnsSuccessResponse()
                throws Exception {

            // Arrange
            String expectedResponse =
                    "Account closed successfully with Acc No : "
                            + accountNumber;

            when(accountService.closeAccount(accountNumber))
                    .thenReturn(expectedResponse);

            // Act & Assert
            mockMvc.perform(
                            patch("/accounts/{accountNumber}/close", accountNumber)
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().string(expectedResponse));

            verify(accountService).closeAccount(accountNumber);
            verifyNoMoreInteractions(accountService);
        }
    }

    @Nested
    @DisplayName("GET /accounts/{accountNumber}/transactions")
    class GetTransactionHistoryTests {

        @Test
        @DisplayName("Should return transaction history with default parameters")
        void getTransactionHistory_WithDefaultParameters_ReturnsHistory()
                throws Exception {

            // Arrange
            TransactionHistoryDto historyDto =
                    TransactionHistoryDto.builder()
                            .transactionType(TransactionType.DEPOSIT)
                            .amount(new BigDecimal("1000"))
                            .description("Cash deposit")
                            .createdAt(Instant.now())
                            .build();

            Page<TransactionHistoryDto> history =
                    new PageImpl<>(
                            List.of(historyDto),
                            PageRequest.of(0, 10),
                            1
                    );

            when(transactionService.getTransactionHistory(
                    eq(accountNumber),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    isNull(),
                    eq(0),
                    eq(10),
                    eq("createdAt"),
                    eq("DESC")
            )).thenReturn(history);

            // Act & Assert
            mockMvc.perform(
                            get(
                                    "/accounts/{accountNumber}/transactions",
                                    accountNumber
                            )
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(
                            "application/json"
                    ))
                    .andExpect(jsonPath("$.content.length()")
                            .value(1))
                    .andExpect(jsonPath("$.content[0].transactionType")
                            .value("DEPOSIT"))
                    .andExpect(jsonPath("$.content[0].amount")
                            .value(1000))
                    .andExpect(jsonPath("$.content[0].description")
                            .value("Cash deposit"));

            verify(transactionService).getTransactionHistory(
                    accountNumber,
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    10,
                    "createdAt",
                    "DESC"
            );

            verifyNoMoreInteractions(transactionService);
        }

        @Test
        @DisplayName("Should pass supplied transaction filters and pagination parameters")
        void getTransactionHistory_WithParameters_PassesParametersToService()
                throws Exception {

            // Arrange
            Instant startDate =
                    Instant.parse("2026-01-01T00:00:00Z");

            Instant endDate =
                    Instant.parse("2026-02-01T00:00:00Z");

            BigDecimal minAmount =
                    new BigDecimal("100");

            BigDecimal maxAmount =
                    new BigDecimal("5000");

            TransactionHistoryDto historyDto =
                    TransactionHistoryDto.builder()
                            .transactionType(TransactionType.WITHDRAWAL)
                            .amount(new BigDecimal("500"))
                            .description("Cash withdrawal")
                            .createdAt(endDate)
                            .build();

            Page<TransactionHistoryDto> history =
                    new PageImpl<>(
                            List.of(historyDto),
                            PageRequest.of(
                                    1,
                                    20
                            ),
                            21
                    );

            when(transactionService.getTransactionHistory(
                    eq(accountNumber),
                    eq(startDate),
                    eq(endDate),
                    eq(minAmount),
                    eq(maxAmount),
                    eq(TransactionType.WITHDRAWAL),
                    eq(1),
                    eq(20),
                    eq("amount"),
                    eq("ASC")
            )).thenReturn(history);

            // Act & Assert
            mockMvc.perform(
                            get(
                                    "/accounts/{accountNumber}/transactions",
                                    accountNumber
                            )
                                    .param(
                                            "startDate",
                                            startDate.toString()
                                    )
                                    .param(
                                            "endDate",
                                            endDate.toString()
                                    )
                                    .param(
                                            "minAmount",
                                            "100"
                                    )
                                    .param(
                                            "maxAmount",
                                            "5000"
                                    )
                                    .param(
                                            "transactionType",
                                            "WITHDRAWAL"
                                    )
                                    .param(
                                            "page",
                                            "1"
                                    )
                                    .param(
                                            "size",
                                            "20"
                                    )
                                    .param(
                                            "sortBy",
                                            "amount"
                                    )
                                    .param(
                                            "sortDir",
                                            "ASC"
                                    )
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()")
                            .value(1))
                    .andExpect(jsonPath("$.content[0].transactionType")
                            .value("WITHDRAWAL"))
                    .andExpect(jsonPath("$.content[0].amount")
                            .value(500));

            // Critical:
            // This verifies that Spring correctly converts HTTP query
            // parameters into the types expected by the controller
            // before delegating to TransactionService.
            verify(transactionService).getTransactionHistory(
                    accountNumber,
                    startDate,
                    endDate,
                    minAmount,
                    maxAmount,
                    TransactionType.WITHDRAWAL,
                    1,
                    20,
                    "amount",
                    "ASC"
            );

            verifyNoMoreInteractions(transactionService);
        }
    }
}
