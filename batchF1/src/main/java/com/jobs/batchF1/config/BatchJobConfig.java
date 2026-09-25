package com.jobs.batchF1.config;

import com.jobs.batchF1.listener.KafkaJobListener;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

@Configuration
public class BatchJobConfig {
    @Bean
    public Job f1StandingsJob(JobRepository jobRepository,
                              Step clearTablesStep,
                              Step importRacesStep,
                              Step importDriversStep,
                              Step importResultsStep,
                              Step importSprintResultsStep,
                              Step partitionedCalculateStandingsStep,
                              Step exportCsvStep,
                              TaskExecutor taskExecutor,
                              KafkaJobListener kafkaJobListener) {

        Flow flowRaces = new FlowBuilder<SimpleFlow>("flowRaces").start(importRacesStep).build();
        Flow flowDrivers = new FlowBuilder<SimpleFlow>("flowDrivers").start(importDriversStep).build();
        Flow flowResults = new FlowBuilder<SimpleFlow>("flowResults").start(importResultsStep).build();
        Flow flowSprint = new FlowBuilder<SimpleFlow>("flowSprint").start(importSprintResultsStep).build();

        Flow parallelIngestionFlow = new FlowBuilder<SimpleFlow>("parallelIngestionFlow")
                .split(taskExecutor)
                .add(flowRaces, flowDrivers, flowResults, flowSprint)
                .build();

        Flow mainFlow = new FlowBuilder<SimpleFlow>("mainFlow")
                .start(clearTablesStep)
                .next(parallelIngestionFlow)
                .next(partitionedCalculateStandingsStep)
                .next(exportCsvStep)
                .build();

        return new JobBuilder("f1StandingsJob", jobRepository)
                .listener(kafkaJobListener)
                .start(mainFlow)
                .end()
                .build();
    }
}


