package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public User createUser(String username, String password) {
        Optional<User> user = userRepository.findByUsername(username);

        if (user.isPresent()) {
            log.warn("Username already exist, user creation failed.");
            throw new IllegalArgumentException("Username already exist.");
        }

        User newUser = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .userRole(UserRole.USER)
                .build();
        userRepository.save(newUser);
        log.info("New user created.");

        return newUser;
    }
}


