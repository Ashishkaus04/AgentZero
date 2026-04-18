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
        return "Scans a target IP for open ports and running services.";
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
            String target = params.getOrDefault("target", "127.0.0.1");
            String scanType = params.getOrDefault("scanType", "basic");
            String nmapFlags = switch (scanType) {
                case "full"    -> "-sV -p-";
                case "stealth" -> "-sS -T2";
                default        -> "--top-ports 100";
            };
            ProcessBuilder pb = new ProcessBuilder("nmap", nmapFlags, target);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) output.append(line).append("\n");
            }
            process.waitFor(30, TimeUnit.SECONDS);
            return ToolResult.builder().toolName(getName()).success(true)
                    .output(output.toString())
                    .executionTimeMs(System.currentTimeMillis() - start).build();
        } catch (Exception e) {
            log.warn("Nmap not found, using Java socket fallback");
            return socketScan(paramsJson, start);
        }
    }

    private ToolResult socketScan(String paramsJson, long start) {
        try {
            Map<String, String> params = mapper.readValue(paramsJson, Map.class);
            String target = params.getOrDefault("target", "127.0.0.1");
            int[] ports = {21,22,23,25,53,80,110,143,443,445,3306,3389,5432,8080,8443};
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
            for (Future<String> f : futures) { String r = f.get(); if (r != null) open.add(r); }
            exec.shutdown();
            return ToolResult.builder().toolName(getName()).success(true)
                    .output(open.isEmpty() ? "No open ports found." : "Open ports:\n" + String.join("\n", open))
                    .executionTimeMs(System.currentTimeMillis() - start).build();
        } catch (Exception ex) {
            return ToolResult.builder().toolName(getName()).success(false)
                    .error("Scan failed: " + ex.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - start).build();
        }
    }
}
