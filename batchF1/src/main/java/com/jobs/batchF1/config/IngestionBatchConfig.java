package com.jobs.batchF1.config;

import com.jobs.batchF1.dto.DriversDto;
import com.jobs.batchF1.dto.RaceDto;
import com.jobs.batchF1.dto.ResultDto;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class IngestionBatchConfig {
    // --- STEP 1: IMPORT RACES ---
    @Bean
    public FlatFileItemReader<RaceDto> racesReader() {
        return new FlatFileItemReaderBuilder<RaceDto>()
                .name("racesReader")
                .resource(new ClassPathResource("data/races.csv"))
                .delimited()
                .quoteCharacter('"')
                .includedFields(0, 1, 2, 4)
                .names("raceId", "year", "round", "name")
                .linesToSkip(1)
                .targetType(RaceDto.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<RaceDto> racesWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<RaceDto>()
                .dataSource(dataSource)
                .sql("INSERT INTO races (race_id, year, round, name) VALUES (:raceId, :year, :round, :name)")
                .beanMapped()
                .build();
    }

    @Bean
    public Step importRacesStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, DataSource dataSource) {
        return new StepBuilder("importRacesStep", jobRepository)
                .<RaceDto, RaceDto>chunk(500)
                .transactionManager(transactionManager)
                .reader(racesReader())
                .writer(racesWriter(dataSource))
                .build();
    }

    // --- STEP 2: IMPORT DRIVERS ---
    @Bean
    public FlatFileItemReader<DriversDto> driversReader() {
        return new FlatFileItemReaderBuilder<DriversDto>()
                .name("driversReader")
                .resource(new ClassPathResource("data/drivers.csv"))
                .delimited()
                .quoteCharacter('"')
                .includedFields(0, 1, 4, 5)
                .names("driverId", "driverRef", "forename", "surname")                .linesToSkip(1)
                .targetType(DriversDto.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<DriversDto> driversWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<DriversDto>()
                .dataSource(dataSource)
                .sql("INSERT INTO drivers (driver_id, driver_ref, forename, surname) VALUES (:driverId, :driverRef, :forename, :surname)")
                .beanMapped()
                .build();
    }

    @Bean
    public Step importDriversStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, DataSource dataSource) {
        return new StepBuilder("importDriversStep", jobRepository)
                .<DriversDto, DriversDto>chunk(500)
                .transactionManager(transactionManager)
                .reader(driversReader())
                .writer(driversWriter(dataSource))
                .build();
    }

    // --- STEP 3: IMPORT RESULTS ---
    @Bean
    public FlatFileItemReader<ResultDto> resultsReader() {
        return new FlatFileItemReaderBuilder<ResultDto>()
                .name("resultsReader")
                .resource(new ClassPathResource("data/results.csv"))
                .delimited()
                .quoteCharacter('"')
                .includedFields(0, 1, 2, 9)
                .names("resultId", "raceId", "driverId", "points")
                .linesToSkip(1)
                .targetType(ResultDto.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<ResultDto> resultsWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ResultDto>()
                .dataSource(dataSource)
                .sql("INSERT INTO results (result_id, race_id, driver_id, points) VALUES (:resultId, :raceId, :driverId, :points)")
                .beanMapped()
                .build();
    }

    @Bean
    public Step importResultsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, DataSource dataSource) {
        return new StepBuilder("importResultsStep", jobRepository)
                .<ResultDto, ResultDto>chunk(1000)
                .transactionManager(transactionManager)
                .reader(resultsReader())
                .writer(resultsWriter(dataSource))
                .build();
    }

    // --- STEP 4: IMPORT SPRINT RESULTS ---
    @Bean
    public FlatFileItemReader<ResultDto> sprintResultsReader() {
        return new FlatFileItemReaderBuilder<ResultDto>()
                .name("sprintResultsReader")
                .resource(new ClassPathResource("data/sprint_results.csv"))
                .delimited()
                .quoteCharacter('"')
                .includedFields(0, 1, 2, 9)
                .names("resultId", "raceId", "driverId", "points")
                .linesToSkip(1)
                .targetType(ResultDto.class)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<ResultDto> sprintResultsWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<ResultDto>()
                .dataSource(dataSource)
                .sql("INSERT INTO sprint_results (result_id, race_id, driver_id, points) VALUES (:resultId, :raceId, :driverId, :points)")
                .beanMapped()
                .build();
    }

    @Bean
    public Step importSprintResultsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, DataSource dataSource) {
        return new StepBuilder("importSprintResultsStep", jobRepository)
                .<ResultDto, ResultDto>chunk(1000)
                .transactionManager(transactionManager)
                .reader(sprintResultsReader())
                .writer(sprintResultsWriter(dataSource))
                .build();
    }
}
