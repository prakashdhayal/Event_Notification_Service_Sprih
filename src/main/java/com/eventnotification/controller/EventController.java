package com.eventnotification.controller;

import com.eventnotification.dto.EventRequest;
import com.eventnotification.dto.EventResponse;
import com.eventnotification.model.EventPayload;
import com.eventnotification.model.EventType;
import com.eventnotification.model.Event;
import com.eventnotification.service.EventProcessingService;

import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for handling event notification operations
 */
@RestController
@RequestMapping("/api")
public class EventController {

    private static final Logger log = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventProcessingService eventProcessingService;

    /**
     * Create a new event for processing
     */
    @PostMapping("/events")
    public ResponseEntity<?> createEvent(@Valid @RequestBody EventRequest eventRequest, 
                                       BindingResult bindingResult) {
        
        log.info("Received event request: type={}, callbackUrl={}", 
            eventRequest.getEventType(), eventRequest.getCallbackUrl());

        // Check if system is accepting events
        if (!eventProcessingService.isAcceptingEvents()) {
            log.warn("System is shutting down, rejecting event request");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(createErrorResponse("System is shutting down, not accepting new events"));
        }

        // Validate request binding
        if (bindingResult.hasErrors()) {
            log.warn("Validation errors in event request: {}", bindingResult.getAllErrors());
            return ResponseEntity.badRequest()
                .body(createValidationErrorResponse(bindingResult));
        }

        try {
            // Validate payload based on event type
            validateEventPayload(eventRequest.getEventType(), eventRequest.getPayload());
            
            // Create event
            Event event = new Event(
                eventRequest.getEventType(),
                eventRequest.getPayload(),
                eventRequest.getCallbackUrl()
            );

            // Add to processing queue
            boolean added = eventProcessingService.addEvent(event);
            
            if (added) {
                log.info("Event {} successfully queued for processing", event.getEventId());
                return ResponseEntity.ok(EventResponse.success(event.getEventId()));
            } else {
                log.warn("Failed to queue event - queue might be full");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(createErrorResponse("Service temporarily unavailable - queue is full"));
            }

        } catch (IllegalArgumentException e) {
            log.warn("Invalid event payload: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid payload: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error processing event request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Internal server error"));
        }
    }

    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", message);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    private Map<String, Object> createValidationErrorResponse(BindingResult bindingResult) {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Validation failed");
        response.put("timestamp", System.currentTimeMillis());
        
        Map<String, String> fieldErrors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error -> 
            fieldErrors.put(error.getField(), error.getDefaultMessage()));
        
        if (!fieldErrors.isEmpty()) {
            response.put("fieldErrors", fieldErrors);
        }
        
        return response;
    }

    private void validateEventPayload(EventType eventType, EventPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload is required");
        }

        // Validate payload matches event type
        boolean validPayload = switch (eventType) {
            case EMAIL -> payload instanceof com.eventnotification.model.EmailPayload;
            case SMS -> payload instanceof com.eventnotification.model.SmsPayload;
            case PUSH -> payload instanceof com.eventnotification.model.PushPayload;
        };

        if (!validPayload) {
            throw new IllegalArgumentException("Payload type does not match event type " + eventType);
        }

        // Call payload-specific validation
        payload.validate();
    }


}