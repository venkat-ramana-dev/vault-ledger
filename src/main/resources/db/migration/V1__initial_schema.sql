CREATE TABLE accounts
(
    account_id          BIGSERIAL PRIMARY KEY,

    account_number      VARCHAR(255) NOT NULL UNIQUE,

    account_holder_name VARCHAR(255) NOT NULL,

    account_status      VARCHAR(50) NOT NULL,

    created_at          TIMESTAMP NOT NULL
);

CREATE TABLE transaction_headers
(
    header_id           BIGSERIAL PRIMARY KEY,

    transaction_type    VARCHAR(50) NOT NULL,

    created_at          TIMESTAMP NOT NULL
);

CREATE TABLE transaction_entries
(
    id                  BIGSERIAL PRIMARY KEY,

    account_id          BIGINT NOT NULL,

    header_id           BIGINT NOT NULL,

    amount              NUMERIC(19,4) NOT NULL,

    entry_direction     VARCHAR(20) NOT NULL,

    created_at          TIMESTAMP NOT NULL,

    CONSTRAINT fk_transaction_entry_account
        FOREIGN KEY (account_id)
        REFERENCES accounts(account_id),

    CONSTRAINT fk_transaction_entry_header
        FOREIGN KEY (header_id)
        REFERENCES transaction_headers(header_id)
);