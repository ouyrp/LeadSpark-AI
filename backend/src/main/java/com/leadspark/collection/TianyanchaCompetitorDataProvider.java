package com.leadspark.collection;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TianyanchaCompetitorDataProvider extends AbstractHttpCompetitorDataProvider {
    private final CompetitorCollectionProperties properties;

    public TianyanchaCompetitorDataProvider(CompetitorCollectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return "tianyancha";
    }

    @Override
    public List<CompetitorCompanySignal> collect(String keyword) {
        return collectFromHttpApi(
                keyword,
                properties.getSources().getTianyancha(),
                "tianyancha",
                "TIANYANCHA_API");
    }
}
