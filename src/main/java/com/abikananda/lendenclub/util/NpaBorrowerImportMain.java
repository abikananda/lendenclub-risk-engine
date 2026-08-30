package com.abikananda.lendenclub.util;

import com.abikananda.lendenclub.Application;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class NpaBorrowerImportMain {

    private NpaBorrowerImportMain() {
    }

    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(Application.class)
                .web(WebApplicationType.NONE)
                .properties(
                        "npa-import.enabled=true",
                        "npa-import.run-on-startup=false")
                .run(args);

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
