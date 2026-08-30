package com.abikananda.lendenclub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartLendingSessionRequest {
    @NotBlank
    private String ownerId;
}
