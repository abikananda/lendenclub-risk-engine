package com.abikananda.lendenclub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WorkflowCheckpointRequest {
    private String loanId;
    private String rule;

    @NotBlank
    private String state;

    private String message;
}
