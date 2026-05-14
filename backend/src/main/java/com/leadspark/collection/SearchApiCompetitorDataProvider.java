package com.leadspark.collection;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchApiCompetitorDataProvider extends AbstractHttpCompetitorDataProvider {
    private final CompetitorCollectionProperties properties;

    public SearchApiCompetitorDataProvider(CompetitorCollectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return "search-api";
    }

    @Override
    public List<CompetitorCompanySignal> collect(String keyword) {
        return collectFromHttpApi(
                keyword,
                properties.getSources().getSearchApi(),
                "search-api",
                "SEARCH_API");
    }
}
