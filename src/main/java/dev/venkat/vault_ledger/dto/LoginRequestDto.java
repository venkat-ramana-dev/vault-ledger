package dev.venkat.vault_ledger.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record LoginRequestDto (

        @NotBlank
        @Size(min = 8, max = 50)
        String username,

        @NotBlank
        @Size(min = 8, max = 20)
        String password){}
