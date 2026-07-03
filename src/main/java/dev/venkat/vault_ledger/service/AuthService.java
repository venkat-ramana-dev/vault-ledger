package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.dto.AuthRequestDto;
import dev.venkat.vault_ledger.dto.AuthResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserService userService;

    private final AccountService accountService;

    public AuthResponseDto register(AuthRequestDto request) {

        // 1. Create User

        // 2. Create Account

        // 3. Return AuthResponseDto
    }
}
