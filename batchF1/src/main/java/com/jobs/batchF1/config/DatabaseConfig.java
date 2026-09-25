package com.jobs.batchF1.config;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
class DatabaseConfig {

    @Bean
    PlatformTransactionManager transactionManager(DataSource dataSource) {
        return new JdbcTransactionManager(dataSource);
    }

    @Bean
    Tasklet clearTablesTasklet(DataSource dataSource) {
        return (contribution, chunkContext) -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            jdbcTemplate.execute("TRUNCATE TABLE results, sprint_results, driver_standings RESTART IDENTITY CASCADE");
            jdbcTemplate.execute("DELETE FROM races");
            jdbcTemplate.execute("DELETE FROM drivers");
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step clearTablesStep(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                Tasklet clearTablesTasklet) {
        return new StepBuilder("clearTablesStep", jobRepository)
                .tasklet(clearTablesTasklet, transactionManager)
                .build();
    }
}