package com.agentzero.tools;

import com.agentzero.model.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class DirectoryFuzzer implements PentestTool {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final List<String> PATHS = List.of(
            "/admin", "/login", "/dashboard", "/api", "/backup",
            "/config", "/debug", "/env", "/health", "/manager",
            "/phpmyadmin", "/robots.txt", "/swagger-ui.html",
            "/actuator", "/.env", "/.git", "/wp-admin",
            "/upload", "/uploads", "/files", "/secret", "/console"
    );

    @Override
    public String getName() { return "dir_fuzz"; }

    @Override
    public String getDescription() {
        return "Discovers hidden directories and files on a web server.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"baseUrl\": \"Base URL e.g. http://127.0.0.1:80\"}";
    }

    @Override
    public ToolResult execute(String paramsJson) {
        long start = System.currentTimeMillis();
        StringBuilder results = new StringBuilder("Directory Fuzzing\n=================\n\n");
        try {
            Map<String, String> params = mapper.readValue(paramsJson, Map.class);
            String baseUrl = params.get("baseUrl").replaceAll("/$", "");
            List<String> found = new ArrayList<>();
            ExecutorService exec = Executors.newFixedThreadPool(10);
            List<Future<String>> futures = new ArrayList<>();
            for (String path : PATHS) {
                String finalUrl = baseUrl + path;
                futures.add(exec.submit(() -> {
                    try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(finalUrl).openConnection();
                        conn.setConnectTimeout(3000);
                        conn.setReadTimeout(3000);
                        conn.setRequestProperty("User-Agent", "AgentZero/1.0");
                        int code = conn.getResponseCode();
                        if (code != 404 && code != 0) return "[" + code + "] " + finalUrl;
                    } catch (Exception ignored) {}
                    return null;
                }));
            }
            for (Future<String> f : futures) { String r = f.get(); if (r != null) found.add(r); }
            exec.shutdown();
            if (found.isEmpty()) results.append("No accessible paths found.\n");
            else { results.append("Found ").append(found.size()).append(" paths:\n\n"); found.forEach(f -> results.append(f).append("\n")); }
        } catch (Exception e) { results.append("Error: ").append(e.getMessage()); }
        return ToolResult.builder().toolName(getName()).success(true)
                .output(results.toString())
                .executionTimeMs(System.currentTimeMillis() - start).build();
    }
}
