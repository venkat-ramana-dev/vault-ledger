package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.entity.User;

public interface UserServiceImpl {

    User createUser(String username, String password);

    User findByUsername(String username);
}
