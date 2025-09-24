package com.eventnotification.controller;

import com.eventnotification.service.EventProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for EventController API endpoints
 * Tests API layer validation, error handling, and request processing
 */
@WebMvcTest(EventController.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventProcessingService eventProcessingService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Set default mock behavior
        when(eventProcessingService.isAcceptingEvents()).thenReturn(true);
        when(eventProcessingService.addEvent(any())).thenReturn(true);
    }

    @Test
    void testValidEventSubmission_Email() throws Exception {
        // Test Case: Valid Event Submission - EMAIL
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test email message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "EMAIL");
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.message").value("Event accepted for processing."));

        verify(eventProcessingService).addEvent(any());
    }

    @Test
    void testValidEventSubmission_SMS() throws Exception {
        // Test Case: Valid Event Submission - SMS
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("phoneNumber", "+1234567890");
        payload.put("message", "Test SMS message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "SMS");
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.message").value("Event accepted for processing."));

        verify(eventProcessingService).addEvent(any());
    }

    @Test
    void testValidEventSubmission_PUSH() throws Exception {
        // Test Case: Valid Event Submission - PUSH
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("deviceId", "device-123");
        payload.put("message", "Test push message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "PUSH");
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.message").value("Event accepted for processing."));

        verify(eventProcessingService).addEvent(any());
    }

    @Test
    void testInvalidEventType() throws Exception {
        // Test Case: Invalid Event Type - Return 400 Bad Request
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "INVALID_TYPE");
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(eventProcessingService, never()).addEvent(any());
    }

    @Test
    void testMissingEventType() throws Exception {
        // Test Case: Missing Event Type - Return 400 Bad Request
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        Map<String, Object> request = new HashMap<>();
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.eventType").exists());

        verify(eventProcessingService, never()).addEvent(any());
    }

    @Test
    void testMissingPayload() throws Exception {
        // Test Case: Missing Payload - Return 400 Bad Request
        
        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "EMAIL");
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.payload").exists());

        verify(eventProcessingService, never()).addEvent(any());
    }

    @Test
    void testMissingCallbackUrl() throws Exception {
        // Test Case: Missing Callback URL - Return 400 Bad Request
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "EMAIL");
        request.put("payload", payload);

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.callbackUrl").exists());

        verify(eventProcessingService, never()).addEvent(any());
    }

    @Test
    void testInvalidEmailPayload() throws Exception {
        // Test Case: Missing Payload Fields - EMAIL without recipient
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "EMAIL");
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid payload: Recipient email is required"));

        verify(eventProcessingService, never()).addEvent(any());
    }

    @Test
    void testInvalidSMSPayload() throws Exception {
        // Test Case: Missing Payload Fields - SMS without phone number
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "SMS");
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid payload: Phone number is required"));

        verify(eventProcessingService, never()).addEvent(any());
    }

    @Test
    void testInvalidPushPayload() throws Exception {
        // Test Case: Missing Payload Fields - PUSH without device ID
        
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "Test message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "PUSH");
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid payload: Device ID is required"));

        verify(eventProcessingService, never()).addEvent(any());
    }

    @Test
    void testSystemShuttingDown() throws Exception {
        // Test Case: System is shutting down - Return 503 Service Unavailable
        
        when(eventProcessingService.isAcceptingEvents()).thenReturn(false);

        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "EMAIL");
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("System is shutting down, not accepting new events"));

        verify(eventProcessingService, never()).addEvent(any());
    }

    @Test
    void testQueueFull() throws Exception {
        // Test Case: Queue is full - Return 503 Service Unavailable
        
        when(eventProcessingService.addEvent(any())).thenReturn(false);

        Map<String, Object> payload = new HashMap<>();
        payload.put("recipient", "test@example.com");
        payload.put("message", "Test message");

        Map<String, Object> request = new HashMap<>();
        request.put("eventType", "EMAIL");
        request.put("payload", payload);
        request.put("callbackUrl", "http://test-callback.com");

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service temporarily unavailable - queue is full"));
    }

    @Test
    void testCallbackWebhookEndpoint() throws Exception {
        // Test Case: Callback webhook endpoint receives POST correctly
        
        Map<String, Object> callbackPayload = new HashMap<>();
        callbackPayload.put("eventId", "test-123");
        callbackPayload.put("status", "COMPLETED");
        callbackPayload.put("eventType", "EMAIL");
        callbackPayload.put("message", "Event processed successfully");

        mockMvc.perform(post("/api/webhook/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(callbackPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.message").value("Callback processed successfully"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testCallbackHealthCheck() throws Exception {
        // Test Case: Callback health check endpoint
        
        mockMvc.perform(get("/api/webhook/callback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.message").value("Callback webhook is ready to receive events"))
                .andExpect(jsonPath("$.endpoint").value("POST /api/webhook/callback"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testMalformedJson() throws Exception {
        // Test Case: Malformed JSON request - Return 400 Bad Request
        
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(eventProcessingService, never()).addEvent(any());
    }

    @Test
    void testEmptyRequestBody() throws Exception {
        // Test Case: Empty request body - Return 400 Bad Request
        
        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(""))
                .andExpect(status().isBadRequest());

        verify(eventProcessingService, never()).addEvent(any());
    }
    
}
