package com.leadspark.collection;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BiddingApiCompetitorDataProvider extends AbstractHttpCompetitorDataProvider {
    private final CompetitorCollectionProperties properties;

    public BiddingApiCompetitorDataProvider(CompetitorCollectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return "bidding-api";
    }

    @Override
    public List<CompetitorCompanySignal> collect(String keyword) {
        return collectFromHttpApi(
                keyword,
                properties.getSources().getBiddingApi(),
                "bidding-api",
                "BIDDING_API");
    }
}
