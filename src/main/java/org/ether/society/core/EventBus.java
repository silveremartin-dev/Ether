/*
 * MIT License
 *
 * Copyright (c) 2024 Gemini AI Assistant
 * Copyright (c) 2024 Silvere Martin-Michiellot
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Author: Silvere Martin-Michiellot (silvere.martin@gmail.com)
 * Contributors: AI Assistant (Antigravity/Claude)
 */
package org.ether.society.core;


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple EventBus for decoupled communication between components.
 * Thread-safe implementation using CopyOnWriteArrayList.
 *
 * @author Silvere Martin-Michiellot
 * @version 1.0.0-beta.1-beta.1
 * @since 1.0.0
 */
public class EventBus {
    private final List<Subscriber<?>> subscribers = new CopyOnWriteArrayList<>();

    /**
     * Subscribes a listener to events of a specific type.
     *
     * @param eventType The class of events to listen for
     * @param listener  The consumer to handle events
     * @param <T>       The event type
     */
    public <T> void subscribe(Class<T> eventType, Consumer<T> listener) {
        subscribers.add(new Subscriber<>(eventType, listener));
    }

    /**
     * Publishes an event to all interested subscribers.
     *
     * @param event The event to publish
     */
    @SuppressWarnings("unchecked")
    public void publish(Object event) {
        for (Subscriber<?> subscriber : subscribers) {
            if (subscriber.eventType.isInstance(event)) {
                ((Subscriber<Object>) subscriber).listener.accept(event);
            }
        }
    }

    /**
     * Internal record to hold subscriber information.
     *
     * @param <T> The event type
     */
    private record Subscriber<T>(Class<T> eventType, Consumer<T> listener) {
    }
}


