package com.eventnotification.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Base class for event payloads
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class EventPayload {
    public abstract void validate() throws IllegalArgumentException;
}