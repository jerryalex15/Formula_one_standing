package org.api.standingsapiservice.repository;

import org.api.standingsapiservice.model.DriverStanding;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class StandingsRepository {

    private final JdbcTemplate jdbcTemplate;

    public StandingsRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Cacheable(value = "standings", key = "#season")
    public List<DriverStanding> findBySeason(int season) {
        System.out.println("→ Requête SQL exécutée pour la saison " + season); // pour observer le cache en action
        return jdbcTemplate.query(
                "SELECT season, position, driver_name, total_points FROM driver_standings WHERE season = ? ORDER BY position ASC",
                (rs, rowNum) -> new DriverStanding(
                        rs.getInt("season"),
                        rs.getInt("position"),
                        rs.getString("driver_name"),
                        rs.getDouble("total_points")
                ),
                season
        );
    }
}
