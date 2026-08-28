package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.BorrowerFact;
import com.abikananda.lendenclub.domain.EvaluationResult;
import com.abikananda.lendenclub.domain.LendingRule;
import com.abikananda.lendenclub.dto.BorrowerEvaluateRequest;
import com.abikananda.lendenclub.dto.BorrowerEvaluateResponse;
import com.abikananda.lendenclub.entity.BorrowerEvaluation;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.repository.BorrowerEvaluationRepository;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.drools.core.base.RuleNameEqualsAgendaFilter;
import org.kie.api.KieServices;
import org.kie.api.command.Command;
import org.kie.api.runtime.StatelessKieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class BorrowerEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(BorrowerEvaluationService.class);

    private final LendingSessionService sessionService;
    private final DroolsEvaluationService droolsService;
    private final AiRiskService aiRiskService;
    private final BorrowerSnapshotRepository snapshotRepository;
    private final BorrowerEvaluationRepository evaluationRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public BorrowerEvaluationService(LendingSessionService sessionService,
                                     DroolsEvaluationService droolsService,
                                     AiRiskService aiRiskService,
                                     BorrowerSnapshotRepository snapshotRepository,
                                     BorrowerEvaluationRepository evaluationRepository,
                                     AuditService auditService,
                                     ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.droolsService = droolsService;
        this.aiRiskService = aiRiskService;
        this.snapshotRepository = snapshotRepository;
        this.evaluationRepository = evaluationRepository;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public BorrowerEvaluateResponse evaluateAndSave(BorrowerEvaluateRequest req) {
        log.info("sessionId={} loanId={} Evaluating borrower", req.getSessionId(), req.getLoanId());

        sessionService.validateAndTouchSession(req.getSessionId());

        try {
            BorrowerSnapshot snapshot = BorrowerSnapshot.builder()
                    .loanId(req.getLoanId())
                    .sessionId(req.getSessionId())
                    .creditScore(req.getCreditScore())
                    .lendenScore(req.getLendenScore())
                    .income(req.getIncome())
                    .loanAmount(req.getLoanAmount())
                    .interestRate(req.getInterestRate())
                    .tenureMonths(req.getTenure())
                    .emi(req.getEmi())
                    .age(req.getAge())
                    .borrowerType(req.getBorrowerType())
                    .repeated(req.getRepeated())
                    .rawPayload(objectMapper.writeValueAsString(req))
                    .build();
            snapshotRepository.save(snapshot);
        } catch (Exception e) {
            log.error("Failed to save borrower snapshot: {}", e.getMessage());
        }

        BorrowerFact fact = BorrowerFact.builder()
                .loanId(req.getLoanId())
                .creditScore(req.getCreditScore())
                .lendenScore(req.getLendenScore())
                .income(req.getIncome())
                .loanAmount(req.getLoanAmount())
                .interestRate(req.getInterestRate())
                .tenure(req.getTenure())
                .emi(req.getEmi())
                .age(req.getAge())
                .borrowerType(req.getBorrowerType())
                .repeated(req.getRepeated())
                .build();

        EvaluationResult result = droolsService.evaluate(fact, req.getSessionId());
        var aiResult = aiRiskService.evaluate(fact);

        BorrowerEvaluation evaluation = BorrowerEvaluation.builder()
                .loanId(req.getLoanId())
                .sessionId(req.getSessionId())
                .decision(result.getDecision())
                .riskLevel(result.getRiskLevel())
                .investmentAmount(result.getInvestmentAmount())
                .ruleName(result.getRuleName())
                .ruleCode(result.getRuleCode())
                .reason(result.getReason())
                .aiRiskScore(aiResult.getRiskScore())
                .engineVersion("1.0.0")
                .build();

        evaluation = evaluationRepository.save(evaluation);

        auditService.logEvent("BORROWER_EVALUATED", req.getSessionId(), req.getLoanId(),
                "Decision=" + result.getDecision() + " Rule=" + result.getRuleName());

        return BorrowerEvaluateResponse.builder()
                .loanId(req.getLoanId())
                .sessionId(req.getSessionId())
                .decision(result.getDecision())
                .riskLevel(result.getRiskLevel())
                .investmentAmount(result.getInvestmentAmount())
                .rule(result.getRuleName())
                .reason(result.getReason())
                .evaluationId(evaluation.getId())
                .build();
    }

    /**
     * Evaluates a borrower against a single predefined rule by its exact name.
     */
    public BorrowerEvaluateResponse evaluateSpecificRule(BorrowerEvaluateRequest req, String ruleName) {
        log.info("sessionId={} loanId={} Evaluating specific rule={}", req.getSessionId(), req.getLoanId(), ruleName);
        BorrowerFact fact = BorrowerFact.builder()
                .loanId(req.getLoanId())
                .creditScore(req.getCreditScore())
                .lendenScore(req.getLendenScore())
                .income(req.getIncome())
                .loanAmount(req.getLoanAmount())
                .interestRate(req.getInterestRate())
                .tenure(req.getTenure())
                .emi(req.getEmi())
                .age(req.getAge())
                .borrowerType(req.getBorrowerType())
                .repeated(req.getRepeated())
                .build();
        EvaluationResult result = droolsService.evaluateSpecificRule(fact, req.getSessionId(), ruleName);

        return BorrowerEvaluateResponse.builder()
                .loanId(req.getLoanId())
                .sessionId(req.getSessionId())
                .decision(result.getDecision())
                .riskLevel(result.getRiskLevel())
                .investmentAmount(result.getInvestmentAmount())
                .rule(result.getRuleName()!=null?LendingRule.fromRuleName(result.getRuleName()).name():null)
                .reason(result.getReason())
                .build();
    }

    public List<BorrowerEvaluation> getEvaluationsForLoan(String loanId) {
        return evaluationRepository.findByLoanId(loanId);
    }

    public Page<BorrowerEvaluation> getRecentEvaluations(Pageable pageable) {
        return evaluationRepository.findAllByOrderByEvaluatedAtDesc(pageable);
    }
}
