package com.jobs.batchF1.scheduler;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class F1JobScheduler {

    private final JobOperator jobOperator;
    private final Job f1StandingsJob;

    public F1JobScheduler(JobOperator jobOperator, Job f1StandingsJob) {
        this.jobOperator = jobOperator;
        this.f1StandingsJob = f1StandingsJob;
    }

    // Exemple : tous les jours à 3h du matin
    @Scheduled(cron = "5/45 * * * * *")
    public void runJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("runId", System.currentTimeMillis()) // rend chaque exécution unique
                .toJobParameters();

        jobOperator.start(f1StandingsJob, params);
    }
}