package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.BorrowerFact;
import com.abikananda.lendenclub.domain.EvaluationResult;
import com.abikananda.lendenclub.domain.LendingRule;
import com.abikananda.lendenclub.dto.BorrowerEvaluateRequest;
import com.abikananda.lendenclub.dto.BorrowerEvaluateResponse;
import com.abikananda.lendenclub.entity.BorrowerEvaluation;
import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.repository.BorrowerEvaluationRepository;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BorrowerEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(BorrowerEvaluationService.class);

    private final LendingSessionService sessionService;
    private final DroolsEvaluationService droolsService;
    private final AiRiskService aiRiskService;
    private final BorrowerSnapshotRepository snapshotRepository;
    private final BorrowerEvaluationRepository evaluationRepository;
    private final BorrowerIdentityService borrowerIdentityService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final String engineVersion;

    public BorrowerEvaluationService(LendingSessionService sessionService,
                                     DroolsEvaluationService droolsService,
                                     AiRiskService aiRiskService,
                                     BorrowerSnapshotRepository snapshotRepository,
                                     BorrowerEvaluationRepository evaluationRepository,
                                     BorrowerIdentityService borrowerIdentityService,
                                     AuditService auditService,
                                     ObjectMapper objectMapper,
                                     @Value("${risk-engine.version:1.0.0}") String engineVersion) {
        this.sessionService = sessionService;
        this.droolsService = droolsService;
        this.aiRiskService = aiRiskService;
        this.snapshotRepository = snapshotRepository;
        this.evaluationRepository = evaluationRepository;
        this.borrowerIdentityService = borrowerIdentityService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.engineVersion = engineVersion;
    }

    @Transactional
    public BorrowerEvaluateResponse evaluateAndSave(BorrowerEvaluateRequest req) {
        log.info("sessionId={} loanId={} Evaluating borrower", req.getSessionId(), req.getLoanId());
        return evaluateAndPersist(req, null);
    }

    @Transactional
    public BorrowerEvaluateResponse evaluateSpecificRule(BorrowerEvaluateRequest req, String ruleName) {
        log.info("sessionId={} loanId={} Evaluating specific rule={}", req.getSessionId(), req.getLoanId(), ruleName);
        return evaluateAndPersist(req, ruleName);
    }

    private BorrowerEvaluateResponse evaluateAndPersist(BorrowerEvaluateRequest req, String ruleName) {
        sessionService.validateAndTouchSession(req.getSessionId());
        saveSnapshot(req);

        BorrowerFact fact = toFact(req);
        EvaluationResult result = ruleName == null
                ? droolsService.evaluate(fact, req.getSessionId())
                : droolsService.evaluateSpecificRule(fact, req.getSessionId(), ruleName);

        String responseRule = result.getRuleName();
        if (ruleName != null && responseRule != null) {
            responseRule = LendingRule.fromRuleName(responseRule).name();
        }

        if (result.getDecision() == null) {
            auditService.logEvent(
                    "BORROWER_EVALUATION_NO_MATCH",
                    req.getSessionId(),
                    req.getLoanId(),
                    "RequestedRule=" + ruleName + " Engine=" + engineVersion);

            return BorrowerEvaluateResponse.builder()
                    .loanId(req.getLoanId())
                    .sessionId(req.getSessionId())
                    .decision(null)
                    .riskLevel(result.getRiskLevel())
                    .investmentAmount(result.getInvestmentAmount())
                    .rule(responseRule)
                    .reason(result.getReason())
                    .evaluationId(null)
                    .build();
        }

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
                .engineVersion(engineVersion)
                .build();

        evaluation = evaluationRepository.save(evaluation);

        auditService.logEvent(
                "BORROWER_EVALUATED",
                req.getSessionId(),
                req.getLoanId(),
                "Decision=" + result.getDecision() + " Rule=" + result.getRuleName() + " Engine=" + engineVersion);

        return BorrowerEvaluateResponse.builder()
                .loanId(req.getLoanId())
                .sessionId(req.getSessionId())
                .decision(result.getDecision())
                .riskLevel(result.getRiskLevel())
                .investmentAmount(result.getInvestmentAmount())
                .rule(responseRule)
                .reason(result.getReason())
                .evaluationId(evaluation.getId())
                .build();
    }

    private void saveSnapshot(BorrowerEvaluateRequest req) {
        try {
            BorrowerProfile profile = borrowerIdentityService.resolveOrCreate(
                    req.getBorrowerName(), req.getGender(), req.getAge());

            BorrowerSnapshot snapshot = BorrowerSnapshot.builder()
                    .loanId(req.getLoanId())
                    .borrowerName(req.getBorrowerName())
                    .borrowerProfile(profile)
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
                    .loanType(req.getLoanType())
                    .repaymentFrequency(req.getRepaymentFrequency())
                    .gender(req.getGender())
                    .riskCategory(req.getRiskCategory())
                    .rawPayload(objectMapper.writeValueAsString(req))
                    .build();
            snapshotRepository.save(snapshot);
            log.info("sessionId={} loanId={} borrowerProfileId={} borrowerPublicId={} snapshot saved",
                    req.getSessionId(), req.getLoanId(), profile.getId(), profile.getPublicId());
        } catch (Exception e) {
            log.error("sessionId={} loanId={} Failed to save borrower snapshot/identity: {}",
                    req.getSessionId(), req.getLoanId(), e.getMessage());
        }
    }

    private BorrowerFact toFact(BorrowerEvaluateRequest req) {
        return BorrowerFact.builder()
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
                .trusted(req.getTrusted())
                .build();
    }

    public List<BorrowerEvaluation> getEvaluationsForLoan(String loanId) {
        return evaluationRepository.findByLoanId(loanId);
    }

    public Page<BorrowerEvaluation> getRecentEvaluations(Pageable pageable) {
        return evaluationRepository.findAllByOrderByEvaluatedAtDesc(pageable);
    }
}
