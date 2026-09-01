package dev.venkat.vault_ledger.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.repository.TransactionEntryRepository;
import dev.venkat.vault_ledger.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Integration Tests: Transaction")
class TransactionIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionEntryRepository transactionEntryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Deposit should increase account balance")
    void deposit_ShouldIncreaseAccountBalance() throws Exception {

        // Arrange - create test user
        User user = User.builder()
                .username("deposituser01")
                .password(passwordEncoder.encode("password123"))
                .userRole(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        user = userRepository.save(user);

        // Arrange - create account with zero starting balance
        Account account = Account.builder()
                .accountNumber("ACC-DEPOSIT-001")
                .accountHolderName("Deposit User")
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .user(user)
                .build();

        account = accountRepository.save(account);

        // Login to obtain a real JWT
        String loginRequest = """
                {
                    "username": "deposituser01",
                    "password": "password123"
                }
                """;

        MvcResult loginResult = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest)
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson =
                objectMapper.readTree(loginResult.getResponse().getContentAsString());

        String token = loginJson.get("token").asText();

        // Verify starting balance
        BigDecimal startingBalance =
                transactionEntryRepository.calculateBalanceByAccountId(
                        account.getId());

        assertThat(startingBalance)
                .isEqualByComparingTo("0");

        // Execute deposit
        String depositRequest = """
                {
                    "amount": 500.00
                }
                """;

        MvcResult depositResult = mockMvc.perform(
                        post("/accounts/" + account.getAccountNumber() + "/deposit")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(depositRequest)
                )
                .andExpect(status().isOk())
                .andReturn();

        // Verify API response
        JsonNode depositJson =
                objectMapper.readTree(
                        depositResult.getResponse().getContentAsString());

        assertThat(depositJson.get("transactionType").asText())
                .isEqualTo("DEPOSIT");

        assertThat(depositJson.get("entryDirection").asText())
                .isEqualTo("CREDIT");

        assertThat(depositJson.get("amount").decimalValue())
                .isEqualByComparingTo("500.00");

        // Verify actual balance in PostgreSQL
        BigDecimal finalBalance =
                transactionEntryRepository.calculateBalanceByAccountId(
                        account.getId());

        assertThat(finalBalance)
                .isEqualByComparingTo("500.00");

        // Verify transaction entry was persisted
        assertThat(transactionEntryRepository.findAll())
                .hasSize(2);

        assertThat(transactionEntryRepository.findAll())
                .anyMatch(entry ->
                        entry.getAmount()
                                .compareTo(new BigDecimal("500.00")) == 0
                                && entry.getEntryDirection()
                                == EntryDirection.CREDIT);
    }
}