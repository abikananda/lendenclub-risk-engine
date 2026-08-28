package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.BorrowerFact;
import com.abikananda.lendenclub.domain.EvaluationResult;
import com.abikananda.lendenclub.exception.RuleEvaluationException;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.AgendaFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DroolsEvaluationService {

    private static final Logger log = LoggerFactory.getLogger(DroolsEvaluationService.class);
    private final KieContainer kieContainer;

    public DroolsEvaluationService(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }

    public EvaluationResult evaluate(BorrowerFact fact, String sessionId) {
        log.info("sessionId={} loanId={} Starting Drools rule evaluation", sessionId, fact.getLoanId());

        KieSession kieSession = kieContainer.newKieSession();
        EvaluationResult result = new EvaluationResult();

        try {
            kieSession.setGlobal("evaluationResult", result);
            kieSession.insert(fact);
            kieSession.fireAllRules();
            log.info("sessionId={} loanId={} Drools evaluation completed. Decision={} Rule={}",
                    sessionId, fact.getLoanId(), result.getDecision(), result.getRuleName());
            return result;
        } catch (RuntimeException ex) {
            log.error("sessionId={} loanId={} Drools evaluation failed", sessionId, fact.getLoanId(), ex);
            throw new RuleEvaluationException(fact.getLoanId(), "ALL_RULES", ex);
        } finally {
            kieSession.dispose();
        }
    }

    public EvaluationResult evaluateSpecificRule(BorrowerFact fact, String sessionId, String ruleName) {
        log.info("sessionId={} loanId={} Evaluating specific rule={}", sessionId, fact.getLoanId(), ruleName);

        KieSession kieSession = kieContainer.newKieSession();
        EvaluationResult result = new EvaluationResult();

        try {
            kieSession.setGlobal("evaluationResult", result);
            kieSession.insert(fact);
            kieSession.insert(result);

            AgendaFilter ruleFilter = match -> match.getRule().getName().equals(ruleName);
            kieSession.fireAllRules(ruleFilter);

            log.info("sessionId={} loanId={} Specific rule evaluation completed. Decision={}",
                    sessionId, fact.getLoanId(), result.getDecision());
            return result;
        } catch (RuntimeException ex) {
            log.error("sessionId={} loanId={} rule={} Drools rule evaluation failed",
                    sessionId, fact.getLoanId(), ruleName, ex);
            throw new RuleEvaluationException(fact.getLoanId(), ruleName, ex);
        } finally {
            kieSession.dispose();
        }
    }
}
