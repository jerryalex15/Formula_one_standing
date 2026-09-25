package com.jobs.batchF1.event;

import java.time.Instant;

public record StandingsCalculatedEvent(
        String jobName,
        String status,
        int minYear,
        int maxYear,
        Instant completedAt
) {}
