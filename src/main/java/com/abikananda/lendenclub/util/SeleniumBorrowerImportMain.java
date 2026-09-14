package com.abikananda.lendenclub.util;

import com.abikananda.lendenclub.Application;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

public final class SeleniumBorrowerImportMain {
    private SeleniumBorrowerImportMain() { }

    public static void main(String[] args) {
        String[] effectiveArgs = Arrays.copyOf(args, args.length + 2);
        effectiveArgs[args.length] = "--selenium-borrower-import.enabled=true";
        effectiveArgs[args.length + 1] = "--selenium-borrower-import.run-on-startup=false";

        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .run(effectiveArgs);
        try {
            var result = context.getBean(SeleniumBorrowerImportUtility.class).sync();
            System.out.printf("Selenium borrower import completed: sourceRows=%d, uniqueProfiles=%d, "
                            + "profilesCreated=%d, snapshotsCreated=%d, snapshotsLinked=%d, "
                            + "existingSnapshots=%d, skippedRows=%d%n",
                    result.sourceRows(), result.uniqueProfiles(), result.profilesCreated(),
                    result.snapshotsCreated(), result.snapshotsLinked(), result.existingSnapshots(),
                    result.skippedRows());
        } finally {
            context.close();
        }
    }
}
