package com.businessprocess.core.model.audit;

import com.businessprocess.core.util.TimeDurationFormatterUtil;
import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
public class Audit {
    // Getters
    private final LocalDateTime startTime;
    private LocalDateTime endTime;
    private Duration executionDuration;

    public Audit() {
        this.startTime = LocalDateTime.now();
    }

    public void complete() {
        this.endTime = LocalDateTime.now();
        calculateDuration();
    }

    private void calculateDuration() {
        if (startTime != null && endTime != null) {
            this.executionDuration = Duration.between(startTime, endTime);
        }
    }

    // Get current duration even if execution is not complete
    public Duration getCurrentDuration() {
        if (executionDuration != null) {
            return executionDuration;
        }
        return Duration.between(startTime, LocalDateTime.now());
    }

    // Get duration in seconds
    public long getDurationInSeconds() {
        return getCurrentDuration().getSeconds();
    }

    // Get duration in milliseconds
    public long getDurationInMillis() {
        return getCurrentDuration().toMillis();
    }

    //get duration in universal format
    public String getFormattedDuration() {
        return TimeDurationFormatterUtil.getFormattedDuration(getCurrentDuration());
    }
}
