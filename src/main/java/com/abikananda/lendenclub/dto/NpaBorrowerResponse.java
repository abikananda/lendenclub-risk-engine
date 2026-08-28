package com.abikananda.lendenclub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NpaBorrowerResponse {
    private Long id;
    private String borrowerName;
    private String normalizedName;
    private Long hitCount;
}
