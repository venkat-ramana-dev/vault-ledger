package dev.venkat.vault_ledger.entity;

import dev.venkat.vault_ledger.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_headers")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionHeader {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, updatable = false)
    private TransactionType transactionType = TransactionType.INITIAL_DEPOSIT;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
