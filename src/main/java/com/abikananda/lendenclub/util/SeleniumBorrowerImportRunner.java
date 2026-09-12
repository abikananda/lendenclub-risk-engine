package com.abikananda.lendenclub.util;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "selenium-borrower-import",
        name = {"enabled", "run-on-startup"}, havingValue = "true")
public class SeleniumBorrowerImportRunner implements ApplicationRunner {
    private final SeleniumBorrowerImportUtility utility;

    public SeleniumBorrowerImportRunner(SeleniumBorrowerImportUtility utility) {
        this.utility = utility;
    }

    @Override
    public void run(ApplicationArguments args) {
        utility.sync();
    }
}
