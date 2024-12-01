CREATE TABLE payment_events
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    buyer_id        BIGINT       NOT NULL,
    is_payment_done BOOLEAN      NOT NULL DEFAULT FALSE,
    payment_key     VARCHAR(255) UNIQUE,
    order_id        VARCHAR(255) UNIQUE,
    type            ENUM('NORMAL') NOT NULL,
    order_name      VARCHAR(255) NOT NULL,
    method          ENUM('EASY_PAY') NOT NULL,
    psp_raw_data    JSON,
    approved_at     DATETIME,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_orders
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_event_id     BIGINT         NOT NULL,
    seller_id            BIGINT         NOT NULL,
    product_id           BIGINT         NOT NULL,
    order_id             VARCHAR(255)   NOT NULL,
    amount               DECIMAL(12, 2) NOT NULL,
    payment_order_status ENUM('NOT_STARTED', 'EXECUTING', 'SUCCESS', 'FAILURE', 'UNKNOWN') NOT NULL DEFAULT 'NOT_STARTED',
    ledger_updated       BOOLEAN        NOT NULL DEFAULT FALSE,
    wallet_updated       BOOLEAN        NOT NULL DEFAULT FALSE,
    failed_count         TINYINT        NOT NULL DEFAULT 0,
    threshold            TINYINT        NOT NULL DEFAULT 5,
    created_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (payment_event_id) REFERENCES payment_events (id)
);

CREATE TABLE payment_order_histories
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    payment_order_id BIGINT   NOT NULL,
    previous_status  ENUM('NOT_STARTED', 'EXECUTING', 'SUCCESS', 'FAILURE', 'UNKNOWN'),
    new_status       ENUM('NOT_STARTED', 'EXECUTING', 'SUCCESS', 'FAILURE', 'UNKNOWN'),
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    changed_by       VARCHAR(255),
    reason           VARCHAR(255),

    FOREIGN KEY (payment_order_id) REFERENCES payment_orders (id)
);

CREATE TABLE outboxes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(255) UNIQUE NOT NULL,
    status ENUM('INIT', 'FAILURE', 'SUCCESS') DEFAULT 'INIT',
    type VARCHAR(40),
    partition_key INT DEFAULT 0,
    payload JSON,
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE wallets (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         user_id BIGINT NOT NULL ,
                         balance DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
                         version INT NOT NULL DEFAULT 0,
                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                         UNIQUE (user_id)
);

CREATE TABLE wallet_transactions (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY ,
                                     wallet_id BIGINT NOT NULL,
                                     amount DECIMAL(15, 2) NOT NULL ,
                                     type ENUM('CREDIT', 'DEBIT') NOT NULL ,
                                     reference_id BIGINT NOT NULL ,
                                     reference_type VARCHAR(50) NOT NULL ,
                                     order_id VARCHAR(255),
                                     idempotency_key VARCHAR(255) UNIQUE NOT NULL ,
                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
#     FOREIGN KEY (wallet_id) REFERENCES wallets(id)
);

CREATE TABLE ledger_transactions (
                                      id BIGINT AUTO_INCREMENT PRIMARY KEY ,
                                      description VARCHAR(100) NOT NULL ,
                                      reference_id BIGINT NOT NULL ,
                                      reference_type VARCHAR(50),
                                      order_id VARCHAR(255),
                                      idempotency_key VARCHAR(255) UNIQUE NOT NULL ,
                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE accounts (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY ,
                          name VARCHAR(100) NOT NULL
);

CREATE TABLE ledger_entries (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY ,
                                amount DECIMAL(15, 2) NOT NULL,
                                account_id BIGINT NOT NULL ,
                                transaction_id BIGINT NOT NULL ,
                                type ENUM('CREDIT', 'DEBIT') NOT NULL,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                FOREIGN KEY (transaction_id) REFERENCES ledger_transactions(id),
                                FOREIGN KEY (account_id) REFERENCES accounts(id)
);

INSERT INTO accounts (id, name) VALUES (1, 'REVENUE');
INSERT INTO accounts (id, name) VALUES (2, 'ITEM_BUYER');

INSERT INTO wallets (user_id, balance, version, created_at, updated_at) VALUES ( 1, 126000.00, 5, '2024-12-01 11:21:18', '2024-12-01 11:33:55');
INSERT INTO wallets (user_id, balance, version, created_at, updated_at) VALUES ( 2, 6000.00, 3, '2024-12-01 11:21:18', '2024-12-01 11:21:18');


DELIMITER $$

CREATE TRIGGER check_balance_after_insert
AFTER INSERT ON ledger_entries
FOR EACH ROW
BEGIN
    DECLARE credit_sum DECIMAL(15, 2);
    DECLARE debit_sum DECIMAL(15, 2);

    SELECT SUM(amount) INTO credit_sum
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id AND type = 'CREDIT';

    SELECT SUM(amount) INTO debit_sum
    FROM ledger_entries
    WHERE transaction_id = NEW.transaction_id AND type = 'DEBIT';

    IF NOT (credit_sum - debit_sum = 0) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'The sum of CREDIT and DEBIT amounts does not balance to zero';
    end if;
END $$

DELIMITER ;

# INSERT INTO ledger_transactions (description, reference_id, reference_type, order_id, idempotency_key)
# VALUES ('Test Transaction', 1, 'Test Type', 'test_order_id_1', 'test_idempotency_key_1');
# INSERT INTO ledger_entries (amount, account_id, transaction_id, type)
# VALUES (100.00, 1, 11, 'CREDIT'), (200.00, 2, 11, 'DEBIT');


DELIMITER $$
CREATE TRIGGER payment_update_before
BEFORE UPDATE ON ledger_entries
FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Updates are not allowed on this table.';
end $$

DELIMITER ;


DELIMITER $$
CREATE TRIGGER payment_delete_before
    BEFORE DELETE ON ledger_entries
    FOR EACH ROW
BEGIN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Delete are not allowed on this table.';
end $$

DELIMITER ;

DELIMITER $$
CREATE TRIGGER after_payment_order_update
AFTER UPDATE ON payment_orders
FOR EACH ROW
BEGIN
    IF NEW.ledger_updated = TRUE AND NEW.wallet_updated = TRUE THEN
        IF (SELECT COUNT(*)
            FROM payment_orders
            WHERE payment_event_id = NEW.payment_event_id
              AND (ledger_updated = FALSE OR wallet_updated = FALSE)) = 0 THEN
            UPDATE payment_events
            SET is_payment_done = TRUE
            WHERE id = NEW.payment_event_id;
        END if;
    END if;
end $$;

DELIMITER ;