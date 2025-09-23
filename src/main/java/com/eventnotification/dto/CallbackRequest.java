package com.eventnotification.dto;

import com.eventnotification.model.EventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for callback notifications sent to client systems
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CallbackRequest {
    
    @JsonProperty("eventId")
    private String eventId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("eventType")
    private EventType eventType;
    
    @JsonProperty("processedAt")
    private String processedAt;
    
    @JsonProperty("errorMessage")
    private String errorMessage;

    public CallbackRequest(String eventId, String status, EventType eventType, String processedAt) {
        this.eventId = eventId;
        this.status = status;
        this.eventType = eventType;
        this.processedAt = processedAt;
    }
}