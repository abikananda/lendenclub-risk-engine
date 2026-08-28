package com.abikananda.lendenclub.util;

import org.slf4j.MDC;
import java.util.UUID;

public class CorrelationIdUtil {

    public static final String MDC_KEY = "correlationId";

    public static String getCorrelationId() {
        String id = MDC.get(MDC_KEY);
        if (id == null) {
            id = generateCorrelationId();
            MDC.put(MDC_KEY, id);
        }
        return id;
    }
    public static void setCorrelationId(String correlationId) {
        MDC.put(MDC_KEY, correlationId);
    }

    public static String generateCorrelationId() {
        return "REQ-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
