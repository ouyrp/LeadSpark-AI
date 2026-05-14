package com.leadspark.collection;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RecruitmentApiCompetitorDataProvider extends AbstractHttpCompetitorDataProvider {
    private final CompetitorCollectionProperties properties;

    public RecruitmentApiCompetitorDataProvider(CompetitorCollectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return "recruitment-api";
    }

    @Override
    public List<CompetitorCompanySignal> collect(String keyword) {
        return collectFromHttpApi(
                keyword,
                properties.getSources().getRecruitmentApi(),
                "recruitment-api",
                "RECRUITMENT_API");
    }
}
