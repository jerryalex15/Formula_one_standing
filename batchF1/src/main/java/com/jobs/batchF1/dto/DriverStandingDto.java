package com.jobs.batchF1.dto;

public record DriverStandingDto(
        Integer season,
        Integer position,
        String driverName,
        Double totalPoints
) {}
