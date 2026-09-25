package org.api.standingsapiservice.controller;

import org.api.standingsapiservice.model.DriverStanding;
import org.api.standingsapiservice.repository.StandingsRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StandingsController {

    private final StandingsRepository repository;

    public StandingsController(StandingsRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/api/standings/{season}")
    public List<DriverStanding> getStandings(@PathVariable int season) {
        return repository.findBySeason(season);
    }
}
