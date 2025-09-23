package com.eventnotification.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core event model representing a notification event
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    
    private String eventId;
    private EventType eventType;
    private EventPayload payload;
    private String callbackUrl;
    private EventStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
    private String errorMessage;

    public Event(EventType eventType, EventPayload payload, String callbackUrl) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.payload = payload;
        this.callbackUrl = callbackUrl;
        this.status = EventStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public void markAsProcessing() {
        this.status = EventStatus.PROCESSING;
    }

    public void markAsCompleted() {
        this.status = EventStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }

    public void markAsFailed(String errorMessage) {
        this.status = EventStatus.FAILED;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }
}