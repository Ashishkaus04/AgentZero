package com.agentzero.tools;

import com.agentzero.model.ToolResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
@Component
public class NmapScanner implements PentestTool {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String getName() { return "nmap_scan"; }

    @Override
    public String getDescription() {
        return "Scans a target IP for open ports and running services. Returns list of open ports, service names and versions.";
    }

    @Override
    public String getParameterSchema() {
        return "{\"target\": \"IP address\", \"scanType\": \"basic | full | stealth\"}";
    }

    @Override
    public ToolResult execute(String paramsJson) {
        long start = System.currentTimeMillis();
        try {
            Map<String, String> params = mapper.readValue(paramsJson, Map.class);
            String target   = params.getOrDefault("target", "127.0.0.1");
            String scanType = params.getOrDefault("scanType", "basic");

            // Build nmap command — each argument as a SEPARATE string (critical fix)
            List<String> cmd = new ArrayList<>();
            cmd.add("nmap");
            switch (scanType) {
                case "full"    -> { cmd.add("-sV"); cmd.add("-p-"); }
                case "stealth" -> { cmd.add("-sS"); cmd.add("-T2"); }
                default        -> { cmd.add("-sV"); cmd.add("--top-ports"); cmd.add("100"); }
            }
            cmd.add(target);

            log.info("Running nmap: {}", String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            boolean finished = process.waitFor(60, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.warn("Nmap timed out, falling back to socket scan");
                return socketScan(target, start);
            }

            String result = output.toString();

            // Detect bad output — nmap printed help/usage instead of scan results
            if (result.contains("Usage:") || result.contains("QUITTING") || result.trim().isEmpty()) {
                log.warn("Nmap returned unexpected output, falling back to socket scan");
                return socketScan(target, start);
            }

            return ToolResult.builder()
                    .toolName(getName()).success(true)
                    .output(result)
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception e) {
            log.warn("Nmap failed ({}), falling back to socket scan", e.getMessage());
            try {
                Map<String, String> params = mapper.readValue(paramsJson, Map.class);
                return socketScan(params.getOrDefault("target", "127.0.0.1"), start);
            } catch (Exception ex) {
                return ToolResult.builder()
                        .toolName(getName()).success(false)
                        .error("Scan failed: " + ex.getMessage())
                        .executionTimeMs(System.currentTimeMillis() - start)
                        .build();
            }
        }
    }

    private ToolResult socketScan(String target, long start) {
        log.info("Running Java socket port scan on {}", target);
        try {
            int[] ports = {21, 22, 23, 25, 53, 80, 110, 143, 443, 445,
                    3306, 3389, 5432, 5900, 6379, 8080, 8443, 9200};
            List<String> open = new ArrayList<>();
            ExecutorService exec = Executors.newFixedThreadPool(10);
            List<Future<String>> futures = new ArrayList<>();

            for (int port : ports) {
                futures.add(exec.submit(() -> {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress(target, port), 1000);
                        return port + "/tcp OPEN";
                    } catch (Exception ex) { return null; }
                }));
            }

            for (Future<String> f : futures) {
                try { String r = f.get(); if (r != null) open.add(r); }
                catch (Exception ignored) {}
            }
            exec.shutdown();

            String output = open.isEmpty()
                    ? "No open ports found on common ports."
                    : "Open ports on " + target + ":\n" + String.join("\n", open);

            return ToolResult.builder().toolName(getName()).success(true)
                    .output(output)
                    .executionTimeMs(System.currentTimeMillis() - start).build();

        } catch (Exception ex) {
            return ToolResult.builder().toolName(getName()).success(false)
                    .error("Socket scan failed: " + ex.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - start).build();
        }
    }
}