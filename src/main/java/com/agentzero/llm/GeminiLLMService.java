package com.agentzero.llm;

import com.agentzero.model.ToolCall;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
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
            @Value("${agentzero.llm.gemini.temperature}") double temperature) {
        this.model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .maxOutputTokens(maxTokens)
                .temperature(temperature)
                .build();
        log.info("GeminiLLMService initialized: {}", modelName);
    }

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
        String raw = response.content().text();
        log.debug("Gemini response: {}", raw);
        return parseResponse(raw);
    }

    public String summarize(String prompt) {
        Response<AiMessage> response = model.generate(
                SystemMessage.from("You are a cybersecurity expert writing penetration test reports."),
                UserMessage.from(prompt));
        return response.content().text();
    }

    private LLMResponse parseResponse(String raw) {
        try {
            String cleaned = raw.strip()
                    .replaceAll("```json\\n?", "")
                    .replaceAll("```\\n?", "")
                    .strip();

            // If response contains multiple blocks, extract the last ACT or DONE block
            if (cleaned.contains("ACT:") || cleaned.contains("DONE:")) {
                int actIdx = cleaned.lastIndexOf("ACT:");
                int doneIdx = cleaned.lastIndexOf("DONE:");
                int idx = Math.max(actIdx, doneIdx);
                if (idx >= 0) {
                    cleaned = cleaned.substring(idx);
                    cleaned = cleaned.replaceFirst("^(ACT:|DONE:|THINK:)\\s*", "");
                }
            } else if (cleaned.contains("THINK:")) {
                cleaned = cleaned.substring(cleaned.lastIndexOf("THINK:"));
                cleaned = cleaned.replaceFirst("^THINK:\\s*", "");
            }

            // Extract first JSON object
            int start = cleaned.indexOf("{");
            int end = cleaned.lastIndexOf("}");
            if (start >= 0 && end > start) {
                cleaned = cleaned.substring(start, end + 1);
            }

            JsonNode node = objectMapper.readTree(cleaned);
            String action = node.path("action").asText("THINK");
            String reasoning = node.path("reasoning").asText("");
            String thought = node.path("thought").asText("");

            if ("ACT".equals(action)) {
                String toolName = node.path("tool").asText();
                String params = node.path("params").toString();
                return LLMResponse.act(reasoning, thought,
                        ToolCall.builder().toolName(toolName)
                                .parameters(params).reasoning(reasoning).build());
            } else if ("DONE".equals(action)) {
                return LLMResponse.done(reasoning, node.path("summary").asText(""));
            } else {
                return LLMResponse.think(reasoning, thought);
            }
        } catch (Exception e) {
            log.warn("Failed to parse LLM response as JSON: {}", e.getMessage());
            return LLMResponse.think("", raw);
        }
    }

    public record LLMResponse(ResponseType type, String reasoning, String thought,
                               ToolCall toolCall, String summary) {
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
