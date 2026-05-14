package com.leadspark.collection;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class MockCompetitorDataProvider implements CompetitorDataProvider {
    private final CompetitorCollectionProperties properties;

    public MockCompetitorDataProvider(CompetitorCollectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public String sourceName() {
        return "mock";
    }

    @Override
    public List<CompetitorCompanySignal> collect(String keyword) {
        if (!properties.getSources().getMock().isEnabled()) {
            return List.of();
        }
        return List.of(
                new CompetitorCompanySignal(
                        keyword,
                        keyword + " 公开案例客户",
                        keyword,
                        "PUBLIC_WEB",
                        "mock-public-source",
                        "https://example.com/" + keyword,
                        keyword + " 同类产品公开案例线索",
                        "来自公开网页、新闻、案例页或合规数据 API 的企业信号占位数据。",
                        LocalDateTime.now(),
                        80),
                new CompetitorCompanySignal(
                        keyword,
                        keyword + " 相关招聘企业",
                        keyword,
                        "PUBLIC_WEB",
                        "mock-public-source",
                        "https://example.com/jobs/" + keyword,
                        keyword + " 相关岗位招聘信号",
                        "企业招聘销售运营、增长、线索获客等岗位，可作为潜在需求信号。",
                        LocalDateTime.now(),
                        68)
        );
    }
}
