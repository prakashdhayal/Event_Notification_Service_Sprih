package com.eventnotification.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Payload for email events
 */
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class EmailPayload extends EventPayload {
    
    @NotBlank(message = "Recipient email is required")
    @Email(message = "Recipient must be a valid email address")
    @JsonProperty("recipient")
    private String recipient;
    
    @NotBlank(message = "Message is required")
    @JsonProperty("message")
    private String message;

    @Override
    public void validate() throws IllegalArgumentException {
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required");
        }
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message is required");
        }
        // Basic email validation
        if (!recipient.contains("@") || !recipient.contains(".")) {
            throw new IllegalArgumentException("Recipient must be a valid email address");
        }
    }
}