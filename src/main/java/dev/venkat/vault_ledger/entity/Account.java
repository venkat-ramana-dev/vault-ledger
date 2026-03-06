package dev.venkat.vault_ledger.entity;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "account_holder_name", nullable = false, updatable = false)
    private String accountHolderName;

}