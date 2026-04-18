package com.agentzero.tools;

import com.agentzero.model.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

@Slf4j
@Component
public class HttpProber implements PentestTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() { return "http_probe"; }

    @Override
    public String getDescription() {
        return "Sends HTTP requests to a target URL and returns status, headers, and body.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"url\": \"Full URL\", \"method\": \"GET | POST\", \"body\": \"POST body (optional)\"}";
    }

    @Override
    public ToolResult execute(String paramsJson) {
        long start = System.currentTimeMillis();
        try {
            Map<String, String> params = mapper.readValue(paramsJson, Map.class);
            String urlStr = params.get("url");
            String method = params.getOrDefault("method", "GET");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("User-Agent", "AgentZero/1.0");
            if ("POST".equals(method) && params.containsKey("body")) {
                conn.setDoOutput(true);
                conn.getOutputStream().write(params.get("body").getBytes());
            }
            int statusCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();
            response.append("Status: ").append(statusCode).append("\n");
            conn.getHeaderFields().forEach((k, v) -> {
                if (k != null) response.append(k).append(": ").append(v).append("\n");
            });
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                response.append("\nBody:\n");
                char[] buf = new char[500];
                int read = reader.read(buf);
                if (read > 0) response.append(new String(buf, 0, read));
            } catch (Exception ignored) {}
            return ToolResult.builder().toolName(getName()).success(true)
                    .output(response.toString())
                    .executionTimeMs(System.currentTimeMillis() - start).build();
        } catch (Exception e) {
            return ToolResult.builder().toolName(getName()).success(false)
                    .error("HTTP probe failed: " + e.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - start).build();
        }
    }
}
