package dev.venkat.vault_ledger.controller;

import dev.venkat.vault_ledger.dto.AuthRequestDto;
import dev.venkat.vault_ledger.dto.AuthResponseDto;
import dev.venkat.vault_ledger.dto.LoginRequestDto;
import dev.venkat.vault_ledger.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@RequestBody AuthRequestDto request) {

        AuthResponseDto responseDto = authService.register(request);

        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody LoginRequestDto request) {

        AuthResponseDto responseDto = authService.login(request);

        return ResponseEntity.ok(responseDto);
    }
}