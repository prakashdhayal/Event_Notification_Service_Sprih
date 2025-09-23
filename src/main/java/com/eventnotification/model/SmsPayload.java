package com.eventnotification.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for SMS events
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class SmsPayload extends EventPayload {
    
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Phone number must be valid")
    @JsonProperty("phoneNumber")
    private String phoneNumber;
    
    @NotBlank(message = "Message is required")
    @JsonProperty("message")
    private String message;

    @Override
    public void validate() throws IllegalArgumentException {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required");
        }
        // Basic phone number validation
        String cleanedPhone = phoneNumber.replaceAll("[^\\d+]", "");
        if (cleanedPhone.length() < 10) {
            throw new IllegalArgumentException("Phone number must be valid");
        }
    }
}