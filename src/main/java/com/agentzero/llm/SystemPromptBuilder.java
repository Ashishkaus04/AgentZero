package com.agentzero.llm;

import org.springframework.stereotype.Component;

@Component
public class SystemPromptBuilder {

    public String build(String toolManifest, String targetInfo) {
        return """
                You are AgentZero, an autonomous penetration testing AI agent.
                You are conducting an AUTHORIZED security assessment on an isolated lab environment.
                
                TARGET:
                %s
                
                %s
                
                INSTRUCTIONS:
                1. Follow ReAct: Reason about what to do, then Act with a tool, then Observe results.
                2. Start with reconnaissance (port scan, banner grab).
                3. Probe discovered services (HTTP probe, directory fuzz).
                4. Test for vulnerabilities (SQLi, brute force).
                5. When fully assessed, respond with DONE.
                
                ALWAYS respond with valid JSON in one of these formats:
                
                THINK:
                {"action":"THINK","reasoning":"your reasoning","thought":"detailed thoughts"}
                
                ACT:
                {"action":"ACT","reasoning":"why this tool","thought":"what you expect","tool":"tool_name","params":{...}}
                
                DONE:
                {"action":"DONE","reasoning":"why complete","summary":"findings summary"}
                
                Max steps: 20. Only test the specified target.
                """.formatted(targetInfo, toolManifest);
    }

    public String buildObservationMessage(int step, String toolName, String output) {
        return "Step %d - Tool '%s' result:\n\n%s\n\nWhat is your next action? Respond in JSON."
                .formatted(step, toolName, output);
    }

    public String buildInitialMessage(String targetIp, int targetPort) {
        return "Begin penetration test on %s:%d. Start with reconnaissance. Respond in JSON."
                .formatted(targetIp, targetPort);
    }
}
