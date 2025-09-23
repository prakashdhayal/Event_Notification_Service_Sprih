package com.eventnotification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for event creation responses
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("message")
    private String message;

    public static EventResponse success(String eventId) {
        return new EventResponse(eventId, "Event accepted for processing.");
    }
}