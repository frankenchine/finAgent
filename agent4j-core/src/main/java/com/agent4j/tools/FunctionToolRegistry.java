/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.tools;

import com.agent4j.api.Tool;

import java.util.Collections;
import java.util.Map;

/**
 * Factory for creating simple function-style tools (name, description, single string argument or none).
 */
public final class FunctionToolRegistry {

    private FunctionToolRegistry() {
    }

    /**
     * Create a tool with no parameters.
     */
    public static Tool noArgTool(String name, String description, NoArgFunction fn) {
        return new FunctionTool(name, description, Collections.emptyMap(), args -> fn.run());
    }

    /**
     * Create a tool that accepts a single string argument (e.g. "city" for get_weather).
     */
    public static Tool stringArgTool(String name, String description, String paramName, StringParamFunction fn) {
        Map<String, Object> schema = Map.of(
                "type", "object",
                "properties", Map.of(paramName, Map.of("type", "string", "description", paramName)),
                "required", java.util.List.of(paramName)
        );
        return new FunctionTool(name, description, schema, args -> {
            Object v = args.get(paramName);
            return fn.run(v != null ? v.toString() : "");
        });
    }

    @FunctionalInterface
    public interface NoArgFunction {
        Object run();
    }

    @FunctionalInterface
    public interface StringParamFunction {
        Object run(String value);
    }

    private static final class FunctionTool implements Tool {
        private final String name;
        private final String description;
        private final Map<String, Object> parameterSchema;
        private final java.util.function.Function<Map<String, Object>, Object> fn;

        FunctionTool(String name, String description, Map<String, Object> parameterSchema,
                     java.util.function.Function<Map<String, Object>, Object> fn) {
            this.name = name;
            this.description = description;
            this.parameterSchema = parameterSchema;
            this.fn = fn;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return description;
        }

        @Override
        public Map<String, Object> getParameterSchema() {
            return parameterSchema;
        }

        @Override
        public Object invoke(ToolContext context) {
            Map<String, Object> args = parseArgs(context.getArgumentsJson());
            return fn.apply(args);
        }

        private static Map<String, Object> parseArgs(String json) {
            if (json == null || json.isBlank()) {
                return Collections.emptyMap();
            }
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> map = mapper.readValue(json, Map.class);
                return map != null ? map : Collections.emptyMap();
            } catch (Exception e) {
                return Collections.emptyMap();
            }
        }
    }
}

