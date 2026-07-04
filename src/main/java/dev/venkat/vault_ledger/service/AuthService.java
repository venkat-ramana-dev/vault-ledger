package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.dto.*;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService implements AuthServiceImpl {

    private final UserService userService;

    private final AccountService accountService;

    private final JwtService jwtService;

    @Transactional
    @Override
    public AuthResponseDto register(AuthRequestDto request) {

        User savedUser = userService.createUser(request.username(), request.password());

        String token = jwtService.generateToken(savedUser.getUsername());

        CreateAccountRequestDto createAccountRequestDto = CreateAccountRequestDto.builder()
                .accountHolderName(request.accountHolderName())
                .initialDeposit(request.initialDeposit())
                .build();

        AccountDto savedAccountDto = accountService.createAccount(savedUser, createAccountRequestDto);

        return AuthResponseDto.builder()
                .token(token)
                .username(request.username())
                .balance(savedAccountDto.balance())
                .accountStatus(savedAccountDto.accountStatus())
                .accountNumber(savedAccountDto.accountNumber())
                .accountHolderName(savedAccountDto.accountHolderName())
                .build();
    }

    @Transactional
    @Override
    public AuthResponseDto login(LoginRequestDto request) {

        User user = userService.login(request.username(), request.password());

        String token = jwtService.generateToken(user.getUsername());

        AccountDto accountDto = accountService.getAccountDetails(user);

        return AuthResponseDto.builder()
                .token(token)
                .username(request.username())
                .balance(accountDto.balance())
                .accountStatus(accountDto.accountStatus())
                .accountHolderName(accountDto.accountHolderName())
                .accountNumber(accountDto.accountNumber())
                .build();
    }

}
