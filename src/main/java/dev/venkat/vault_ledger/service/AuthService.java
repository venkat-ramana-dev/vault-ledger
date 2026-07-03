package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.dto.AccountDto;
import dev.venkat.vault_ledger.dto.AuthRequestDto;
import dev.venkat.vault_ledger.dto.AuthResponseDto;
import dev.venkat.vault_ledger.dto.CreateAccountRequestDto;
import dev.venkat.vault_ledger.entity.User;
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

        User savedUser = userService.createUser(request.username(), request.password());

        CreateAccountRequestDto createAccountRequestDto = CreateAccountRequestDto.builder()
                .accountHolderName(request.accountHolderName())
                .initialDeposit(request.initialDeposit())
                .build();

        AccountDto savedAccountDto = accountService.createAccount(savedUser, createAccountRequestDto);

        return AuthResponseDto.builder()
                .username(request.username())
                .balance(savedAccountDto.balance())
                .accountStatus(savedAccountDto.accountStatus())
                .accountNumber(savedAccountDto.accountNumber())
                .accountHolderName(savedAccountDto.accountHolderName())
                .build();
    }
}
