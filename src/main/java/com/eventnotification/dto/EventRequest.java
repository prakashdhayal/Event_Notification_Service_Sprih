package com.eventnotification.dto;

import com.eventnotification.model.EventPayload;
import com.eventnotification.model.EventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
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
    @Valid
    @JsonProperty("payload")
    private EventPayload payload;
    
    @NotBlank(message = "Callback URL is required")
    @JsonProperty("callbackUrl")
    private String callbackUrl;
}