package com.jobs.batchF1.dto;

// Ce même DTO sera utilisé par le Reader pour results.csv ET sprint_results.csv
public record ResultDto(
        Integer resultId,
        Integer raceId,
        Integer driverId,
        Double points
) {}

