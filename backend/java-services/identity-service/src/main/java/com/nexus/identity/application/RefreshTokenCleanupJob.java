package com.nexus.identity.application;

import com.nexus.identity.infrastructure.persistence.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/** Retention policy: delete refresh tokens 30 days after they expired (docs/database §6). */
@Component
public class RefreshTokenCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);
    static final Duration RETENTION = Duration.ofDays(30);

    private final RefreshTokenRepository repo;
    private final Clock clock;

    public RefreshTokenCleanupJob(RefreshTokenRepository repo, Clock clock) {
        this.repo = repo;
        this.clock = clock;
    }

    @Scheduled(cron = "${nexus.identity.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void purge() {
        int deleted = repo.deleteExpiredBefore(clock.instant().minus(RETENTION));
        if (deleted > 0) log.info("Purged {} expired refresh tokens", deleted);
    }
}
