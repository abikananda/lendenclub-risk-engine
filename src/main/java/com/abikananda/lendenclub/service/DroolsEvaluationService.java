package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.domain.BorrowerFact;
import com.abikananda.lendenclub.domain.EvaluationResult;
import org.kie.api.KieServices;
import org.kie.api.command.Command;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.api.runtime.rule.AgendaFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        } finally {
            kieSession.dispose();
        }
    }

    /**
     * Evaluates a borrower against a single predefined rule by its exact name.
     */
    public EvaluationResult evaluateSpecificRule(BorrowerFact fact, String sessionId, String ruleName) {
        log.info("sessionId={} loanId={} Evaluating specific rule={}", sessionId, fact.getLoanId(), ruleName);

        KieSession kieSession = kieContainer.newKieSession();
        EvaluationResult result = new EvaluationResult();

        try {
            kieSession.setGlobal("evaluationResult", result);
            kieSession.insert(fact);
            kieSession.insert(result);

            // Filter to execute ONLY the specified rule
            AgendaFilter ruleFilter = match -> match.getRule().getName().equals(ruleName);
            kieSession.fireAllRules(ruleFilter);

            log.info("sessionId={} loanId={} Specific rule evaluation completed. Decision={}",
                    sessionId, fact.getLoanId(), result.getDecision());

            return result;
        } finally {
            kieSession.dispose();
        }
    }

}