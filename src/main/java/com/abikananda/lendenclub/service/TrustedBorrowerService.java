package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.entity.BorrowerProfile;
import com.abikananda.lendenclub.repository.TrustedBorrowerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrustedBorrowerService {

    private final TrustedBorrowerRepository repository;

    public TrustedBorrowerService(TrustedBorrowerRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public boolean isTrusted(BorrowerProfile profile) {
        return profile != null
                && profile.getId() != null
                && repository.existsByBorrowerProfile_IdAndActiveTrue(profile.getId());
    }
}
