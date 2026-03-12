package com.agentzero.tools;

import com.agentzero.model.ToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ToolRegistry {

    private final Map<String, PentestTool> tools = new HashMap<>();

    public ToolRegistry(
            NmapScanner nmapScanner,
            HttpProber httpProber,
            SqlInjectionTester sqliTester,
            DirectoryFuzzer dirFuzzer,
            BannerGrabber bannerGrabber,
            BruteForcer bruteForcer
    ) {
        register(nmapScanner);
        register(httpProber);
        register(sqliTester);
        register(dirFuzzer);
        register(bannerGrabber);
        register(bruteForcer);
        log.info("ToolRegistry initialized with {} tools", tools.size());
    }

    private void register(PentestTool tool) {
        tools.put(tool.getName(), tool);
        log.debug("Registered tool: {}", tool.getName());
    }

    public ToolResult execute(String toolName, String paramsJson) {
        PentestTool tool = tools.get(toolName);
        if (tool == null) {
            return ToolResult.builder()
                    .toolName(toolName)
                    .success(false)
                    .error("Unknown tool: " + toolName + ". Available: " + getToolNames())
                    .build();
        }
        log.info("Executing tool: {} with params: {}", toolName, paramsJson);
        return tool.execute(paramsJson);
    }

    public List<String> getToolNames() {
        return List.copyOf(tools.keySet());
    }

    // Build tool manifest string for LLM system prompt
    public String buildToolManifest() {
        StringBuilder sb = new StringBuilder();
        sb.append("AVAILABLE TOOLS:\n\n");
        tools.values().forEach(tool -> {
            sb.append("Tool: ").append(tool.getName()).append("\n");
            sb.append("Description: ").append(tool.getDescription()).append("\n");
            sb.append("Parameters: ").append(tool.getParameterSchema()).append("\n\n");
        });
        return sb.toString();
    }
}
