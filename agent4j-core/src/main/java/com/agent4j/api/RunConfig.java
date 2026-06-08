/*
 * Copyright (c) 2025 agent4j
 * Licensed under the Apache License, Version 2.0
 * https://www.apache.org/licenses/LICENSE-2.0
 */
package com.agent4j.api;

import java.util.function.Consumer;

/**
 * Optional per-run configuration. Defaults preserve the existing runner behavior.
 */
public final class RunConfig {

    private final ModelSettings modelSettings;
    private final Class<?> outputType;
    private final RunHooks hooks;
    private final Consumer<RunEvent> eventConsumer;
    private final ToolExecutionConfig toolExecutionConfig;
    private final boolean streamModel;

    private RunConfig(Builder b) {
        this.modelSettings = b.modelSettings;
        this.outputType = b.outputType;
        this.hooks = b.hooks;
        this.eventConsumer = b.eventConsumer;
        this.toolExecutionConfig = b.toolExecutionConfig != null ? b.toolExecutionConfig : ToolExecutionConfig.defaults();
        this.streamModel = b.streamModel;
    }

    public ModelSettings getModelSettings() {
        return modelSettings;
    }

    public Class<?> getOutputType() {
        return outputType;
    }

    public RunHooks getHooks() {
        return hooks;
    }

    public Consumer<RunEvent> getEventConsumer() {
        return eventConsumer;
    }

    public ToolExecutionConfig getToolExecutionConfig() {
        return toolExecutionConfig;
    }

    public boolean isStreamModel() {
        return streamModel;
    }

    public static RunConfig defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private ModelSettings modelSettings;
        private Class<?> outputType;
        private RunHooks hooks;
        private Consumer<RunEvent> eventConsumer;
        private ToolExecutionConfig toolExecutionConfig;
        private boolean streamModel;

        public Builder modelSettings(ModelSettings modelSettings) {
            this.modelSettings = modelSettings;
            return this;
        }

        public Builder outputType(Class<?> outputType) {
            this.outputType = outputType;
            return this;
        }

        public Builder hooks(RunHooks hooks) {
            this.hooks = hooks;
            return this;
        }

        public Builder eventConsumer(Consumer<RunEvent> eventConsumer) {
            this.eventConsumer = eventConsumer;
            return this;
        }

        public Builder toolExecutionConfig(ToolExecutionConfig toolExecutionConfig) {
            this.toolExecutionConfig = toolExecutionConfig;
            return this;
        }

        public Builder streamModel(boolean streamModel) {
            this.streamModel = streamModel;
            return this;
        }

        public RunConfig build() {
            return new RunConfig(this);
        }
    }
}
