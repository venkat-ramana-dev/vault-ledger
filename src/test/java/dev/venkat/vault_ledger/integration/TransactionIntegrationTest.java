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

        assertThat(transactionEntryRepository.findAll())
                .anyMatch(entry ->
                        entry.getAmount()
                                .compareTo(new BigDecimal("500.00")) == 0
                                && entry.getEntryDirection()
                                == EntryDirection.CREDIT);
    }

    @Test
    @DisplayName("Withdraw should decrease account balance")
    void withdraw_ShouldDecreaseAccountBalance() throws Exception {

        // Arrange - create test user
        User user = User.builder()
                .username("withdrawuser01")
                .password(passwordEncoder.encode("password123"))
                .userRole(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        user = userRepository.save(user);

        // Arrange - create account
        Account account = Account.builder()
                .accountNumber("ACC-WITHDRAW-001")
                .accountHolderName("Withdraw User")
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .user(user)
                .build();

        account = accountRepository.save(account);

        Long accountId = account.getId();

        // Give the account an initial balance of 1000
        // through the real deposit API.
        String loginRequest = """
            {
                "username": "withdrawuser01",
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
                objectMapper.readTree(
                        loginResult.getResponse().getContentAsString());

        String token = loginJson.get("token").asText();

        // Create balance of 1000 through the real deposit endpoint
        String depositRequest = """
            {
                "amount": 1000.00
            }
            """;

        mockMvc.perform(
                        post("/accounts/" + account.getAccountNumber() + "/deposit")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(depositRequest)
                )
                .andExpect(status().isOk());

        // Verify starting balance
        BigDecimal startingBalance =
                transactionEntryRepository.calculateBalanceByAccountId(
                        account.getId());

        assertThat(startingBalance)
                .isEqualByComparingTo("1000.00");

        // Execute withdrawal
        String withdrawRequest = """
            {
                "amount": 400.00
            }
            """;

        MvcResult withdrawResult = mockMvc.perform(
                        post("/accounts/" + account.getAccountNumber() + "/withdraw")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(withdrawRequest)
                )
                .andExpect(status().isOk())
                .andReturn();

        // Verify API response
        JsonNode withdrawJson =
                objectMapper.readTree(
                        withdrawResult.getResponse().getContentAsString());

        assertThat(withdrawJson.get("transactionType").asText())
                .isEqualTo("WITHDRAWAL");

        assertThat(withdrawJson.get("entryDirection").asText())
                .isEqualTo("DEBIT");

        assertThat(withdrawJson.get("amount").decimalValue())
                .isEqualByComparingTo("400.00");

        // Verify actual balance in PostgreSQL
        BigDecimal finalBalance =
                transactionEntryRepository.calculateBalanceByAccountId(
                        account.getId());

        assertThat(finalBalance)
                .isEqualByComparingTo("600.00");

        // Verify withdrawal debit entry was persisted
        assertThat(transactionEntryRepository.findAll())
                .anyMatch(entry ->
                        entry.getAccount().getId().equals(accountId)
                                && entry.getAmount()
                                .compareTo(new BigDecimal("400.00")) == 0
                                && entry.getEntryDirection()
                                == EntryDirection.DEBIT);
    }

    @Test
    @DisplayName("Transfer should decrease sender balance and increase receiver balance")
    void transfer_ShouldUpdateBothAccountBalances() throws Exception {

        // Arrange - create sender
        User senderUser = User.builder()
                .username("senderuser01")
                .password(passwordEncoder.encode("password123"))
                .userRole(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        senderUser = userRepository.save(senderUser);

        Account senderAccount = Account.builder()
                .accountNumber("ACC-TRANSFER-001")
                .accountHolderName("Sender User")
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .user(senderUser)
                .build();

        senderAccount = accountRepository.save(senderAccount);

        // Arrange - create receiver
        User receiverUser = User.builder()
                .username("receiveruser01")
                .password(passwordEncoder.encode("password123"))
                .userRole(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        receiverUser = userRepository.save(receiverUser);

        Account receiverAccount = Account.builder()
                .accountNumber("ACC-TRANSFER-002")
                .accountHolderName("Receiver User")
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .user(receiverUser)
                .build();

        receiverAccount = accountRepository.save(receiverAccount);

        // Login as sender
        String loginRequest = """
            {
                "username": "senderuser01",
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
                objectMapper.readTree(
                        loginResult.getResponse().getContentAsString());

        String token = loginJson.get("token").asText();

        // Give sender an initial balance of 1000
        String senderDeposit = """
            {
                "amount": 1000.00
            }
            """;

        mockMvc.perform(
                        post("/accounts/" + senderAccount.getAccountNumber() + "/deposit")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(senderDeposit)
                )
                .andExpect(status().isOk());

        // Give receiver an initial balance of 500
        // We use the same authenticated sender token because
        // the current controller/service does not check account ownership.
        String receiverLoginRequest = """
            {
                "username": "receiveruser01",
                "password": "password123"
            }
            """;

        MvcResult receiverLoginResult = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(receiverLoginRequest)
                )
                .andExpect(status().isOk())
                .andReturn();

        JsonNode receiverLoginJson =
                objectMapper.readTree(
                        receiverLoginResult.getResponse().getContentAsString());

        String receiverToken = receiverLoginJson.get("token").asText();

        String receiverDeposit = """
            {
                "amount": 500.00
            }
            """;

        mockMvc.perform(
                        post("/accounts/" + receiverAccount.getAccountNumber() + "/deposit")
                                .header("Authorization", "Bearer " + receiverToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(receiverDeposit)
                )
                .andExpect(status().isOk());


        // Verify starting balances
        BigDecimal senderStartingBalance =
                transactionEntryRepository.calculateBalanceByAccountId(
                        senderAccount.getId());

        BigDecimal receiverStartingBalance =
                transactionEntryRepository.calculateBalanceByAccountId(
                        receiverAccount.getId());

        assertThat(senderStartingBalance)
                .isEqualByComparingTo("1000.00");

        assertThat(receiverStartingBalance)
                .isEqualByComparingTo("500.00");

        // Execute transfer: sender → receiver, 300
        String transferRequest = """
            {
                "toAccountNumber": "ACC-TRANSFER-002",
                "amount": 300.00
            }
            """;

        MvcResult transferResult = mockMvc.perform(
                        post("/accounts/" + senderAccount.getAccountNumber() + "/transfer")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(transferRequest)
                )
                .andExpect(status().isOk())
                .andReturn();

        // Verify API response
        JsonNode transferJson =
                objectMapper.readTree(
                        transferResult.getResponse().getContentAsString());

        assertThat(transferJson.get("fromAccountNumber").asText())
                .isEqualTo("ACC-TRANSFER-001");

        assertThat(transferJson.get("toAccountNumber").asText())
                .isEqualTo("ACC-TRANSFER-002");

        assertThat(transferJson.get("fromAccountHolderName").asText())
                .isEqualTo("Sender User");

        assertThat(transferJson.get("toAccountHolderName").asText())
                .isEqualTo("Receiver User");

        assertThat(transferJson.get("transactionType").asText())
                .isEqualTo("TRANSFER");

        assertThat(transferJson.get("amount").decimalValue())
                .isEqualByComparingTo("300.00");

        // Verify actual balances in PostgreSQL
        BigDecimal senderFinalBalance =
                transactionEntryRepository.calculateBalanceByAccountId(
                        senderAccount.getId());

        BigDecimal receiverFinalBalance =
                transactionEntryRepository.calculateBalanceByAccountId(
                        receiverAccount.getId());

        assertThat(senderFinalBalance)
                .isEqualByComparingTo("700.00");

        assertThat(receiverFinalBalance)
                .isEqualByComparingTo("800.00");
    }

    @Test
    @DisplayName("Deposit should reject access to another user's account")
    void deposit_ShouldRejectUnauthorizedAccount() throws Exception {

        // Arrange - create account owned by another user
        User owner = User.builder()
                .username("depositowner01")
                .password(passwordEncoder.encode("password123"))
                .userRole(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        owner = userRepository.save(owner);

        Account ownerAccount = Account.builder()
                .accountNumber("ACC-DEPOSIT-OWNER-001")
                .accountHolderName("Deposit Owner")
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .user(owner)
                .build();

        ownerAccount = accountRepository.save(ownerAccount);

        User attacker = User.builder()
                .username("depositattacker01")
                .password(passwordEncoder.encode("password123"))
                .userRole(UserRole.USER)
                .createdAt(Instant.now())
                .build();

        attacker = userRepository.save(attacker);

        Account attackerAccount = Account.builder()
                .accountNumber("ACC-DEPOSIT-ATTACKER-001")
                .accountHolderName("Deposit Attacker")
                .accountStatus(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .user(attacker)
                .build();

        accountRepository.save(attackerAccount);

        String loginRequest = """
        {
            "username": "depositattacker01",
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
                objectMapper.readTree(
                        loginResult.getResponse().getContentAsString());

        String attackerToken = loginJson.get("token").asText();

        String depositRequest = """
        {
            "amount": 500.00
        }
        """;

        // Act + Assert
        mockMvc.perform(
                        post("/accounts/" + ownerAccount.getAccountNumber() + "/deposit")
                                .header("Authorization", "Bearer " + attackerToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(depositRequest)
                )
                .andExpect(status().isNotFound());
    }
}