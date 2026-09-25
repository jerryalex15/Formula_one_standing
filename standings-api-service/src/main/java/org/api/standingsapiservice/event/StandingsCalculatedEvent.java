package org.api.standingsapiservice.event;

import java.time.Instant;

// identique à producer
public record StandingsCalculatedEvent(
        String jobName,
        String status,
        int minYear,
        int maxYear,
        Instant completedAt
) {}
