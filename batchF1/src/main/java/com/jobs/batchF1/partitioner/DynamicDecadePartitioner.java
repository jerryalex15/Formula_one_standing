package com.jobs.batchF1.partitioner;

import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class DynamicDecadePartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;
    private final int startYear;

    public DynamicDecadePartitioner(DataSource dataSource, int startYear) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.startYear = startYear;
    }

    /**
     * Create a set of distinct {@link ExecutionContext} instances together with a unique
     * identifier for each one. The identifiers should be short, mnemonic values, and only
     * have to be unique within the return value (e.g. use an incrementer).
     *
     * @param gridSize the size of the map to return
     * @return a map from identifier to input parameters
     */
    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // 1. Requête SQL dynamique pour trouver l'année maximale disponible en BDD
        Integer maxYearInDb = jdbcTemplate.queryForObject("SELECT MAX(year) FROM races", Integer.class);
        int endYear = (maxYearInDb != null) ? maxYearInDb : startYear;

        Map<String, ExecutionContext> result = new HashMap<>();
        int range = (endYear - startYear + 1) / gridSize;
        int currentStart = startYear;

        for (int i = 0; i < gridSize; i++) {
            ExecutionContext value = new ExecutionContext();
            int currentEnd = (i == gridSize - 1) ? endYear : (currentStart + range - 1);

            value.putInt("minYear", currentStart);
            value.putInt("maxYear", currentEnd);
            result.put("partition_" + i, value);

            currentStart = currentEnd + 1;
        }
        return result;
    }
}
