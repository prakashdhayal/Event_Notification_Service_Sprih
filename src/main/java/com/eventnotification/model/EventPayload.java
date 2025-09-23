package com.eventnotification.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class for event payloads
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = EmailPayload.class, name = "EMAIL"),
    @JsonSubTypes.Type(value = SmsPayload.class, name = "SMS"),
    @JsonSubTypes.Type(value = PushPayload.class, name = "PUSH")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class EventPayload {
    public abstract void validate() throws IllegalArgumentException;
}