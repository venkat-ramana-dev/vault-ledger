package dev.venkat.vault_ledger.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.entity.TransactionHeader;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.AccountStatus;
import dev.venkat.vault_ledger.enums.EntryDirection;
import dev.venkat.vault_ledger.enums.TransactionType;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.repository.AccountRepository;
import dev.venkat.vault_ledger.repository.TransactionEntryRepository;
import dev.venkat.vault_ledger.repository.TransactionHeaderRepository;
import dev.venkat.vault_ledger.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@DisplayName("Integration Tests: Authentication Flow")
class AuthIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionHeaderRepository transactionHeaderRepository;

    @Autowired
    private TransactionEntryRepository transactionEntryRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        deleteTestUser("testuser01");
        deleteTestUser("loginuser01");
    }

    private void deleteTestUser(String username) {

        userRepository.findByUsername(username).ifPresent(user -> {

            accountRepository.findByUser(user).ifPresent(account -> {

                List<TransactionEntry> entries =
                        transactionEntryRepository.findAll()
                                .stream()
                                .filter(entry ->
                                        entry.getAccount().getId()
                                                .equals(account.getId()))
                                .toList();

                transactionEntryRepository.deleteAll(entries);

                accountRepository.delete(account);
            });

            userRepository.delete(user);
        });
    }

    @Test
    @DisplayName("Registration should create user, account and initial deposit")
    void register_ShouldCreateUserAccountAndInitialDeposit() throws Exception {

        String request = """
                {
                    "username": "testuser01",
                    "password": "password123",
                    "accountHolderName": "Test User",
                    "initialDeposit": 1000.00
                }
                """;

        MvcResult result = mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        // Verify HTTP response
        assertThat(response).contains("\"username\":\"testuser01\"");
        assertThat(response).contains("\"role\":\"USER\"");
        assertThat(response).contains("\"accountHolderName\":\"Test User\"");
        assertThat(response).contains("\"accountStatus\":\"ACTIVE\"");
        assertThat(json.get("balance").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(response).contains("\"token\"");

        // Verify user was persisted
        User user = userRepository.findByUsername("testuser01")
                .orElseThrow();

        assertThat(user.getUsername()).isEqualTo("testuser01");
        assertThat(user.getUserRole()).isEqualTo(UserRole.USER);
        assertThat(user.getPassword()).isNotEqualTo("password123");

        // Verify account was persisted
        Account account = accountRepository.findByUser(user)
                .orElseThrow();

        assertThat(account.getAccountHolderName())
                .isEqualTo("Test User");
        assertThat(account.getAccountStatus())
                .isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getAccountNumber())
                .isNotBlank();

        // Verify actual balance in PostgreSQL
        BigDecimal balance =
                transactionEntryRepository.calculateBalanceByAccountId(
                        account.getId());

        assertThat(balance)
                .isEqualByComparingTo("1000.00");

        // Verify initial deposit transaction exists
        List<TransactionHeader> headers =
                transactionHeaderRepository.findAll();

        assertThat(headers)
                .anyMatch(header ->
                        header.getTransactionType()
                                == TransactionType.INITIAL_DEPOSIT);
    }

    @Test
    @DisplayName("Registered user should be able to login")
    void login_AfterRegistration_ShouldReturnAuthenticatedResponse()
            throws Exception {

        // Register the user first
        String registerRequest = """
                {
                    "username": "loginuser01",
                    "password": "password123",
                    "accountHolderName": "Login User",
                    "initialDeposit": 500.00
                }
                """;

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(registerRequest)
                )
                .andExpect(status().isOk());

        // Login using the real registered credentials
        String loginRequest = """
                {
                    "username": "loginuser01",
                    "password": "password123"
                }
                """;

        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginRequest)
                )
                .andExpect(status().isOk())
                .andReturn();

        String response = result.getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(response);

        // Verify real authentication response
        assertThat(response).contains("\"username\":\"loginuser01\"");
        assertThat(response).contains("\"role\":\"USER\"");
        assertThat(response).contains("\"accountHolderName\":\"Login User\"");
        assertThat(response).contains("\"accountStatus\":\"ACTIVE\"");
        assertThat(json.get("balance").decimalValue()).isEqualByComparingTo("500.00");
        assertThat(response).contains("\"token\"");
    }
}