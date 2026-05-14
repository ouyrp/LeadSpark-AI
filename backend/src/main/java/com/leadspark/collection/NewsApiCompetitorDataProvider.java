package com.leadspark.collection;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NewsApiCompetitorDataProvider extends AbstractHttpCompetitorDataProvider {
    private final CompetitorCollectionProperties properties;

    public NewsApiCompetitorDataProvider(CompetitorCollectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return "news-api";
    }

    @Override
    public List<CompetitorCompanySignal> collect(String keyword) {
        return collectFromHttpApi(
                keyword,
                properties.getSources().getNewsApi(),
                "news-api",
                "NEWS_API");
    }
}
