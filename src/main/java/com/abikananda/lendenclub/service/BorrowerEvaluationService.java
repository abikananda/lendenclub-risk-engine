package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.BorrowerFact;
import com.abikananda.lendenclub.domain.EvaluationResult;
import com.abikananda.lendenclub.domain.LendingDecision;
import com.abikananda.lendenclub.domain.InvestmentStatus;
import com.abikananda.lendenclub.domain.RiskLevel;
import com.abikananda.lendenclub.domain.LendingRule;
import com.abikananda.lendenclub.dto.BorrowerEvaluateRequest;
import com.abikananda.lendenclub.dto.BorrowerEvaluateResponse;
import com.abikananda.lendenclub.entity.BorrowerEvaluation;
import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.entity.BorrowerSnapshot;
import com.abikananda.lendenclub.repository.BorrowerEvaluationRepository;
import com.abikananda.lendenclub.repository.InvestmentRepository;
import com.abikananda.lendenclub.repository.BorrowerSnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;

@Service
public class BorrowerEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(BorrowerEvaluationService.class);

    private final LendingSessionService sessionService;
    private final DroolsEvaluationService droolsService;
    private final AiRiskService aiRiskService;
    private final HybridRiskDecisionService hybridRiskDecisionService;
    private final BorrowerSnapshotRepository snapshotRepository;
    private final BorrowerEvaluationRepository evaluationRepository;
    private final InvestmentRepository investmentRepository;
    private final BorrowerIdentityService borrowerIdentityService;
    private final TrustedBorrowerService trustedBorrowerService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final String engineVersion;

    public BorrowerEvaluationService(LendingSessionService sessionService,
                                     DroolsEvaluationService droolsService,
                                     AiRiskService aiRiskService,
                                     HybridRiskDecisionService hybridRiskDecisionService,
                                     BorrowerSnapshotRepository snapshotRepository,
                                     BorrowerEvaluationRepository evaluationRepository,
                                     InvestmentRepository investmentRepository,
                                     BorrowerIdentityService borrowerIdentityService,
                                     TrustedBorrowerService trustedBorrowerService,
                                     AuditService auditService,
                                     ObjectMapper objectMapper,
                                     @Value("${risk-engine.version:1.0.0}") String engineVersion) {
        this.sessionService = sessionService;
        this.droolsService = droolsService;
        this.aiRiskService = aiRiskService;
        this.hybridRiskDecisionService = hybridRiskDecisionService;
        this.snapshotRepository = snapshotRepository;
        this.evaluationRepository = evaluationRepository;
        this.investmentRepository = investmentRepository;
        this.borrowerIdentityService = borrowerIdentityService;
        this.trustedBorrowerService = trustedBorrowerService;
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
        var session = sessionService.validateAndTouchSession(req.getSessionId());

        if (investmentRepository.existsByLender_IdAndLoanIdAndStatus(
                session.getLender().getId(), req.getLoanId(), InvestmentStatus.SUCCESS)) {
            return BorrowerEvaluateResponse.builder()
                    .loanId(req.getLoanId()).sessionId(req.getSessionId())
                    .decision(LendingDecision.SKIP).riskLevel(RiskLevel.UNKNOWN)
                    .investmentAmount(BigDecimal.ZERO)
                    .reason("Loan already invested by this lender")
                    .build();
        }

        var previousApproval = evaluationRepository
                .findFirstBySessionIdAndLoanIdAndDecisionOrderByEvaluatedAtDesc(
                        req.getSessionId(), req.getLoanId(), LendingDecision.INVEST);
        if (previousApproval.isPresent()) {
            BorrowerEvaluation approved = previousApproval.orElseThrow();
            log.info("sessionId={} loanId={} Skipping duplicate rule evaluation; already approved by rule={}",
                    req.getSessionId(), req.getLoanId(), approved.getRuleCode());
            auditService.logEvent(
                    "BORROWER_DUPLICATE_APPROVAL_SKIPPED",
                    req.getSessionId(),
                    req.getLoanId(),
                    "Already approved by rule=" + approved.getRuleCode()
                            + " evaluationId=" + approved.getId());
            return BorrowerEvaluateResponse.builder()
                    .loanId(req.getLoanId())
                    .sessionId(req.getSessionId())
                    .decision(LendingDecision.SKIP)
                    .riskLevel(approved.getRiskLevel())
                    .investmentAmount(BigDecimal.ZERO)
                    .rule(approved.getRuleCode())
                    .reason("Loan was already approved in this session by rule " + approved.getRuleCode())
                    .evaluationId(approved.getId())
                    .build();
        }

        BorrowerProfile profile = resolveProfileAndSaveSnapshot(req);

        boolean trusted = isTrustedRule(ruleName)
                ? trustedBorrowerService.isTrusted(profile)
                : Boolean.TRUE.equals(req.getTrusted());

        if (isTrustedRule(ruleName)) {
            log.info("sessionId={} loanId={} trustedRule={} borrowerProfileId={} trusted={}",
                    req.getSessionId(), req.getLoanId(), ruleName,
                    profile == null ? null : profile.getId(), trusted);
        }

        BorrowerFact fact = toFact(req, trusted);
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
        EvaluationResult finalResult = hybridRiskDecisionService.apply(result, aiResult);

        BorrowerEvaluation evaluation = BorrowerEvaluation.builder()
                .loanId(req.getLoanId())
                .sessionId(req.getSessionId())
                .droolsDecision(result.getDecision())
                .droolsRiskLevel(result.getRiskLevel())
                .droolsInvestmentAmount(result.getInvestmentAmount())
                .decision(finalResult.getDecision())
                .riskLevel(finalResult.getRiskLevel())
                .investmentAmount(finalResult.getInvestmentAmount())
                .ruleName(result.getRuleName())
                .ruleCode(result.getRuleCode())
                .reason(finalResult.getReason())
                .aiRiskScore(aiResult.getRiskScore())
                .aiRiskLevel(aiResult.getRiskLevel())
                .aiRecommendation(aiResult.getRecommendation())
                .aiConfidence(aiResult.getConfidence())
                .aiMaximumAmount(aiResult.getMaximumRecommendedAmount())
                .aiRationale(aiResult.getRationale())
                .aiConcerns(toJson(aiResult.getConcerns()))
                .aiPositiveFactors(toJson(aiResult.getPositiveFactors()))
                .aiProvider(aiResult.getProvider())
                .aiModel(aiResult.getModel())
                .aiPromptVersion(aiResult.getPromptVersion())
                .aiStatus(aiResult.getStatus())
                .aiLatencyMs(aiResult.getLatencyMs())
                .engineVersion(engineVersion)
                .build();

        evaluation = evaluationRepository.save(evaluation);

        auditService.logEvent(
                "BORROWER_EVALUATED",
                req.getSessionId(),
                req.getLoanId(),
                "Decision=" + finalResult.getDecision() + " DroolsDecision=" + result.getDecision() + " Rule=" + result.getRuleName() + " Engine=" + engineVersion);

        return BorrowerEvaluateResponse.builder()
                .loanId(req.getLoanId())
                .sessionId(req.getSessionId())
                .decision(finalResult.getDecision())
                .riskLevel(finalResult.getRiskLevel())
                .investmentAmount(finalResult.getInvestmentAmount())
                .rule(responseRule)
                .reason(finalResult.getReason())
                .evaluationId(evaluation.getId())
                .build();
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Could not serialize AI assessment metadata: {}", e.getMessage());
            return null;
        }
    }

    private BorrowerProfile resolveProfileAndSaveSnapshot(BorrowerEvaluateRequest req) {
        try {
            BorrowerProfile profile = borrowerIdentityService.resolveOrCreate(
                    req.getBorrowerName(), req.getGender(), req.getBorrowerType(), req.getAge());

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
                    .emi(estimatedMonthlyPayment(req))
                    .age(req.getAge())
                    .borrowerType(req.getBorrowerType())
                    .repeated(req.getRepeated())
                    .loanType(req.getLoanType())
                    .repaymentFrequency(normalizeRepaymentFrequency(req.getRepaymentFrequency()))
                    .gender(req.getGender())
                    .riskCategory(req.getRiskCategory())
                    .rawPayload(objectMapper.writeValueAsString(req))
                    .build();
            snapshotRepository.save(snapshot);
            log.info("sessionId={} loanId={} borrowerProfileId={} borrowerPublicId={} snapshot saved",
                    req.getSessionId(), req.getLoanId(), profile.getId(), profile.getPublicId());
            return profile;
        } catch (Exception e) {
            log.error("sessionId={} loanId={} Failed to save borrower snapshot/identity: {}",
                    req.getSessionId(), req.getLoanId(), e.getMessage());
            return null;
        }
    }

    private BorrowerFact toFact(BorrowerEvaluateRequest req, boolean trusted) {
        return BorrowerFact.builder()
                .loanId(req.getLoanId())
                .creditScore(req.getCreditScore())
                .lendenScore(req.getLendenScore())
                .income(req.getIncome())
                .loanAmount(req.getLoanAmount())
                .interestRate(req.getInterestRate())
                .tenure(req.getTenure())
                .emi(estimatedMonthlyPayment(req))
                .age(req.getAge())
                .borrowerType(req.getBorrowerType())
                .repaymentFrequency(normalizeRepaymentFrequency(req.getRepaymentFrequency()))
                .repeated(req.getRepeated())
                .trusted(trusted)
                .build();
    }

    private BigDecimal estimatedMonthlyPayment(BorrowerEvaluateRequest req) {
        return req.getLoanAmount().divide(
                BigDecimal.valueOf(req.getTenure()), 8, RoundingMode.HALF_UP);
    }

    private String normalizeRepaymentFrequency(String repaymentFrequency) {
        if (repaymentFrequency == null || repaymentFrequency.isBlank()) return null;
        return repaymentFrequency.trim().toUpperCase(Locale.ROOT);
    }

    private boolean isTrustedRule(String ruleName) {
        if (ruleName == null || ruleName.isBlank()) return false;
        try {
            return LendingRule.fromRuleName(ruleName).name().startsWith("TRUSTED_LENDERS_");
        } catch (IllegalArgumentException ignored) {
            return false;
        }
    }

    public List<BorrowerEvaluation> getEvaluationsForLoan(String loanId) {
        return evaluationRepository.findByLoanId(loanId);
    }

    public Page<BorrowerEvaluation> getRecentEvaluations(Pageable pageable) {
        return evaluationRepository.findAllByOrderByEvaluatedAtDesc(pageable);
    }
}
