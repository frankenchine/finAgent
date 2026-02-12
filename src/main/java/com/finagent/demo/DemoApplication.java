package com.finagent.demo;

import com.finagent.api.Agent;
import com.finagent.api.AgentRunner;
import com.finagent.api.Handoff;
import com.finagent.api.RunRequest;
import com.finagent.api.RunResult;
import com.finagent.core.AgentDefinition;
import com.finagent.memory.InMemorySession;
import com.finagent.tools.FunctionToolRegistry;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    @Bean
    public ApplicationRunner helloWorldExample(AgentRunner runner) {
        return args -> {
            Agent agent = new AgentDefinition()
                    .setName("Assistant")
                    .setInstructions("You are a helpful assistant. Reply concisely.")
                    .addTool(FunctionToolRegistry.stringArgTool(
                            "get_weather",
                            "Get the weather for a city",
                            "city",
                            city -> "The weather in " + city + " is sunny."
                    ))
                    .build();

            RunRequest request = RunRequest.builder()
                    .input("What's the weather in Tokyo?")
                    .maxTurns(10)
                    .build();

            RunResult result = runner.run(agent, request);
            System.out.println("Final output: " + result.getFinalOutput());
        };
    }

    @Bean
    public ApplicationRunner sessionExample(AgentRunner runner) {
        return args -> {
            Agent agent = new AgentDefinition()
                    .setName("Assistant")
                    .setInstructions("Reply very concisely.")
                    .build();

            InMemorySession session = new InMemorySession("conversation_123");

            RunResult r1 = runner.run(agent, RunRequest.builder()
                    .input("What city is the Golden Gate Bridge in?")
                    .session(session)
                    .build());
            System.out.println("Turn 1: " + r1.getFinalOutput());

            RunResult r2 = runner.run(agent, RunRequest.builder()
                    .input("What state is it in?")
                    .session(session)
                    .build());
            System.out.println("Turn 2: " + r2.getFinalOutput());
        };
    }

    @Bean
    public ApplicationRunner handoffExample(AgentRunner runner) {
        return args -> {
            Agent spanishAgent = new AgentDefinition()
                    .setName("Spanish agent")
                    .setInstructions("You only speak Spanish.")
                    .build();
            Agent englishAgent = new AgentDefinition()
                    .setName("English agent")
                    .setInstructions("You only speak English.")
                    .build();
            Handoff toSpanish = new Handoff() {
                @Override
                public String getToolName() {
                    return "transfer_to_spanish_agent";
                }
                @Override
                public String getToolDescription() {
                    return "Hand off to the Spanish-speaking agent.";
                }
                @Override
                public Agent getTargetAgent() {
                    return spanishAgent;
                }
            };
            Handoff toEnglish = new Handoff() {
                @Override
                public String getToolName() {
                    return "transfer_to_english_agent";
                }
                @Override
                public String getToolDescription() {
                    return "Hand off to the English-speaking agent.";
                }
                @Override
                public Agent getTargetAgent() {
                    return englishAgent;
                }
            };
            Agent triageAgent = new AgentDefinition()
                    .setName("Triage agent")
                    .setInstructions("Hand off to the appropriate agent based on the language of the request.")
                    .addHandoff(toSpanish)
                    .addHandoff(toEnglish)
                    .build();

            RunResult result = runner.run(triageAgent, RunRequest.builder()
                    .input("Hola, ¿cómo estás?")
                    .maxTurns(10)
                    .build());
            System.out.println("Handoff example output: " + result.getFinalOutput());
        };
    }
}
