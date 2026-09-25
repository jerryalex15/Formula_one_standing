package org.api.standingsapiservice.listener;

import org.api.standingsapiservice.event.StandingsCalculatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class StandingsEventListener {

    private static final Logger log = LoggerFactory.getLogger(StandingsEventListener.class);

    @KafkaListener(topics = "driver-standings-calculated", groupId = "standings-api-group")
    @CacheEvict(value = "standings", allEntries = true)
    public void onStandingsCalculated(StandingsCalculatedEvent event) {
        log.info("Nouveau classement disponible : {} ({} → {}) — cache invalidé",
                event.jobName(), event.minYear(), event.maxYear());
    }
}
