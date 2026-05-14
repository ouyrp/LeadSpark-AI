package com.leadspark.collection;

import java.time.LocalDateTime;

public record CompetitorCompanySignal(
        String keyword,
        String companyName,
        String productName,
        String sourceType,
        String sourceName,
        String sourceUrl,
        String title,
        String summary,
        LocalDateTime signalTime,
        int confidence
) {
}
