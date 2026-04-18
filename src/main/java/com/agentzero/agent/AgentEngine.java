package com.agentzero.agent;

import com.agentzero.llm.GeminiLLMService;
import com.agentzero.llm.GeminiLLMService.ConversationTurn;
import com.agentzero.llm.GeminiLLMService.LLMResponse;
import com.agentzero.llm.GeminiLLMService.ResponseType;
import com.agentzero.llm.SystemPromptBuilder;
import com.agentzero.model.ToolResult;
import com.agentzero.tools.ToolRegistry;
import com.agentzero.websocket.AgentEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentEngine {

    private final GeminiLLMService llmService;
    private final ToolRegistry toolRegistry;
    private final SystemPromptBuilder promptBuilder;
    private final AgentSessionManager sessionManager;
    private final AgentEventPublisher eventPublisher;

    @Value("${agentzero.agent.max-steps}")
    private int maxSteps;

    @Value("${agentzero.agent.step-delay-ms}")
    private long stepDelayMs;

    // Add this helper method to AgentEngine.java
    private LLMResponse reasonWithRetry(String systemPrompt, List<ConversationTurn> history) {
        int maxRetries = 3;
        for (int i = 0; i < maxRetries; i++) {
            try {
                return llmService.reason(systemPrompt, history);
            } catch (RuntimeException e) {
                if (e.getMessage().contains("429") && i < maxRetries - 1) {
                    log.warn("Rate limited, waiting 60s before retry {}/{}", i + 1, maxRetries);
                    try { Thread.sleep(60000); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new RuntimeException("Max retries exceeded");
    }

    @Async
    public void runPentest(String sessionId, String targetIp, int targetPort) {
        log.info("AgentZero starting | session={} target={}:{}", sessionId, targetIp, targetPort);
        sessionManager.updateStatus(sessionId, "RUNNING");
        eventPublisher.publishSessionStart(sessionId, targetIp, targetPort);

        String systemPrompt = promptBuilder.build(
                toolRegistry.buildToolManifest(),
                "IP: " + targetIp + "\nPort: " + targetPort);

        List<ConversationTurn> history = new ArrayList<>();
        String currentUserMessage = promptBuilder.buildInitialMessage(targetIp, targetPort);
        int step = 0;

        try {
            while (step < maxSteps) {
                step++;

                // Check if stopped by user
                if (sessionManager.isStopped(sessionId)) {
                    eventPublisher.publishStopped(sessionId);
                    return;
                }

                log.info("ReAct step {}/{} | session={}", step, maxSteps, sessionId);

                // ── THINK ──────────────────────────────────────────
                LLMResponse response = reasonWithRetry(systemPrompt,
                        buildHistoryWithCurrent(history, currentUserMessage));

                sessionManager.saveStep(sessionId, step, "THINK",
                        response.reasoning() + "\n" + response.thought(), null);
                eventPublisher.publishThink(sessionId, step, response.reasoning());

                // ── DONE ───────────────────────────────────────────
                if (response.type() == ResponseType.DONE) {
                    log.info("Agent DONE at step {} | session={}", step, sessionId);
                    eventPublisher.publishDone(sessionId, response.summary());
                    sessionManager.complete(sessionId, response.summary());
                    return;
                }

                // ── ACT ────────────────────────────────────────────
                if (response.type() == ResponseType.ACT && response.toolCall() != null) {
                    String toolName = response.toolCall().getToolName();
                    String params = response.toolCall().getParameters();

                    log.info("Calling tool: {} | session={}", toolName, sessionId);
                    eventPublisher.publishAct(sessionId, step, toolName, params);
                    sessionManager.saveStep(sessionId, step, "ACT",
                            "Tool: " + toolName + " | Params: " + params, toolName);

                    // ── OBSERVE ────────────────────────────────────
                    ToolResult result = toolRegistry.execute(toolName, params);
                    sessionManager.saveStep(sessionId, step, "OBSERVE",
                            result.isSuccess() ? result.getOutput() : result.getError(), toolName);
                    eventPublisher.publishObserve(sessionId, step, toolName, result);
                    sessionManager.extractAndSaveVulnerabilities(sessionId, toolName, result);

                    // Update conversation
                    history.add(new ConversationTurn(currentUserMessage, "Using tool: " + toolName));
                    String output = result.isSuccess() ? result.getOutput() : "ERROR: " + result.getError();
                    if (output != null && output.length() > 1000) output = output.substring(0, 1000) + "\n...[truncated]";
                    currentUserMessage = promptBuilder.buildObservationMessage(step, toolName, output);

                } else {
                    // Pure THINK — push agent to act
                    history.add(new ConversationTurn(currentUserMessage, response.thought()));
                    currentUserMessage = "Continue. Which tool should you use next? Respond with ACT in JSON.";
                }

                Thread.sleep(stepDelayMs);
            }

            // Max steps reached
            log.warn("Max steps reached | session={}", sessionId);
            eventPublisher.publishDone(sessionId, "Assessment terminated after " + maxSteps + " steps.");
            sessionManager.complete(sessionId, "Max steps reached.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sessionManager.updateStatus(sessionId, "STOPPED");
            eventPublisher.publishStopped(sessionId);
        } catch (Exception e) {
            log.error("Pentest failed | session={} | {}", sessionId, e.getMessage(), e);
            sessionManager.updateStatus(sessionId, "FAILED");
            eventPublisher.publishError(sessionId, e.getMessage());
        }
    }

    private List<ConversationTurn> buildHistoryWithCurrent(
            List<ConversationTurn> history, String currentMessage) {
        List<ConversationTurn> all = new ArrayList<>(history);
        all.add(new ConversationTurn(currentMessage, null));
        return all;
    }
}
