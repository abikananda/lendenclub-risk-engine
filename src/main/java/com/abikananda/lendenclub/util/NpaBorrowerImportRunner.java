package com.abikananda.lendenclub.util;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "npa-import", name = {"enabled", "run-on-startup"}, havingValue = "true")
public class NpaBorrowerImportRunner implements ApplicationRunner {

    private final NpaBorrowerImportUtility importUtility;

    public NpaBorrowerImportRunner(NpaBorrowerImportUtility importUtility) {
        this.importUtility = importUtility;
    }

    @Override
    public void run(ApplicationArguments args) {
        importUtility.sync();
    }
}
