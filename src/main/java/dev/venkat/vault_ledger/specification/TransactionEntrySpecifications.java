package dev.venkat.vault_ledger.specification;

import dev.venkat.vault_ledger.entity.Account;
import dev.venkat.vault_ledger.entity.TransactionEntry;
import dev.venkat.vault_ledger.entity.TransactionHeader;
import dev.venkat.vault_ledger.enums.TransactionType;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionEntrySpecifications {

    public static Specification<TransactionEntry> belongsToAccount(Account account) {
        return (root, query, cb) -> {
            if (account == null) return cb.conjunction();
            return cb.equal(root.get("account"), account);
        };
    }

    public static Specification<TransactionEntry> dateBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return (root, query, cb) -> {
            if (startDate == null && endDate == null) return cb.conjunction();

            if (startDate != null && endDate != null) {
                return cb.between(root.get("createdAt"), startDate, endDate);
            } else if (startDate != null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), startDate);
            } else {
                return cb.lessThanOrEqualTo(root.get("createdAt"), endDate);
            }
        };
    }

    public static Specification<TransactionEntry> amountBetween(BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, cb) -> {
            if (minAmount == null && maxAmount == null) return cb.conjunction();

            if (minAmount != null && maxAmount != null) {
                return cb.between(root.get("amount"), minAmount, maxAmount);
            } else if (minAmount != null) {
                return cb.greaterThanOrEqualTo(root.get("amount"), minAmount);
            } else {
                return cb.lessThanOrEqualTo(root.get("amount"), maxAmount);
            }
        };
    }

    public static Specification<TransactionEntry> transactionType(TransactionType transactionType) {
        return (root, query, cb) -> {
            if (transactionType == null) return cb.conjunction();

            Join<TransactionEntry, TransactionHeader> headerJoin = root.join("transactionHeader");
            return cb.equal(headerJoin.get("transactionType"), transactionType);
        };
    }
}
