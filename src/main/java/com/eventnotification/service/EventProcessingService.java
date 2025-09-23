package com.eventnotification.service;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Service;

import com.eventnotification.model.Event;
import com.eventnotification.model.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service class for processing event notifications
 */
@Service
public class EventProcessingService {

    private static final Logger log = LoggerFactory.getLogger(EventProcessingService.class);

    private final ConcurrentHashMap<EventType, BlockingQueue<Event>> queues = new ConcurrentHashMap<>();
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

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

}