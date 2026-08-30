package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.repository.BorrowerProfileRepository;
import com.abikananda.lendenclub.util.BorrowerProfileIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Locale;
import java.util.function.Function;

@Service
public class BorrowerProfileCreator {

    private static final Logger log = LoggerFactory.getLogger(BorrowerProfileCreator.class);
    private static final int MAX_ATTEMPTS = 5;

    private final BorrowerProfileRepository repository;
    private final BorrowerProfileIdGenerator idGenerator;
    private final TransactionTemplate requiresNewTransaction;

    public BorrowerProfileCreator(BorrowerProfileRepository repository,
                                  BorrowerProfileIdGenerator idGenerator,
                                  PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.idGenerator = idGenerator;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public BorrowerProfile create(Function<String, BorrowerProfile> profileFactory) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            String publicId = idGenerator.generate();
            try {
                BorrowerProfile created = requiresNewTransaction.execute(status ->
                        repository.saveAndFlush(profileFactory.apply(publicId)));
                if (created == null) {
                    throw new IllegalStateException("Borrower profile creation returned no result");
                }
                return created;
            } catch (DataIntegrityViolationException ex) {
                if (!isPublicIdCollision(ex) || attempt == MAX_ATTEMPTS) {
                    throw ex;
                }
                log.warn("Borrower profile ID collision for {}; regenerating (attempt {}/{})",
                        publicId, attempt, MAX_ATTEMPTS);
            }
        }
        throw new IllegalStateException("Unable to generate a unique borrower profile ID");
    }

    private static boolean isPublicIdCollision(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("public_id")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
