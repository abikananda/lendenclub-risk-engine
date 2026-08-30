package com.abikananda.lendenclub.util;

import com.abikananda.lendenclub.Application;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

public final class NpaBorrowerImportMain {

    private NpaBorrowerImportMain() {
    }

    public static void main(String[] args) {
        String[] effectiveArgs = Arrays.copyOf(args, args.length + 2);
        effectiveArgs[args.length] = "--npa-import.enabled=true";
        effectiveArgs[args.length + 1] = "--npa-import.run-on-startup=false";

        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .run(effectiveArgs);

        try {
            NpaBorrowerImportUtility.ImportResult result = context
                    .getBean(NpaBorrowerImportUtility.class)
                    .sync();

            System.out.printf(
                    "NPA import completed: manualLendingRows=%d, defaultBorrowerRows=%d, uniqueNames=%d, inserted=%d, reactivated=%d, unchanged=%d%n",
                    result.manualLendingRows(),
                    result.defaultBorrowerRows(),
                    result.uniqueNames(),
                    result.inserted(),
                    result.reactivated(),
                    result.unchanged());
        } finally {
            context.close();
        }
    }
}
