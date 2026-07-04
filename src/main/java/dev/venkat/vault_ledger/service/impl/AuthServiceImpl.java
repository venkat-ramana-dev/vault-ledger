package dev.venkat.vault_ledger.service.impl;

import dev.venkat.vault_ledger.dto.AuthRequestDto;
import dev.venkat.vault_ledger.dto.AuthResponseDto;
import dev.venkat.vault_ledger.dto.LoginRequestDto;

public interface AuthServiceImpl {

    AuthResponseDto register(AuthRequestDto request);

    AuthResponseDto login(LoginRequestDto request);
}
