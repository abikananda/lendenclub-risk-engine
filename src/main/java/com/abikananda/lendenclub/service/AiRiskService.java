package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.AiRiskResult;
import com.abikananda.lendenclub.domain.BorrowerFact;

public interface AiRiskService {
    AiRiskResult evaluate(BorrowerFact borrower);
}
