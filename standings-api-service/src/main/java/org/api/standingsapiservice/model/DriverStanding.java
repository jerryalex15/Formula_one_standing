package org.api.standingsapiservice.model;

public record DriverStanding(
        int season,
        int position,
        String driverName,
        double totalPoints
) {}