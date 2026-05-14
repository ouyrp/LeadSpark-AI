package com.leadspark.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class AbstractHttpCompetitorDataProvider implements CompetitorDataProvider {
    private static final Logger log = LoggerFactory.getLogger(AbstractHttpCompetitorDataProvider.class);

    private final RestClient restClient = RestClient.builder().build();

    protected List<CompetitorCompanySignal> collectFromHttpApi(
            String keyword,
            CompetitorCollectionProperties.Source source,
            String sourceName,
            String sourceType) {
        if (!source.isConfigured()) {
            return List.of();
        }

        try {
            URI uri = UriComponentsBuilder.fromUriString(source.getBaseUrl())
                    .queryParam("keyword", keyword)
                    .queryParam("searchKey", keyword)
                    .queryParam("q", keyword)
                    .build()
                    .toUri();

            @SuppressWarnings("unchecked")
            Map<String, Object> payload = restClient.get()
                    .uri(uri)
                    .header("Authorization", "Bearer " + source.getApiKey())
                    .header("Token", source.getApiKey())
                    .retrieve()
                    .body(Map.class);

            return parsePayload(keyword, payload, sourceName, sourceType);
        } catch (RuntimeException ex) {
            log.warn("{} collection failed for keyword '{}': {}", sourceName, keyword, ex.getMessage());
            return List.of();
        }
    }

    private List<CompetitorCompanySignal> parsePayload(
            String keyword,
            Map<String, Object> payload,
            String sourceName,
            String sourceType) {
        if (payload == null || payload.isEmpty()) {
            return List.of();
        }

        Object data = firstNonNull(payload, "data", "result", "results", "items", "list");
        if (data instanceof Map<?, ?> map) {
            data = firstNonNull(map, "records", "items", "list", "data", "result");
        }

        if (!(data instanceof List<?> items)) {
            return List.of();
        }

        List<CompetitorCompanySignal> signals = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map<?, ?> row)) {
                continue;
            }

            String companyName = value(row, "companyName", "name", "enterpriseName", "entName", "orgName");
            if (companyName.isBlank()) {
                continue;
            }

            String title = value(row, "title", "productName", "brief", "name");
            String summary = value(row, "summary", "description", "desc", "content", "brief");
            String sourceUrl = value(row, "url", "sourceUrl", "link");

            signals.add(new CompetitorCompanySignal(
                    keyword,
                    companyName,
                    keyword,
                    sourceType,
                    sourceName,
                    sourceUrl,
                    title.isBlank() ? keyword + " 企业信号" : title,
                    summary.isBlank() ? sourceName + " 返回的企业公开数据" : summary,
                    LocalDateTime.now(),
                    75));
        }
        return signals;
    }

    private Object firstNonNull(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String value(Map<?, ?> map, String... keys) {
        Object result = firstNonNull(map, keys);
        return result == null ? "" : String.valueOf(result);
    }
}
