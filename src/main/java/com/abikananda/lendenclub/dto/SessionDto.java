package com.abikananda.lendenclub.dto;

import com.abikananda.lendenclub.domain.LendingSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private LendingSessionStatus status;
    private OffsetDateTime startedAt;
}
