package com.agentzero.tools;

import com.agentzero.model.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BruteForcer implements PentestTool {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final List<String[]> CREDS = List.of(
            new String[]{"admin","admin"}, new String[]{"admin","password"},
            new String[]{"admin","123456"}, new String[]{"root","root"},
            new String[]{"root","toor"}, new String[]{"user","user"},
            new String[]{"test","test"}, new String[]{"guest","guest"},
            new String[]{"admin","admin123"}, new String[]{"admin",""}
    );

    @Override
    public String getName() { return "brute_force"; }

    @Override
    public String getDescription() {
        return "Attempts common username/password combinations against a login endpoint.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"loginUrl\": \"Login URL\", \"usernameField\": \"field name\", " +
               "\"passwordField\": \"field name\", \"successIndicator\": \"text on success\"}";
    }

    @Override
    public ToolResult execute(String paramsJson) {
        long start = System.currentTimeMillis();
        StringBuilder results = new StringBuilder("Brute Force Results\n===================\n\n");
        try {
            Map<String, String> params = mapper.readValue(paramsJson, Map.class);
            String loginUrl = params.get("loginUrl");
            String userField = params.getOrDefault("usernameField", "username");
            String passField = params.getOrDefault("passwordField", "password");
            String success = params.getOrDefault("successIndicator", "dashboard");
            List<String> found = new ArrayList<>();
            for (String[] cred : CREDS) {
                try {
                    String body = userField + "=" + cred[0] + "&" + passField + "=" + cred[1];
                    HttpURLConnection conn = (HttpURLConnection) new URL(loginUrl).openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(3000);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.getOutputStream().write(body.getBytes());
                    StringBuilder resp = new StringBuilder();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = r.readLine()) != null) resp.append(line);
                    } catch (Exception ignored) {}
                    if (resp.toString().toLowerCase().contains(success.toLowerCase()) || conn.getResponseCode() == 302) {
                        found.add("VALID: " + cred[0] + " / " + cred[1]);
                    }
                } catch (Exception ignored) {}
            }
            if (found.isEmpty()) results.append("No valid credentials found. Tested ").append(CREDS.size()).append(" pairs.\n");
            else { results.append("CREDENTIALS FOUND:\n"); found.forEach(f -> results.append(f).append("\n")); }
        } catch (Exception e) { results.append("Error: ").append(e.getMessage()); }
        return ToolResult.builder().toolName(getName()).success(true)
                .output(results.toString())
                .executionTimeMs(System.currentTimeMillis() - start).build();
    }
}
