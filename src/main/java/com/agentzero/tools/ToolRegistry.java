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

    public ToolRegistry(NmapScanner nmap, HttpProber http, SqlInjectionTester sqli,
                        DirectoryFuzzer fuzzer, BannerGrabber banner, BruteForcer brute) {
        register(nmap); register(http); register(sqli);
        register(fuzzer); register(banner); register(brute);
        log.info("ToolRegistry initialized with {} tools", tools.size());
    }

    private void register(PentestTool tool) {
        tools.put(tool.getName(), tool);
    }

    public ToolResult execute(String toolName, String paramsJson) {
        PentestTool tool = tools.get(toolName);
        if (tool == null) {
            return ToolResult.builder().toolName(toolName).success(false)
                    .error("Unknown tool: " + toolName + ". Available: " + getToolNames()).build();
        }
        log.info("Executing tool: {}", toolName);
        return tool.execute(paramsJson);
    }

    public List<String> getToolNames() {
        return List.copyOf(tools.keySet());
    }

    public String buildToolManifest() {
        StringBuilder sb = new StringBuilder("AVAILABLE TOOLS:\n\n");
        tools.values().forEach(t -> sb.append("Tool: ").append(t.getName()).append("\n")
                .append("Description: ").append(t.getDescription()).append("\n")
                .append("Parameters: ").append(t.getParameterSchema()).append("\n\n"));
        return sb.toString();
    }
}
