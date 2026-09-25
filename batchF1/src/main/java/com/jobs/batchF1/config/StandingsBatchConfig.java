package com.jobs.batchF1.config;

import com.jobs.batchF1.dto.DriverStandingDto;
import com.jobs.batchF1.partitioner.DynamicDecadePartitioner;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader;
import org.springframework.batch.infrastructure.item.database.Order;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.infrastructure.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.infrastructure.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.batch.infrastructure.item.file.FlatFileItemWriter;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Map;

@Configuration
public class StandingsBatchConfig {
    // Reader worker : lit les points calculés pour la tranche d'années reçue en paramètre
    @Bean
    @StepScope
    JdbcPagingItemReader<DriverStandingDto> standingsWorkerReader(
            DataSource dataSource,
            @Value("#{stepExecutionContext['minYear']}") Integer minYear,
            @Value("#{stepExecutionContext['maxYear']}") Integer maxYear) throws Exception {

        SqlPagingQueryProviderFactoryBean provider = new SqlPagingQueryProviderFactoryBean();
        provider.setDataSource(dataSource);
        provider.setSelectClause("SELECT r.year AS season, DENSE_RANK() OVER (PARTITION BY r.year ORDER BY SUM(p.points) DESC) AS position, CONCAT(d.forename, ' ', d.surname) AS driver_name, SUM(p.points) AS total_points");
        provider.setFromClause("FROM (SELECT race_id, driver_id, points FROM results UNION ALL SELECT race_id, driver_id, points FROM sprint_results) p JOIN races r ON p.race_id = r.race_id JOIN drivers d ON p.driver_id = d.driver_id");
        provider.setWhereClause("WHERE r.year BETWEEN :minYear AND :maxYear");
        provider.setGroupClause("GROUP BY r.year, d.driver_id, d.forename, d.surname");
        provider.setSortKeys(Map.of("season", Order.ASCENDING, "position", Order.ASCENDING));

        return new JdbcPagingItemReaderBuilder<DriverStandingDto>()
                .name("standingsWorkerReader")
                .dataSource(dataSource)
                .queryProvider(provider.getObject())
                .parameterValues(Map.of("minYear", minYear, "maxYear", maxYear)) // Injection propre
                .pageSize(200)
                .rowMapper((rs, rowNum) -> new DriverStandingDto(
                        rs.getInt("season"),
                        rs.getInt("position"),
                        rs.getString("driver_name"),
                        rs.getDouble("total_points")
                ))
                .build();
    }

    // Writer worker : enregistre le classement calculé dans la table finale driver_standings
    @Bean
    JdbcBatchItemWriter<DriverStandingDto> standingsWorkerWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<DriverStandingDto>()
                .dataSource(dataSource)
                .sql("INSERT INTO driver_standings (season, position, driver_name, total_points) VALUES (:season, :position, :driverName, :totalPoints)")
                .beanMapped()
                .build();
    }

    // Worker Step : exécuté en parallèle par plusieurs threads
    @Bean
    Step calculateStandingsWorkerStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      JdbcPagingItemReader<DriverStandingDto> standingsWorkerReader,
                                      JdbcBatchItemWriter<DriverStandingDto> standingsWorkerWriter) {
        return new StepBuilder("calculateStandingsWorkerStep", jobRepository)
                .<DriverStandingDto, DriverStandingDto>chunk(100)
                .transactionManager(transactionManager)
                .reader(standingsWorkerReader)
                .writer(standingsWorkerWriter)
                .build();
    }

    // Master Step : découpe 1950-2026 en 4 partitions et délègue aux workers
    @Bean
    Step partitionedCalculateStandingsStep(JobRepository jobRepository,
                                           Step calculateStandingsWorkerStep,
                                           TaskExecutor taskExecutor,
                                           DataSource dataSource) {
        int currentYear = LocalDate.now().getYear();
        return new StepBuilder("partitionedCalculateStandingsStep", jobRepository)
                .partitioner("calculateStandingsWorkerStep", new DynamicDecadePartitioner(dataSource, 1950))
                .step(calculateStandingsWorkerStep)
                .gridSize(4)
                .taskExecutor(taskExecutor)
                .build();
    }

    // Step Final (Mono-thread) : extrait la BDD vers le CSV final ordonné
    @Bean
    Step exportCsvStep(JobRepository jobRepository,
                       PlatformTransactionManager transactionManager,
                       DataSource dataSource) throws Exception {

        SqlPagingQueryProviderFactoryBean provider = new SqlPagingQueryProviderFactoryBean();
        provider.setDataSource(dataSource);
        provider.setSelectClause("SELECT season, position, driver_name, total_points");
        provider.setFromClause("FROM driver_standings");
        provider.setSortKeys(Map.of("season", Order.ASCENDING, "position", Order.ASCENDING));

        JdbcPagingItemReader<DriverStandingDto> csvReader = new JdbcPagingItemReaderBuilder<DriverStandingDto>()
                .name("csvExportReader")
                .dataSource(dataSource)
                .queryProvider(provider.getObject())
                .pageSize(500)
                .rowMapper((rs, rowNum) -> new DriverStandingDto(
                        rs.getInt("season"),
                        rs.getInt("position"),
                        rs.getString("driver_name"),
                        rs.getDouble("total_points")
                ))
                .build();

        FlatFileItemWriter<DriverStandingDto> csvWriter = new FlatFileItemWriterBuilder<DriverStandingDto>()
                .name("csvExportWriter")
                .resource(new FileSystemResource("output/f1_driver_standings_1950_now.csv"))
                .delimited()
                .names("season", "position", "driverName", "totalPoints")
                .headerCallback(writer -> writer.write("season,position,driverName,totalPoints"))
                .build();

        return new StepBuilder("exportCsvStep", jobRepository)
                .<DriverStandingDto, DriverStandingDto>chunk(500)
                .transactionManager(transactionManager)
                .reader(csvReader)
                .writer(csvWriter)
                .build();
    }
}
