package com.abikananda.lendenclub.service;

import com.abikananda.lendenclub.config.SeleniumBorrowerImportProperties;
import com.abikananda.lendenclub.dto.SeleniumBorrowerRecord;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class SeleniumBorrowerClient {
    private static final String MIGRATION_HEADER = "X-Migration-Key";
    private final RestClient.Builder restClientBuilder;
    private final SeleniumBorrowerImportProperties properties;

    public SeleniumBorrowerClient(RestClient.Builder restClientBuilder,
                                  SeleniumBorrowerImportProperties properties) {
        this.restClientBuilder = restClientBuilder;
        this.properties = properties;
    }

    public IdPage ids(long afterId) {
        validateConfiguration();
        return restClientBuilder.build().get()
                .uri(properties.getBaseUrl() + "/ids?afterId={afterId}&limit={limit}",
                        afterId, Math.min(500, Math.max(1, properties.getPageSize())))
                .header(MIGRATION_HEADER, properties.getApiKey())
                .retrieve().body(IdPage.class);
    }

    public SeleniumBorrowerRecord borrower(long id) {
        validateConfiguration();
        return restClientBuilder.build().get()
                .uri(properties.getBaseUrl() + "/{id}", id)
                .header(MIGRATION_HEADER, properties.getApiKey())
                .retrieve().body(SeleniumBorrowerRecord.class);
    }

    private void validateConfiguration() {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank())
            throw new IllegalStateException("SELENIUM_BORROWER_API_URL is required");
        if (properties.getApiKey() == null || properties.getApiKey().isBlank())
            throw new IllegalStateException("SELENIUM_BORROWER_API_KEY is required");
    }

    public record IdPage(List<Long> ids, long nextCursor, boolean hasMore) { }
}
