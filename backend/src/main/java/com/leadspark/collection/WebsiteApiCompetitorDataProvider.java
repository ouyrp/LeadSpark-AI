package com.leadspark.collection;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WebsiteApiCompetitorDataProvider extends AbstractHttpCompetitorDataProvider {
    private final CompetitorCollectionProperties properties;

    public WebsiteApiCompetitorDataProvider(CompetitorCollectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return "website-api";
    }

    @Override
    public List<CompetitorCompanySignal> collect(String keyword) {
        return collectFromHttpApi(
                keyword,
                properties.getSources().getWebsiteApi(),
                "website-api",
                "WEBSITE_API");
    }
}
