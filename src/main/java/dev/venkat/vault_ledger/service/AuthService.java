package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.dto.*;
import dev.venkat.vault_ledger.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

    public AuthResponseDto login(LoginRequestDto request) {

        User user = userService.login(request.username(), request.password());

        AccountDto accountDto = accountService.getAccountDetails(user);

        return AuthResponseDto.builder()
                .username(request.username())
                .balance(accountDto.balance())
                .accountStatus(accountDto.accountStatus())
                .accountHolderName(accountDto.accountHolderName())
                .accountNumber(accountDto.accountNumber())
                .build();
    }

}
