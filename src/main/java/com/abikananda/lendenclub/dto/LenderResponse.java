package com.abikananda.lendenclub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LenderResponse {
    private String sessionId;
    private LenderDto lender;
    private SessionDto session;
}
