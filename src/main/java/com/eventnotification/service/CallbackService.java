package com.eventnotification.service;

import com.eventnotification.dto.CallbackRequest;
import com.eventnotification.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.format.DateTimeFormatter;

/**
 * Service responsible for sending callback notifications to client systems
 */
@Service
public class CallbackService {

    private static final Logger log = LoggerFactory.getLogger(CallbackService.class);

    @Value("${app.callback.retry-attempts:1}")
    private int retryAttempts;

    private final RestTemplate restTemplate;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public CallbackService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Send callback notification for an event
     */
    public void sendCallback(Event event) {
        if (event.getCallbackUrl() == null || event.getCallbackUrl().trim().isEmpty()) {
            log.warn("No callback URL provided for event {}", event.getEventId());
            return;
        }

        CallbackRequest callbackRequest = createCallbackRequest(event);
        
        for (int attempt = 1; attempt <= retryAttempts; attempt++) {
            try {
                boolean success = sendCallbackRequest(event.getCallbackUrl(), callbackRequest);
                if (success) {
                    log.info("Callback sent successfully for event {} (attempt {})", 
                        event.getEventId(), attempt);
                    return;
                }
            } catch (Exception e) {
                log.warn("Callback attempt {} failed for event {}: {}", 
                    attempt, event.getEventId(), e.getMessage());
                
                if (attempt == retryAttempts) {
                    log.error("All callback attempts failed for event {}", event.getEventId());
                } else {
                    try {
                        Thread.sleep(1000 * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }

    private CallbackRequest createCallbackRequest(Event event) {
        String processedAt = event.getProcessedAt() != null ? 
            event.getProcessedAt().format(formatter) : null;

        CallbackRequest request = new CallbackRequest(
            event.getEventId(),
            event.getStatus().name(),
            event.getEventType(),
            processedAt
        );

        if (event.getErrorMessage() != null) {
            request.setErrorMessage(event.getErrorMessage());
        }

        return request;
    }

    private boolean sendCallbackRequest(String callbackUrl, CallbackRequest callbackRequest) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create request entity
            HttpEntity<CallbackRequest> requestEntity = new HttpEntity<>(callbackRequest, headers);
            
            // Send POST request
            ResponseEntity<String> response = restTemplate.exchange(
                callbackUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            int statusCode = response.getStatusCode().value();
            
            if (statusCode >= 200 && statusCode < 300) {
                log.debug("Callback successful for URL {}: HTTP {}", callbackUrl, statusCode);
                return true;
            } else {
                log.warn("Callback failed for URL {}: HTTP {}", callbackUrl, statusCode);
                return false;
            }
            
        } catch (RestClientException e) {
            log.warn("Callback request failed for URL {}: {}", callbackUrl, e.getMessage());
            throw e;
        }
    }
}