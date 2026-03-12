package com.agentzero.llm;

import com.agentzero.model.ToolCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GeminiLLMService {

    private final GoogleAiGeminiChatModel model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiLLMService(
            @Value("${agentzero.llm.gemini.api-key}") String apiKey,
            @Value("${agentzero.llm.gemini.model}") String modelName,
            @Value("${agentzero.llm.gemini.max-tokens}") int maxTokens,
            @Value("${agentzero.llm.gemini.temperature}") double temperature
    ) {
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxOutputTokens(maxTokens)
                .temperature(temperature)
                .build();

        log.info("GeminiLLMService initialized with model: {}", modelName);
    }

    /**
     * Core ReAct reasoning call.
     * Sends the full attack history and expects the LLM to decide:
     *   - THINK: reasoning about what to do next
     *   - ACT: which tool to call and with what params
     *   - DONE: when the pentest is complete
     */
    public LLMResponse reason(String systemPrompt, List<ConversationTurn> history) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));

        for (ConversationTurn turn : history) {
            messages.add(UserMessage.from(turn.userMessage()));
            if (turn.assistantMessage() != null) {
                messages.add(AiMessage.from(turn.assistantMessage()));
            }
        }

        log.debug("Sending {} messages to Gemini", messages.size());
        Response<AiMessage> response = model.generate(messages);
        String rawResponse = response.content().text();
        log.debug("Gemini raw response: {}", rawResponse);

        return parseResponse(rawResponse);
    }

    /**
     * Simple single-turn call for report summarization
     */
    public String summarize(String prompt) {
        Response<AiMessage> response = model.generate(
            SystemMessage.from("You are a cybersecurity expert writing penetration test reports."),
            UserMessage.from(prompt)
        );
        return response.content().text();
    }

    /**
     * Parse LLM response into structured LLMResponse
     * LLM is prompted to respond in JSON format
     */
    private LLMResponse parseResponse(String raw) {
        try {
            // Strip markdown code blocks if present
            String cleaned = raw.strip();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json\\n?", "").replaceAll("```\\n?", "").strip();
            }

            JsonNode node = objectMapper.readTree(cleaned);
            String action = node.path("action").asText("THINK");
            String reasoning = node.path("reasoning").asText("");
            String thought = node.path("thought").asText("");

            if ("ACT".equals(action)) {
                String toolName = node.path("tool").asText();
                String params = node.path("params").toString();
                return LLMResponse.act(reasoning, thought,
                        ToolCall.builder()
                                .toolName(toolName)
                                .parameters(params)
                                .reasoning(reasoning)
                                .build());
            } else if ("DONE".equals(action)) {
                String summary = node.path("summary").asText("");
                return LLMResponse.done(reasoning, summary);
            } else {
                return LLMResponse.think(reasoning, thought);
            }

        } catch (Exception e) {
            log.warn("Failed to parse LLM JSON response, treating as THINK: {}", e.getMessage());
            return LLMResponse.think("", raw);
        }
    }

    // ─── Response Types ───────────────────────────────────────────

    public record LLMResponse(
            ResponseType type,
            String reasoning,
            String thought,
            ToolCall toolCall,
            String summary
    ) {
        static LLMResponse think(String reasoning, String thought) {
            return new LLMResponse(ResponseType.THINK, reasoning, thought, null, null);
        }

        static LLMResponse act(String reasoning, String thought, ToolCall toolCall) {
            return new LLMResponse(ResponseType.ACT, reasoning, thought, toolCall, null);
        }

        static LLMResponse done(String reasoning, String summary) {
            return new LLMResponse(ResponseType.DONE, reasoning, null, null, summary);
        }
    }

    public enum ResponseType { THINK, ACT, DONE }

    public record ConversationTurn(String userMessage, String assistantMessage) {}
}
