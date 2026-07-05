package dev.venkat.vault_ledger.service;

import dev.venkat.vault_ledger.dto.*;
import dev.venkat.vault_ledger.entity.User;
import dev.venkat.vault_ledger.enums.UserRole;
import dev.venkat.vault_ledger.exception.InvalidCredentialsException;
import dev.venkat.vault_ledger.service.impl.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService implements AuthServiceImpl {

    private final UserService userService;

    private final AccountService accountService;

    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

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
                .role(savedUser.getUserRole())
                .balance(savedAccountDto.balance())
                .accountStatus(savedAccountDto.accountStatus())
                .accountNumber(savedAccountDto.accountNumber())
                .accountHolderName(savedAccountDto.accountHolderName())
                .build();
    }

    @Transactional
    @Override
    public AuthResponseDto login(LoginRequestDto request) {

        Authentication authenticationResponse = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        if (authenticationResponse.isAuthenticated()) {
            SecurityContextHolder.getContext().setAuthentication(authenticationResponse);
        } else {
            throw  new InvalidCredentialsException("Incorrect username or password");
        }

        User user = userService.findByUsername(request.username());

        String token = jwtService.generateToken(request.username());

        if (!user.getUserRole().equals(UserRole.USER)) {
            return AuthResponseDto.builder()
                    .token(token)
                    .username(request.username())
                    .role(user.getUserRole())
                    .accountNumber(null)
                    .balance(null)
                    .accountHolderName(null)
                    .accountStatus(null)
                    .build();
        }

        AccountDto accountDto = accountService.getAccountDetails(user);

        return AuthResponseDto.builder()
                .token(token)
                .username(request.username())
                .role(user.getUserRole())
                .balance(accountDto.balance())
                .accountStatus(accountDto.accountStatus())
                .accountHolderName(accountDto.accountHolderName())
                .accountNumber(accountDto.accountNumber())
                .build();
    }

}
