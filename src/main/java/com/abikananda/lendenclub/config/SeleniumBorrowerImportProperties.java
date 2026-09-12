package com.abikananda.lendenclub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "selenium-borrower-import")
public class SeleniumBorrowerImportProperties {
    private boolean enabled;
    private boolean runOnStartup;
}
