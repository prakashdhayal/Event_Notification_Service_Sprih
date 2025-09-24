package com.eventnotification.dto;

import com.eventnotification.model.EventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for event creation requests
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {
    
    @NotNull(message = "Event type is required")
    @JsonProperty("eventType")
    private EventType eventType;
    
    @NotNull(message = "Payload is required")
    @JsonProperty("payload")
    private JsonNode payload;
    
    @NotBlank(message = "Callback URL is required")
    @JsonProperty("callbackUrl")
    private String callbackUrl;
}