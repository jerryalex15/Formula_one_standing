package com.jobs.batchF1.listener;

import com.jobs.batchF1.event.StandingsCalculatedEvent;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

@Component
public class KafkaJobListener implements JobExecutionListener {

    private static final String TOPIC = "driver-standings-calculated";

    private final KafkaTemplate<String, StandingsCalculatedEvent> kafkaTemplate;

    public KafkaJobListener(KafkaTemplate<String, StandingsCalculatedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus().isUnsuccessful()) {
            return; // on ne publie que si le job a réussi
        }

        StandingsCalculatedEvent event = new StandingsCalculatedEvent(
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getStatus().toString(),
                1950,
                LocalDate.now().getYear(),
                Instant.now()
        );

        kafkaTemplate.send(TOPIC, event.jobName(), event);
    }
}