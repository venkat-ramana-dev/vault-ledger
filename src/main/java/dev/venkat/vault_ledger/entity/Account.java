package dev.venkat.vault_ledger.entity;

import dev.venkat.vault_ledger.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(name = "account_holder_name", nullable = false)
    private String accountHolderName;

    @Column(nullable = false)
    private BigDecimal balance;

    @Column(nullable = false)
    private AccountStatus accountStatus;

}