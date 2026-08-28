package dev.venkat.vault_ledger.entity;

import dev.venkat.vault_ledger.enums.EntryDirection;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transaction_entries")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 4, updatable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_direction", nullable = false, updatable = false)
    private EntryDirection entryDirection;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false, updatable = false)
    private Account account;

    @ManyToOne
    @JoinColumn(name = "header_id", nullable = false, updatable = false)
    private TransactionHeader transactionHeader;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}

