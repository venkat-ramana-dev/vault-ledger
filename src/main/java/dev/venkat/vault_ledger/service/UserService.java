package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public User createUser(String username, String password) {

    }
}