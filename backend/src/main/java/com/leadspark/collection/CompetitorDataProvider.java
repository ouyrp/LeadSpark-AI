package com.leadspark.collection;

import java.util.List;

public interface CompetitorDataProvider {
    String sourceName();

    List<CompetitorCompanySignal> collect(String keyword);
}
