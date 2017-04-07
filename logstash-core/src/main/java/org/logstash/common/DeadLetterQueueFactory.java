/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.logstash.common;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.logstash.common.io.DeadLetterQueueWriteManager;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * This class manages static collection of {@link DeadLetterQueueWriteManager} instances that
 * can be created, retrieved, or closed by a specific String-valued key.
 */
public class DeadLetterQueueFactory {

    private static final Logger logger = LogManager.getLogger(DeadLetterQueueFactory.class);
    private static final ConcurrentHashMap<String, DeadLetterQueueWriteManager> REGISTRY = new ConcurrentHashMap<>();
    private static final long MAX_SEGMENT_SIZE_BYTES = 10 * 1024 * 1024;

    /**
     * This class is only meant to be used statically, and therefore
     * the constructor is private.
     */
    private DeadLetterQueueFactory() {
    }

    /**
     * Retrieves an existing {@link DeadLetterQueueWriteManager} associated with {@param id}, or
     * opens a new one to be returned.
     *
     * @param id The identifier context for this dlq manager
     * @param dlqPath The path to use for the queue's backing data directory. contains sub-directories
     *                for each {@param id}
     * @return The write manager for the specific id's dead-letter-queue context
     */
    public static DeadLetterQueueWriteManager getWriter(String id, String dlqPath) {
        return REGISTRY.computeIfAbsent(id, k -> {
            try {
                return new DeadLetterQueueWriteManager(Paths.get(dlqPath, k), MAX_SEGMENT_SIZE_BYTES, Long.MAX_VALUE);
            } catch (IOException e) {
                logger.error("unable to create dead letter queue writer", e);
            }
            return null;
        });
    }

    /**
     * Removes the {@link DeadLetterQueueWriteManager} associated with {@param id} and
     * calls {@link DeadLetterQueueWriteManager#close()} on it.
     *
     * @param id The specific key to fetch the dlq manager from.
     * @throws IOException If the queue manager is unable to safely close
     */
    public static void close(String id) throws IOException {
        DeadLetterQueueWriteManager manager = REGISTRY.remove(id);
        manager.close();
    }
}