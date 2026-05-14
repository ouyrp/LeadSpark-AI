package com.leadspark.collection;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QichachaCompetitorDataProvider extends AbstractHttpCompetitorDataProvider {
    private final CompetitorCollectionProperties properties;

    public QichachaCompetitorDataProvider(CompetitorCollectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return "qichacha";
    }

    @Override
    public List<CompetitorCompanySignal> collect(String keyword) {
        return collectFromHttpApi(
                keyword,
                properties.getSources().getQichacha(),
                "qichacha",
                "QICHACHA_API");
    }
}
