package com.jobs.batchF1.dto;

public record RaceDto(
        Integer raceId,
        Integer year,
        Integer round,
        String name
) {}
