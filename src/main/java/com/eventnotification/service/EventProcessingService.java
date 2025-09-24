package com.eventnotification.service;

import com.eventnotification.model.Event;
import com.eventnotification.model.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service class for processing event notifications
 */
@Service
public class EventProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingService.class);

    @Autowired
    private CallbackService callbackService;

    @Value("${app.event-processing.failure-rate}")
    private double failureRate;

    @Value("${app.event-processing.queue-capacity}")
    private int queueCapacity;

    @Value("${app.event-processing.thread-pool.email}")
    private int emailThreads;

    @Value("${app.event-processing.thread-pool.sms}")
    private int smsThreads;

    @Value("${app.event-processing.thread-pool.push}")
    private int pushThreads;

    @Value("${app.event-processing.processing-time.email}")
    private int emailProcessingTime;

    @Value("${app.event-processing.processing-time.sms}")
    private int smsProcessingTime;

    @Value("${app.event-processing.processing-time.push}")
    private int pushProcessingTime;

    
    private final ConcurrentHashMap<EventType, BlockingQueue<Event>> queues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EventType, ExecutorService> executors = new ConcurrentHashMap<>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Random random = new Random();

    @PostConstruct
    public void initialize() {
        log.info("Initializing Event Processing Service");
        
        // Initialize queues
        for (EventType eventType : EventType.values()) {
            queues.put(eventType, new LinkedBlockingQueue<>(queueCapacity));
        }

        // Initialize thread pools for each event type
        executors.put(EventType.EMAIL, Executors.newFixedThreadPool(emailThreads));
        executors.put(EventType.SMS, Executors.newFixedThreadPool(smsThreads));
        executors.put(EventType.PUSH, Executors.newFixedThreadPool(pushThreads));

        // Start worker threads for each event type
        startWorkers();
        
        log.info("Event Processing Service initialized successfully");
    }

    private void startWorkers() {
        for (EventType eventType : EventType.values()) {
            ExecutorService executor = executors.get(eventType);
            BlockingQueue<Event> queue = queues.get(eventType);
            
            // Start workers for this event type
            int threadCount = getThreadCount(eventType);
            for (int i = 0; i < threadCount; i++) {
                final int workerId = i + 1;
                executor.submit(() -> processEvents(eventType, queue, workerId));
            }
            
            log.info("Started {} worker threads for {} events", threadCount, eventType);
        }
    }

    private int getThreadCount(EventType eventType) {
        return switch (eventType) {
            case EMAIL -> emailThreads;
            case SMS -> smsThreads;
            case PUSH -> pushThreads;
        };
    }

    /**
     * Check if system is accepting new events
     */
    public boolean isAcceptingEvents() {
        return !shutdown.get();
    }

    /**
     * Add an event to the appropriate queue for processing
     */
    public boolean addEvent(Event event) {
        if (event == null) {
            log.warn("Rejecting null event");
            return false;
        }
        
        if (shutdown.get()) {
            log.warn("System is shutting down, rejecting new event: {}", event.getEventId());
            return false;
        }

        BlockingQueue<Event> queue = queues.get(event.getEventType());
        if (queue == null) {
            log.error("No queue found for event type: {}", event.getEventType());
            return false;
        }

        boolean added = queue.offer(event);
        if (added) {
            log.info("Event {} added to {} queue. Queue size: {}", 
                event.getEventId(), event.getEventType(), queue.size());
        } else {
            log.warn("Failed to add event {} to {} queue - queue is full", 
                event.getEventId(), event.getEventType());
        }
        
        return added;
    }

    /**
     * Worker method that processes events from a specific queue
     */
    private void processEvents(EventType eventType, BlockingQueue<Event> queue, int workerId) {
        String workerName = eventType + "-Worker-" + workerId;
        Thread.currentThread().setName(workerName);
        
        log.info("{} started", workerName);
        
        while (!shutdown.get() || !queue.isEmpty()) {
            try {
                // Wait for an event with timeout to allow shutdown checking
                Event event = queue.poll(1, TimeUnit.SECONDS);
                if (event != null) {
                    processEvent(event, workerName);
                }
            } catch (InterruptedException e) {
                log.info("{} was interrupted", workerName);
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected error in {}: {}", workerName, e.getMessage(), e);
            }
        }
        
        log.info("{} finished processing", workerName);
    }

    /**
     * Process a single event with appropriate delay and failure simulation
     */
    private void processEvent(Event event, String workerName) {
        log.info("{} processing event: {}", workerName, event.getEventId());
        
        try {
            event.markAsProcessing();
            
            // Simulate processing time based on event type
            int processingTime = getProcessingTime(event.getEventType());
            Thread.sleep(processingTime * 1000L);
            
            // Simulate random failures
            if (random.nextDouble() < failureRate) {
                String errorMessage = "Simulated processing failure";
                event.markAsFailed(errorMessage);
                log.warn("{} - Event {} failed: {}", workerName, event.getEventId(), errorMessage);
            } else {
                event.markAsCompleted();
                log.info("{} - Event {} completed successfully", workerName, event.getEventId());
            }
            
            // Send callback notification
            sendCallback(event);
            
        } catch (InterruptedException e) {
            log.warn("{} - Processing interrupted for event {}", workerName, event.getEventId());
            event.markAsFailed("Processing interrupted");
            sendCallback(event);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("{} - Error processing event {}: {}", workerName, event.getEventId(), e.getMessage(), e);
            event.markAsFailed("Processing error: " + e.getMessage());
            sendCallback(event);
        }
    }

    private int getProcessingTime(EventType eventType) {
        return switch (eventType) {
            case EMAIL -> emailProcessingTime;
            case SMS -> smsProcessingTime;
            case PUSH -> pushProcessingTime;
        };
    }

    private void sendCallback(Event event) {
        try {
            callbackService.sendCallback(event);
        } catch (Exception e) {
            log.error("Failed to send callback for event {}: {}", event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * Gracefully shutdown the service
     * Ensures ALL events in queues are processed before termination
     */
    @PreDestroy
    public void shutdown() {
        log.info("Starting graceful shutdown of Event Processing Service");
        
        // Stop accepting new events
        shutdown.set(true);
        log.info("Stopped accepting new events");
        
        // Log current queue sizes for visibility
        logQueueSizes();
        
        // Shutdown all executors
        for (EventType eventType : EventType.values()) {
            ExecutorService executor = executors.get(eventType);
            if (executor != null) {
                executor.shutdown();
                
                try {
                    log.info("Waiting for all {} events to complete processing...", eventType);
                    while (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                        BlockingQueue<Event> queue = queues.get(eventType);
                        int queueSize = queue != null ? queue.size() : 0;
                        log.info("Still processing {} events - {} events remaining in queue", 
                                eventType, queueSize);
                    }
                    
                    log.info("All {} events completed successfully", eventType);
                    
                } catch (InterruptedException e) {
                    log.warn("Shutdown interrupted while waiting for {} executor to terminate", eventType);
                    log.info("Attempting to continue processing remaining {} events despite interruption", eventType);
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // Final queue size check
        logQueueSizes();
        log.info("Event Processing Service shutdown completed - all events processed");
    }
    
    /**
     * Log current queue sizes for monitoring during shutdown
     */
    private void logQueueSizes() {
        for (EventType eventType : EventType.values()) {
            BlockingQueue<Event> queue = queues.get(eventType);
            if (queue != null) {
                log.info("{} queue size: {}", eventType, queue.size());
            }
        }
    }

}