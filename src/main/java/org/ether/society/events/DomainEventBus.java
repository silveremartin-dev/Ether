/*
 * MIT License
 * Copyright (c) 2024-2026 Silvere Martin-Michiellot
 */
package org.ether.society.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * High-Performance Deterministic Synchronous Domain Event Bus.
 * Supports zero-allocation event dispatching, strict handler prioritization,
 * and deterministic phase flushing.
 *
 * @author Silvere Martin-Michiellot
 * @version 4.0.0
 */
public class DomainEventBus {
    private static final Logger logger = LoggerFactory.getLogger(DomainEventBus.class);
    private static final DomainEventBus INSTANCE = new DomainEventBus();

    public static DomainEventBus getInstance() {
        return INSTANCE;
    }

    public enum Priority {
        FIRST(0),
        HIGH(100),
        NORMAL(500),
        LOW(900),
        LAST(1000);

        private final int order;
        Priority(int order) { this.order = order; }
        public int getOrder() { return order; }
    }

    public record PrioritizedHandler<T>(Consumer<T> handler, Priority priority, int registrationIndex) {}

    private final Map<Class<?>, List<PrioritizedHandler<?>>> subscriberMap = new ConcurrentHashMap<>();
    private final List<Object> pendingEvents = new ArrayList<>();
    private int registrationSeq = 0;

    /**
     * Subscribes a typed consumer to a specific domain event class.
     */
    public synchronized <T> void subscribe(Class<T> eventType, Priority priority, Consumer<T> handler) {
        if (eventType == null || handler == null) return;
        Priority prio = priority != null ? priority : Priority.NORMAL;
        subscriberMap.computeIfAbsent(eventType, k -> new ArrayList<>())
                     .add(new PrioritizedHandler<>(handler, prio, ++registrationSeq));
        
        // Keep list sorted by priority order then registration sequence for strict determinism
        subscriberMap.get(eventType).sort((a, b) -> {
            int cmp = Integer.compare(a.priority().getOrder(), b.priority().getOrder());
            if (cmp != 0) return cmp;
            return Integer.compare(a.registrationIndex(), b.registrationIndex());
        });
    }

    public <T> void subscribe(Class<T> eventType, Consumer<T> handler) {
        subscribe(eventType, Priority.NORMAL, handler);
    }

    /**
     * Publishes an event immediately in a synchronous, deterministic order.
     */
    @SuppressWarnings("unchecked")
    public synchronized <T> void publishNow(T event) {
        if (event == null) return;
        List<PrioritizedHandler<?>> handlers = subscriberMap.get(event.getClass());
        if (handlers != null) {
            for (PrioritizedHandler<?> ph : handlers) {
                try {
                    ((Consumer<T>) ph.handler()).accept(event);
                } catch (Exception ex) {
                    logger.error("Error executing handler for event '{}': {}", event.getClass().getSimpleName(), ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     * Enqueues an event for deferred deterministic phase flush.
     */
    public synchronized void enqueue(Object event) {
        if (event != null) {
            pendingEvents.add(event);
        }
    }

    /**
     * Flushes and executes all pending events in deterministic FIFO order.
     */
    public synchronized void flushPendingEvents() {
        if (pendingEvents.isEmpty()) return;
        List<Object> currentBatch = new ArrayList<>(pendingEvents);
        pendingEvents.clear();
        for (Object event : currentBatch) {
            publishNow(event);
        }
    }

    public synchronized void clear() {
        subscriberMap.clear();
        pendingEvents.clear();
        registrationSeq = 0;
    }
}