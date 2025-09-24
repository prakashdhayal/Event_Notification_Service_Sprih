package com.eventnotification;

import com.eventnotification.controller.EventController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.eventnotification.service.EventProcessingService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the Event Notification System
 * Tests the complete flow from HTTP request to event processing
 */
@WebMvcTest(EventController.class)
class EventNotificationSystemIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private EventProcessingService eventProcessingService;

    @BeforeEach
    void setUp() {
        // Mock that the system is accepting events
        when(eventProcessingService.isAcceptingEvents()).thenReturn(true);
        // Mock that events are successfully added to the queue
        when(eventProcessingService.addEvent(any())).thenReturn(true);
    }

    @Test
    void testCompleteEmailEventFlow() throws Exception {
        // Test Case: Complete Email Event Flow
        String emailRequest = """
            {
                "eventType": "EMAIL",
                "payload": {
                    "recipient": "test@example.com",
                    "message": "Integration test email message"
                },
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emailRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCompleteSmsEventFlow() throws Exception {
        // Test Case: Complete SMS Event Flow
        String smsRequest = """
            {
                "eventType": "SMS",
                "payload": {
                    "phoneNumber": "+1234567890",
                    "message": "Integration test SMS message"
                },
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(smsRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testCompletePushEventFlow() throws Exception {
        // Test Case: Complete Push Event Flow  
        String pushRequest = """
            {
                "eventType": "PUSH",
                "payload": {
                    "deviceId": "device123",
                    "message": "Integration test push message"
                },
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pushRequest))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventId").exists())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void testInvalidEventTypeHandling() throws Exception {
        // Test Case: Invalid Event Type Handling
        String invalidRequest = """
            {
                "eventType": "INVALID_TYPE",
                "payload": {
                    "recipient": "test@example.com",
                    "message": "Test message"
                },
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingPayloadHandling() throws Exception {
        // Test Case: Missing Payload Handling
        String requestWithoutPayload = """
            {
                "eventType": "EMAIL",
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithoutPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMissingCallbackUrlHandling() throws Exception {
        // Test Case: Missing Callback URL Handling
        String requestWithoutCallback = """
            {
                "eventType": "EMAIL",
                "payload": {
                    "recipient": "test@example.com",
                    "message": "Test message"
                }
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestWithoutCallback))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testInvalidEmailPayloadHandling() throws Exception {
        // Test Case: Invalid Email Payload - Invalid email format
        String invalidEmailRequest = """
            {
                "eventType": "EMAIL",
                "payload": {
                    "recipient": "invalid-email-format",
                    "message": "Test message"
                },
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidEmailRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testInvalidSmsPayloadHandling() throws Exception {
        // Test Case: Invalid SMS Payload - Short phone number
        String invalidSmsRequest = """
            {
                "eventType": "SMS",
                "payload": {
                    "phoneNumber": "123",
                    "message": "Test message"
                },
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidSmsRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testEmptyPayloadFieldsHandling() throws Exception {
        // Test Case: Empty Payload Fields
        String emptyFieldsRequest = """
            {
                "eventType": "EMAIL",
                "payload": {
                    "recipient": "",
                    "message": ""
                },
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emptyFieldsRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void testSystemShuttingDownScenario() throws Exception {
        // Test Case: System Shutting Down - Should return 503
        when(eventProcessingService.isAcceptingEvents()).thenReturn(false);
        
        String emailRequest = """
            {
                "eventType": "EMAIL",
                "payload": {
                    "recipient": "test@example.com",
                    "message": "Test message"
                },
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emailRequest))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("System is shutting down, not accepting new events"));
    }

    @Test
    void testQueueFullScenario() throws Exception {
        // Test Case: Queue Full - Should return 503
        when(eventProcessingService.addEvent(any())).thenReturn(false);
        
        String emailRequest = """
            {
                "eventType": "EMAIL",
                "payload": {
                    "recipient": "test@example.com",
                    "message": "Test message"
                },
                "callbackUrl": "http://test-callback.com"
            }
            """;

        mockMvc.perform(post("/api/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(emailRequest))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Service temporarily unavailable - queue is full"));
    }
}