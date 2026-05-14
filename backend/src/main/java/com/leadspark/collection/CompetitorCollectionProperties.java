package com.leadspark.collection;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "leadspark.competitor-collection")
public class CompetitorCollectionProperties {
    private boolean enabled = true;
    private String cron = "0 30 2 * * *";
    private long tenantId = 1L;
    private List<String> keywords = new ArrayList<>();
    private Sources sources = new Sources();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public long getTenantId() {
        return tenantId;
    }

    public void setTenantId(long tenantId) {
        this.tenantId = tenantId;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public Sources getSources() {
        return sources;
    }

    public void setSources(Sources sources) {
        this.sources = sources;
    }

    public static class Sources {
        private Source mock = new Source();
        private Source qichacha = new Source();
        private Source tianyancha = new Source();
        private Source searchApi = new Source();
        private Source internal = new Source();
        private Source recruitmentApi = new Source();
        private Source biddingApi = new Source();
        private Source newsApi = new Source();
        private Source websiteApi = new Source();

        public Source getMock() {
            return mock;
        }

        public void setMock(Source mock) {
            this.mock = mock;
        }

        public Source getQichacha() {
            return qichacha;
        }

        public void setQichacha(Source qichacha) {
            this.qichacha = qichacha;
        }

        public Source getTianyancha() {
            return tianyancha;
        }

        public void setTianyancha(Source tianyancha) {
            this.tianyancha = tianyancha;
        }

        public Source getSearchApi() {
            return searchApi;
        }

        public void setSearchApi(Source searchApi) {
            this.searchApi = searchApi;
        }

        public Source getInternal() {
            return internal;
        }

        public void setInternal(Source internal) {
            this.internal = internal;
        }

        public Source getRecruitmentApi() {
            return recruitmentApi;
        }

        public void setRecruitmentApi(Source recruitmentApi) {
            this.recruitmentApi = recruitmentApi;
        }

        public Source getBiddingApi() {
            return biddingApi;
        }

        public void setBiddingApi(Source biddingApi) {
            this.biddingApi = biddingApi;
        }

        public Source getNewsApi() {
            return newsApi;
        }

        public void setNewsApi(Source newsApi) {
            this.newsApi = newsApi;
        }

        public Source getWebsiteApi() {
            return websiteApi;
        }

        public void setWebsiteApi(Source websiteApi) {
            this.websiteApi = websiteApi;
        }
    }

    public static class Source {
        private boolean enabled;
        private String baseUrl = "";
        private String apiKey = "";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isConfigured() {
            return enabled && baseUrl != null && !baseUrl.isBlank();
        }
    }
}
