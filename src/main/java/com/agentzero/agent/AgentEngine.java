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

import java.time.LocalDateTime;
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

    /**
     * Main ReAct loop — runs asynchronously
     * THINK → ACT → OBSERVE → THINK → ACT → OBSERVE → ... → DONE
     */
    @Async
    public void runPentest(String sessionId, String targetIp, int targetPort) {
        log.info("AgentZero starting pentest | session={} target={}:{}", sessionId, targetIp, targetPort);

        sessionManager.updateStatus(sessionId, "RUNNING");
        eventPublisher.publishSessionStart(sessionId, targetIp, targetPort);

        // Build system prompt with tool manifest
        String targetInfo = "IP: " + targetIp + "\nPort: " + targetPort;
        String systemPrompt = promptBuilder.build(
                toolRegistry.buildToolManifest(),
                targetInfo
        );

        List<ConversationTurn> history = new ArrayList<>();
        String currentUserMessage = promptBuilder.buildInitialMessage(targetIp, targetPort);

        int step = 0;

        try {
            while (step < maxSteps) {
                step++;
                log.info("ReAct step {}/{} | session={}", step, maxSteps, sessionId);

                // ── THINK ────────────────────────────────────────
                eventPublisher.publishThink(sessionId, step, "Reasoning about next action...");

                LLMResponse response = llmService.reason(systemPrompt, 
                    buildHistoryWithCurrent(history, currentUserMessage));

                // Save THINK step
                sessionManager.saveStep(sessionId, step, "THINK",
                        response.reasoning() + "\n" + response.thought(), null);

                eventPublisher.publishThink(sessionId, step, response.reasoning());

                // ── CHECK IF DONE ────────────────────────────────
                if (response.type() == ResponseType.DONE) {
                    log.info("Agent declared DONE at step {} | session={}", step, sessionId);
                    sessionManager.saveStep(sessionId, step, "THINK",
                            "ASSESSMENT COMPLETE: " + response.summary(), null);
                    eventPublisher.publishDone(sessionId, response.summary());
                    sessionManager.complete(sessionId, response.summary());
                    return;
                }

                // ── ACT ──────────────────────────────────────────
                if (response.type() == ResponseType.ACT && response.toolCall() != null) {
                    String toolName = response.toolCall().getToolName();
                    String params = response.toolCall().getParameters();

                    log.info("Agent calling tool: {} | session={}", toolName, sessionId);
                    eventPublisher.publishAct(sessionId, step, toolName, params);

                    sessionManager.saveStep(sessionId, step, "ACT",
                            "Calling tool: " + toolName + "\nParams: " + params, toolName);

                    // ── OBSERVE ───────────────────────────────────
                    ToolResult result = toolRegistry.execute(toolName, params);

                    log.info("Tool {} completed | success={} | session={}",
                            toolName, result.isSuccess(), sessionId);

                    sessionManager.saveStep(sessionId, step, "OBSERVE",
                            result.getOutput() != null ? result.getOutput() : result.getError(),
                            toolName);

                    eventPublisher.publishObserve(sessionId, step, toolName, result);

                    // Check for vulnerabilities in output and save them
                    sessionManager.extractAndSaveVulnerabilities(sessionId, toolName, result);

                    // Update conversation history
                    String assistantMsg = "I will use tool: " + toolName;
                    history.add(new ConversationTurn(currentUserMessage, assistantMsg));

                    // Next user message = observation
                    currentUserMessage = promptBuilder.buildObservationMessage(
                            step, toolName,
                            result.isSuccess() ? result.getOutput() : "ERROR: " + result.getError()
                    );

                } else {
                    // LLM returned THINK only — keep reasoning
                    history.add(new ConversationTurn(currentUserMessage, response.thought()));
                    currentUserMessage = "Continue your analysis. What tool should you use next? Respond with ACT.";
                }

                // Throttle to avoid API rate limiting
                Thread.sleep(stepDelayMs);
            }

            // Max steps reached
            log.warn("Max steps reached | session={}", sessionId);
            eventPublisher.publishDone(sessionId, "Maximum steps reached. Assessment terminated.");
            sessionManager.complete(sessionId, "Assessment terminated after " + maxSteps + " steps.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.info("Pentest interrupted | session={}", sessionId);
            sessionManager.updateStatus(sessionId, "STOPPED");
            eventPublisher.publishStopped(sessionId);
        } catch (Exception e) {
            log.error("Pentest failed | session={} | error={}", sessionId, e.getMessage(), e);
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
