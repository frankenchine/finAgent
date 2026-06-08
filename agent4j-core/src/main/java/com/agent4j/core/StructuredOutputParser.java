/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.core;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses final model output into the requested Java type when possible.
 */
final class StructuredOutputParser {

    private final ObjectMapper objectMapper = new ObjectMapper();

    Object parse(Object output, Class<?> outputType) {
        if (outputType == null || outputType == String.class || output == null || outputType.isInstance(output)) {
            return output;
        }
        if (!(output instanceof String)) {
            return objectMapper.convertValue(output, outputType);
        }
        try {
            return objectMapper.readValue((String) output, outputType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse final output as " + outputType.getName(), e);
        }
    }
}
