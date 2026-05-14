package com.leadspark.collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CompetitorCollectionScheduler {
    private static final Logger log = LoggerFactory.getLogger(CompetitorCollectionScheduler.class);

    private final CompetitorCollectionProperties properties;
    private final CompetitorCollectionService collectionService;

    public CompetitorCollectionScheduler(
            CompetitorCollectionProperties properties,
            CompetitorCollectionService collectionService) {
        this.properties = properties;
        this.collectionService = collectionService;
    }

    @Scheduled(cron = "${leadspark.competitor-collection.cron}", zone = "Asia/Shanghai")
    public void runDaily() {
        if (!properties.isEnabled()) {
            log.info("competitor collection skipped because it is disabled");
            return;
        }
        log.info("competitor collection started by scheduler");
        collectionService.run("SCHEDULED");
    }
}
