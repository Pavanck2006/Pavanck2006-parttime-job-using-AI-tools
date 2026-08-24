package com.parttimejob.util;

import com.parttimejob.exception.BadRequestException;

import java.time.LocalDate;
import java.time.LocalTime;

public class DateValidationUtils {

    public static void validateJobSchedule(LocalDate jobDate, LocalTime startTime, LocalTime endTime) {
        if (jobDate == null) {
            throw new BadRequestException("Job date is required");
        }
        if (jobDate.isBefore(LocalDate.now())) {
            throw new BadRequestException("Job date cannot be in the past: " + jobDate);
        }
        if (startTime == null || endTime == null) {
            throw new BadRequestException("Start time and end time are required");
        }
        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new BadRequestException("Start time (" + startTime + ") must be before end time (" + endTime + ")");
        }
    }
}
