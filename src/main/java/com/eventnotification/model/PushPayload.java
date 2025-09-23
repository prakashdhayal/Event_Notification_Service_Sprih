package com.eventnotification.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for push notification events
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class PushPayload extends EventPayload {
    
    @NotBlank(message = "Device ID is required")
    @JsonProperty("deviceId")
    private String deviceId;
    
    @NotBlank(message = "Message is required")
    @JsonProperty("message")
    private String message;

    @Override
    public void validate() throws IllegalArgumentException {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID is required");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required");
        }
    }
}